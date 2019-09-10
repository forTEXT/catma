package de.catma.ui;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.module.annotate.TaggerView;

public enum ParameterComponentValue {
	TAGGER( catmaApplication -> {
		//TODO: no replacement yet
//		String sourceDocumentId = catmaApplication.getParameter(Parameter.TAGGER_DOCUMENT);
//		
//		TaggerView taggerView = catmaApplication.openSourceDocument(sourceDocumentId);
//		SourceDocument sourceDocument = taggerView.getSourceDocument();
//		
//		List<UserMarkupCollectionReference> collectionRefs = 
//				sourceDocument.getUserMarkupCollectionRefs();
//
//		Set<String> tagsetDefinitionUuids = new HashSet<>();
//		for (UserMarkupCollectionReference ref : collectionRefs) {
//			try {
//				UserMarkupCollection umc = taggerView.openUserMarkupCollection(ref);
//				for (TagsetDefinition tagsetDefinition : umc.getTagLibrary()) {
//					if (!tagsetDefinitionUuids.contains(tagsetDefinition.getUuid())) {
//						tagsetDefinitionUuids.add(tagsetDefinition.getUuid());
//						taggerView.openTagsetDefinition(catmaApplication, 	tagsetDefinition.getUuid(), tagsetDefinition.getVersion());
//					}
//				}
//			}
//			catch (IOException e) {
//				catmaApplication.showAndLogError(Messages.getString("ParameterComponentValue.errorOpeningAnnotations"), e); //$NON-NLS-1$
//			}
//		}
//		
//		
//		String[] additionalTagsetDefinitionUuids = catmaApplication.getParameters(Parameter.TAGGER_TAGSETDEF);
//		if ((additionalTagsetDefinitionUuids != null) && (additionalTagsetDefinitionUuids.length > 0)) {
//			for (String tagsetDefinitionUuid : additionalTagsetDefinitionUuids) {
//				if (!tagsetDefinitionUuids.contains(tagsetDefinitionUuid)) {
//					try {
//						taggerView.openTagsetDefinition(catmaApplication, tagsetDefinitionUuid, null);
//						tagsetDefinitionUuids.add(tagsetDefinitionUuid);
//					}
//					catch (IOException e) {
//						catmaApplication.showAndLogError(Messages.getString("ParameterComponentValue.errorOpeningTagLibrary"), e); //$NON-NLS-1$
//					}
//				}
//			}
//		}		
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
