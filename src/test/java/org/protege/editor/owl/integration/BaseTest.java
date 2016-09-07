package org.protege.editor.owl.integration;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.protege.editor.owl.client.LocalHttpClient;
//import org.protege.editor.owl.model.history.HistoryManagerImpl;
import org.protege.editor.owl.server.http.HTTPServer;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLRuntimeException;

import edu.stanford.protege.metaproject.ConfigurationManager;
import edu.stanford.protege.metaproject.api.PlainPassword;
import edu.stanford.protege.metaproject.api.PolicyFactory;
import edu.stanford.protege.metaproject.api.UserId;

public abstract class BaseTest {

    private static final String orginalConfigLocation = "src/test/resources/server-configuration.json";
    private static final String workingConfigLocation = "src/test/resources/working-server-configuration.json";

    protected static final String SERVER_ADDRESS = "http://localhost:8080";
    protected static final String ADMIN_SERVER_ADDRESS = "http://localhost:8081";

    protected static final DocumentRevision R0 = DocumentRevision.START_REVISION;
    protected static final DocumentRevision R1 = DocumentRevision.create(1);
    protected static final DocumentRevision R2 = DocumentRevision.create(2);
    protected static final DocumentRevision R3 = DocumentRevision.create(3);
    protected static final DocumentRevision R4 = DocumentRevision.create(4);
    protected static final DocumentRevision R5 = DocumentRevision.create(5);

    protected static PolicyFactory f = ConfigurationManager.getFactory();

    protected OWLOntologyManager owlManager;
    
    protected SessionRecorder histManager;
    
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

    public void connectToServer(String address) throws Exception {
        UserId userId = f.getUserId("root");
        PlainPassword password = f.getPlainPassword("rootpwd");
        admin = login(userId, password, address);
    }
    
   
    @BeforeClass
    public static void startServer() throws Exception {
        initServerConfiguration();
        httpServer = new HTTPServer();
        httpServer.start();
    }

    private static void initServerConfiguration() throws IOException {
        File originalCopy = new File(orginalConfigLocation);
        File workingCopy = new File(workingConfigLocation);
        FileUtils.copyFile(originalCopy, workingCopy);
        System.setProperty(HTTPServer.SERVER_CONFIGURATION_PROPERTY, workingConfigLocation);
    }
    


    protected LocalHttpClient getAdmin() {
        return admin;
    }

    protected static LocalHttpClient login(UserId userId, PlainPassword password, String address) throws Exception {
        
        return new LocalHttpClient(userId.get(), password.getPassword(), address);
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
        removeServerConfiguration();
    }

    private static void removeServerConfiguration() {
        File workingCopy = new File(workingConfigLocation);
        if (workingCopy.exists()) {
            workingCopy.delete();
        }
    }
    
}
