package de.catma.repository.git;

import java.io.File;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.junit.Test;

public class GitTest {

	@Test
	public void test() {
		Git git = null;
		try {
			git = Git.open(new File("c:/test/git-subs/plugin/"));
			
			List<DiffEntry> entries = git.diff().call();
			
			for (DiffEntry entry : entries) {
				System.out.println(entry);
			}
			
			
			
			
			
			
			System.out.println(git.status().call().getUncommittedChanges());
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (git != null) {
				git.close();
			}
		}
		
		
		
		
	}

}
