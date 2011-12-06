package de.catma.core.document.source;

import java.util.Collections;
import java.util.List;

public class IndexInfoSet {

    private List<String> unseparableCharacterSequences;
    private List<Character> userDefinedSeparatingCharacters;
    private List<IndexRegion> indexRegions;
    
    /**
     * @return a (possibly empty) list of unseparable character sequences,
     * does not return <oode>null</oode>
     */
    public List<String> getUnseparableCharacterSequences() {
        return (unseparableCharacterSequences==null) ?
                Collections.<String>emptyList() : unseparableCharacterSequences;
    }
    
    /**
     * @return a (possibly empty) list of user defined speparating character sequences,
     * does not return <oode>null</oode>
     */
    public List<Character> getUserDefinedSeparatingCharacters() {
        return (userDefinedSeparatingCharacters ==null) ?
                Collections.<Character>emptyList() : userDefinedSeparatingCharacters;
    }

}
