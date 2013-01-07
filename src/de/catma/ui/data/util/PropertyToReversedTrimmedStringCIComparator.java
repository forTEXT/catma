package de.catma.ui.data.util;

import java.io.Serializable;
import java.util.Comparator;

public class PropertyToReversedTrimmedStringCIComparator implements Comparator<Object>,
	Serializable {

	public int compare(Object o1, Object o2) {
        int r = 0;
        // Normal non-null comparison
        if (o1 != null && o2 != null) {
        	// TODO: use locale from search options, same for non reversed version
        	StringBuilder b1 = 
        			new StringBuilder(o1.toString().trim().toLowerCase());
        	StringBuilder b2 = 
        			new StringBuilder(o2.toString().trim().toLowerCase());
            r = b1.reverse().toString().compareTo(b2.reverse().toString());
        } else if (o1 == o2) {
            // Objects are equal if both are null
            r = 0;
        } else {
            if (o1 == null) {
                r = -1; // null is less than non-null
            } else {
                r = 1; // non-null is greater than null
            }
        }
        return r;
	}
}

