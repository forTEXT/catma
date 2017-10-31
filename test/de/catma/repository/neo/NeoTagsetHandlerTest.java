package de.catma.repository.neo;

import de.catma.repository.neo.exceptions.NeoTagsetHandlerException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

public class NeoTagsetHandlerTest {
	// how to test for exceptions: https://stackoverflow.com/a/31826781
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void insertTagset() throws Exception {
		NeoTagsetHandler neoTagsetHandler = new NeoTagsetHandler();

		thrown.expect(NeoTagsetHandlerException.class);
		thrown.expectMessage("Not implemented");
		neoTagsetHandler.insertTagset(null);
	}

}
