package org.vaadin.gerrit.factories;

import org.vaadin.gerrit.commands.ListMembersCommand;

public interface CommandFactory {
    ListMembersCommand createListMembersCommand(String groupName);
}
