package org.gitlab4j.api;

import org.gitlab4j.api.models.GroupFilter;

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
