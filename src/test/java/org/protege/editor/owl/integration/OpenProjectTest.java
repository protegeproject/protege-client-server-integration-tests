package org.protege.editor.owl.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.util.ChangeUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.util.GetUncommittedChangesVisitor;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.RevisionMetadata;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.List;
import java.util.Optional;

import edu.stanford.protege.metaproject.api.Description;
import edu.stanford.protege.metaproject.api.Name;
import edu.stanford.protege.metaproject.api.PlainPassword;
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
        projectId = f.getProjectId("pizza");
        Name projectName = f.getName("Pizza Project" );
        Description description = f.getDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        UserId owner = f.getUserId("root");
        ProjectOptions options = null;

        /*
         * Create a new project
         */
        OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(pizzaOntology());
        GetUncommittedChangesVisitor visitor = new GetUncommittedChangesVisitor(ontology);
        List<OWLOntologyChange> changes = visitor.getChanges();
        RevisionMetadata metadata = new RevisionMetadata(
                getAdmin().getUserInfo().getId(),
                getAdmin().getUserInfo().getName(),
                getAdmin().getUserInfo().getEmailAddress(),
                "First commit");
        CommitBundle commitBundle = new CommitBundleImpl(R0, new Commit(metadata, changes));
        getAdmin().createProject(projectId, projectName, description, owner, Optional.ofNullable(options), Optional.ofNullable(commitBundle));
    }

    @Test
    public void shouldDownloadRemoteChanges() throws Exception {
        /*
         * Login as Guest
         */
        UserId guestId = f.getUserId("guest");
        PlainPassword guestPassword = f.getPlainPassword("guestpwd");
        Client guest = login(guestId, guestPassword);
        
        VersionedOWLOntology vont = guest.openProject(projectId);
        ChangeHistory changeHistoryFromClient = vont.getChangeHistory();
        
        // Assert the remote change history
        assertThat("The local change history should not be empty", !changeHistoryFromClient.isEmpty());
        assertThat(changeHistoryFromClient.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromClient.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromClient.getMetadata().size(), is(1));
        assertThat(changeHistoryFromClient.getRevisions().size(), is(1));
        assertThat(changeHistoryFromClient.getChangesForRevision(R1).size(), is(945));
        
        ChangeHistory changeHistoryFromServer = ChangeUtils.getAllChanges(vont.getServerDocument());
        
        // Assert the remote change history
        assertThat("The remote change history should not be empty", !changeHistoryFromServer.isEmpty());
        assertThat(changeHistoryFromServer.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromServer.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromServer.getMetadata().size(), is(1));
        assertThat(changeHistoryFromServer.getRevisions().size(), is(1));
        assertThat(changeHistoryFromServer.getChangesForRevision(R1).size(), is(945));
    }

    @Test
    public void shouldConstructOntology() throws Exception {
        /*
         * Login as Guest
         */
        UserId guestId = f.getUserId("guest");
        PlainPassword guestPassword = f.getPlainPassword("guestpwd");
        Client guest = login(guestId, guestPassword);
        
        VersionedOWLOntology vont = guest.openProject(projectId);
        OWLOntology ontology = vont.getOntology();
        
        // Assert the produced ontology
        OWLOntology originalOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(pizzaOntology());
        assertThat(ontology.getSignature(), is(originalOntology.getSignature()));
        assertThat(ontology.getAxiomCount(), is(originalOntology.getAxiomCount()));
    }

    @After
    public void removeProject() throws Exception {
        getAdmin().deleteProject(projectId, true);
    }
}
