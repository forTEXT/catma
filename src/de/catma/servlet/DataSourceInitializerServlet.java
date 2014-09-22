package de.catma.servlet;

import java.util.Properties;
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

import de.catma.document.repository.RepositoryPropertiesName;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.indexer.graph.CatmaGraphDbName;
import de.catma.indexer.graph.NodeType;
import de.catma.indexer.graph.SourceDocumentProperty;
import de.catma.indexer.graph.TermProperty;
import de.catma.repository.db.CatmaDataSourceName;

public class DataSourceInitializerServlet extends HttpServlet {
	
    @Override
    public void init(ServletConfig cfg) throws ServletException {
        super.init(cfg);

        try {
	        log("CATMA DB Datasource initializing...");
	        
	        InitialContext context = new InitialContext();
	        
			Properties properties = 
					(Properties) context.lookup(
							RepositoryPropertiesName.CATMAPROPERTIES.name());
	
			int repoIndex = 1; // assume that the first configured repo is the local db repo
			String user = RepositoryPropertyKey.RepositoryUser.getProperty(
					properties, repoIndex);
					
			String pass = RepositoryPropertyKey.RepositoryPass.getProperty(
					properties, repoIndex);
			
			String url = RepositoryPropertyKey.RepositoryUrl.getProperty(
					properties, repoIndex);
	
			ComboPooledDataSource cpds = new ComboPooledDataSource();
			
			cpds.setDriverClass( "org.gjt.mm.mysql.Driver" ); //loads the jdbc driver 
			cpds.setJdbcUrl(url); 
			cpds.setUser(user);
			cpds.setPassword(pass); 
			cpds.setIdleConnectionTestPeriod(10);
	
			context.bind(CatmaDataSourceName.CATMADS.name(), cpds);
			
			log("CATMA DB DataSource initialized.");
			
			log("CATMA Graph DataSource initializing...");
			String graphDbPath = properties.getProperty(RepositoryPropertyKey.GraphDbPath.name());
			
			final GraphDatabaseService graphDb = 
				new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(graphDbPath)
				.loadPropertiesFromFile(cfg.getServletContext().getRealPath("neo4j.properties"))
				.newGraphDatabase();
			
			context.bind(CatmaGraphDbName.CATMAGRAPHDB.name(), graphDb);

			log("CATMA Graph DataSource initialized.");
	        
            try ( Transaction tx = graphDb.beginTx() )
            {
                Schema schema = graphDb.schema();
                for (IndexDefinition indexDef : schema.getIndexes(NodeType.Position)) {
                	System.out.println(schema.getIndexState(indexDef));
                }
                
            	if (!hasIndex(schema, NodeType.SourceDocument, SourceDocumentProperty.localUri)) {
            		schema.indexFor(NodeType.SourceDocument)
                      .on(SourceDocumentProperty.localUri.name())
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
    		((ComboPooledDataSource)new InitialContext().lookup(
    				CatmaDataSourceName.CATMADS.name())).close();
    		log("CATMA DB DataSource is closed.");
    	}
    	catch (Exception e) {
    		log("Error closing CATMA DB DataSource", e);
    	}
    	try {
    		log("Closing CATMA Graph DataSource...");
    		((GraphDatabaseService)new InitialContext().lookup(
    				CatmaGraphDbName.CATMAGRAPHDB.name())).shutdown();
    		log("CATMA Graph DataSource is closed.");
    	}
    	catch (Exception e) {
    		log("Error closing CATMA Graph DataSource", e);
    	}
    }

}
