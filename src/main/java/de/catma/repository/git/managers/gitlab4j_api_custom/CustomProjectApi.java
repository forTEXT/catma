package de.catma.repository.git.managers.gitlab4j_api_custom;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.gitlab4j.api.AbstractApi;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.Project;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.catma.util.Pair;

public class CustomProjectApi extends AbstractApi {

	private ExecutorService parallelExecutor;
	
	public CustomProjectApi(GitLabApi gitLabApi, ExecutorService parallelExecutor) {
		super(gitLabApi);
		this.parallelExecutor = parallelExecutor;
	}

    public List<Member> getAllMembers(Integer projectId) throws GitLabApiException {
        Response response = get(Response.Status.OK, this.getDefaultPerPageParam(), "projects", projectId, "members","all");
        return (response.readEntity(new GenericType<List<Member>>() {}));
    }
    
    public Map<String, AccessLevel> getResourcePermissions(Integer projectId) throws GitLabApiException {
    	CompletionService<Pair<Integer,Set<Project>>> cs = new ExecutorCompletionService<>(parallelExecutor);
    	List<Integer> accessLevels = Lists.newArrayList(
    			AccessLevel.GUEST.value,
    			AccessLevel.REPORTER.value,
    			AccessLevel.DEVELOPER.value,
    			AccessLevel.MAINTAINER.value,
    			AccessLevel.OWNER.value
    			);
        for (Integer level: accessLevels){
        	cs.submit(new Callable<Pair<Integer, Set<Project>>>(
        			) {
						@Override
						public Pair<Integer,Set<Project>> call() throws Exception {
					    	MultivaluedMap<String, String> params = CustomProjectApi.this.getDefaultPerPageParam();
					    	params.add("min_access_level", level.toString());
					        Response response = get(Response.Status.OK, params, "projects");
					        List<Project> projects = response.readEntity(new GenericType<List<Project>>() {});
					        ;
							return new Pair<>(level, projects
									.stream()
									.filter(p -> 
										p.getNamespace().getId().intValue() == projectId)
									.collect(Collectors.toSet()));		
					}
			});
        }
        
        Map<String, AccessLevel> resultMap = Maps.newConcurrentMap();
        try {
	        int n = accessLevels.size();
	        for (int i = 0; i < n; ++i) {
	        	Pair<Integer, Set<Project>> response;
					response = cs.take().get();
				
	            if (response != null){
	            	for(Project p : response.getSecond()){
	            		if(! resultMap.containsKey(p.getName()) || resultMap.get(p.getName()).value < response.getFirst()){
	            			resultMap.put(p.getName(), AccessLevel.forValue(response.getFirst()));
	            		}
	            	}
	            	response.getFirst();
	            }
	        }
        } catch (InterruptedException e) {
        	throw new GitLabApiException(e);
        } catch (ExecutionException e) {
        	throw new GitLabApiException(e);
        }
        return resultMap;
    }
}
