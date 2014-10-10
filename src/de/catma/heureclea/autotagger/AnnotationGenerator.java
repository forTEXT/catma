package de.catma.heureclea.autotagger;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import de.catma.document.repository.RepositoryPropertiesName;
import de.catma.document.repository.RepositoryPropertyKey;

public class AnnotationGenerator {
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyMMddhhmm");

	private String generatorPath;
	private String logFolder;

	public AnnotationGenerator() throws NamingException {
		InitialContext context = new InitialContext();
		Properties properties = 
			(Properties) context.lookup(
				RepositoryPropertiesName.CATMAPROPERTIES.name());
		
		this.generatorPath = 
			properties.getProperty(RepositoryPropertyKey.AnnotationGeneratorPath.name());
		this.logFolder = properties.getProperty("heurecleaExportFolder");
	}

	public void generate(String corpusId, TagsetIdentification tagsetIdentification) throws IOException, InterruptedException {
		ProcessBuilder pb =
			   new ProcessBuilder(generatorPath, corpusId, tagsetIdentification.name());
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
