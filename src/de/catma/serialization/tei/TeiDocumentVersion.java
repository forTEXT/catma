package de.catma.serialization.tei;



/**
 * Created by IntelliJ IDEA.
 * User: mp
 * Date: 08.05.2010
 * Time: 11:43:25
 * To change this template use File | Settings | File Templates.
 */
public enum TeiDocumentVersion {
	V3(3, new V3TeiDocumentConverter()),
    V2(2, V3, new V2TeiDocumentConverter()),
    V1(1, V2),
    ;

	private static final String TARGET_INFO_XML_ID = "TARGET_INFO"; //legacy
    private int version;
    private TeiDocumentVersion nextVersion;
    private TeiDocumentConverter converter;

    private TeiDocumentVersion(int version, TeiDocumentConverter converter) {
        this(version, null, converter);
    }

    private TeiDocumentVersion(
            int version,
            TeiDocumentVersion nextVersion ) {
        this(version, nextVersion, null);
    }
    private TeiDocumentVersion(
            int version,
            TeiDocumentVersion nextVersion,
            TeiDocumentConverter converter) {
        
        this.version = version;
        this.nextVersion = nextVersion;
        this.converter = converter;
    }

    private void convertToNextVersion(TeiDocument teiDocument) {
        if (nextVersion != null) {
            nextVersion.converter.convert(teiDocument);
        }
    }

    public boolean isCurrentVersion() {
        return (nextVersion == null);
    }

    public static TeiDocumentVersion getVersion(int version) {
        for (TeiDocumentVersion tdv : values() ) {
            if (tdv.version == version) {
                return tdv;
            }
        }

        throw new IllegalArgumentException( 
        		TeiDocumentVersion.class.getSimpleName() + " " + version + " unknown!");
    }

    public static void convertToLatest(TeiDocument teiDocument) {
    	
    	TeiElement oldTargetInfo = teiDocument.getElementByID(TARGET_INFO_XML_ID);
    	if ((oldTargetInfo != null) && oldTargetInfo.parentIs(TeiElementName.ab) 
    			&& (oldTargetInfo.getTeiElementParent() != null) 
    			&& (oldTargetInfo.getTeiElementParent().parentIs(TeiElementName.sourceDesc))) {
    		oldTargetInfo.getAttribute( 
    				Attribute.xmlid.getLocalName(), 
    				Attribute.xmlid.getNamespaceURI() ).setValue(TechnicalDescription.CATMA_TECH_DESC_XML_ID);
    	}
    	
    	teiDocument.loadHeader();
    	
        while(!teiDocument.getTeiHeader().getTechnicalDescription().getVersion().isCurrentVersion()) {
        	teiDocument.getTeiHeader().getTechnicalDescription().getVersion().convertToNextVersion(
                    teiDocument);
        }
    }


    public static TeiDocumentVersion getCurrentVersion() {
        return V3;
    }

    @Override
    public String toString() {
        return String.valueOf(version);
    }
}
