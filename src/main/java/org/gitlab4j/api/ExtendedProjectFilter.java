package org.gitlab4j.api;

import org.gitlab4j.api.models.ProjectFilter;

public class ExtendedProjectFilter extends ProjectFilter
{
    private Boolean active;

    public ExtendedProjectFilter withActive(Boolean active)
    {
        this.active = active;
        return this;
    }

    public GitLabApiForm getQueryParams() {
        return super.getQueryParams().withParam("active", this.active);
    }
}
