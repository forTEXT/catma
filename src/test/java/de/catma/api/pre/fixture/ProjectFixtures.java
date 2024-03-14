package de.catma.api.pre.fixture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.mockito.Mockito;

import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.project.ProjectReference;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.user.User;

public class ProjectFixtures {

	public static List<ProjectReference> setUpProjectList(RemoteGitManagerRestrictedFactory remoteGitManagerRestrictedFactoryMock) throws IOException {
		String dummyIdent = "dummyIdent";
		
		RemoteGitManagerRestricted remoteGitManagerRestrictedMock = Mockito.mock(RemoteGitManagerRestricted.class);
		
		when(remoteGitManagerRestrictedFactoryMock.create(anyString())).thenReturn(remoteGitManagerRestrictedMock);
		
		User userMock = Mockito.mock(User.class);
		
		when(remoteGitManagerRestrictedFactoryMock.create(anyString())).thenReturn(remoteGitManagerRestrictedMock);

		when(remoteGitManagerRestrictedMock.getUser()).thenReturn(userMock);
		when(remoteGitManagerRestrictedMock.getUsername()).thenReturn(dummyIdent);
		
		when(userMock.getIdentifier()).thenReturn(dummyIdent);
		

		ProjectReference pr1 = new ProjectReference("p1", "user666", "My first Project", "a description");
		ProjectReference pr2 = new ProjectReference("p2", "user666", "My second Project", "another description");
		List<ProjectReference> prList = List.of(pr1, pr2);
		
		when(remoteGitManagerRestrictedMock.getProjectReferences()).thenReturn(prList);
		return prList;
	}
}
