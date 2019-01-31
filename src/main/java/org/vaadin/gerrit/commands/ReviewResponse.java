package org.vaadin.gerrit.commands;

import java.util.Map;
import org.vaadin.gerrit.model.AddReviewerResult;

public class ReviewResponse implements GerritResponse {
    public static ReviewResponse fromCommandOutput(String response) {

        return new ReviewResponse();
    }


    public Map<String, Short> labels;

    public Map<String, AddReviewerResult> reviewers;

    @Override
    public boolean hasErrors() {
        return false;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }
}
