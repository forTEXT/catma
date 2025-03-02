package de.catma.ui.events;

/**
 * This event is fired when a group is created, deleted, renamed or left and
 * signals to {@link de.catma.ui.module.dashboard.GroupListView} that the list of groups needs to be refreshed.
 */
public class GroupsChangedEvent {
	private final Long deletedGroupId;

	public GroupsChangedEvent() {
		this(null);
	}

	public GroupsChangedEvent(Long deletedGroupId) {
		this.deletedGroupId = deletedGroupId;
	}

	public Long getDeletedGroupId() {
		return deletedGroupId;
	}
}
