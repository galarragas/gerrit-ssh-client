package org.vaadin.gerrit.commands;

import org.vaadin.gerrit.GerritClientException;
import org.vaadin.gerrit.GerritConnection;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ListMembersCommandTest {

    private ListMembersCommand sut = new ListMembersCommand("foobar");

    private GerritConnection connection;

    @Before
    public void setup() throws GerritClientException {
        sut = new ListMembersCommand("foobar");

        connection = mock(GerritConnection.class);
        when(connection.executeCommand(anyString())).thenReturn(
                "id\tusername\tfull name\temail\n" +
                "100000\tjim\tJim Bob\tsomebody@example.com"
        );
    }

    @Test
    public void commandIsNotRecursive() throws GerritClientException {
        getResponse();

        verify(connection).executeCommand("gerrit ls-members 'foobar'");
    }

    @Test
    public void commandIsRecursive() throws GerritClientException {
        sut.setRecursive(true);

        getResponse();

        verify(connection).executeCommand("gerrit ls-members 'foobar' --recursive");
    }

    private ListMembersResponse getResponse() {
        try {
            return sut.getResponse(connection);
        } catch (GerritClientException e) {
            fail("Exception got when calling response '" + e.getMessage() + "");
        }

        return null;
    }

}