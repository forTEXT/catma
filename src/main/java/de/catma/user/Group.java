package de.catma.user;

import de.catma.project.ProjectReference;

import java.util.List;
import java.util.Set;

public interface Group {
    Long getId();

    String getName();

    List<ProjectReference> getSharedProjects();

    Set<Member> getMembers();
}
