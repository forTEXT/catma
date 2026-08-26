package de.catma.repository.git;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import org.gitlab4j.api.EnhancedPager;

import de.catma.project.BackendPager;

public class GitPager<X, T> implements BackendPager<T> {
	
	private EnhancedPager<X> delegate;
	private Function<X, T> typeMapper;
	
	public GitPager(EnhancedPager<X> delegate, Function<X, T> typeMapper) {
		super();
		this.delegate = delegate;
		this.typeMapper = typeMapper;
	}

	@Override
	public List<T> next() {
		return delegate.next().stream().map(typeMapper).toList();
	}

	// EnhancedPager wraps any GitLabApiException in a RuntimeException, we translate that back into an IOException where the BackendPager interface allows
	// it, so that callers keep seeing backend failures as IOExceptions
	@Override
	public List<T> first() throws IOException {
		try {
			return delegate.first().stream().map(typeMapper).toList();
		} catch (RuntimeException e) {
			throw new IOException(e);
		}
	}

	@Override
	public List<T> current() throws IOException {
		try {
			return delegate.current().stream().map(typeMapper).toList();
		} catch (RuntimeException e) {
			throw new IOException(e);
		}
	}

	@Override
	public List<T> page(int pageNumber) {
		return delegate.page(pageNumber).stream().map(typeMapper).toList();
	}

	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public int getCurrentPage() {
		return delegate.getCurrentPage();
	}
	
	@Override
	public int nextPage() {
		return delegate.getNextPage();
	}
}
