package de.catma.ui.module.project.corpusimport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.properties.CATMAPropertyKey;
import de.catma.ui.CatmaApplication;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.module.project.ProjectView;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class CorpusImportDialog extends AbstractOkCancelDialog<Pair<File, List<CorpusImportDocumentMetadata>>> {
	private ProgressBar progressBar;
	private TextField nameField;
	private PasswordField passField;
	private Button btListCorpora;
	private Grid<CorpusImportMetadata> corporaGrid;
	private ArrayList<CorpusImportMetadata> corpusMetadataList;
	private ListDataProvider<CorpusImportMetadata> corpusMetadataProvider;
	private Pair<File, List<CorpusImportDocumentMetadata>> result;
	private final String tempDir;

	public CorpusImportDialog(SaveCancelListener<Pair<File, List<CorpusImportDocumentMetadata>>> listener) throws IOException {
		super("CATMA 5 Corpus Import", listener);
		this.corpusMetadataList = new ArrayList<CorpusImportMetadata>();
		this.corpusMetadataProvider = new ListDataProvider<CorpusImportMetadata>(this.corpusMetadataList);
		tempDir = ((CatmaApplication)UI.getCurrent()).acquirePersonalTempFolder();
	}

	@Override
	protected void handleOkPressed() {

		if (corporaGrid.getSelectedItems().isEmpty()) {
			Notification.show("Info", "Please select a Corpus from the list first!", Type.HUMANIZED_MESSAGE);
		}
		else {
			final CorpusImportMetadata corpusMetadata = corporaGrid.getSelectedItems().iterator().next();
			BackgroundServiceProvider backgroundServiceProvider = (BackgroundServiceProvider)UI.getCurrent();
			progressBar.setVisible(true);
			progressBar.setIndeterminate(true);
			progressBar.setCaption("Loading Corpus " + corpusMetadata);
			
			backgroundServiceProvider.submit("load-corpus", new DefaultProgressCallable<Pair<File, List<CorpusImportDocumentMetadata>>>() {
				@Override
				public Pair<File, List<CorpusImportDocumentMetadata>> call() throws Exception {
					
					File corpusFile = getCorpusFile(corpusMetadata);
					
					return new Pair<>(corpusFile, null);
				}
			}, new ExecutionListener<Pair<File, List<CorpusImportDocumentMetadata>>>() {
				@Override
				public void done(Pair<File, List<CorpusImportDocumentMetadata>> result) {
					
					try {
						List<CorpusImportDocumentMetadata> documentMetadataList = getDocumentMetadata(corpusMetadata);
						result.setSecond(documentMetadataList);
						progressBar.setVisible(false);
						
						CorpusImportDialog.this.result = result;
						
						CorpusImportDialog.super.handleOkPressed();
					} catch (Exception e) {
						progressBar.setVisible(false);
						Notification.show(
							"Error", 
							String.format(
									"Error retrieving Document metadata! "
									+ "\n The underlying error message was:\n%2$s", 
									e.getMessage()), 
							Type.ERROR_MESSAGE);	
						handleCancelPressed();
					}
				}

				@Override
				public void error(Throwable t) {
					Logger.getLogger(ProjectView.class.getName()).log(
							Level.SEVERE, 
							String.format("Error loading CATMA 5 Corpus %1$s!", corpusMetadata.toString()),
							t);
					String errorMsg = t.getMessage();
					if ((errorMsg == null) || (errorMsg.trim().isEmpty())) {
						errorMsg = "";
					}
					progressBar.setVisible(false);

					Notification.show(
						"Error", 
						String.format(
								"Error listing CATMA 5 Corpus %1$s! "
								+ "\n The underlying error message was:\n%2$s", 
								corpusMetadata.toString(),
								errorMsg), 
						Type.ERROR_MESSAGE);	
					handleCancelPressed();
				}
			});
		}
	}

	protected File getCorpusFile(CorpusImportMetadata corpusMetadata) throws Exception {
		IDGenerator idGenerator = new IDGenerator();
		
		File tempFile = new File(new File(tempDir), idGenerator.generate());

		if (tempFile.exists()) {
			tempFile.delete();
		}
		
		try (InputStream is = getAPIInputStream("corpus/get?cid="+corpusMetadata.getID()+"&format=xml,tei")) {
			try (FileOutputStream fos = new FileOutputStream(tempFile)) {
				IOUtils.copy(is, fos);
			}
		}

		return tempFile;
	}

	private List<CorpusImportDocumentMetadata> getDocumentMetadata(CorpusImportMetadata corpusMetadata) throws Exception {
		List<CorpusImportDocumentMetadata> documentMetadataList = new ArrayList<>();
		
		try (InputStream is = getAPIInputStream("corpus/list?cid="+corpusMetadata.getID())) {
			
			Gson gson = new Gson();
			
			
			String corpusContent = IOUtils.toString(is, "UTF-8");
			CorpusImportMetadata corpusImportMetadata = gson.fromJson(corpusContent, CorpusImportMetadata.class);
			for (CorpusImportDocumentMetadata corpusImportDocumentMetadata : corpusImportMetadata.getContents()) {
				documentMetadataList.add(corpusImportDocumentMetadata);
			}
		}
		
		return documentMetadataList;
	}

	@Override
	protected void addContent(ComponentContainer content) {
		HorizontalLayout credentialPanel = new HorizontalLayout();
		content.addComponent(credentialPanel);
		
		nameField = new TextField("Username");
		credentialPanel.addComponent(nameField);
		passField = new PasswordField("Password");
		credentialPanel.addComponent(passField);
		
		btListCorpora = new Button("List Corpora");
		btListCorpora.addClickListener(event -> handleListCorpora());
		credentialPanel.addComponent(btListCorpora);
		
		corporaGrid = new Grid<CorpusImportMetadata>(corpusMetadataProvider);
		corporaGrid.setSizeFull();
		corporaGrid.addColumn(cm -> cm.getID()).setCaption("ID").setExpandRatio(1);
		corporaGrid.addColumn(cm -> cm.getName()).setCaption("Name").setExpandRatio(2);
		
		ActionGridComponent<Grid<CorpusImportMetadata>> corporaActionGridComponent = 
				new ActionGridComponent<Grid<CorpusImportMetadata>>(new Label("Available Corpora"), corporaGrid);		
		corporaActionGridComponent.setMargin(false);
		corporaActionGridComponent.getActionGridBar().setAddBtnVisible(false);
		corporaActionGridComponent.getActionGridBar().setMoreOptionsBtnVisible(false);
        
		corporaActionGridComponent.setSelectionModeFixed(SelectionMode.SINGLE);
		corporaActionGridComponent.getActionGridBar().setMargin(new MarginInfo(false, true, false, true));

		
		content.addComponent(corporaActionGridComponent);
		((AbstractOrderedLayout) content).setExpandRatio(corporaActionGridComponent, 0.6f);
		
		this.progressBar = new ProgressBar();
		progressBar.setVisible(false);
		content.addComponent(progressBar);
	}

	private void handleListCorpora() {
		try {
			corpusMetadataList.clear();
			try (InputStream is = getAPIInputStream("corpus/list")) {
				String corpusList = IOUtils.toString(is, "UTF-8");
				
				Gson gson = new Gson();
				java.lang.reflect.Type collectionType = new TypeToken<Collection<CorpusImportMetadata>>(){}.getType();
				Collection<CorpusImportMetadata> buffer = gson.fromJson(corpusList, collectionType);
				
				this.corpusMetadataList.addAll(buffer);
			}
		}
		catch (Exception e) {
			Logger.getLogger(ProjectView.class.getName()).log(
					Level.SEVERE, 
					"Error listing CATMA 5 Corpora!", 
					e);
			String errorMsg = e.getMessage();
			if ((errorMsg == null) || (errorMsg.trim().isEmpty())) {
				errorMsg = "";
			}

			Notification.show(
				"Error", 
				String.format(
						"Error listing CATMA 5 Corpora! "
						+ "\n The underlying error message was:\n%1$s", 
						errorMsg), 
				Type.ERROR_MESSAGE);					
		}
		finally {
			corpusMetadataProvider.refreshAll();
		}
	}

	private InputStream getAPIInputStream(String apiPath) throws IOException {
		String authString = nameField.getValue() + ":" + passField.getValue();
		byte[] authStringBase64EncodedBytes = Base64.encodeBase64(authString.getBytes());
		String authStringBase64Encoded = new String(authStringBase64EncodedBytes);

		URL url = new URL(CATMAPropertyKey.CATMA_5_API_URL.getValue() + apiPath);
		URLConnection urlConnection = url.openConnection();
		urlConnection.setReadTimeout(7200000); // 2h, corpus generation in CATMA 5 can take a while
		urlConnection.setRequestProperty("Authorization", "Basic " + authStringBase64Encoded);
		return urlConnection.getInputStream();
	}

	@Override
	protected Pair<File, List<CorpusImportDocumentMetadata>> getResult() {
		return result;
	}
}
