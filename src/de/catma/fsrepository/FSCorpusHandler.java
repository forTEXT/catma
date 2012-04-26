package de.catma.fsrepository;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Node;
import nu.xom.Nodes;
import de.catma.core.document.Corpus;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

class FSCorpusHandler {

	private final static String CORPUS_FOLDER = "corpora";
	
	private static enum Field {
		name,
		sourceURI,
		userMarkupURI,
		staticMarkupURI,
		;
		private String toSimpleXQuery() {
			return "//" + this.toString();
		}
	}
	
	private String repoFolderPath;
	private String corpusFolderPath;

	public FSCorpusHandler(String repoFolderPath) {
		super();
		this.repoFolderPath = repoFolderPath;
		this.corpusFolderPath = 
				this.repoFolderPath + "/" + CORPUS_FOLDER;
	}
	
	public Set<Corpus> loadCorpora(FSRepository fsRepository) throws IOException {
		Set<Corpus> corpora = new HashSet<Corpus>();
		File corpusFolder = new File(corpusFolderPath);
		File[] corpusFiles = corpusFolder.listFiles();
		
		for (File corpusFile : corpusFiles) {
			Corpus corpus = loadCorpus(corpusFile, fsRepository);
			if (corpus !=null) {
				corpora.add(corpus);
			}
		}
		
		return corpora;
	}

	private Corpus loadCorpus(File corpusFile, FSRepository fsRepository) throws IOException {
		try {
			Document corpusDoc = new Builder().build(corpusFile);
			
			Nodes nameNode = corpusDoc.query(Field.name.toSimpleXQuery());
			String corpusName = nameNode.get(0).getValue();
			Corpus corpus = new Corpus(corpusName);
			
			Nodes sourceNodes = corpusDoc.query(Field.sourceURI.toSimpleXQuery());
			for (int i=0; i<sourceNodes.size(); i++) {
				Node sourceNode = sourceNodes.get(i);
				
				SourceDocument sourceDocument = 
						fsRepository.getSourceDocument(
								FSRepository.getFileURL(sourceNode.getValue(), repoFolderPath));
				corpus.addSourceDocument(sourceDocument);
			}
			
			Nodes staticMarkupURINodes = corpusDoc.query(Field.staticMarkupURI.toSimpleXQuery());
			
			for (int i=0; i<staticMarkupURINodes.size(); i++) {
				Node staticMarkupURINode = staticMarkupURINodes.get(i);
				String staticMarkupURI = 
						FSRepository.getFileURL(
								staticMarkupURINode.getValue(), repoFolderPath);
				StaticMarkupCollectionReference staticMarkupCollRef = 
						new StaticMarkupCollectionReference(staticMarkupURI, staticMarkupURI);
				corpus.addStaticMarkupCollectionReference(staticMarkupCollRef);
			}
			
			Nodes userMarkupURINodes = corpusDoc.query(Field.userMarkupURI.toSimpleXQuery());
			
			for (int i=0; i<userMarkupURINodes.size(); i++) {
				Node userMarkupURINode = userMarkupURINodes.get(i);
				String userMarkupURI = 
						FSRepository.getFileURL(
								userMarkupURINode.getValue(), repoFolderPath);
				UserMarkupCollectionReference userMarkupCollRef = 
						new UserMarkupCollectionReference(userMarkupURI, userMarkupURI);
				corpus.addUserMarkupCollectionReference(userMarkupCollRef);
			}
			
			return corpus;
			
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	public String getCorpusFolderPath() {
		return corpusFolderPath;
	}
}
