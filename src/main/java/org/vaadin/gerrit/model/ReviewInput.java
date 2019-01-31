package org.vaadin.gerrit.model;

import java.util.Optional;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;

//Documentation at https://review.openstack.org/Documentation/cmd-review.html
@AutoValue
public abstract class ReviewInput {

    public static final String LABEL_CODE_REVIEW = "Code-Review";

    public enum NotifyHandling {
        NONE,
        OWNER,
        OWNER_REVIEWERS,
        ALL
    }

    public enum Action {
        SUBMIT,
        ABANDON,
        RESTORE,
        PUBLISH,
        REBASE,
        DELETE
    }

    abstract Optional<String> project();
    abstract Optional<String> branch();
    abstract Optional<String> message();
    abstract NotifyHandling notifyHandling();
    abstract Action action();
    abstract Optional<String> verified();
    abstract Optional<String> codeReview();
    abstract ImmutableSet<Pair<String, Short>> labels();
    abstract Optional<String> tag();
    abstract String commitOrChangeSetSpec();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder project(Optional<String> value);
        public abstract Builder branch(Optional<String> value);
        public abstract Builder message(Optional<String> value);
        public abstract Builder notifyHandling(NotifyHandling value);
        public abstract Builder action(Action value);
        public abstract Builder verified(Optional<String> value);
        public abstract Builder codeReview(Optional<String> value);
        public abstract Builder labels(ImmutableSet<Pair<String, Short>> value);
        public abstract ImmutableSet.Builder<Pair<String, Short>> labelsBuilder();

        public Builder addLabel(String key, int value) {
            labelsBuilder().add(Pair.of(key, (short)value));
            return this;
        }

        public abstract Builder tag(Optional<String> value);
        public abstract Builder commitOrChangeSetSpec(String value);

        public Builder recommend() { return addLabel(LABEL_CODE_REVIEW, 1); }
        public Builder dislike() { return addLabel(LABEL_CODE_REVIEW, -1); }
        public Builder noScore() { return addLabel(LABEL_CODE_REVIEW, 0); }
        public Builder approve() { return addLabel(LABEL_CODE_REVIEW, 2); }
        public Builder reject() { return addLabel(LABEL_CODE_REVIEW, -2); }

        public abstract ReviewInput build();
    }
}
