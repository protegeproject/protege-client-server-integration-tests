package org.protege.editor.owl.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

import edu.stanford.protege.metaproject.api.Description;
import edu.stanford.protege.metaproject.api.Name;
import edu.stanford.protege.metaproject.api.PlainPassword;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.ProjectOptions;
import edu.stanford.protege.metaproject.api.UserId;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class OpenProjectTest extends BaseTest {

    private ProjectId projectId;

    @Before
    public void createProject() throws Exception {
        /*
         * User inputs part
         */
        projectId = f.getProjectId("pizza-" + System.currentTimeMillis()); // currentTimeMilis() for uniqueness
        Name projectName = f.getName("Pizza Project");
        Description description = f.getDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        UserId owner = f.getUserId("root");
        Optional<ProjectOptions> options = Optional.ofNullable(null);
        
        Project proj = f.getProject(projectId, projectName, description, PizzaOntology.getResource(), owner, options);
       
        getAdmin().createProject(proj);
    }

    @Test
    public void shouldDownloadRemoteChanges() throws Exception {
        /*
         * Login as Guest
         */
        UserId guestId = f.getUserId("guest");
        PlainPassword guestPassword = f.getPlainPassword("guestpwd");
        LocalHttpClient guest = login(guestId, guestPassword);
        
        ServerDocument serverDocument = guest.openProject(projectId);
        VersionedOWLOntology vont = guest.buildVersionedOntology(serverDocument, owlManager, projectId);
        ChangeHistory changeHistoryFromClient = vont.getChangeHistory();
        
        // Assert the remote change history
        assertThat("The local change history should be empty", changeHistoryFromClient.isEmpty());
        assertThat(changeHistoryFromClient.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromClient.getHeadRevision(), is(R0));
        assertThat(changeHistoryFromClient.getMetadata().size(), is(0));
        assertThat(changeHistoryFromClient.getRevisions().size(), is(0));
        
        ChangeHistory changeHistoryFromServer = ((LocalHttpClient) guest).getAllChanges(vont.getServerDocument());
        
        // Assert the remote change history
        assertThat("The remote change history should be empty", changeHistoryFromServer.isEmpty());
        assertThat(changeHistoryFromServer.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromServer.getHeadRevision(), is(R0));
        assertThat(changeHistoryFromServer.getMetadata().size(), is(0));
        assertThat(changeHistoryFromServer.getRevisions().size(), is(0));
    }

    @Test
    public void shouldConstructOntology() throws Exception {
        /*
         * Login as Guest
         */
        UserId guestId = f.getUserId("guest");
        PlainPassword guestPassword = f.getPlainPassword("guestpwd");
        Client guest = login(guestId, guestPassword);
        
        ServerDocument serverDocument = guest.openProject(projectId);
        VersionedOWLOntology vont = ((LocalHttpClient) guest).buildVersionedOntology(serverDocument, owlManager, projectId);
        OWLOntology ontology = vont.getOntology();
        
        // Assert the produced ontology
        OWLOntology originalOntology = owlManager.getOntology(IRI.create(PizzaOntology.getId()));
        assertThat(ontology, is(originalOntology));
        assertThat(ontology.getSignature(), is(originalOntology.getSignature()));
        assertThat(ontology.getAxiomCount(), is(originalOntology.getAxiomCount()));
        assertThat(ontology.getAxioms(), is(originalOntology.getAxioms()));
    }

    @After
    public void removeProject() throws Exception {
        getAdmin().deleteProject(projectId, true);
    }
}
