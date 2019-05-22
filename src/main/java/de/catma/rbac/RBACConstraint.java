package de.catma.rbac;

import java.util.function.Predicate;

/**
 * A constraint that executes a certain task if condition is met or not at runtime.
 * <p>
 * <code>
 * RBACConstraint rbacConstraint = RBACConstraint.ifNotAuthorized((project) -> 
        			(remoteGitManager.isAuthorizedOnProject(remoteGitManager.getUser(), RBACPermission.PROJECT_MEMBERS_EDIT, project.getProjectId())),
        			() -> aComponent.setVisible(false))
        		);
 * </code>
 * </p>
 * At a later point when proj is available e.g. data has been loaded we enforce all conditions by executing.
 * <p>
 * <code>
 * rbacConstraint.enforce(projectReference);
 * </code>
 * </p>
 * @author db
 *
 * @param <T>
 */
public class RBACConstraint<T> {

	private final Predicate<T> authorizationCondition;
	private final Runnable task;
	
	/**
	 * Factory method to create an instance of {@link RBACConstraint}
	 * If <code>authorizationCondition</code> evaluates to true then <code>task</code> get's executed at runtime 
	 * when {@link #enforce(T t)} has been called. 
	 * @param authorizationCondition
	 * @param task
	 * @return instance of 
	 */
	public static <T> RBACConstraint<T> ifAuthorized(Predicate<T> authorizationCondition, Runnable task){
		return new RBACConstraint<>(authorizationCondition, task);
	}
	
	/**
	 * Factory method to create an instance of {@link RBACConstraint}
	 * If <code>authorizationCondition</code> evaluates to false then <code>task</code> get's executed at runtime 
	 * when {@link #enforce(T t)} has been called. 
	 * @param authorizationCondition
	 * @param task
	 * @return instance of 
	 */
	public static <T> RBACConstraint<T> ifNotAuthorized(Predicate<T> authorizationCondition, Runnable task){
		return new RBACConstraint<>(authorizationCondition.negate(),task);
	}
	
	private RBACConstraint(Predicate<T> authorizationCondition, Runnable task){
		this.authorizationCondition = authorizationCondition;
		this.task = task;
	}
	
	/**
	 * Tests the <code>authorizationCondition</code> with the runtime parameter <code>t</code> and executes the <code>task</code> if condition was met.  
	 * @param t
	 */
	public void enforce(T t) {
		if(authorizationCondition.test(t)){
			task.run();
		}
	}
	
}
