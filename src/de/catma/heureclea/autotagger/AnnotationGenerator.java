package de.catma.heureclea.autotagger;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.naming.NamingException;

import de.catma.document.repository.RepositoryPropertyKey;

public class AnnotationGenerator {
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyMMddhhmm");

	private String generatorPath;
	private String logFolder;

	public AnnotationGenerator() throws NamingException {
		this.generatorPath = 
			RepositoryPropertyKey.AnnotationGeneratorPath.getValue();
		this.logFolder = RepositoryPropertyKey.HeurecleaFolder.getValue();
	}

	public void generate(
			String corpusId, TagsetIdentification tagsetIdentification, 
			String identifier, String token) throws IOException, InterruptedException {
		
		if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
			token = "\"" + token + "\"";
		}
		
		ProcessBuilder pb =
			   new ProcessBuilder(
					   generatorPath, 
					   corpusId, tagsetIdentification.name(), 
					   identifier, token);
		File log = new File(
				logFolder, 
				"AnnotationGenerator" + FORMATTER.format(new Date()) + ".log");
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		
		Process proc = pb.start();

		if(proc.waitFor() != 0){
			throw new IOException(
				"error execution annotation generator see error log for details " 
				+ log.getAbsolutePath());
		}
	}
}
