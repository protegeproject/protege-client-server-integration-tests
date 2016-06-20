package org.protege.editor.owl.integration;

import java.io.File;
import java.net.URISyntaxException;
import java.util.UUID;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.BeforeClass;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.LocalRMIClient;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.ui.DefaultUserAuthenticator;
import org.protege.editor.owl.client.util.ServerUtils;
import org.protege.editor.owl.server.http.HTTPServer;
import org.protege.editor.owl.server.transport.rmi.RemoteLoginService;
import org.protege.editor.owl.server.transport.rmi.RmiLoginService;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.junit.AfterClass;

import edu.stanford.protege.metaproject.Manager;
import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.MetaprojectFactory;
import edu.stanford.protege.metaproject.api.PlainPassword;
import edu.stanford.protege.metaproject.api.UserId;

public abstract class BaseTest {

    protected static final String SERVER_ADDRESS = "http://localhost:8080";
    protected static final int REGISTRY_PORT = 5200;

    protected static final DocumentRevision R0 = DocumentRevision.START_REVISION;
    protected static final DocumentRevision R1 = DocumentRevision.create(1);
    protected static final DocumentRevision R2 = DocumentRevision.create(2);
    protected static final DocumentRevision R3 = DocumentRevision.create(3);
    protected static final DocumentRevision R4 = DocumentRevision.create(4);
    protected static final DocumentRevision R5 = DocumentRevision.create(5);

    protected static MetaprojectFactory f = Manager.getFactory();

    protected OWLOntologyManager owlManager;
    
    private static HTTPServer httpServer = null;

    private LocalHttpClient admin;

    protected static class PizzaOntology {

        static final String getId() {
            return "http://www.co-ode.org/ontologies/pizza/pizza.owl";
        }

        static final File getResource() {
            try {
                return new File(NewProjectTest.class.getResource("/pizza.owl").toURI());
            }
            catch (URISyntaxException e) {
                throw new OWLRuntimeException("File not found", e);
            }
        }
    }
    
    protected static class LargeOntology {

        static final String getId() {
            return "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
        }

        static final File getResource() {
            try {
                return new File(NewProjectTest.class.getResource("/thesaurus.owl").toURI());
            }
            catch (URISyntaxException e) {
                throw new OWLRuntimeException("File not found", e);
            }
        }
    }

    @Before
    public void setup() {
        owlManager = OWLManager.createOWLOntologyManager();
    }

    @Before
    public void connectToServer() throws Exception {
        UserId userId = f.getUserId("root");
        PlainPassword password = f.getPlainPassword("rootpwd");
        admin = login(userId, password);
    }
    
   
    @BeforeClass
    public static void startServer() throws Exception {
    	String cfn = "server-configuration.json";
    	
    	File f = new File(cfn);
    	boolean bool = f.exists();
    	
    	httpServer = new HTTPServer(cfn);
    	httpServer.start();
    	
    }
    


    protected LocalHttpClient getAdmin() {
        return admin;
    }

    protected static LocalHttpClient login(UserId userId, PlainPassword password) throws Exception {
        
        return new LocalHttpClient(userId.get(), password.getPassword(), SERVER_ADDRESS);
    }

    protected static String uuid8char() {
        final UUID uuid = UUID.randomUUID();
        return uuid.toString().replace("-", "").substring(0, 8);
    }

    protected static class CauseMatcher extends TypeSafeMatcher<Throwable> {
        
        private final Class<? extends Throwable> type;
        private final String expectedMessage;
     
        public CauseMatcher(Class<? extends Throwable> type, String expectedMessage) {
            this.type = type;
            this.expectedMessage = expectedMessage;
        }
     
        @Override
        protected boolean matchesSafely(Throwable item) {
            return item.getClass().isAssignableFrom(type)
                    && item.getMessage().contains(expectedMessage);
        }
     
        @Override
        public void describeTo(Description description) {
            description.appendText("expects type ")
                    .appendValue(type)
                    .appendText(" and a message ")
                    .appendValue(expectedMessage);
        }
    }
    

    @AfterClass
    public static void stopServer() throws Exception {
    	httpServer.stop();

    }
    
}
