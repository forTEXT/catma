package de.catma.repository;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;
import de.catma.repository.db.DBRepository;
import de.catma.repository.db.model.DBSourceDocument;
import de.catma.repository.db.model.DBUser;


public class RepoTest {

	private DBRepository repository;

//	@Before
//	public void setup() {
//		repository = new DBRepository();
//	}
//	
//	
//	@Test
//	public void addUser() throws Throwable {
//		DBUser user = new DBUser("mp", false);
//		repository.addUser(user);
//		
//		user = new DBUser("nase", false);
//		DBSourceDocument dbSDoc = 
//				new DBSourceDocument(
//					"testTitle", "testPublisher", 
//					"testAuthor", "testDescription", "testUri", 
//					FileType.TEXT.toString(),
//					Charset.forName("UTF-8").name(),
//					FileOSType.DOS.name(),
//					42,
//					null, null, false, Locale.ENGLISH.toString(),"localUri");
//		user.getDbSourceDocuments().add(dbSDoc);
//		repository.addUser(user);
//	}
}
