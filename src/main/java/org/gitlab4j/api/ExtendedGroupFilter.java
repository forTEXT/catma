package org.gitlab4j.api;

import org.gitlab4j.api.models.GroupFilter;

/**
 * Extends {@link GroupFilter} with the <code>active</code> query parameter, which upstream doesn't offer (as of 5.8.1). Used to filter out groups that are
 * pending deletion.
 * <p>
 * It delegates to <code>super.getQueryParams()</code>, so upstream's own parameters are picked up automatically.
 * <p>
 * NB: this class lives in gitlab4j's own package only to keep CATMA's patches to gitlab4j in one place - it doesn't use anything that isn't accessible
 * from outside the package.
 */
public class ExtendedGroupFilter extends GroupFilter {
    private Boolean active;

    public ExtendedGroupFilter withActive(Boolean active) {
        this.active = active;
        return this;
    }

    public GitLabApiForm getQueryParams() {
        return super.getQueryParams().withParam("active", this.active);
    }
}
