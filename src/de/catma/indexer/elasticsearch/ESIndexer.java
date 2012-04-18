package de.catma.indexer.elasticsearch;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.Response;

import de.catma.core.document.Range;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.tag.TagLibrary;
import de.catma.indexer.Indexer;
import de.catma.indexer.TermInfo;
import de.catma.indexer.WhitespaceAndPunctuationAnalyzer;
import de.catma.indexer.unseparablecharactersequence.CharTree;
import de.catma.indexer.unseparablecharactersequence.CharTreeFactory;

public class ESIndexer implements Indexer {

	private ESCommunication esComm;
	private Logger logger = LoggerFactory.getLogger(ESInstaller.class);

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
		esComm.indexTerms(sourceDoc.getID(), terms);
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
		logger.info("searching...");
		if (termList == null)
			return null;

		List<ESTermIndexDocument> termids = getTermIds(documentIdList, termList);
		Map<String, List<ESTermIndexDocument>> termIdsPerTerm = new HashMap<String, List<ESTermIndexDocument>>();
		Map<String, Map<Integer, ESPositionIndexDocument>> termOffsets = new HashMap<String, Map<Integer, ESPositionIndexDocument>>();
		Map<String, List<Range>> result = new HashMap<String, List<Range>>();

		for (ESTermIndexDocument term : termids) {
			termIdsPerTerm.containsKey(term.getTerm());
			List<ESTermIndexDocument> tdocs = termIdsPerTerm
					.get(term.getTerm());
			if (tdocs == null) {
				tdocs = new ArrayList<ESTermIndexDocument>();
			}
			tdocs.add(term);
			termIdsPerTerm.put(term.getTerm(), tdocs);
		}
		String lastTerm = termList.get(0);

		for (String currentTerm : termList) {

			HashMap<ESTermIndexDocument, Future<Response>> httpRequests = new HashMap<ESTermIndexDocument, Future<Response>>();

			for (ESTermIndexDocument term : termIdsPerTerm.get(currentTerm)) {

				logger.info("fetching positions for term " + term.getTerm()
						+ " from: " + term.getDocumentId());
				JSONObject j_query = new JSONObject();
				j_query.put("from", 0);
				j_query.put("size", 1000);
				JSONObject j_bool = new JSONObject();
				j_query.put("query", j_bool);
				JSONObject j_should = new JSONObject();
				j_bool.put("bool", j_should);
				JSONArray j_should_arr = new JSONArray();
				j_should.put("should", j_should_arr);

				j_should_arr.put(new JSONObject().put("term", new JSONObject()
						.put("termId_l", term.getTermId()
								.getLeastSignificantBits())));
				j_should_arr.put(new JSONObject().put("term", new JSONObject()
						.put("termId_m", term.getTermId()
								.getMostSignificantBits())));
				if (termOffsets.containsKey(lastTerm)) {
					List<Integer> relevantoffsets = new ArrayList<Integer>();
					for (ESPositionIndexDocument pos : termOffsets
							.get(lastTerm).values()) {
						if (pos.getDocumentId().equals(term.getDocumentId())) {
							relevantoffsets.add(pos.getTokenOffset() + 1);
						}
					}
					logger.info("ok filter added for: " + currentTerm);
					logger.info("filter: "
							+ Arrays.toString(relevantoffsets.toArray()));
					j_should_arr.put(new JSONObject().put("terms",
							new JSONObject().put("tokenoffset", new JSONArray(
									relevantoffsets))));
					j_should.put("minimum_number_should_match", 3); // number of
																	// items:
																	// termId_m,
																	// termId_l,
																	// tokenoffset
				} else {
					j_should.put("minimum_number_should_match", 2); // number of
																	// items:
																	// termId_m,
																	// termId_l
				}

				logger.info("req: " + j_query.toString());
				Future<Response> f = esComm.httpTransport
						.preparePost(
								esComm.positionIndexUrl() + "/" + "_search")
						.setBody(j_query.toString()).execute();
				httpRequests.put(term, f);
			}
			ESCommunication.waitForRequests(httpRequests.values());

			for (Map.Entry<ESTermIndexDocument, Future<Response>> entry : httpRequests
					.entrySet()) {
				Response r = entry.getValue().get();
				JSONObject hitdoc = new JSONObject(r.getResponseBody());
				if (hitdoc.has("hits")) {
					JSONObject hits0 = hitdoc.getJSONObject("hits");
					JSONArray hits = hits0.getJSONArray("hits");
					Map<Integer, ESPositionIndexDocument> offsets = new HashMap<Integer, ESPositionIndexDocument>();
					for (int i = 0; i < hits.length(); i++) {
						JSONObject j = hits.getJSONObject(i);
						JSONObject source = j.getJSONObject("_source");
						ESPositionIndexDocument position = ESPositionIndexDocument
								.fromJSON(source);
						logger.info("term[" + entry.getKey().getTerm() + "]@"
								+ position.getTokenOffset());
						offsets.put(position.getTokenOffset(), position);
					}
					if (termOffsets.containsKey(entry.getKey().getTerm())) {
						offsets.putAll(termOffsets
								.get(entry.getKey().getTerm()));
					}
					termOffsets.put(entry.getKey().getTerm(), offsets);
				}
			}
			lastTerm = currentTerm;
		}
		// build resultset, very cheap
		for (ESPositionIndexDocument pos : termOffsets.get(lastTerm).values()) {
			int offset = pos.getTokenOffset() - (termList.size() - 1);

			Map<Integer, ESPositionIndexDocument> firstPositions = termOffsets
					.get(termList.get(0));
			ESPositionIndexDocument firstmatch = firstPositions.get(offset);

			Range r = new Range(firstmatch.getRange().getStartPoint(), pos
					.getRange().getEndPoint());
			List<Range> ranges = result.get(pos.getDocumentId());
			if (ranges == null) {
				ranges = new ArrayList<Range>();
			}
			ranges.add(r);
			result.put(pos.getDocumentId(), ranges);
		}
		return result;
	}

	private List<ESTermIndexDocument> getTermIds(List<String> documentIdList,
			List<String> termList) throws Exception {
		logger.info("fetching termids from termindex");
		if (termList == null)
			return null;

		List<Future<Response>> httpRequests = new ArrayList<Future<Response>>();

		for (String term : termList) {
			JSONObject searchobj = new JSONObject();
			searchobj.put("from", 0);
			searchobj.put("size", 1000);
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
			httpRequests.add(f);
		}
		ESCommunication.waitForRequests(httpRequests);

		List<ESTermIndexDocument> termids = new ArrayList<ESTermIndexDocument>();

		for (Future<Response> f : httpRequests) {
			Response r = f.get();
			JSONObject hitdoc = new JSONObject(r.getResponseBody());
			JSONArray hits = hitdoc.getJSONObject("hits").getJSONArray("hits");
			for (int i = 0; i < hits.length(); i++) {
				JSONObject j = hits.getJSONObject(i);
				ESTermIndexDocument termdoc = ESTermIndexDocument.fromJSON(j
						.getJSONObject("_source"));
				logger.info("term found: " + termdoc.getTerm() + "@"
						+ termdoc.getTermId());
				termids.add(termdoc);
			}
		}

		return termids;
	}

	public void index(List<TagReference> tagReferences,
			String sourceDocumentID, String userMarkupCollectionID,
			TagLibrary tagLibrary) throws Exception {
		List<ESTagReferenceDocument> esTagReferences = new ArrayList<ESTagReferenceDocument>();
		for(TagReference tagReference : tagReferences ){
			esTagReferences.add(new ESTagReferenceDocument(sourceDocumentID, userMarkupCollectionID, tagReference, tagLibrary));			
		}
		esComm.indexTagReferences(esTagReferences);
	}
	
	/**
	 * Closes the async http transport client
	 */
	public void close() {
		this.esComm.close();
	}
	
}
