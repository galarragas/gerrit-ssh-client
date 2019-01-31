package org.vaadin.gerrit.commands;

import org.vaadin.gerrit.GerritClientException;
import org.vaadin.gerrit.GerritConnection;

public abstract class GerritCommand<Response extends GerritResponse> {
    protected abstract String getCommand();
    protected abstract Response parseCommandOutput(String commandOutput);

    public Response getResponse(GerritConnection connection) throws GerritClientException {
        String commandOutput = connection.executeCommand(getCommand());

        final Response response = parseCommandOutput(commandOutput);

        if(response.hasErrors()) {
            throw new GerritClientException(response.getErrorMessage());
        }

        return response;
    }
}
