package de.catma.servlet;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.indexer.IndexBufferManagerName;
import de.catma.indexer.graph.CatmaGraphDbName;
import de.catma.indexer.graph.NodeType;
import de.catma.indexer.graph.SourceDocumentProperty;
import de.catma.indexer.graph.TermProperty;
import de.catma.indexer.indexbuffer.IndexBufferManager;
import de.catma.repository.db.CatmaDataSourceName;

public class DataSourceInitializerServlet extends HttpServlet {
	
    @Override
    public void init(ServletConfig cfg) throws ServletException {
        super.init(cfg);

        try {
	        log("CATMA DB Datasource initializing...");
	        
	        InitialContext context = new InitialContext();
	        
			int repoIndex = 1; // assume that the first configured repo is the local db repo
			String user = RepositoryPropertyKey.RepositoryUser.getIndexedValue(repoIndex);
					
			String pass = RepositoryPropertyKey.RepositoryPass.getIndexedValue(repoIndex);
			
			String url = RepositoryPropertyKey.RepositoryUrl.getIndexedValue(repoIndex);
	
			ComboPooledDataSource cpds = new ComboPooledDataSource();
			
			cpds.setDriverClass( "org.gjt.mm.mysql.Driver" ); //loads the jdbc driver 
			cpds.setJdbcUrl(url); 
			cpds.setUser(user);
			cpds.setPassword(pass); 
			cpds.setIdleConnectionTestPeriod(10);
			CatmaDataSourceName.CATMADS.setDataSource(cpds);
			
			context.bind(CatmaDataSourceName.CATMADS.name(), cpds);
			
			log("CATMA DB DataSource initialized.");
			
			log("CATMA Graph DataSource initializing...");
			String graphDbPath = RepositoryPropertyKey.GraphDbPath.getIndexedValue(repoIndex);
			
			final GraphDatabaseService graphDb = 
				new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(graphDbPath))
				.loadPropertiesFromFile(cfg.getServletContext().getRealPath("neo4j.properties"))
				.newGraphDatabase();
			
			CatmaGraphDbName.CATMAGRAPHDB.setGraphDatabaseService(graphDb);

	        
            try ( Transaction tx = graphDb.beginTx() )
            {
                Schema schema = graphDb.schema();
                for (IndexDefinition indexDef : schema.getIndexes()) {
                	log(indexDef.toString() + " " + schema.getIndexState(indexDef));
                }
                
            	if (!hasIndex(schema, NodeType.SourceDocument, SourceDocumentProperty.localUri)) {
            		schema.indexFor(NodeType.SourceDocument)
                      .on(SourceDocumentProperty.localUri.name())
                      .create();	
            	}
                
            	if (!hasIndex(schema, NodeType.SourceDocument, SourceDocumentProperty.deleted)) {
            		schema.indexFor(NodeType.SourceDocument)
                      .on(SourceDocumentProperty.deleted.name())
                      .create();	
            	}
            	
            	if (!hasIndex(schema, NodeType.Term, TermProperty.literal)) {
                	schema.indexFor(NodeType.Term)
	                	.on(TermProperty.literal.name())
	                	.create();
                }
                
                if (!hasIndex(schema, NodeType.Term, TermProperty.freq)) {
                	schema.indexFor(NodeType.Term)
	                	.on(TermProperty.freq.name())
	                	.create();
                }
                tx.success();
            }
            try (Transaction tx = graphDb.beginTx()) {
            	Schema schema = graphDb.schema();	
	            schema.awaitIndexesOnline(120, TimeUnit.SECONDS );
            }
            catch (IllegalStateException ise) {
            	log("indexes not online yet: " + ise.getMessage());
            }
            
            log("CATMA Graph DataSource initialized.");
            
            IndexBufferManager indexBufferManager = new IndexBufferManager();
            IndexBufferManagerName.INDEXBUFFERMANAGER.setIndeBufferManager(indexBufferManager);
        }
        catch (Exception e) {
        	throw new ServletException(e);
        }
	}
    
    
    private boolean hasIndex(Schema schema, Label nodeType, Enum<?> propName) {
    	for (IndexDefinition indexDef : schema.getIndexes(nodeType)) {
	    	for (String property : indexDef.getPropertyKeys()) {
	    		if (property.equals(propName.name())) {
	    			return true;
	    		}
	    	}
    	}
    	return false;
	}


	@Override
    public void destroy() {
    	super.destroy();
    	try {
    		log("Closing CATMA DB DataSource...");
    		((ComboPooledDataSource)CatmaDataSourceName.CATMADS.getDataSource()).close();
    		log("CATMA DB DataSource is closed.");
    	}
    	catch (Exception e) {
    		log("Error closing CATMA DB DataSource", e);
    	}
    	try {
    		log("Closing CATMA Graph DataSource...");
    		CatmaGraphDbName.CATMAGRAPHDB.getGraphDatabaseService().shutdown();
    		log("CATMA Graph DataSource is closed.");
    	}
    	catch (Exception e) {
    		log("Error closing CATMA Graph DataSource", e);
    	}
    }

}
