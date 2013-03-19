/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui.repository;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import nu.xom.ParsingException;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.terminal.DownloadStream;
import com.vaadin.terminal.FileResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Form;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window.Notification;
import com.vaadin.ui.themes.Reindeer;

import de.catma.CatmaApplication;
import de.catma.document.repository.Repository;
import de.catma.document.repository.UnknownUserException;
import de.catma.document.source.contenthandler.BOMFilterInputStream;
import de.catma.serialization.tei.TeiDocument;
import de.catma.serialization.tei.TeiDocumentFactory;
import de.catma.serialization.tei.TeiTagLibrarySerializationHandler;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagManager;
import de.catma.ui.dialog.FormDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleValueDialog;
import de.catma.ui.dialog.UploadDialog;
import de.catma.ui.repository.sharing.SharingOptions;
import de.catma.ui.repository.sharing.SharingOptionsFieldFactory;
import de.catma.util.CloseSafe;
import de.catma.util.ContentInfoSet;

public class TagLibraryPanel extends HorizontalSplitPanel {

	private final static String SORTCAP_PROP = "SORTCAP";

	private final ContentInfoSet emptyContentInfoSet = new ContentInfoSet();
	
	private Tree tagLibrariesTree;
	
	private Button btOpenTagLibrary;
	private Button btCreateTagLibrary;
	private MenuItem miMoreTagLibraryActions;
	private Form contentInfoForm;
	private Button btEditContentInfo;
	private Button btSaveContentInfoChanges;
	private Button btDiscardContentInfoChanges;
	
	private PropertyChangeListener tagLibraryChangedListener;

	private Repository repository;

	private TagManager tagManager;

	private HierarchicalContainer tagLibraryContainer;

	public TagLibraryPanel(TagManager tagManager, Repository repository) {
		this.repository = repository;
		this.tagManager = tagManager;
		initComponents();
		initActions();
		initListeners();
	}

	private void initListeners() {
		tagLibraryChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getOldValue() == null) { //insert
					TagLibraryReference tagLibraryRef = 
							(TagLibraryReference)evt.getNewValue();
					addTagLibraryReferenceToTree(tagLibraryRef);
					tagLibraryContainer.sort(
						new Object[] {SORTCAP_PROP}, new boolean[] { true });

				}
				else if (evt.getNewValue() == null) { //remove
					TagLibraryReference tagLibraryRef = 
							(TagLibraryReference)evt.getOldValue();
					tagLibrariesTree.removeItem(tagLibraryRef);
				}
				else { //update
					tagLibrariesTree.requestRepaint();
				}
			}
		};
		this.repository.addPropertyChangeListener(
				Repository.RepositoryChangeEvent.tagLibraryChanged, 
				tagLibraryChangedListener);
	}

	private void initComponents() {
		addComponent(createTagLibraryPanel());
		addComponent(createContentInfoPanel());
		setSplitPosition(70);
	}
	
	private Component createTagLibraryPanel() {
		VerticalLayout tagLibraryPanel = new VerticalLayout();
		tagLibraryPanel.setSpacing(true);
		tagLibraryPanel.setSizeFull();
		Component tagLibraryTreePanel = createTagLibraryTreePanel();
		tagLibraryPanel.addComponent(tagLibraryTreePanel);
		tagLibraryPanel.setExpandRatio(tagLibraryTreePanel, 1.0f);
		tagLibraryPanel.addComponent(createTagLibraryButtonPanel());
		
		return tagLibraryPanel;
	}

	private Component createTagLibraryButtonPanel() {
		
		HorizontalLayout tagLibraryButtonPanel = new HorizontalLayout();
		tagLibraryButtonPanel.setSpacing(true);
		
		btOpenTagLibrary = new Button("Open Tag Library");
		btOpenTagLibrary.setEnabled(false);
		tagLibraryButtonPanel.addComponent(btOpenTagLibrary);

		btCreateTagLibrary = new Button("Create Tag Library");
		tagLibraryButtonPanel.addComponent(btCreateTagLibrary);
		
		MenuBar menuMoreTagLibraryActions = new MenuBar();
		miMoreTagLibraryActions = 
				menuMoreTagLibraryActions.addItem("More actions...", null);
		tagLibraryButtonPanel.addComponent(menuMoreTagLibraryActions);
		
		return tagLibraryButtonPanel;
		
	}

	private Component createTagLibraryTreePanel() {

		Panel tagLibraryPanel = new Panel();
		tagLibraryPanel.getContent().setSizeUndefined();
		tagLibraryPanel.setSizeFull();

		tagLibraryContainer = new HierarchicalContainer();

		tagLibrariesTree = new Tree();
		tagLibrariesTree.setContainerDataSource(tagLibraryContainer);
		
		tagLibrariesTree.setCaption("Tag Libraries");
		tagLibrariesTree.addStyleName("bold-label-caption");
		tagLibrariesTree.setImmediate(true);
		tagLibrariesTree.setItemCaptionMode(Tree.ITEM_CAPTION_MODE_ID);
		
		tagLibraryContainer.addContainerProperty(SORTCAP_PROP, String.class, null);
		
		for (TagLibraryReference tlr : repository.getTagLibraryReferences()) {
			addTagLibraryReferenceToTree(tlr);
		}
		tagLibraryContainer.sort(new Object[] {SORTCAP_PROP}, new boolean[] { true });

		tagLibraryPanel.addComponent(tagLibrariesTree);
		
		return tagLibraryPanel;
	}
	
	private Component createContentInfoPanel() {
		VerticalLayout contentInfoPanel = new VerticalLayout();
		contentInfoPanel.setSpacing(true);
		contentInfoPanel.setSizeFull();
		contentInfoPanel.setMargin(false, false, true, true);
		Component contentInfoForm = createContentInfoForm();
		contentInfoPanel.addComponent(contentInfoForm);
		contentInfoPanel.setExpandRatio(contentInfoForm, 1.0f);
		
		contentInfoPanel.addComponent(createContentInfoButtonsPanel());
		
		return contentInfoPanel;
	}


	private Component createContentInfoButtonsPanel() {
		HorizontalLayout content = new HorizontalLayout();
		content.setSpacing(true);
		
		Panel contentInfoButtonsPanel = new Panel(content);
		
		contentInfoButtonsPanel.setStyleName(Reindeer.PANEL_LIGHT);
		
		btEditContentInfo = new Button("Edit");
		contentInfoButtonsPanel.addComponent(btEditContentInfo);
		btSaveContentInfoChanges = new Button("Save");
		btSaveContentInfoChanges.setVisible(false);
		contentInfoButtonsPanel.addComponent(btSaveContentInfoChanges);
		btDiscardContentInfoChanges = new Button("Discard");
		btDiscardContentInfoChanges.setVisible(false);
		contentInfoButtonsPanel.addComponent(btDiscardContentInfoChanges);
		
		return contentInfoButtonsPanel;
	}
	
	private Component createContentInfoForm() {
		
		Panel contentInfoPanel = new Panel();
		contentInfoPanel.getContent().setSizeUndefined();
		contentInfoPanel.getContent().setWidth("100%");
		contentInfoPanel.setSizeFull();
		
		contentInfoForm = new Form();
		contentInfoForm.setSizeFull();
		contentInfoForm.setCaption("Information");
		contentInfoForm.setWriteThrough(false);
		contentInfoForm.setReadOnly(true);
		contentInfoForm.setEnabled(false);
		
		BeanItem<ContentInfoSet> contentInfoItem = 
				new BeanItem<ContentInfoSet>(emptyContentInfoSet);
		contentInfoForm.setItemDataSource(contentInfoItem);
		contentInfoForm.setVisibleItemProperties(new String[] {
				"title", "author", "description", "publisher"
		});
		
		contentInfoForm.setReadOnly(true);
		contentInfoPanel.addComponent(contentInfoForm);
		
		return contentInfoPanel;
	}
	
	private void initActions() {
		
		btCreateTagLibrary.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				final String nameProperty = "name";
				SingleValueDialog singleValueDialog = new SingleValueDialog();
						
				singleValueDialog.getSingleValue(
						getApplication().getMainWindow(),
						"Create a new Tag Library",
						"You have to enter a name!",
						new SaveCancelListener<PropertysetItem>() {
					public void cancelPressed() {}
					public void savePressed(
							PropertysetItem propertysetItem) {
						Property property = 
								propertysetItem.getItemProperty(
										nameProperty);
						String name = (String)property.getValue();
						try {
							repository.createTagLibrary(name);
						} catch (IOException e) {
							((CatmaApplication)getApplication()).showAndLogError(
								"Error creating the Tag Library!", e);
						}
					}
				}, nameProperty);
			}
		});
		
		
		tagLibrariesTree.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				
				Object value = event.getProperty().getValue();
				btOpenTagLibrary.setEnabled(value!=null);
				if (value != null) {
					contentInfoForm.setEnabled(true);
					contentInfoForm.setItemDataSource(
						new BeanItem<ContentInfoSet>(
							new ContentInfoSet(
								((TagLibraryReference)value).getContentInfoSet())));
					contentInfoForm.setVisibleItemProperties(new String[] {
							"title", "author", "description", "publisher"
					});

				}
				else {
					contentInfoForm.setEnabled(false);
					contentInfoForm.setItemDataSource(
							new BeanItem<ContentInfoSet>(emptyContentInfoSet));
					contentInfoForm.setVisibleItemProperties(new String[] {
							"title", "author", "description", "publisher"
					});

				}
				contentInfoForm.setReadOnly(true);
			}
		});
		
		btOpenTagLibrary.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				Object value = tagLibrariesTree.getValue();
				handleOpenTagLibraryRequest(value);
			}
		});

		miMoreTagLibraryActions.addItem("Import Tag Library", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				handleTagLibraryImport();
			}
		});
		
		miMoreTagLibraryActions.addItem("Export Tag Library", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				handleTagLibraryExportRequest(tagLibrariesTree.getValue());
			}
		});
		
		miMoreTagLibraryActions.addItem("Remove Tag Library", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				handleTagLibraryRemoval();
			}
		});

		miMoreTagLibraryActions.addItem("Share Tag Library", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				handleShareTagLibraryRequest(tagLibrariesTree.getValue());
			}
		});

		btEditContentInfo.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				btEditContentInfo.setVisible(false);
				btSaveContentInfoChanges.setVisible(true);
				btDiscardContentInfoChanges.setVisible(true);
				contentInfoForm.setReadOnly(false);
			}
		});
		
		btSaveContentInfoChanges.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				btEditContentInfo.setVisible(true);
				btSaveContentInfoChanges.setVisible(false);
				btDiscardContentInfoChanges.setVisible(false);
				contentInfoForm.commit();
				contentInfoForm.setReadOnly(true);				
				Object value = tagLibrariesTree.getValue();
				@SuppressWarnings("unchecked")
				BeanItem<ContentInfoSet> item = 
						(BeanItem<ContentInfoSet>)contentInfoForm.getItemDataSource();
				ContentInfoSet contentInfoSet = item.getBean();

				tagManager.updateTagLibrary(
						(TagLibraryReference)value, contentInfoSet);
			}
		});
		
		btDiscardContentInfoChanges.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				btEditContentInfo.setVisible(true);
				btSaveContentInfoChanges.setVisible(false);
				btDiscardContentInfoChanges.setVisible(false);
				contentInfoForm.discard();
				contentInfoForm.setReadOnly(true);				
			}
		});
		
		this.tagLibrariesTree.addListener(new ItemClickListener() {
			
			public void itemClick(ItemClickEvent event) {			
				if (event.isDoubleClick()) {
					Object value = event.getItemId();
					handleOpenTagLibraryRequest(value);
				}
			}
		});
	}
	
	private void handleShareTagLibraryRequest(Object value) {
		if (value != null) {
			final TagLibraryReference tagLibraryReference = 
					(TagLibraryReference)value;
			
			SharingOptions sharingOptions = new SharingOptions();
			
			FormDialog<SharingOptions> sharingOptionsDlg = new FormDialog<SharingOptions>(
				"Please enter the person you want to share with", 
				new BeanItem<SharingOptions>(sharingOptions),
				new SharingOptionsFieldFactory(), 
				new SaveCancelListener<SharingOptions>() {
					public void cancelPressed() {}
					public void savePressed(SharingOptions result) {
						try {
							repository.share(
									tagLibraryReference, 
									result.getUserIdentification(), 
									result.getAccessMode());
						} catch (IOException e) {
							if (e.getCause() instanceof UnknownUserException) {
								getWindow().showNotification(
										"Sharing failed!", e.getCause().getMessage(), 
										Notification.TYPE_WARNING_MESSAGE);
							}
							else {
								((CatmaApplication)getApplication()).showAndLogError(
									"Error sharing this corpus!", e);
							}
						}
					}
				});
			sharingOptionsDlg.setVisibleItemProperties(
					new Object[] {"userIdentification", "accessMode"});
			sharingOptionsDlg.show(getApplication().getMainWindow());
		}
		else {
			getWindow().showNotification(
					"Information", "Please select a Tag Library first!",
					Notification.TYPE_TRAY_NOTIFICATION);
		}

	}

	private void handleTagLibraryExportRequest(Object value) {
		if (value != null) {
			TagLibraryReference tagLibraryReference = 
					(TagLibraryReference)value;
			try {
				final TagLibrary tagLibrary = 
						repository.getTagLibrary(tagLibraryReference);
				
				TeiDocumentFactory factory = new TeiDocumentFactory();
				TeiDocument teiDocument = 
						factory.createEmptyDocument(tagLibrary.getId());
				
				teiDocument.getTeiHeader().setValues(
					tagLibrary.getContentInfoSet());
				
				new TeiTagLibrarySerializationHandler(
						teiDocument, tagManager).serialize(tagLibrary);
				
				ByteArrayOutputStream teiDocOut = new ByteArrayOutputStream();
				teiDocument.printXmlDocument(teiDocOut);
				
				final ByteArrayInputStream teiDownloadStream = 
						new ByteArrayInputStream(teiDocOut.toByteArray());

				getWindow().open(new FileResource(null, getApplication()) {
					public com.vaadin.terminal.DownloadStream getStream() {
						DownloadStream ds = 
							new DownloadStream(
								teiDownloadStream, 
								getMIMEType(), getFilename());
						ds.setParameter(
							"Content-Disposition", 
							"attachment; filename="
			                    + getFilename());
			            ds.setCacheTime(0);
			            return ds;
					};
					public String getMIMEType() {
						return "application/xml";
					};
					
					public String getFilename() {
						return tagLibrary.toString() + ".xml";
					};
				},
				"_blank");

				
				
			} catch (IOException e) {
				((CatmaApplication)getApplication()).showAndLogError(
					"Error opening the Tag Library!", e);
			} catch (ParsingException parsingException) {
				((CatmaApplication)getApplication()).showAndLogError(
						"Error exporting the Tag Library!", parsingException);
			}
		}	
		else {
			getWindow().showNotification(
					"Information", "Please select a Tag Library first!",
					Notification.TYPE_TRAY_NOTIFICATION);
		}

	}

	private void handleOpenTagLibraryRequest(Object value) {
		if (value != null) {
			TagLibraryReference tagLibraryReference = 
					(TagLibraryReference)value;
			TagLibrary tagLibrary;
			try {
				tagLibrary = repository.getTagLibrary(tagLibraryReference);
				((CatmaApplication)getApplication()).openTagLibrary(repository, tagLibrary);
			} catch (IOException e) {
				((CatmaApplication)getApplication()).showAndLogError(
					"Error opening the Tag Library!", e);
			}
		}	
		else {
			getWindow().showNotification(
					"Information", "Please select a Tag Library first!",
					Notification.TYPE_TRAY_NOTIFICATION);
		}
	}

	private void handleTagLibraryImport() {
		UploadDialog uploadDialog =
				new UploadDialog("Upload Tag Library", 
						new SaveCancelListener<byte[]>() {
			
			public void cancelPressed() {}
			
			public void savePressed(byte[] result) {
				InputStream is = new ByteArrayInputStream(result);
				try {
					if (BOMFilterInputStream.hasBOM(result)) {
						is = new BOMFilterInputStream(
								is, Charset.forName("UTF-8"));
					}
					
					repository.importTagLibrary(is);
					
					
				} catch (IOException e) {
					((CatmaApplication)getApplication()).showAndLogError(
						"Error importing the Tag Library!", e);
				}
				finally {
					CloseSafe.close(is);
				}
			}
			
		});
		uploadDialog.show(getApplication().getMainWindow());
	}

	private void addTagLibraryReferenceToTree(TagLibraryReference tlr) {
		tagLibrariesTree.addItem(tlr);
		tagLibrariesTree.getItem(tlr).getItemProperty(SORTCAP_PROP).setValue(
				(tlr.toString()==null)?"":tlr.toString());
		tagLibrariesTree.setChildrenAllowed(tlr, false);
	}
	

	private void handleTagLibraryRemoval() {
		final TagLibraryReference tagLibraryReference = 
				(TagLibraryReference) tagLibrariesTree.getValue();
		
		if (tagLibraryReference != null) {
			ConfirmDialog.show(
				getApplication().getMainWindow(), 
				"Do you really want to delete Tag Library '"
						+ tagLibraryReference.toString() + "'?",
		        new ConfirmDialog.Listener() {

		            public void onClose(ConfirmDialog dialog) {
		                if (dialog.isConfirmed()) {
		                	try {
								repository.delete(tagLibraryReference);
							} catch (IOException e) {
								((CatmaApplication)getApplication()).showAndLogError(
									"Error deleting the Tag Library!", e);
							}
		                }
		            }
		        });
		}
		
	}
	
	public void close() {
		this.repository.removePropertyChangeListener(
				Repository.RepositoryChangeEvent.tagLibraryChanged, 
				tagLibraryChangedListener);
	}
}
