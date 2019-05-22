package de.catma;

import java.util.Iterator;
import java.util.List;

public interface Pager<T> extends Iterator<List<T>> {

    public int getItemsPerPage();
    public int getTotalPages();
    public int getTotalItems();
    public int getCurrentPage();
    public List<T> first() throws Exception;
    public List<T> last() throws Exception;
    public List<T> previous() throws Exception;
    public List<T> current() throws Exception;
    public List<T> page(int pageNumber);
	
}
