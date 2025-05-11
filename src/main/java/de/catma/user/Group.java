package de.catma.user;

import de.catma.project.ProjectReference;

import java.util.List;
import java.util.Set;

public interface Group {
    Long getId();

    String getName();

    String getDescription();

    List<ProjectReference> getSharedProjects();

    Set<Member> getMembers();
    
    void setMembers(Set<Member> members);
}
