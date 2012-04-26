package de.catma.indexer.elasticsearch;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ning.http.client.Response;

public class ESDocumentIterator<T> implements
		Iterator<T>, Iterable<T> {

	T current;
	ESDocumentFactory<T> factory;
	protected JSONArray hits;
	protected int cursor;
	protected int size;

	public ESDocumentIterator(Future<Response> response,
			ESDocumentFactory<T> factory) {
		this.factory = factory;
		Response r = null;

		try {
			r = response.get();
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (ExecutionException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		JSONObject hitdoc = null;
		try {
			hitdoc = new JSONObject(r.getResponseBody());
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			JSONObject hitsobj = hitdoc.getJSONObject("hits");
			this.hits = hitsobj.getJSONArray("hits");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.size = hits.length();
		this.cursor = 0;
	}

	public Iterator<T> iterator() {
		return this;
	}

	public boolean hasNext() {
		if (hits == null) {
			return false;
		}

		try {
			this.current = factory.fromJSON(hits.getJSONObject(cursor)
					.getJSONObject("_source"));
		} catch (JSONException e) {
			// TODO: logging!!!
			this.current = null;
			return false;
		}
		return true;

	}

	public T next() {
		if (current == null) {
			throw new NoSuchElementException();
		}
		cursor++;
		return this.current;
	}

	public void remove() {
	}

}
