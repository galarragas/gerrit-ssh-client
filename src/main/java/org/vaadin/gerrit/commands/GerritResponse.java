package org.vaadin.gerrit.commands;

public interface GerritResponse {
    boolean hasErrors();
    String getErrorMessage();
}
