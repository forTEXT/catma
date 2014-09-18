package de.catma.servlet;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.IndexManager;
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
	        log("CATMA Datasource initializing...");
	        
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
			
			log("CATMA DataSource initialized.");
			
			String graphDbPath = properties.getProperty(RepositoryPropertyKey.GraphDbPath.name());
			
			final GraphDatabaseService graphDb = 
				new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(graphDbPath)
				.loadPropertiesFromFile(cfg.getServletContext().getRealPath("neo4j.properties"))
				.newGraphDatabase();
			
			context.bind(CatmaGraphDbName.CATMAGRAPHDB.name(), graphDb);
			
	        Runtime.getRuntime().addShutdownHook( new Thread() {
	            @Override
	            public void run() {
	            	try {
	            		graphDb.shutdown();
	            	}
	            	catch (Exception e) {
	            		e.printStackTrace();
	            	}
	            }
	        });
	
	        
//            try ( Transaction tx = graphDb.beginTx() )
//            {
//                IndexManager indexManager = graphDb.index();
//                for (String name : indexManager.nodeIndexNames()) {
//                	System.out.println(name);
//                }
//                Schema schema = graphDb.schema();
//                schema.indexFor(NodeType.SourceDocument)
//                        .on(SourceDocumentProperty.localUri.name())
//                        .create();
//                
//                schema.indexFor(NodeType.Term)
//                		.on(TermProperty.literal.name())
//                		.create();
//
//                schema.indexFor(NodeType.Term)
//                		.on(TermProperty.freq.name())
//                		.create();
//                
//                tx.success();
//            }
//            try (Transaction tx = graphDb.beginTx()) {
//            	Schema schema = graphDb.schema();	
//	            schema.awaitIndexesOnline(100, TimeUnit.SECONDS );
//            }
        }
        catch (Exception e) {
        	throw new ServletException(e);
        }
	}
    
    
    @Override
    public void destroy() {
    	super.destroy();
    	
    	try {
    		log("Closing CATMA DataSource...");
    		((ComboPooledDataSource)new InitialContext().lookup(
    				CatmaDataSourceName.CATMADS.name())).close();
    		log("CATMA DataSource is closed.");
    	}
    	catch (Exception e) {
    		log("Error closing CATMA DataSource", e);
    	}
    }

}
