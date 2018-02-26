package de.catma.ui.project;

import java.util.List;
import java.util.stream.Collectors;

import org.vaadin.addons.lazyquerycontainer.Query;

import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;

import de.catma.Pager;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;

public class ProjectQuery implements Query {
	
	private ProjectManager projectManager;
	private Pager<ProjectReference> projectPager;
	
	public ProjectQuery(ProjectManager projectManager) throws Exception {
		super();
		this.projectManager = projectManager;
		this.projectPager = this.projectManager.getProjectReferences();
	}

	@Override
	public int size() {
		return projectPager.getTotalItems();
	}

	@Override
	public List<Item> loadItems(int startIndex, int count) {
		int page = (startIndex / count)+1;
		
		return projectPager
				.page(page)
				.stream()
				.map(projectReference -> new BeanItem<>(projectReference))
				.collect(Collectors.toList());
	}

	@Override
	public void saveItems(List<Item> addedItems, List<Item> modifiedItems, List<Item> removedItems) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean deleteAllItems() {
		// not supported
		return false;
	}

	@Override
	public Item constructItem() {
		// TODO Auto-generated method stub
		return null;
	}


}
