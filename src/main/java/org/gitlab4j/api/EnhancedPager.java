package org.gitlab4j.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.gitlab4j.api.utils.JacksonJson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>This class defines an Iterator implementation that is used as a paging iterator for all API methods that
 * return a List of objects.  It hides the details of interacting with the GitLab API when paging is involved
 * simplifying accessing large lists of objects.</p>
 *
 * <p>Example usage:</p>
 *
 * <pre>
 *   // Get a Pager instance that will page through the projects with 10 projects per page
 *   Pager&lt;Project&gt; projectPager = gitlabApi.getProjectsApi().getProjectsPager(10);
 *
 *   // Iterate through the pages and print out the name and description
 *   while (projectsPager.hasNext())) {
 *       List&lt;Project&gt; projects = projectsPager.next();
 *       for (Project project : projects) {
 *           System.out.println(project.getName() + " : " + project.getDescription());
 *       }
 *   }
 * </pre>
 *
 * <p><b>NB: this is a CATMA copy of gitlab4j-api's {@link Pager}, taken from version 5.8.1.</b> It lives in gitlab4j's own package because it needs
 * package-private members ({@link GitLabApiForm}, {@link AbstractApi#get}). It exists because upstream's implementation is unusable against endpoints that
 * don't return the <code>X-Total</code>/<code>X-Total-Pages</code> headers, which GitLab dropped for the commits endpoint (see
 * <a href="https://gitlab.com/gitlab-org/gitlab/-/merge_requests/43159">gitlab-org MR 43159</a>). Upstream reads those headers into
 * <code>totalPages</code>/<code>totalItems</code> and falls back to <code>X-Next-Page</code> only for <code>hasNext()</code>, leaving both totals at
 * <code>-1</code> and every other member that consults them broken - see the comments in <code>page()</code> below for the resulting failure, which is why
 * this copy tracks <code>X-Next-Page</code> throughout instead. <code>getTotalPages()</code>, <code>getTotalItems()</code>, <code>last()</code> and
 * <code>all()</code> are omitted for the same reason: without a known total they cannot be implemented.
 *
 * <p>Everything apart from that is a verbatim copy. When updating gitlab4j-api, diff this class against the new <code>Pager</code> and port any upstream
 * changes across (between 5.0.1 and 5.8.1 there were none of substance).
 *
 * @param <T> the GitLab4J type contained in the List.
 */
public class EnhancedPager<T> implements Iterator<List<T>>, Constants {

    private int itemsPerPage;
    private int currentPage;
    // CATMA: replaces upstream's totalPages/totalItems/kaminariNextPage
    private int nextPage;

    private List<String> pageParam = new ArrayList<>(1);
    private List<T> currentItems;
    private Stream<T> pagerStream = null;

    private AbstractApi api;
    private MultivaluedMap<String, String> queryParams;
    private Object[] pathArgs;

    private static JacksonJson jacksonJson = new JacksonJson();
    private static ObjectMapper mapper = jacksonJson.getObjectMapper();
    private JavaType javaType;

    /**
     * Creates a Pager instance to access the API through the specified path and query parameters.
     *
     * @param api the AbstractApi implementation to communicate through
     * @param type the GitLab4J type that will be contained in the List
     * @param itemsPerPage items per page
     * @param queryParams HTTP query params
     * @param pathArgs HTTP path arguments
     * @throws GitLabApiException if any error occurs
     */
    public EnhancedPager(
            AbstractApi api,
            Class<T> type,
            int itemsPerPage,
            MultivaluedMap<String, String> queryParams,
            Object... pathArgs)
            throws GitLabApiException {

        javaType = mapper.getTypeFactory().constructCollectionType(List.class, type);

        if (itemsPerPage < 1) {
            itemsPerPage = api.getDefaultPerPage();
        }

        // Make sure the per_page parameter is present
        if (queryParams == null) {
            queryParams =
                    new GitLabApiForm().withParam(PER_PAGE_PARAM, itemsPerPage).asMap();
        } else {
            queryParams.remove(PER_PAGE_PARAM);
            queryParams.add(PER_PAGE_PARAM, Integer.toString(itemsPerPage));
        }

        // Set the page param to 1
        pageParam = new ArrayList<>();
        pageParam.add("1");
        queryParams.put(PAGE_PARAM, pageParam);
        Response response = api.get(Response.Status.OK, queryParams, pathArgs);

        try {
            currentItems = mapper.readValue((InputStream) response.getEntity(), javaType);
        } catch (Exception e) {
            throw new GitLabApiException(e);
        }

        if (currentItems == null) {
            throw new GitLabApiException("Invalid response from from GitLab server");
        }

        this.api = api;
        this.queryParams = queryParams;
        this.pathArgs = pathArgs;
        this.itemsPerPage = getIntHeaderValue(response, PER_PAGE);

        // Some API endpoints do not return the "X-Per-Page" header when there is only 1 page, check for that condition
        // and act accordingly
        if (this.itemsPerPage == -1) {
            this.itemsPerPage = itemsPerPage;
            return;
        }

        // CATMA: GitLab's commits endpoint no longer returns the X-Total/X-Total-Pages headers, see
        // https://gitlab.com/gitlab-org/gitlab/-/merge_requests/43159 - we track X-Next-Page instead
        nextPage = getIntHeaderValue(response, NEXT_PAGE_HEADER);
    }

    /**
     * Get the specified header value from the Response instance.
     *
     * @param response the Response instance to get the value from
     * @param key the HTTP header key to get the value for
     * @return the specified header value from the Response instance, or null if the header is not present
     * @throws GitLabApiException if any error occurs
     */
    private String getHeaderValue(Response response, String key) throws GitLabApiException {

        String value = response.getHeaderString(key);
        value = (value != null ? value.trim() : null);
        if (value == null || value.length() == 0) {
            return (null);
        }

        return (value);
    }

    /**
     * Get the specified integer header value from the Response instance.
     *
     * @param response the Response instance to get the value from
     * @param key the HTTP header key to get the value for
     * @return the specified integer header value from the Response instance, or -1 if the header is not present
     * @throws GitLabApiException if any error occurs
     */
    private int getIntHeaderValue(Response response, String key) throws GitLabApiException {

        String value = getHeaderValue(response, key);
        if (value == null) {
            return -1;
        }

        try {
            return (Integer.parseInt(value));
        } catch (NumberFormatException nfe) {
            throw new GitLabApiException("Invalid '" + key + "' header value (" + value + ") from server");
        }
    }

    /**
     * Sets the "page" query parameter.
     *
     * @param page the value for the "page" query parameter
     */
    private void setPageParam(int page) {
        pageParam.set(0, Integer.toString(page));
        queryParams.put(PAGE_PARAM, pageParam);
    }

    /**
     * Get the items per page value.
     *
     * @return the items per page value
     */
    public int getItemsPerPage() {
        return (itemsPerPage);
    }

    /**
     * Get the current page of the iteration.
     *
     * @return the current page of the iteration
     */
    public int getCurrentPage() {
        return (currentPage);
    }

    /**
     * Returns the true if there are additional pages to iterate over, otherwise returns false.
     *
     * @return true if there are additional pages to iterate over, otherwise returns false
     */
    @Override
    public boolean hasNext() {
        return (currentPage < nextPage);
    }

    /**
     * Returns the next List in the iteration containing the next page of objects.
     *
     * @return the next List in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     * @throws RuntimeException if a GitLab API error occurs, will contain a wrapped GitLabApiException with the details of the error
     */
    @Override
    public List<T> next() {
        return (page(currentPage + 1));
    }

    /**
     * This method is not implemented and will throw an UnsupportedOperationException if called.
     *
     * @throws UnsupportedOperationException when invoked
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the first page of List. Will rewind the iterator.
     *
     * @return the first page of List
     */
    public List<T> first() {
        return (page(1));
    }

    /**
     * Returns the previous page of List. Will set the iterator to the previous page.
     *
     * @return the previous page of List
     */
    public List<T> previous() {
        return (page(currentPage - 1));
    }

    /**
     * Returns the current page of List.
     *
     * @return the current page of List
     */
    public List<T> current() {
        return (page(currentPage));
    }

    /**
     * Returns the specified page of List.
     *
     * @param pageNumber the page to get
     * @return the specified page of List
     * @throws NoSuchElementException if the iteration has no more elements
     * @throws RuntimeException if a GitLab API error occurs, will contain a wrapped GitLabApiException with the details of the error
     */
    public List<T> page(int pageNumber) {

        if (currentPage == 0 && pageNumber == 1) {
            currentPage = 1;
            return (currentItems);
        }

        if (currentPage == pageNumber) {
            return (currentItems);
        }

        // CATMA: upstream throws NoSuchElementException here if 'pageNumber > totalPages && pageNumber > kaminariNextPage'.
        // With the X-Total headers absent totalPages stays at -1, so the left operand is true for every valid page number
        // and the check collapses to 'pageNumber > kaminariNextPage'. That rejects any forward jump of more than one page
        // (ProjectEventPanel allows those, see setAllowInputPastLastPageNumber) and, once the last page is reached and
        // kaminariNextPage becomes -1, every page number. There is no total to validate against, so we drop the check.

        try {

            setPageParam(pageNumber);
            Response response = api.get(Response.Status.OK, queryParams, pathArgs);
            currentItems = mapper.readValue((InputStream) response.getEntity(), javaType);
            currentPage = pageNumber;

            nextPage = getIntHeaderValue(response, NEXT_PAGE_HEADER);

            return (currentItems);

        } catch (GitLabApiException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the value of the X-Next-Page header of the most recently fetched page, or -1 if there is no next page.
     *
     * @return the index of the next page, or -1
     */
    public int getNextPage() {
        return (nextPage);
    }

    /**
     * Builds and returns a Stream instance which is pre-populated with all items from all pages.
     *
     * @return a Stream instance which is pre-populated with all items from all pages
     * @throws IllegalStateException if Stream has already been issued
     * @throws GitLabApiException if any other error occurs
     */
    public Stream<T> stream() throws GitLabApiException, IllegalStateException {

        if (pagerStream == null) {
            synchronized (this) {
                if (pagerStream == null) {

                    // Make sure that current page is 0, this will ensure the whole list is streamed
                    // regardless of what page the instance is currently on.
                    currentPage = 0;

                    // Create a Stream.Builder to contain all the items. This is more efficient than
                    // getting a List with all() and streaming that List
                    Stream.Builder<T> streamBuilder = Stream.builder();

                    // Iterate through the pages and append each page of items to the stream builder
                    while (hasNext()) {
                        next().forEach(streamBuilder);
                    }

                    pagerStream = streamBuilder.build();
                    return (pagerStream);
                }
            }
        }

        throw new IllegalStateException("Stream already issued");
    }

    // CATMA: lazyStream() is omitted because upstream's PagerSpliterator takes a Pager, which this class does not extend.

}
