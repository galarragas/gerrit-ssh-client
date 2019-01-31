package org.vaadin.gerrit.model;

import java.util.List;

public class AddReviewerResult {
    /** The identifier of an account or group that was to be added as a reviewer. */
    public String input;

    /** If non-null, a string describing why the reviewer could not be added. */
    public String error;

    /**
     * Non-null and true if the reviewer cannot be added without explicit confirmation. This may be
     * the case for groups of a certain size.
     */
    public Boolean confirm;


}