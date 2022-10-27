package de.catma.repository.git.managers;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.user.User;

class JGitRepoManagerTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test() {
		try(LocalGitRepositoryManager repoManager = new JGitRepoManager(
			"c:/data/catmadata/localgit/",
			new User() {
				@Override
				public Long getUserId() {return null;}
				@Override
				public String getName() {return null;}
				@Override
				public String getEmail() {return null;}
				
				@Override
				public String getIdentifier() {
					return "mp";
				}
				
			})) {
			repoManager.open("mp", "CATMA_DE298B6B-B3CC-4E39-A178-A48AC7CCA6F7_Alice");
			Set<String> paths = repoManager.getAdditiveBranchDifferences("colab1");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}

}
