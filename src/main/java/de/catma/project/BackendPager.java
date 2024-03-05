package de.catma.project;

import java.io.IOException;
import java.util.List;

public interface BackendPager<T> {
	
	
	public List<T> next();
	
	public List<T> first() throws IOException;
	
	public List<T> current() throws IOException;
	
	public List<T> page(int pageNumber);
	
	public boolean hasNext();

	int getCurrentPage();
	
	int nextPage();

}
