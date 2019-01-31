package org.vaadin.gerrit.commands;

import org.vaadin.gerrit.model.ReviewInput;

public class ReviewCommand extends GerritCommand<ReviewResponse> {
    private final ReviewInput input;
    public ReviewCommand(ReviewInput input) {
        this.input = input;
    }

    @Override
    protected String getCommand() {
        return null;
    }

    @Override
    protected ReviewResponse parseCommandOutput(String commandOutput) {
        return null;
    }
}
