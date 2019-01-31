package org.vaadin.gerrit.commands;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.vaadin.gerrit.GerritClientException;
import org.vaadin.gerrit.GerritConnection;

public class ListMembersCommand extends GerritCommand<ListMembersResponse> {
    private boolean recursive;
    private final String groupName;

    @Inject
    public ListMembersCommand(@Assisted String groupName) {

        this.groupName = groupName;
        this.recursive = false;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    @Override
    protected String getCommand() {
        //GroupName has to escaped: https://code.google.com/p/gerrit/issues/detail?id=2589
        return String.format("gerrit ls-members '%s'%s", groupName, recursive ? " --recursive" : "");
    }

    @Override
    protected ListMembersResponse parseCommandOutput(String commandOutput) {
        return ListMembersResponse.fromCommandOutput(commandOutput);
    }
}

