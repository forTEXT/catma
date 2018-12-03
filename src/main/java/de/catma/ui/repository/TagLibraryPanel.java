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
import java.text.MessageFormat;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.v7.data.Property;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.data.util.BeanItem;
import com.vaadin.v7.data.util.HierarchicalContainer;
import com.vaadin.v7.data.util.PropertysetItem;
import com.vaadin.v7.event.ItemClickEvent;
import com.vaadin.v7.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.v7.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.v7.ui.Form;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.v7.ui.Tree;
import com.vaadin.ui.UI;
import com.vaadin.v7.ui.VerticalLayout;
import com.vaadin.v7.ui.themes.Reindeer;

import de.catma.document.repository.Repository;
import de.catma.document.repository.UnknownUserException;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.contenthandler.BOMFilterInputStream;
import de.catma.serialization.tei.TeiDocument;
import de.catma.serialization.tei.TeiDocumentFactory;
import de.catma.serialization.tei.TeiTagLibrarySerializationHandler;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagManager;
import de.catma.ui.CatmaApplication;
import de.catma.ui.dialog.FormDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleValueDialog;
import de.catma.ui.dialog.UploadDialog;
import de.catma.ui.repository.sharing.SharingOptions;
import de.catma.ui.repository.sharing.SharingOptionsFieldFactory;
import de.catma.util.CloseSafe;
import nu.xom.ParsingException;

@Deprecated
public class TagLibraryPanel extends HorizontalSplitPanel {

	private final static String SORTCAP_PROP = "SORTCAP"; //$NON-NLS-1$

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

	private FileDownloader currentFileDownloader;

	private Button btExportTagLibrary;

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
					tagLibrariesTree.markAsDirty();
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
		
		btOpenTagLibrary = new Button(Messages.getString("TagLibraryPanel.openTagLibrary")); //$NON-NLS-1$
		btOpenTagLibrary.addStyleName("primary-button"); //$NON-NLS-1$
		btOpenTagLibrary.setEnabled(false);
		tagLibraryButtonPanel.addComponent(btOpenTagLibrary);

		btCreateTagLibrary = new Button(Messages.getString("TagLibraryPanel.createTagLibrary")); //$NON-NLS-1$
		btCreateTagLibrary.addStyleName("secondary-button"); //$NON-NLS-1$
		tagLibraryButtonPanel.addComponent(btCreateTagLibrary);
		
		btExportTagLibrary = new Button(Messages.getString("TagLibraryPanel.exportTagLibrary")); //$NON-NLS-1$
		tagLibraryButtonPanel.addComponent(btExportTagLibrary);
		
		MenuBar menuMoreTagLibraryActions = new MenuBar();
		miMoreTagLibraryActions = 
				menuMoreTagLibraryActions.addItem(Messages.getString("TagLibraryPanel.moreActions"), null); //$NON-NLS-1$
		tagLibraryButtonPanel.addComponent(menuMoreTagLibraryActions);
		
		return tagLibraryButtonPanel;
		
	}

	private Component createTagLibraryTreePanel() {
		VerticalLayout tagLibraryPanelContent = new VerticalLayout();
		tagLibraryPanelContent.setMargin(true);
		
		Panel tagLibraryPanel = new Panel(tagLibraryPanelContent);
		tagLibraryPanel.getContent().setSizeUndefined();
		tagLibraryPanel.setSizeFull();

		tagLibraryContainer = new HierarchicalContainer();

		tagLibrariesTree = new Tree();
		tagLibrariesTree.setContainerDataSource(tagLibraryContainer);
		
		tagLibrariesTree.setCaption(Messages.getString("TagLibraryPanel.tagLibraries")); //$NON-NLS-1$
		tagLibrariesTree.addStyleName("bold-label-caption"); //$NON-NLS-1$
		tagLibrariesTree.setImmediate(true);
		tagLibrariesTree.setItemCaptionMode(ItemCaptionMode.ID);
		
		tagLibraryContainer.addContainerProperty(SORTCAP_PROP, String.class, null);
		
		for (TagLibraryReference tlr : repository.getTagLibraryReferences()) {
			addTagLibraryReferenceToTree(tlr);
		}
		tagLibraryContainer.sort(new Object[] {SORTCAP_PROP}, new boolean[] { true });

		tagLibraryPanelContent.addComponent(tagLibrariesTree);
		
		return tagLibraryPanel;
	}
	
	private Component createContentInfoPanel() {
		VerticalLayout contentInfoPanel = new VerticalLayout();
		contentInfoPanel.setSpacing(true);
		contentInfoPanel.setSizeFull();
		contentInfoPanel.setMargin(new MarginInfo(false, false, true, true));
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
		
		btEditContentInfo = new Button(Messages.getString("TagLibraryPanel.Edit")); //$NON-NLS-1$
		btEditContentInfo.setEnabled(false);

		content.addComponent(btEditContentInfo);
		btSaveContentInfoChanges = new Button(Messages.getString("TagLibraryPanel.Save")); //$NON-NLS-1$
		btSaveContentInfoChanges.setVisible(false);
		content.addComponent(btSaveContentInfoChanges);
		btDiscardContentInfoChanges = new Button(Messages.getString("TagLibraryPanel.Discard")); //$NON-NLS-1$
		btDiscardContentInfoChanges.setVisible(false);
		content.addComponent(btDiscardContentInfoChanges);
		
		return contentInfoButtonsPanel;
	}
	
	private Component createContentInfoForm() {
		VerticalLayout contentInfoPanelContent = new VerticalLayout();
		contentInfoPanelContent.setMargin(true);
		Panel contentInfoPanel = new Panel(contentInfoPanelContent);
		contentInfoPanel.getContent().setSizeUndefined();
		contentInfoPanel.getContent().setWidth("100%"); //$NON-NLS-1$
		contentInfoPanel.setSizeFull();
		
		contentInfoForm = new Form();
		contentInfoForm.setSizeFull();
		contentInfoForm.setCaption(Messages.getString("TagLibraryPanel.contentInformation")); //$NON-NLS-1$
		contentInfoForm.setBuffered(true);
		contentInfoForm.setReadOnly(true);
		contentInfoForm.setEnabled(false);
		
		BeanItem<ContentInfoSet> contentInfoItem = 
				new BeanItem<ContentInfoSet>(emptyContentInfoSet);
		contentInfoForm.setItemDataSource(contentInfoItem);
		contentInfoForm.setVisibleItemProperties(new String[] {
				"title", "author", "description", "publisher" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		});
		
		contentInfoForm.setReadOnly(true);
		contentInfoPanelContent.addComponent(contentInfoForm);
		
		return contentInfoPanel;
	}
	
	private void initActions() {
		
		btCreateTagLibrary.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				final String nameProperty = "name"; //$NON-NLS-1$
				SingleValueDialog singleValueDialog = new SingleValueDialog();
						
				singleValueDialog.getSingleValue(
						Messages.getString("TagLibraryPanel.createNewTagLibrary"), //$NON-NLS-1$
						Messages.getString("TagLibraryPanel.enterNameObligation"), //$NON-NLS-1$
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
							((CatmaApplication)UI.getCurrent()).showAndLogError(
								Messages.getString("TagLibraryPanel.errorCreatingTagLibrary"), e); //$NON-NLS-1$
						}
					}
				}, nameProperty);
			}
		});
		
		
		tagLibrariesTree.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				
				Object value = event.getProperty().getValue();
				btOpenTagLibrary.setEnabled(value!=null);
				if (value != null) {
					contentInfoForm.setEnabled(true);
					btEditContentInfo.setEnabled(true);
					contentInfoForm.setItemDataSource(
						new BeanItem<ContentInfoSet>(
							new ContentInfoSet(
								((TagLibraryReference)value).getContentInfoSet())));
					contentInfoForm.setVisibleItemProperties(new String[] {
							"title", "author", "description", "publisher" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					});

				}
				else {
					btEditContentInfo.setEnabled(false);
					contentInfoForm.setEnabled(false);
					contentInfoForm.setItemDataSource(
							new BeanItem<ContentInfoSet>(emptyContentInfoSet));
					contentInfoForm.setVisibleItemProperties(new String[] {
							"title", "author", "description", "publisher" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					});

				}
				contentInfoForm.setReadOnly(true);
				prepareFileExport(value);
			}
		});
		
		btOpenTagLibrary.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				Object value = tagLibrariesTree.getValue();
				handleOpenTagLibraryRequest(value);
			}
		});

		
		miMoreTagLibraryActions.addItem(Messages.getString("TagLibraryPanel.importTagLibrary"), new Command() { //$NON-NLS-1$
			
			public void menuSelected(MenuItem selectedItem) {
				handleTagLibraryImport();
			}
		});
		btExportTagLibrary.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				// handle empty selection
				handleTagLibraryExportRequest(tagLibrariesTree.getValue());
			}
		});
		
		miMoreTagLibraryActions.addItem(Messages.getString("TagLibraryPanel.removeTagLibrary"), new Command() { //$NON-NLS-1$
			
			public void menuSelected(MenuItem selectedItem) {
				handleTagLibraryRemoval();
			}
		});

		miMoreTagLibraryActions.addItem(Messages.getString("TagLibraryPanel.shareTagLibrary"), new Command() { //$NON-NLS-1$
			
			public void menuSelected(MenuItem selectedItem) {
				handleShareTagLibraryRequest(tagLibrariesTree.getValue());
			}
		});

		btEditContentInfo.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				btEditContentInfo.setVisible(false);
				btSaveContentInfoChanges.setVisible(true);
				btDiscardContentInfoChanges.setVisible(true);
				contentInfoForm.setReadOnly(false);
			}
		});
		
		btSaveContentInfoChanges.addClickListener(new ClickListener() {
			
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
		
		btDiscardContentInfoChanges.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				btEditContentInfo.setVisible(true);
				btSaveContentInfoChanges.setVisible(false);
				btDiscardContentInfoChanges.setVisible(false);
				contentInfoForm.discard();
				contentInfoForm.setReadOnly(true);				
			}
		});
		
		this.tagLibrariesTree.addItemClickListener(new ItemClickListener() {
			
			public void itemClick(ItemClickEvent event) {			
				if (event.isDoubleClick()) {
					Object value = event.getItemId();
					handleOpenTagLibraryRequest(value);
				}
			}
		});
	}
	
	protected void prepareFileExport(Object value) {
		
		if (currentFileDownloader != null) {
			currentFileDownloader.remove();
			currentFileDownloader = null;
		}
		if (value != null) {
			final TagLibraryReference tagLibraryReference = 
					(TagLibraryReference)value;

			StreamResource resultStreamResource = 
				new StreamResource(
					new StreamSource() {
						@Override
						public InputStream getStream() {
							return createExportResultStream(tagLibraryReference);
						}
					}, tagLibraryReference.toString().replaceAll("[^A-Za-z0-9]", "_") + ".xml" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			resultStreamResource.setCacheTime(0);
			
			currentFileDownloader = new FileDownloader(resultStreamResource);
			currentFileDownloader.extend(btExportTagLibrary);		
		}
	}

	protected InputStream createExportResultStream(TagLibraryReference tagLibraryReference) {
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
			
			return teiDownloadStream;
			
		} catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
				Messages.getString("TagLibraryPanel.errorOpeningTagLibrary"), e); //$NON-NLS-1$
		} catch (ParsingException parsingException) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
				Messages.getString("TagLibraryPanel.errorExportingTagLibrary"), parsingException); //$NON-NLS-1$
		}
		return null;
	}

	private void handleShareTagLibraryRequest(Object value) {
		if (value != null) {
			final TagLibraryReference tagLibraryReference = 
					(TagLibraryReference)value;
			
			SharingOptions sharingOptions = new SharingOptions();
			
			FormDialog<SharingOptions> sharingOptionsDlg = new FormDialog<SharingOptions>(
				Messages.getString("TagLibraryPanel.enterPersonToShare"),  //$NON-NLS-1$
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
							if (e instanceof UnknownUserException) {
								Notification.show(
										Messages.getString("TagLibraryPanel.sharingFailed"), e.getMessage(),  //$NON-NLS-1$
										Type.ERROR_MESSAGE);
							}
							else {
								((CatmaApplication)UI.getCurrent()).showAndLogError(
									Messages.getString("TagLibraryPanel.errorSharing"), e); //$NON-NLS-1$
							}
						}
					}
				});
			sharingOptionsDlg.setVisibleItemProperties(
					new Object[] {"userIdentification", "accessMode"}); //$NON-NLS-1$ //$NON-NLS-2$
			sharingOptionsDlg.show();
		}
		else {
			Notification.show(
					Messages.getString("TagLibraryPanel.infoTitle"), Messages.getString("TagLibraryPanel.selectTagLibraryFirst"), //$NON-NLS-1$ //$NON-NLS-2$
					Type.TRAY_NOTIFICATION);
		}

	}

	private void handleTagLibraryExportRequest(Object value) {
		if (value == null) {
			Notification.show(
					Messages.getString("TagLibraryPanel.infoTitle"), Messages.getString("TagLibraryPanel.selectTagLibraryFirst"), //$NON-NLS-1$ //$NON-NLS-2$
					Type.TRAY_NOTIFICATION);
		}
	}

	private void handleOpenTagLibraryRequest(Object value) {
		if (value != null) {
			TagLibraryReference tagLibraryReference = 
					(TagLibraryReference)value;
			TagLibrary tagLibrary;
			try {
				tagLibrary = repository.getTagLibrary(tagLibraryReference);
				((CatmaApplication)UI.getCurrent()).openTagLibrary(repository, tagLibrary);
			} catch (IOException e) {
				((CatmaApplication)UI.getCurrent()).showAndLogError(
					Messages.getString("TagLibraryPanel.errorOpeningTagLibrary"), e); //$NON-NLS-1$
			}
		}	
		else {
			Notification.show(
					Messages.getString("TagLibraryPanel.infoTitle"), Messages.getString("TagLibraryPanel.selectTagLibraryFirst"), //$NON-NLS-1$ //$NON-NLS-2$
					Type.TRAY_NOTIFICATION);
		}
	}

	private void handleTagLibraryImport() {
		UploadDialog uploadDialog =
				new UploadDialog(Messages.getString("TagLibraryPanel.uploadTagLibrary"),  //$NON-NLS-1$
						new SaveCancelListener<byte[]>() {
			
			public void cancelPressed() {}
			
			public void savePressed(byte[] result) {
				InputStream is = new ByteArrayInputStream(result);
				try {
					if (BOMFilterInputStream.hasBOM(result)) {
						is = new BOMFilterInputStream(
								is, Charset.forName("UTF-8")); //$NON-NLS-1$
					}
					
					repository.importTagLibrary(is);
					
					
				} catch (IOException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
						Messages.getString("TagLibraryPanel.errorImportingTagLibrary"), e); //$NON-NLS-1$
				}
				finally {
					CloseSafe.close(is);
				}
			}
			
		});
		uploadDialog.show();
	}

	private void addTagLibraryReferenceToTree(TagLibraryReference tlr) {
		tagLibrariesTree.addItem(tlr);
		tagLibrariesTree.getItem(tlr).getItemProperty(SORTCAP_PROP).setValue(
				(tlr.toString()==null)?"":tlr.toString()); //$NON-NLS-1$
		tagLibrariesTree.setChildrenAllowed(tlr, false);
	}
	

	private void handleTagLibraryRemoval() {
		final TagLibraryReference tagLibraryReference = 
				(TagLibraryReference) tagLibrariesTree.getValue();
		
		if (tagLibraryReference != null) {
			ConfirmDialog.show(
				UI.getCurrent(), 
				MessageFormat.format(Messages.getString("TagLibraryPanel.deleteTagLibraryQuestion"), tagLibraryReference.toString()), //$NON-NLS-1$
		        new ConfirmDialog.Listener() {

		            public void onClose(ConfirmDialog dialog) {
		                if (dialog.isConfirmed()) {
		                	try {
								repository.delete(tagLibraryReference);
							} catch (IOException e) {
								((CatmaApplication)UI.getCurrent()).showAndLogError(
									Messages.getString("TagLibraryPanel.errorDeletingTagLibrary"), e); //$NON-NLS-1$
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
