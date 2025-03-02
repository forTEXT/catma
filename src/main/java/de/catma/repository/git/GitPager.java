package de.catma.repository.git;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import org.gitlab4j.api.EnhancedPager;
import org.gitlab4j.api.GitLabApiException;

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

	@Override
	public List<T> first() throws IOException {
		try {
			return delegate.first().stream().map(typeMapper).toList();
		} catch (GitLabApiException e) {
			throw new IOException(e);
		}
	}

	@Override
	public List<T> current() throws IOException {
		try {
			return delegate.current().stream().map(typeMapper).toList();
		} catch (GitLabApiException e) {
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
