package de.catma.repository.git;

import de.catma.project.ProjectReference;
import de.catma.user.Member;
import org.gitlab4j.api.models.Group;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class GitGroup implements de.catma.user.Group {

    private final Set<Member> members;
    private final Group delegate;
    private final List<ProjectReference> sharedProjects;

    public GitGroup(Group delegate, Set<Member> members, List<ProjectReference> sharedProjects) {
        this.delegate = delegate;
        this.members = members;
        this.sharedProjects = sharedProjects;
    }

    @Override
    public Long getId() {
        return delegate.getId();
    }

    @Override
    public String getName() {
        return delegate.getName();
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
