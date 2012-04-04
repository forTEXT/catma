package de.catma.indexer.elasticsearch;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.eclipse.jetty.util.ajax.JSON;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ning.http.client.Response;

import de.catma.core.document.Range;
import de.catma.core.document.source.SourceDocument;
import de.catma.indexer.Indexer;
import de.catma.indexer.TermInfo;
import de.catma.indexer.WhitespaceAndPunctuationAnalyzer;
import de.catma.indexer.unseparablecharactersequence.CharTree;
import de.catma.indexer.unseparablecharactersequence.CharTreeFactory;

public class ESIndexer implements Indexer {

	private ESCommunication esComm;

	public ESIndexer() {
		esComm = new ESCommunication();
	}

	public void index(SourceDocument sourceDoc,
			List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters, Locale locale)
			throws Exception {
		// just in case something went wrong, better use the default than
		// nothing
		if (locale == null) {
			locale = Locale.getDefault();
		}

		CharTreeFactory ctf = new CharTreeFactory();
		CharTree unseparableCharSeqTree = ctf
				.createCharMap(unseparableCharacterSequences);

		WhitespaceAndPunctuationAnalyzer analyzer = new WhitespaceAndPunctuationAnalyzer(
				unseparableCharSeqTree,
				buildPatternFrom(userDefinedSeparatingCharacters), locale);

		TokenStream ts = analyzer.tokenStream(null, // our analyzer does not use
													// the fieldname
				new StringReader(sourceDoc.getContent()));

		Map<String, List<TermInfo>> terms = new HashMap<String, List<TermInfo>>();

		int positionCounter = 0;
		while (ts.incrementToken()) {
			CharTermAttribute termAttr = (CharTermAttribute) ts
					.getAttribute(CharTermAttribute.class);

			OffsetAttribute offsetAttr = (OffsetAttribute) ts
					.getAttribute(OffsetAttribute.class);

			TermInfo ti = new TermInfo(termAttr.toString(),
					offsetAttr.startOffset(), offsetAttr.endOffset(),
					positionCounter);

			if (!terms.containsKey(ti.getTerm())) {
				terms.put(ti.getTerm(), new ArrayList<TermInfo>());
			}
			terms.get(ti.getTerm()).add(ti);
			positionCounter++;
			System.out.println(ti);
		}
		esComm.addToIndex(sourceDoc.getID(), terms);
	}

	/**
	 * Creates an OR-ed regex pattern from the list of user defined separating
	 * characters.
	 * 
	 * @param userDefinedSeparatingCharacters
	 *            the list of user defined separating characters
	 * @return the pattern
	 */
	private Pattern buildPatternFrom(
			List<Character> userDefinedSeparatingCharacters) {

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

	public Map<String, List<Range>> searchTerm(List<String> documentIdList,
			List<String> termList) throws Exception {

		if (termList == null)
			return null;
		
		List<Future<Response>> httpRequests = new ArrayList<Future<Response>>();

		for (String term : termList) {
			JSONObject searchobj = new JSONObject();
			searchobj.put(
					"query",
					new JSONObject().put("term",
							new JSONObject().put("term", term)));
			if (documentIdList != null) {
				searchobj.put("filter", new JSONObject().put("documentId",
						new JSONArray(documentIdList)));
			}
			Future<Response> f = esComm.httpTransport
					.preparePost(esComm.termIndexUrl() + "/" + "_search")
					.setBody(searchobj.toString()).execute();
			Response r =  f.get();
			JSONObject hitdoc = new JSONObject(r.getResponseBody());
			JSONArray hits = hitdoc.getJSONObject("hits").getJSONArray("hits");
			
			for(int i=0;i< hits.length();i++){
				JSONObject j = hits.getJSONObject(i);
				String termId = j.getString("_id");
				List<ESPositionIndexDocument> positions = this.getPositions(termId);
			}
		}
		return null;
	}

	private List<ESPositionIndexDocument> getPositions(String termId) {
		// TODO Auto-generated method stub
		return null;
	}

}
