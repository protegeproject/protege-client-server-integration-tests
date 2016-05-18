package org.protege.editor.owl.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.protege.editor.owl.client.LocalClient;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.ui.DefaultUserAuthenticator;
import org.protege.editor.owl.client.util.ChangeUtils;
import org.protege.editor.owl.client.util.ServerUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.transport.rmi.RemoteLoginService;
import org.protege.editor.owl.server.transport.rmi.RmiLoginService;
import org.protege.editor.owl.server.util.GetUncommittedChangesVisitor;
import org.protege.editor.owl.server.versioning.VersionedOWLOntologyImpl;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.RevisionMetadata;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLRuntimeException;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.stanford.protege.metaproject.Manager;
import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.Description;
import edu.stanford.protege.metaproject.api.MetaprojectFactory;
import edu.stanford.protege.metaproject.api.Name;
import edu.stanford.protege.metaproject.api.PlainPassword;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.ProjectOptions;
import edu.stanford.protege.metaproject.api.UserId;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
@RunWith(MockitoJUnitRunner.class)
public class NewProjectTest {

    private static final String SERVER_ADDRESS = "rmi://localhost:5100";
    private static final int REGISTRY_PORT = 5200;

    private static MetaprojectFactory f = Manager.getFactory();

    private Client localClient;

    protected static final File pizzaOntology() {
        try {
            return new File(NewProjectTest.class.getResource("/pizza.owl").toURI());
        }
        catch (URISyntaxException e) {
            throw new OWLRuntimeException("File not found", e);
        }
    }

    @Before
    public void connectToServer() throws Exception {
        RemoteLoginService loginService = (RemoteLoginService) ServerUtils
                .getRemoteService(SERVER_ADDRESS, REGISTRY_PORT, RmiLoginService.LOGIN_SERVICE);
        DefaultUserAuthenticator authenticator = new DefaultUserAuthenticator(loginService);

        UserId userId = f.getUserId("root");
        PlainPassword plainPassword = f.getPlainPassword("rootpwd");
        AuthToken authToken = authenticator.hasValidCredentials(userId, plainPassword);

        localClient = new LocalClient(authToken, SERVER_ADDRESS, REGISTRY_PORT);
    }

    @Test
    public void createNewProject() throws Exception {
        /*
         * [GUI] The input project properties
         */
        String uniqueness = createUniqueId();
        ProjectId projectId = f.getProjectId("pizza-" + uniqueness);
        Name projectName = f.getName("Pizza Project (" + uniqueness + ")" );
        Description description = f.getDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        UserId owner = f.getUserId("root");
        ProjectOptions options = null;
        
        /*
         * [GUI] The input target ontology
         */
        OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(pizzaOntology());

        /*
         * [NewProjectAction] Compute the initial commit from the input ontology
         */
        GetUncommittedChangesVisitor visitor = new GetUncommittedChangesVisitor(ontology);
        List<OWLOntologyChange> uncommittedChanges = visitor.getChanges();
        RevisionMetadata metadata = new RevisionMetadata(
                localClient.getUserInfo().getId(),
                localClient.getUserInfo().getName(),
                localClient.getUserInfo().getEmailAddress(),
                "First commit");
        CommitBundle commitBundle = new CommitBundleImpl(metadata, uncommittedChanges, DocumentRevision.START_REVISION);
        
        /*
         * [NewProjectAction] Call the remote method for creating a new project with an initial commit.
         * The method will return a ServerDocument which contains the remote resource information.
         */
        ServerDocument document = localClient.createProject(
                projectId, projectName, description, owner, Optional.ofNullable(options), Optional.ofNullable(commitBundle));
        
        /*
         * [NewProjectAction] Finally create the local tracking object that contains a local copy of
         * the change history, the OWL ontology and the remote reference (i.e., ServerDocument).
         */
        VersionedOWLOntology vont = new VersionedOWLOntologyImpl(document, ontology);
        vont.addRevision(metadata, uncommittedChanges);
        
        // Assert the server document
        assertThat(document, is(notNullValue()));
        assertThat(document.getServerAddress(), is(URI.create(SERVER_ADDRESS)));
        assertThat(document.getRegistryPort(), is(REGISTRY_PORT));
        assertThat(document.getHistoryFile(), is(notNullValue()));
        assertThat(document.getHistoryFile().length(), is(greaterThan(new Long(0))));
        
        // Assert the remote change history
        ChangeHistory remoteChangeHistory = ChangeUtils.getAllChanges(document);
        assertThat("The remote change history should not be empty", !remoteChangeHistory.isEmpty());
        assertThat(remoteChangeHistory.getBaseRevision(), is(DocumentRevision.START_REVISION));
        assertThat(remoteChangeHistory.getHeadRevision(), is(DocumentRevision.create(1)));
        
        // Assert the versioned ontology
        assertThat(vont.getBaseRevision(), is(DocumentRevision.START_REVISION));
        assertThat(vont.getHeadRevision(), is(DocumentRevision.create(1)));
        
        // Assert the local change history
        ChangeHistory localChangeHistory = vont.getChangeHistory();
        assertThat("The local change history should not be empty", !localChangeHistory.isEmpty());
        assertThat(localChangeHistory.getBaseRevision(), is(DocumentRevision.START_REVISION));
        assertThat(localChangeHistory.getHeadRevision(), is(DocumentRevision.create(1)));
    }

    private String createUniqueId() {
        final UUID uuid = UUID.randomUUID();
        return uuid.toString().replace("-", "").substring(0, 8);
    }
}
