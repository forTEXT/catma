package org.gitlab4j.api;

import org.gitlab4j.api.models.ProjectFilter;

/**
 * Extends {@link ProjectFilter} with the <code>active</code> query parameter, which upstream doesn't offer (as of 5.8.1). Used to filter out projects that are
 * pending deletion.
 * <p>
 * NB: this class lives in gitlab4j's own package because {@link GitLabApiForm} is package-private. It delegates to <code>super.getQueryParams()</code>, so
 * upstream's own parameters are picked up automatically.
 */
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
