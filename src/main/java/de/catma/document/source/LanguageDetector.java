/*
 * CATMA Computer Aided Text Markup and Analysis
 *
 *    Copyright (C) 2008-2010  University Of Hamburg
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.catma.document.source;

import com.aliasi.classify.BaseClassifier;
import com.aliasi.classify.Classification;
import com.aliasi.util.AbstractExternalizable;

import java.io.IOException;
import java.util.Locale;

/**
 * Facility to guess the language of content.
 * 
 * @author marco.petris@web.de
 * 
 * @see com.aliasi.classify.BaseClassifier
 */
public class LanguageDetector {

    private BaseClassifier<CharSequence> classifier;

    /**
     * Constructor.
     *
     * @throws IOException failure to access the .classifier-file
     * @see com.aliasi.util.AbstractExternalizable
     */
    @SuppressWarnings("unchecked")
    public LanguageDetector() throws IOException {
        try {
            classifier
                = (BaseClassifier<CharSequence>)AbstractExternalizable.readResourceObject(
                    this.getClass(),
                    "/de/catma/document/source/resources/langid-leipzig.classifier");
        }
        catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    /**
     * @param content the content we want the language for
     * @return the language classification for the given content
     */
    public Classification detect(String content) {
        return classifier.classify(content); 
    }

    /**
     * @param classification the language classification
     * @return the locale for that language classification or <code>null</code> if classification failed
     */
    public Locale getLocale(Classification classification) {
        Locale locale = null;
        String bestCat = classification.bestCategory();
        if (bestCat != null) {
            locale = new Locale(bestCat, bestCat.toUpperCase());
        }
        return locale;
    }
    
}
