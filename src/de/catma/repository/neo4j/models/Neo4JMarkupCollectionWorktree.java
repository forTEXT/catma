package de.catma.repository.neo4j.models;

import de.catma.document.AccessMode;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.repository.neo4j.exceptions.Neo4JUserMarkupCollectionException;
import de.catma.tag.TagLibrary;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NodeEntity(label="MarkupCollectionWorktree")
public class Neo4JMarkupCollectionWorktree {
	@Id
	private String revisionHash;

	private String uuid;
	private String name;
	private String description;
	private String author;
	private String publisher;

	@Relationship(type="HAS_TAG_INSTANCE", direction=Relationship.OUTGOING)
	private List<Neo4JTagInstance> tagInstances;

	@Relationship(type="REFERENCES_SOURCE_DOCUMENT", direction=Relationship.OUTGOING)
	private Neo4JSourceDocumentWorktree sourceDocument;

	@Relationship(type="REFERENCES_TAGSET", direction=Relationship.OUTGOING)
	private List<Neo4JTagsetWorktree> tagsets;


	public Neo4JMarkupCollectionWorktree() {
		this.tagInstances = new ArrayList<>();
	}

	public Neo4JMarkupCollectionWorktree(UserMarkupCollection userMarkupCollection)
			throws Neo4JUserMarkupCollectionException {

		this();

		this.setUserMarkupCollection(userMarkupCollection);
	}

	public UserMarkupCollection getUserMarkupCollection() throws Neo4JUserMarkupCollectionException {
//		List<TagReference> tagReferences = this.tagInstances.stream().map(Neo4JTagInstance::getTagReferences)
//				.collect(Collectors.toList());
		List<TagReference> tagReferences = new ArrayList<>();
		for (Neo4JTagInstance tagInstance : this.tagInstances) {
			tagReferences.addAll(tagInstance.getTagReferences());
		}

		ContentInfoSet contentInfoSet = new ContentInfoSet(this.author, this.description, this.publisher, this.name);

		// we are hoping to get rid of tag libraries altogether
		TagLibrary tagLibrary = new TagLibrary(null, null);

		UserMarkupCollection userMarkupCollection = new UserMarkupCollection(
				null, this.uuid, contentInfoSet, tagLibrary, tagReferences, AccessMode.WRITE
		);
		userMarkupCollection.setRevisionHash(this.revisionHash);

		return userMarkupCollection;
	}

	public void setUserMarkupCollection(UserMarkupCollection userMarkupCollection)
			throws Neo4JUserMarkupCollectionException {

		this.revisionHash = userMarkupCollection.getRevisionHash();
		this.uuid = userMarkupCollection.getUuid();
		this.name = userMarkupCollection.getContentInfoSet().getTitle();
		this.description = userMarkupCollection.getContentInfoSet().getDescription();
		this.author = userMarkupCollection.getContentInfoSet().getAuthor();
		this.publisher = userMarkupCollection.getContentInfoSet().getPublisher();

		Map<String, List<TagReference>> tagReferencesGroupedByTagInstance = userMarkupCollection.getTagReferences()
				.stream().collect(Collectors.groupingBy(tr -> tr.getTagInstance().getUuid()));

		// TODO: figure out how to do this with .stream().map while handling exceptions properly
		// see https://stackoverflow.com/a/33218789 & https://stackoverflow.com/a/30118121 for pointers
//		tagReferencesGroupedByTagInstance.entrySet().stream().map((k,v) -> new Neo4JTagInstance(v))
//				.collect(Collectors.toList());
		this.tagInstances.clear();
		for (List<TagReference> tagReferences : tagReferencesGroupedByTagInstance.values()) {
			this.tagInstances.add(new Neo4JTagInstance(tagReferences));
		}
	}
}
