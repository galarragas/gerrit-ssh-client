package org.vaadin.gerrit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

// Many of the APIs in the Gerrit REST Client are not supported so I'm implementing a very simplified version here
public class ITGerritServerSupport {
    private final HttpClient httpClient;
    private final String gerritHost;
    private final int gerritPort;

    public ITGerritServerSupport(String host, int port, String adminUsername, String adminPwd) {
        this.gerritHost = host;
        this.gerritPort = port;

        CredentialsProvider basicAuthProvider = new BasicCredentialsProvider();
        basicAuthProvider.setCredentials(
                new AuthScope(gerritHost, gerritPort),
                new UsernamePasswordCredentials(adminUsername, adminPwd));
        httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(basicAuthProvider).build();
    }

    public void createGroup(String groupName) {
        createEntity("group/" + groupName, String.format("group '%s'", groupName));
    }

    public int createTestUser(String userName, Optional<String> publicKeyPathMaybe, List<String> groups) {
        String publicKeyDescription = publicKeyPathMaybe.map(publicKeyPath -> {
                    try (Stream<String> pkFileContent = Files.lines(Paths.get(publicKeyPath))) {
                        return pkFileContent
                                .findFirst()
                                .map(pk -> String.format("  \"ssh_key\": \"%s\",\n", pk))
                                .orElseThrow(() -> new RuntimeException(String.format("Invalid empty public key file '%s'", publicKeyPath)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            ).orElse("");

        String userCreationRequstBody = String.format("{\n" +
                "  \"name\": \"%s\",\n" +
                "  \"email\": \"%s@email.com\",\n" +
                "%s" +
                "  \"groups\": %s\n" +
                "}", userName, userName, publicKeyDescription, groups);

        HttpPut createAccountRequest = new HttpPut(authenticatedGerritEndpointFor("accounts/" + userName));
        createAccountRequest.setEntity(new StringEntity(userCreationRequstBody, ContentType.APPLICATION_JSON));

        String accountCreationResponse = createEntity(createAccountRequest, "account 'test'");

        Map<String, Object> createdUserInfo =
                new Gson().fromJson(accountCreationResponse, new TypeToken<Map<String, Object>>(){}.getType());

        return (Integer) createdUserInfo.get("_account_id");
    }

    public void createTestRepo() {
        createEntity("projects/test", "repository 'test'");
    }

    private String createEntity(String entityPath, String entityDescription) {
        return createEntity(new HttpPut(authenticatedGerritEndpointFor(entityPath)), entityDescription);
    }

    private String createEntity(HttpPut creationRequest, String entityDescription) {
        try {
            return httpClient.execute(creationRequest, creationResponse -> {
                if (creationResponse.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                    throw new RuntimeException("Unable to create " + entityDescription);
                }
                return  EntityUtils.toString(creationResponse.getEntity());
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String authenticatedGerritEndpointFor(String path) {
        return String.format("http://%s:%d/a/%s/", gerritHost, gerritPort, path);
    }

}
