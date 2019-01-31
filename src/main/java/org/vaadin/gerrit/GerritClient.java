package org.vaadin.gerrit;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.vaadin.gerrit.commands.ListMembersCommand;
import org.vaadin.gerrit.commands.ListMembersResponse;
import org.vaadin.gerrit.credentials.Credentials;
import org.vaadin.gerrit.factories.CommandFactory;
import org.vaadin.gerrit.factories.GerritConnectionFactory;

import java.util.Arrays;
import java.util.Optional;

public class GerritClient {

    public static final int DEFAULT_PORT = 29418;
    private final String host;
    private final int port;
    private final Credentials credentials;
    private final CommandFactory commandFactory;
    private final GerritConnectionFactory connectionFactory;

    public GerritClient(String host, Credentials credentials) {
        this(host, DEFAULT_PORT, credentials);
    }

    public GerritClient(String host, int port, Credentials credentials) {
        this(Guice.createInjector(new GerritClientModule()), host, port, credentials);
    }

    private GerritClient(Injector injector, String host, int port, Credentials credentials) {
        this(injector.getInstance(CommandFactory.class), injector.getInstance(GerritConnectionFactory.class), host, port, credentials);
    }

    GerritClient(CommandFactory commandFactory, GerritConnectionFactory connectionFactory, String host, int port, Credentials credentials) {
        this.commandFactory = commandFactory;
        this.connectionFactory = connectionFactory;
        this.host = host;
        this.port = port;
        this.credentials = credentials;
    }

    public Member[] getGroupMembers(String groupName) throws GerritClientException {
        ListMembersCommand command = commandFactory.createListMembersCommand(groupName);
        command.setRecursive(true);

        GerritConnection connection = connectionFactory.getConnection(host, port, credentials);

        ListMembersResponse response = command.getResponse(connection);

        if(response.hasErrors()) {
            throw new GerritClientException(response.getErrorMessage());
        }

        return response.getMembers();
    }

    public Member getMemberFromGroup(int id, String groupName) throws GerritClientException {
        Optional<Member> member = Arrays.stream(getGroupMembers(groupName))
                                        .filter(m -> m.Id == id)
                                        .findFirst();

        if(member.isPresent()) {
            return member.get();
        }

        throw new GerritClientException(String.format("Member with id '%s' is not found in group '%s.'", id, groupName));
    }
}
