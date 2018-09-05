package de.catma.repository.git.managers;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.catma.Pager;

public class GitLabPager<T, S> implements Pager<T> {

	private org.gitlab4j.api.Pager<S> delegate;
	private Function<S, T> mapper;

	public GitLabPager(org.gitlab4j.api.Pager<S> delegate, Function<S, T> mapper) {
		super();
		this.delegate = delegate;
		this.mapper = mapper;
	}

	public int getItemsPerPage() {
		return delegate.getItemsPerPage();
	}

	public int getTotalPages() {
		return delegate.getTotalPages();
	}

	public int getTotalItems() {
		return delegate.getTotalItems();
	}

	public int getCurrentPage() {
		return delegate.getCurrentPage();
	}

	public boolean hasNext() {
		return delegate.hasNext();
	}

	public List<T> next() {
		return delegate.next().stream().map(mapper).collect(Collectors.toList());
	}

	public List<T> first() throws Exception {
		return delegate.first().stream().map(mapper).collect(Collectors.toList());
	}

	public List<T> last() throws Exception {
		return delegate.last().stream().map(mapper).collect(Collectors.toList());
	}

	public List<T> previous() throws Exception {
		return delegate.previous().stream().map(mapper).collect(Collectors.toList());
	}

	public List<T> current() throws Exception {
		return delegate.current().stream().map(mapper).collect(Collectors.toList());
	}

	public List<T> page(int pageNumber) {
		return delegate.page(pageNumber).stream().map(mapper).collect(Collectors.toList());
	}
	
	
	
}
