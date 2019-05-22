package de.catma.rbac;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link RBACConstraintEnforcer} can register multiple {@link RBACConstraint} and enforce them 
 * at a later point when <code>T t</code> is available e.g. when data is ready usually reload() methods.
 * <p>
 * <code>
 *       rbacEnforcer.register(
 *       		RBACConstraint.ifNotAuthorized((project) -> 
 *       			(remoteGitManager.isAuthorizedOnProject(remoteGitManager.getUser(),RBACPermission.PROJECT_MEMBERS_EDIT, project.getProjectId())),
 *       			() -> aComponent.setVisible(false))
 *       		);
 *
 *
 * </code>
 * </p>
 * Then in <code>reload()</code> we can enforce all {@link RBACConstraint} by executing.
 * <p>
 * <code>
 *         rbacEnforcer.enforceConstraints(projectReference);
 * </code>
 * </p>
 * @author db
 *
 * @param <T>
 */
public class RBACConstraintEnforcer<T> {
	
	private List<RBACConstraint<T>> constraints = new ArrayList<>();
	
	/**
	 * Builder method that registers a <code>RBACConstraint</code> for later enforcement.
	 * 
	 * @param constraint
	 * @return
	 */
	public RBACConstraintEnforcer<T> register(RBACConstraint<T> constraint){
		constraints.add(constraint);
		
		return this;
	}
	
	/**
	 * truncates all defined constraints
	 */
	public void truncate(){
		constraints = new ArrayList<>();
	}
	
	/**
	 * Enforces all registered <code>RBACConstraint</code> by executing {@link RBACConstraint#enforce(T t)} for all.
	 * @param t
	 */
	public void enforceConstraints(T t){
		constraints.stream().forEach(constraint -> constraint.enforce(t));
	}
}
