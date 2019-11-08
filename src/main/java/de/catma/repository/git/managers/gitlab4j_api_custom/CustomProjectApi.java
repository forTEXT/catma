package de.catma.repository.git.managers.gitlab4j_api_custom;

import java.util.List;
import java.util.Map;
import java.util.Set;
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

public class CustomProjectApi extends AbstractApi {

	public CustomProjectApi(GitLabApi gitLabApi) {
		super(gitLabApi);
	}

    public List<Member> getAllMembers(Integer projectId) throws GitLabApiException {
        Response response = get(Response.Status.OK, this.getDefaultPerPageParam(), "projects", projectId, "members","all");
        return (response.readEntity(new GenericType<List<Member>>() {}));
    }
    
    public Map<String, AccessLevel> getResourcePermissions(Integer projectId) throws GitLabApiException {
    	List<Integer> accessLevels = Lists.newArrayList(
    			AccessLevel.GUEST.value,
    			AccessLevel.REPORTER.value,
    			AccessLevel.DEVELOPER.value,
    			AccessLevel.MAINTAINER.value,
    			AccessLevel.OWNER.value);
        Map<String, AccessLevel> resultMap = Maps.newHashMap();

        for (final Integer level: accessLevels) {
	    	MultivaluedMap<String, String> params = CustomProjectApi.this.getDefaultPerPageParam();
	    	params.add("min_access_level", level.toString());
	        Response response = get(Response.Status.OK, params, "projects");
	        List<Project> resourceAndContainerProjects = response.readEntity(new GenericType<List<Project>>() {});
	        Set<Project> filteredOnGroupProjects = 
	        	resourceAndContainerProjects
					.stream()
					.filter(p -> 
						p.getNamespace().getId().equals(projectId)) // projectId is the GitLab namespace/groupId
					.collect(Collectors.toSet());
					
			for (Project p : filteredOnGroupProjects) {
        		if(! resultMap.containsKey(p.getName()) 
            			|| resultMap.get(p.getName()).value.intValue() < level.intValue()) {
            			
        			resultMap.put(p.getName(), AccessLevel.forValue(level));
        		}
			}
        }
        
        return resultMap;
    }
}
