package org.vaadin.gerrit;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;

// Many of the APIs in the Gerrit REST Client are not supported so I'm implementing a very simplified version here
public class ITGerritServerSupport {
    Logger logger = Logger.getLogger(getClass().getName());

    private final HttpClient httpClient;
    private final String gerritHost;
    private final int gerritPort;

    public static String accountNameFor(String userName) {
        return Normalizer.normalize(userName, Normalizer.Form.NFD)
                .replace(" ", "-")
                .replaceAll("[^\\x00-\\x7F]", "");
    }


    public ITGerritServerSupport(String host, int port, String adminUsername, String adminPwd) {
        this.gerritHost = host;
        this.gerritPort = port;

        CredentialsProvider basicAuthProvider = new BasicCredentialsProvider();
        basicAuthProvider.setCredentials(
                new AuthScope(gerritHost, gerritPort),
                new UsernamePasswordCredentials(adminUsername, adminPwd));
        httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(basicAuthProvider).build();
    }

    public String createGroup(String groupName) {
        try {
            String response = createEntity("groups/" + groupName, String.format("group '%s'", URLEncoder.encode(groupName, "UTF-8")));

            Map<String, Object> responseJson = asJsonObjectMap(response);

            return responseJson.get("id").toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteGroupMembers(String groupId, List<Integer> memberIDs) {
        try {
            //POST /groups/{group-id}/members.delete
            HttpPost deleteMembersRequest = new HttpPost(authenticatedGerritEndpointFor(String.format("groups/%s/members.delete", groupId)));
            deleteMembersRequest.setEntity(
                    new StringEntity(String.format("{\"members\": %s}", memberIDs), APPLICATION_JSON));
            HttpResponse deleteMembersResponse = httpClient.execute(deleteMembersRequest);
            if ((deleteMembersResponse.getStatusLine().getStatusCode() / 100) != 2) {
                throw new RuntimeException(
                        String.format("Unable to remove accounts %s from group %s , response is %s",
                                memberIDs, groupId, deleteMembersResponse));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addSshKeyToAdminAccount(String publicKeyPath) {
        try {
            String publicKey;
            try (Stream<String> pkFileContent = Files.lines(Paths.get(publicKeyPath))) {
                publicKey = pkFileContent
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException(String.format("Invalid empty public key file '%s'", publicKeyPath)));
            }

            HttpPost addKeyRequest = new HttpPost(authenticatedGerritEndpointFor("accounts/self/sshkeys"));
            addKeyRequest.setEntity(new StringEntity(publicKey));
            HttpResponse addKeyResponse = httpClient.execute(addKeyRequest);

            if ((addKeyResponse.getStatusLine().getStatusCode() / 100) != 2) {
                throw new RuntimeException("Unable to add SSH key to admin account, response is " + addKeyResponse);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public int createTestUser(String userName, Optional<String> publicKeyPathMaybe, List<String> groups) {
        try {
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

            String userAccount = accountNameFor(userName);
            String userCreationRequstBody = String.format("{" +
                            "  \"name\": \"%s\", " +
                            "  \"email\": \"%s@email.com\", " +
                            "%s" +
                            "  \"groups\": %s" +
                            " }",
                    userName,
                    userAccount,
                    publicKeyDescription,
                    groups.stream().map(groupName -> String.format("\"%s\"", groupName)).collect(Collectors.toList())
            );

            HttpPut createAccountRequest = new HttpPut(authenticatedGerritEndpointFor("accounts/" + userAccount));
            createAccountRequest.setEntity(new StringEntity(userCreationRequstBody, APPLICATION_JSON));

            String accountCreationResponse = createEntity(createAccountRequest, String.format("account '%s'", userAccount));

            Map<String, Object> createdUserInfo = asJsonObjectMap(accountCreationResponse);

            return ((Double) createdUserInfo.get("_account_id")).intValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void workaroundForInital302OnRestCallToGerrit() {
        try {
            HttpGet findUserRequest = new HttpGet(authenticatedGerritEndpointFor("accounts/", "?q=name:admin"));
            HttpResponse response;
            for (int i = 0; i < 10; i++) {
                Thread.sleep(1000L);
                logger.info("Placeholder call to accounts..");
                response = httpClient.execute(findUserRequest);
                if (response.getStatusLine().getStatusCode() != 302) {
                    logger.info("Response is now " + response.getStatusLine() + " breaking loop");
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Integer> getUserId(String username) {
        try {
            HttpGet findUserRequest = new HttpGet(authenticatedGerritEndpointFor("accounts/", String.format("q=name:%s", username)));
            return httpClient.execute(findUserRequest, findUserResponse -> {
                String findUserResponseBody = EntityUtils.toString(findUserResponse.getEntity());
                List<Map<String, Object>> userInfos = new Gson()
                        .fromJson(findUserResponseBody, new TypeToken<List<Map<String, Object>>>() {
                        }.getType());

                return userInfos.stream().findFirst().map(userInfo -> ((Double) userInfo.get("_account_id")).intValue());
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean groupExists(String name) {
        return entityExists(name, "groups/");
    }


    public boolean projectExists(String name) {
        return entityExists(name, "projects/");
    }

    public boolean userExists(String name) {
        return getUserId(name).isPresent();
    }

    public void createProject(String name) {
        try {
            createEntity("projects/" + name, "repository 'test'");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean entityExists(String name, String path) {
        try {
            return httpClient.execute(new HttpGet(authenticatedGerritEndpointFor(path)),
                    creationResponse -> asJsonObjectMap(EntityUtils.toString(creationResponse.getEntity()))
            ).containsKey(name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private String createEntity(String entityPath, String entityDescription) throws URISyntaxException {
        return createEntity(new HttpPut(authenticatedGerritEndpointFor(entityPath)), entityDescription);
    }

    private String createEntity(HttpPut creationRequest, String entityDescription) {
        try {
            return httpClient.execute(creationRequest, creationResponse -> {
                if (creationResponse.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                    throw new RuntimeException("Unable to create " + entityDescription + " failure is " + creationResponse.getStatusLine());
                }
                return EntityUtils.toString(creationResponse.getEntity());
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private URI authenticatedGerritEndpointFor(String path) throws URISyntaxException {
        return authenticatedGerritEndpointFor(path, null);
    }

    private URI authenticatedGerritEndpointFor(String path, String query) throws URISyntaxException {
        return new URI("http", null, gerritHost, gerritPort, String.format("/a/%s", path), query, null);
    }

    private Map<String, Object> asJsonObjectMap(String jsonObjectStr) {
        return new Gson().fromJson(jsonObjectStr, new TypeToken<Map<String, Object>>() {
        }.getType());
    }
}
