package de.catma.ui;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.tagger.TaggerView;

public enum ParameterComponentValue {
	TAGGER( catmaApplication -> {
		String sourceDocumentId = catmaApplication.getParameter(Parameter.TAGGER_DOCUMENT);
		
		TaggerView taggerView = catmaApplication.openSourceDocument(sourceDocumentId);
		SourceDocument sourceDocument = taggerView.getSourceDocument();
		
		List<UserMarkupCollectionReference> collectionRefs = 
				sourceDocument.getUserMarkupCollectionRefs();

		Set<String> tagsetDefinitionUuids = new HashSet<>();
		for (UserMarkupCollectionReference ref : collectionRefs) {
			try {
				UserMarkupCollection umc = taggerView.openUserMarkupCollection(ref);
				for (TagsetDefinition tagsetDefinition : umc.getTagLibrary()) {
					if (!tagsetDefinitionUuids.contains(tagsetDefinition.getUuid())) {
						tagsetDefinitionUuids.add(tagsetDefinition.getUuid());
						taggerView.openTagsetDefinition(catmaApplication, 	tagsetDefinition.getUuid(), tagsetDefinition.getVersion());
					}
				}
			}
			catch (IOException e) {
				catmaApplication.showAndLogError("Error opening Markup Collection", e);
			}
		}
		
		
		
		String tagsetDefinitionUuid = catmaApplication.getParameter(Parameter.TAGGER_TAGSETDEF);
		if (!tagsetDefinitionUuids.contains(tagsetDefinitionUuid)) {
			try {
				taggerView.openTagsetDefinition(catmaApplication, tagsetDefinitionUuid, null);
			}
			catch (IOException e) {
				catmaApplication.showAndLogError("Error opening Tag Library", e);
			}
		}
		
	})
	;
	
	private static interface Handler {
		public void show(CatmaApplication catmaApplication);
	}
	
	private Handler handler;
	
	private ParameterComponentValue(Handler handler) {
		this.handler = handler;
	}
	
	public void show(CatmaApplication catmaApplication) {
		handler.show(catmaApplication);
	}
	
	public static void show(CatmaApplication catmaApplication, String componentValue) {
		if ((componentValue == null) || componentValue.trim().isEmpty()) {
			return;
		}
		for (ParameterComponentValue pcv : values()) {
			if (pcv.name().toLowerCase().equals(componentValue.trim().toLowerCase())) {
				pcv.show(catmaApplication);
			}
		}
	}
}
