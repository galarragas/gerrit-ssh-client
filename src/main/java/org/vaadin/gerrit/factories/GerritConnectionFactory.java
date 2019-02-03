package org.vaadin.gerrit.factories;

import org.vaadin.gerrit.GerritConnection;
import org.vaadin.gerrit.credentials.Credentials;

public interface GerritConnectionFactory {
    GerritConnection getConnection(String host, int port, Credentials credentials);
}
