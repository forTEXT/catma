package de.catma.repository.git;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.catma.project.ProjectReference;
import de.catma.user.Member;

public class GitGroup implements de.catma.user.Group {

    private Set<Member> members;
    private final List<ProjectReference> sharedProjects;
	private Long id;
	private String name;

    public GitGroup(Long id, String name, Set<Member> members, List<ProjectReference> sharedProjects) {
        this.id = id;
        this.name = name;
        this.members = members;
        this.sharedProjects = sharedProjects;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public List<ProjectReference> getSharedProjects() {
        return sharedProjects;
    }

    @Override
    public Set<Member> getMembers() {
        return members;
    }

    @Override
    public void setMembers(Set<Member> members) {
    	this.members = members;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitGroup gitGroup = (GitGroup) o;
        return Objects.equals(getId(), gitGroup.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
