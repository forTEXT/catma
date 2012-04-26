package de.catma.indexer.elasticsearch;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.Response;

import de.catma.core.document.Range;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.tag.TagLibrary;
import de.catma.core.util.IDGenerator;
import de.catma.core.util.Pair;
import de.catma.indexer.Indexer;
import de.catma.indexer.TermInfo;
import de.catma.indexer.WhitespaceAndPunctuationAnalyzer;
import de.catma.indexer.unseparablecharactersequence.CharTree;
import de.catma.indexer.unseparablecharactersequence.CharTreeFactory;
import de.catma.queryengine.QueryResultRow;
import de.catma.queryengine.QueryResultRowArray;

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
				Map<Integer, ESPositionIndexDocument> offsets = new HashMap<Integer, ESPositionIndexDocument>();

				for (ESPositionIndexDocument position : new ESDocumentIterator<ESPositionIndexDocument>(
						entry.getValue(), new ESPositionIndexDocumentFactory())) {

					logger.info("term[" + entry.getKey().getTerm() + "]@"
							+ position.getTokenOffset());
					offsets.put(position.getTokenOffset(), position);
				}
				if (termOffsets.containsKey(entry.getKey().getTerm())) {
					offsets.putAll(termOffsets.get(entry.getKey().getTerm()));
				}
				termOffsets.put(entry.getKey().getTerm(), offsets);
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
			searchTerm(documentIdList, term, httpRequests);
		}
		ESCommunication.waitForRequests(httpRequests);

		List<ESTermIndexDocument> termids = new ArrayList<ESTermIndexDocument>();

		for (Future<Response> req : httpRequests) {
			for (ESTermIndexDocument est : new ESDocumentIterator<ESTermIndexDocument>(
					req, new ESTermIndexDocumentFactory())) {
				logger.info("term found: " + est.getTerm() + "@"
						+ est.getTermId());
				termids.add(est);
			}
		}

		return termids;
	}

	public void index(List<TagReference> tagReferences,
			String sourceDocumentID, String userMarkupCollectionID,
			TagLibrary tagLibrary) throws Exception {
		List<ESTagReferenceDocument> esTagReferences = new ArrayList<ESTagReferenceDocument>();
		for (TagReference tagReference : tagReferences) {
			esTagReferences.add(new ESTagReferenceDocument(sourceDocumentID,
					userMarkupCollectionID, tagReference, tagLibrary));
		}
		esComm.indexTagReferences(esTagReferences);
	}

	public QueryResultRowArray searchTag(String tagPath, boolean isPrefixSearch)
			throws Exception {

		logger.info("fetching tags from tagreferenceindex");
		if (tagPath == null || tagPath.isEmpty())
			return null;

		List<Future<Response>> httpRequests = new ArrayList<Future<Response>>();

		JSONObject searchobj = new JSONObject();
		searchobj.put("from", 0);
		searchobj.put("size", 10000);
		JSONObject queryobj = new JSONObject();
		searchobj.put("query", queryobj);
		if (isPrefixSearch == false) {
			queryobj.put("term", new JSONObject().put("tagPath", tagPath));
		} else {
			queryobj.put("prefix", new JSONObject().put("tagPath", tagPath));
		}
		logger.info(searchobj.toString());
		Future<Response> f = esComm.httpTransport
				.preparePost(esComm.tagReferenceIndexUrl() + "/" + "_search")
				.setBody(searchobj.toString()).execute();
		httpRequests.add(f);

		ESCommunication.waitForRequests(httpRequests);

		QueryResultRowArray results = new QueryResultRowArray();

		for (Future<Response> req : httpRequests) {
			for (ESTagReferenceDocument tagdoc : new ESDocumentIterator<ESTagReferenceDocument>(
					req, new ESTagReferenceDocumentFactory())) {
				logger.info("tag found: " + tagdoc);
				results.add(new QueryResultRow(tagdoc.getDocumentId(), tagdoc
						.getRange(), null, tagdoc.getUserMarkupCollectionId(),
						IDGenerator.UUIDToCatmaID(tagdoc.getTagDefinitionId()),
						IDGenerator.UUIDToCatmaID(tagdoc.getTagInstanceId())));
			}
		}

		return results;
	}

	public Map<String, Set<Range>> searchColocation(List<String> documentIds,
			String term1, String term2, int proximity) throws JSONException,
			IOException, InterruptedException, ExecutionException {
		// Sanity check input
		if (term1 == null || term1.isEmpty())
			return null;
		if (term2 == null || term2.isEmpty())
			return null;
		if (proximity <= 0)
			return null;

		// Vars
		List<Future<Response>> httpRequests = new ArrayList<Future<Response>>();
		Map<String, Set<Range>> result = new HashMap<String, Set<Range>>();
		Map<UUID, ESTermIndexDocument> termLookup = new HashMap<UUID, ESTermIndexDocument>();
		Map<String, Pair<ESTermIndexDocument, ESTermIndexDocument>> termPairs = new HashMap<String, Pair<ESTermIndexDocument, ESTermIndexDocument>>();
		Map<ESPositionIndexDocument, Future<Response>> positionPairs = new HashMap<ESPositionIndexDocument, Future<Response>>();

		searchTerm(documentIds, term1, httpRequests);
		searchTerm(documentIds, term2, httpRequests);

		ESCommunication.waitForRequests(httpRequests);

		TreeSet<ESTermIndexDocument> frequenciesOfTerms = new TreeSet<ESTermIndexDocument>(
				new Comparator<ESTermIndexDocument>() {
					public int compare(ESTermIndexDocument o1,
							ESTermIndexDocument o2) {
						return new Integer(o1.getFrequency()).compareTo(o2
								.getFrequency());
					};
				});

		// Fill pairs
		for (Future<Response> req : httpRequests) {
			for (ESTermIndexDocument est : new ESDocumentIterator<ESTermIndexDocument>(
					req, new ESTermIndexDocumentFactory())) {
				termLookup.put(est.getTermId(), est);
				Pair<ESTermIndexDocument, ESTermIndexDocument> pair = termPairs
						.get(est.getDocumentId());
				if (pair == null) {
					logger.info("add single pair: " + est.getTermId() + " "
							+ est.getTerm());
					termPairs.put(est.getDocumentId(),
							new Pair<ESTermIndexDocument, ESTermIndexDocument>(
									est, null));
				} else {
					termPairs.put(est.getDocumentId(),
							new Pair<ESTermIndexDocument, ESTermIndexDocument>(
									pair.getFirst(), est));
					logger.info("adding matching pair: "
							+ pair.getFirst().getDocumentId() + " "
							+ pair.getFirst().getTerm() + "<->" + est.getTerm());
				}
			}
		}

		// fill frequencies
		for (Map.Entry<String, Pair<ESTermIndexDocument, ESTermIndexDocument>> entry : termPairs
				.entrySet()) {
			ESTermIndexDocument first = entry.getValue().getFirst();
			ESTermIndexDocument second = entry.getValue().getSecond();

			if (second != null) {
				if (first.getFrequency() < second.getFrequency())
					frequenciesOfTerms.add(entry.getValue().getFirst());
				else
					frequenciesOfTerms.add(entry.getValue().getSecond());
			}
		}

		httpRequests = new ArrayList<Future<Response>>();

		for (ESTermIndexDocument term : frequenciesOfTerms) {
			httpRequests.add(searchPosition(term.getTermId()));
		}

		ESCommunication.waitForRequests(httpRequests);

		for (Future<Response> req : httpRequests) {
			ESPositionIndexDocumentFactory factory = new ESPositionIndexDocumentFactory();
			for (ESPositionIndexDocument pos : new ESDocumentIterator<ESPositionIndexDocument>(
					req, factory)) {
				ESTermIndexDocument l_term1 = termLookup.get(pos.getTermId());
				logger.info("first position found: " + pos.getRange());

				Pair<ESTermIndexDocument, ESTermIndexDocument> pair = termPairs
						.get(l_term1.getDocumentId());
				ESTermIndexDocument l_term2;
				if (l_term1.getTerm().equals(pair.getFirst().getTerm()))
					l_term2 = pair.getSecond();
				else
					l_term2 = pair.getFirst();

				Future<Response> fu = searchPosition(
						l_term2.getTermId(),
						new Range(pos.getTokenOffset() - proximity, pos
								.getTokenOffset() + proximity));
				positionPairs.put(pos, fu);
			}
		}

		ESCommunication.waitForRequests(positionPairs.values());

		for (Map.Entry<ESPositionIndexDocument, Future<Response>> positionResponse : positionPairs
				.entrySet()) {
			ESPositionIndexDocumentFactory factory = new ESPositionIndexDocumentFactory();
			for (ESPositionIndexDocument pos : new ESDocumentIterator<ESPositionIndexDocument>(
					positionResponse.getValue(), factory)) {
				ESTermIndexDocument l_term1 = termLookup.get(positionResponse
						.getKey().getTermId());
				ESTermIndexDocument l_term2 = termLookup.get(pos.getTermId());
				ESTermIndexDocument firstTerm;
				Range firstRange;
				logger.info("found second pos: " + pos.getRange());
				if (l_term1.getTerm().equals(term1)) {
					firstTerm = l_term1;
					firstRange = positionResponse.getKey().getRange();
				} else {
					firstTerm = l_term2;
					firstRange = pos.getRange();
				}

				Set<Range> ranges = result.get(firstTerm.getDocumentId());
				if (ranges == null)
					ranges = new HashSet<Range>();

				ranges.add(firstRange);
				result.put(firstTerm.getDocumentId(), ranges);
			}
		}

		for (Map.Entry<String, Set<Range>> entry : result.entrySet()) {
			for (Range range : entry.getValue()) {
				logger.info("Found matching condition in doc: "
						+ entry.getKey() + " " + range);
			}

		}
		return result;
	}

	private Future<Response> searchPosition(UUID termId) throws JSONException,
			IOException {
		Future<Response> f;
		// searchobj.put("from", 0);
		// searchobj.put("size", 10000);
		JSONObject queryObj_top = new JSONObject();
		JSONObject filtered = new JSONObject();
		JSONObject queryObj_filt = new JSONObject();
		queryObj_top.put("query", filtered);
		queryObj_top.put("from", 0);
		queryObj_top.put("size", 10000);

		filtered.put("filtered", queryObj_filt);
		queryObj_filt.put(
				"query",
				new JSONObject().put(
						"term",
						new JSONObject().put("termId_l",
								termId.getLeastSignificantBits())));
		JSONArray andFilter = new JSONArray();
		JSONObject filterObj = new JSONObject().put("and", andFilter);
		andFilter.put(
				0,
				new JSONObject().put(
						"term",
						new JSONObject().put("termId_m",
								termId.getMostSignificantBits())));

		queryObj_filt.put("filter", filterObj);
		logger.info("getting first positions from: "
				+ esComm.positionIndexUrl() + " using " + queryObj_top);
		f = esComm.httpTransport
				.preparePost(esComm.positionIndexUrl() + "/" + "_search")
				.setBody(queryObj_top.toString()).execute();

		return (f);
	}

	private Future<Response> searchPosition(UUID termId, Range range)
			throws JSONException, IOException {
		Future<Response> f;
		// searchobj.put("from", 0);
		// searchobj.put("size", 10000);
		JSONObject queryObj_top = new JSONObject();
		JSONObject filtered = new JSONObject();
		JSONObject queryObj_filt = new JSONObject();
		queryObj_top.put("query", filtered);
		queryObj_top.put("from", 0);
		queryObj_top.put("size", 10000);

		filtered.put("filtered", queryObj_filt);
		queryObj_filt.put(
				"query",
				new JSONObject().put(
						"term",
						new JSONObject().put("termId_l",
								termId.getLeastSignificantBits())));
		JSONArray andFilter = new JSONArray();
		JSONObject rangeObj = new JSONObject();
		rangeObj.put(
				"tokenoffset",
				new JSONObject().put("from", range.getStartPoint()).put("to",
						range.getEndPoint()));
		JSONObject filterObj = new JSONObject().put("and", andFilter);
		andFilter.put(0, new JSONObject().put("range", rangeObj));
		andFilter.put(
				1,
				new JSONObject().put(
						"term",
						new JSONObject().put("termId_m",
								termId.getMostSignificantBits())));

		queryObj_filt.put("filter", filterObj);
		logger.info("getting positions from: " + esComm.positionIndexUrl()
				+ " using " + queryObj_top);
		f = esComm.httpTransport
				.preparePost(esComm.positionIndexUrl() + "/" + "_search")
				.setBody(queryObj_top.toString()).execute();

		return (f);
	}

	private void searchTerm(List<String> documentIds, String term,
			List<Future<Response>> httpRequests) throws JSONException,
			IOException {
		JSONObject searchobj = new JSONObject();
		searchobj.put("from", 0);
		searchobj.put("size", 10000);
		searchobj.put("query", new JSONObject().put("term",
				new JSONObject().put("term", term)));
		if (documentIds != null) {
			searchobj.put("filter", new JSONObject().put("documentId",
					new JSONArray(documentIds)));
		}
		Future<Response> f = esComm.httpTransport
				.preparePost(esComm.termIndexUrl() + "/" + "_search")
				.setBody(searchobj.toString()).execute();

		httpRequests.add(f);
	}

	/**
	 * Closes the async http transport client
	 */
	public void close() {
		this.esComm.close();
	}

}
