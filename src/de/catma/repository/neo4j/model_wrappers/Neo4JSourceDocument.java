package de.catma.repository.neo4j.model_wrappers;

import de.catma.document.source.*;
import de.catma.document.source.contenthandler.SourceContentHandler;
import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;
import de.catma.repository.neo4j.Neo4JRelationshipType;
import de.catma.repository.neo4j.exceptions.Neo4JSourceDocumentException;
import de.catma.repository.neo4j.models.Neo4JTerm;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@NodeEntity(label="SourceDocument")
public class Neo4JSourceDocument {
	@Id
	@GeneratedValue
	private Long id;

	private String uuid;
	private Integer length;
	private String revisionHash;

	// could use a @Properties annotated Map for each of the InfoSet classes below, but need to investigate further
	// in order to avoid generic Map<String, Object> that then requires each value to be cast
	// see http://neo4j.com/docs/ogm-manual/current/reference/#reference:annotating-entities
	// (3.4.1.1. @Properties: dynamically mapping properties to graph)

	// IndexInfoSet
	private List<String> unseparableCharacterSequences;
	private List<Character> userDefinedSeparatingCharacters;
	private String locale;

	// ContentInfoSet
	private String author;
	private String description;
	private String publisher;
	private String title;

	// TechInfoSet
	private String fileName;
	private String fileType;
	private String charset;
	private String fileOSType;
	private Long checksum;
	private String mimeType;
	private String uri;

	@Relationship(type=Neo4JRelationshipType.HAS_TERM, direction=Relationship.OUTGOING)
	private List<Neo4JTerm> terms;

	public Neo4JSourceDocument() {
		this.unseparableCharacterSequences = new ArrayList<>();
		this.userDefinedSeparatingCharacters = new ArrayList<>();
		this.terms = new ArrayList<>();
	}

	public Neo4JSourceDocument(SourceDocument sourceDocument) throws Neo4JSourceDocumentException {
		this();

		this.setSourceDocument(sourceDocument);
	}

	public Long getId() {
		return this.id;
	}

	public String getUuid() {
		return this.uuid;
	}

	public Integer getLength() {
		return this.length;
	}

	public String getRevisionHash() {
		return this.revisionHash;
	}

	public SourceDocument getSourceDocument() throws Neo4JSourceDocumentException {
		TechInfoSet techInfoSet = new TechInfoSet(
				FileType.valueOf(this.fileType),
				Charset.forName(this.charset),
				FileOSType.valueOf(this.fileOSType),
				this.checksum
		);
		techInfoSet.setFileName(this.fileName);
		techInfoSet.setMimeType(this.mimeType);

		try {
			techInfoSet.setURI(this.uri == null ? null : new URI(this.uri));
		}
		catch (URISyntaxException e) {
			throw new Neo4JSourceDocumentException("Failed to reconstruct URI object", e);
		}

		ContentInfoSet contentInfoSet = new ContentInfoSet(this.author, this.description, this.publisher, this.title);

		IndexInfoSet indexInfoSet = new IndexInfoSet(
				this.unseparableCharacterSequences,
				this.userDefinedSeparatingCharacters,
				Locale.forLanguageTag(this.locale)
		);

		SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo(indexInfoSet, contentInfoSet, techInfoSet);

		SourceContentHandler sourceContentHandler = new StandardContentHandler();
		sourceContentHandler.setSourceDocumentInfo(sourceDocumentInfo);

		// we could now reconstruct the source document contents from this.terms, but it's unlikely that the source
		// document would be edited through the graph DB

		SourceDocument sourceDocument = new SourceDocument(this.uuid, sourceContentHandler);
		sourceDocument.setRevisionHash(this.revisionHash);

		return sourceDocument;
	}

	public void setSourceDocument(SourceDocument sourceDocument) throws Neo4JSourceDocumentException {
		this.revisionHash = sourceDocument.getRevisionHash();

		this.uuid = sourceDocument.getID();

		try {
			this.length = sourceDocument.getLength();
		}
		catch (IOException e) {
			throw new Neo4JSourceDocumentException("Failed to get source document length", e);
		}

		// IndexInfoSet
		IndexInfoSet indexInfoSet = sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
				.getIndexInfoSet();
		this.unseparableCharacterSequences = indexInfoSet.getUnseparableCharacterSequences();
		this.userDefinedSeparatingCharacters = indexInfoSet.getUserDefinedSeparatingCharacters();

		Locale locale = indexInfoSet.getLocale();
		this.locale = locale.toLanguageTag();

		// ContentInfoSet
		ContentInfoSet contentInfoSet = sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
				.getContentInfoSet();
		this.author = contentInfoSet.getAuthor();
		this.description = contentInfoSet.getDescription();
		this.publisher = contentInfoSet.getPublisher();
		this.title = contentInfoSet.getTitle();

		// TechInfoSet
		TechInfoSet techInfoSet = sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getTechInfoSet();
		this.fileName = techInfoSet.getFileName();
		this.fileType = techInfoSet.getFileType().toString();
		this.charset = techInfoSet.getCharset().name();
		this.fileOSType = techInfoSet.getFileOSType().toString();
		this.checksum = techInfoSet.getChecksum();
		this.mimeType = techInfoSet.getMimeType();

		URI uri = techInfoSet.getURI();
		this.uri = uri == null ? null : uri.toString();



		String sourceDocumentContent;
		Map<String, List<TermInfo>> terms;

		try {
			sourceDocumentContent = sourceDocument.getContent();

			TermExtractor termExtractor = new TermExtractor(
					sourceDocumentContent, this.unseparableCharacterSequences, this.userDefinedSeparatingCharacters,
					locale
			);
			terms = termExtractor.getTerms();
		}
		catch (IOException e) {
			throw new Neo4JSourceDocumentException("Failed to get source document content or terms", e);
		}

		this.setTerms(terms);
	}

	public Map<String, List<TermInfo>> getTerms() {
		return this.terms.stream().collect(Collectors.toMap(Neo4JTerm::getLiteral, Neo4JTerm::getTermInfos));
	}

	public void setTerms(Map<String, List<TermInfo>> terms) {
		this.terms.clear();

		for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
			String term = entry.getKey();
			List<TermInfo> termInfos = entry.getValue();

			this.terms.add(new Neo4JTerm(term, termInfos));
		}
	}
}
