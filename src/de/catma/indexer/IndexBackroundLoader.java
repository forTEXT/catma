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

package de.catma.indexer;

import java.util.List;
import java.util.Locale;

/**
 * The background loader that handles loading of the {@link org.catma.indexer.Index}.
 * 
 * @author Marco Petris
 *
 */
public class IndexBackroundLoader {

    /**
     * Loads the index.
     * @param content the content to index
     * @param unseparableCharacterSequences the list of unseparable character sequences
     * @param userDefinedSeparatingCharacters the list of user defined separating characters
     * @param locale the locale of the main language of the content
     * @param documentName the name of the document
     * @param executionListener a listener that will be notified when index loading has completed
     */
    public void load(
            final String content,
            final List<String> unseparableCharacterSequences,
            final List<Character> userDefinedSeparatingCharacters,
            final Locale locale,
            final String documentName, ExecutionListener<Index> executionListener) {

        BackgroundService.SINGLETON.submit(
            new DefaultProgressCallable<Index>() {

                public Index call() throws Exception {
                    getProgressListener().setIndeterminate(
                            true, "FileManager.creatingIndex",
                            documentName);

                    StopWatch stopWatch = new StopWatch();
                    Log.DEFAULT_LOGGER.info(LogText.SINGLETON.getString(
                        "IndexBackgroundLoader.startIndexing", documentName ));
                    
                    Index idx = Indexer.SINGLETON.createIndex(
                        content, unseparableCharacterSequences,
                        userDefinedSeparatingCharacters,
                        locale);

                    Log.DEFAULT_LOGGER.info(LogText.SINGLETON.getString(
                        "IndexBackgroundLoader.finishedIndexing",
                            documentName, stopWatch.toString() ));

                    getProgressListener().setIndeterminate(
                            false, "FileManager.creatingIndex",
                            documentName);

                    return idx;
                }

            },
            executionListener
        );

        
    }

}
