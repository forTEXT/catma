package de.catma.project;

public class ForkStatus {
	
	private final boolean resourceAlreadyExists;
	private final boolean targetHasConflicts;
	private final boolean targetNotClean;
	private final boolean success;
	
	private ForkStatus(boolean resourceAlreadyExists, boolean targetHasConflicts, boolean targetNotClean, boolean success) {
		super();
		this.resourceAlreadyExists = resourceAlreadyExists;
		this.targetHasConflicts = targetHasConflicts;
		this.targetNotClean = targetNotClean;
		this.success = success;
	}

	public boolean isResourceAlreadyExists() {
		return resourceAlreadyExists;
	}

	public boolean isTargetHasConflicts() {
		return targetHasConflicts;
	}

	public boolean isTargetNotClean() {
		return targetNotClean;
	}
	
	public boolean isSuccess() {
		return success;
	}
	
	public static ForkStatus resourceAlreadyExists() {
		return new ForkStatus(true, false, false, false);
	}
	
	public static ForkStatus targetHasConflicts() {
		return new ForkStatus(false, true, true, false);
	}
	
	public static ForkStatus targetNotClean() {
		return new ForkStatus(false, false, true, false);
	}

	public static ForkStatus success() {
		return new ForkStatus(false, false, false, true);
	}
}
