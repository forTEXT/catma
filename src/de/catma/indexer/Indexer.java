/*
 *    CATMA Computer Aided Text Markup and Analysis
 * 
 *    Copyright (C) 2009  University Of Hamburg
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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;


import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * This class handles the construction of the {@link org.catma.indexer.Index} of a
 * {@link org.catma.document.source.SourceDocument}.
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public enum Indexer {
    SINGLETON;

    /**
     * Constructor.
     */
    private Indexer() {
    }

    /**
     * Creates an index. This method should be called in the background via
     * the {@link org.catma.indexer.IndexBackroundLoader}, since indexing takes its time.
     *
     * @param content the content to index
     * @param unseparableCharacterSequences the list of unseparable character sequences
     * @param userDefinedSeparatingCharacters the list of user defined separating characters
     * @param locale the locale of the main language of the content
     * @return the created Index
     * @throws IOException can have various reasons, see instance for details
     * @see org.catma.indexer.IndexBackroundLoader
     * @see org.catma.backgroundservice.BackgroundService
     */
    public Index createIndex(
            String content,
            List<String> unseparableCharacterSequences,
            List<Character> userDefinedSeparatingCharacters,
            Locale locale ) throws IOException {

        // just in case something went wrong, better use the default than nothing 
        if (locale == null) {
            locale = Locale.getDefault();
        }

        Directory directory = new RAMDirectory();

        CharTreeFactory ctf = new CharTreeFactory();
        CharTree unseparableCharSeqTree =
                ctf.createCharMap(unseparableCharacterSequences); 

        WhitespaceAndPunctuationAnalyzer analyzer =
                new WhitespaceAndPunctuationAnalyzer(
                        unseparableCharSeqTree,
                        buildPatternFrom(userDefinedSeparatingCharacters),
                        locale);

        IndexWriter iwriter =
                new IndexWriter(
                directory, analyzer, true,
                IndexWriter.MaxFieldLength.UNLIMITED);

        TokenStream ts =
                analyzer.tokenStream(
                        null, // our analyzer does not use the fieldname 
                        new StringReader(content));

        // build the token list

        ArrayList<TermInfo> tokenList = new ArrayList<TermInfo>();

        while(ts.incrementToken()) {
            TermAttribute termAttr =
                    (TermAttribute)ts.getAttribute(TermAttribute.class);
            OffsetAttribute offsetAttr =
                    (OffsetAttribute)ts.getAttribute(OffsetAttribute.class);

            TermInfo ti =  new TermInfo(termAttr.term(),
                offsetAttr.startOffset(), offsetAttr.endOffset());
            tokenList.add(ti);
        }

        // store the list in the index

        for( int idx=0; idx<tokenList.size(); idx++) {
            TermInfo curTermInfo = tokenList.get(idx);
            Document doc = new Document();

            // index term
            doc.add(new Field(Fieldname.term.name(),
                    curTermInfo.getTerm(), Field.Store.YES,
                    Field.Index.NOT_ANALYZED));

            // store position
            doc.add(new Field(Fieldname.startOffset.name(),
                    String.valueOf(curTermInfo.getRange().getStartPoint()), Field.Store.YES,
                    Field.Index.NO));

            // store position
            doc.add(new Field(Fieldname.endOffset.name(),
                    String.valueOf(curTermInfo.getRange().getEndPoint()), Field.Store.YES,
                    Field.Index.NO));

//            // index predecessors and successors
//            for(
//                int subIdx = Math.max(0,idx-preSucCount);
//                subIdx<Math.min(tokenList.size(),idx+preSucCount);
//                subIdx++) {
//                if (subIdx != idx) {
//                    doc.add(
//                        new Field(
//                            Fieldname.posX.name()+String.valueOf(subIdx-idx),
//                            String.valueOf(tokenList.get(subIdx).getTerm()),
//                            Field.Store.NO,
//                            Field.Index.ANALYZED));
//                }
//            }

            iwriter.addDocument(doc);

        }

        iwriter.optimize();
        iwriter.close();
        ts.close();

        return new Index(directory, analyzer, tokenList.size());
    }

    /**
     * Creates an OR-ed regex pattern from the list of user defined separating characters.
     * @param userDefinedSeparatingCharacters the list of user defined separating characters
     * @return the pattern
     */
    private Pattern buildPatternFrom( List<Character> userDefinedSeparatingCharacters) {

        if (userDefinedSeparatingCharacters.isEmpty()) {
            return null;
        }
        
        StringBuilder patternBuilder = new StringBuilder();
        String conc = "";

        for (Character c : userDefinedSeparatingCharacters) {
            patternBuilder.append(conc);
            patternBuilder.append(Pattern.quote(c.toString()));
            conc = "|"; // OR
        }

        return Pattern.compile(patternBuilder.toString());
    }
}
