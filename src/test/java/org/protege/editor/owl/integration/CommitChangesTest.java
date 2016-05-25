package org.protege.editor.owl.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Declaration;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.IRI;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.SubClassOf;

import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
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
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;

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
public class CommitChangesTest extends BaseTest {

    private static final String ONTOLOGY_ID = "http://www.co-ode.org/ontologies/pizza/pizza.owl#";

    private static final OWLClass DOMAIN_CONCEPT = Class(IRI(ONTOLOGY_ID, "DomainConcept"));
    private static final OWLClass CUSTOMER = Class(IRI(ONTOLOGY_ID, "Customer"));

    private ProjectId projectId;

    private Client guest;

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
        OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(pizzaOntology());

        /*
         * Create a new project
         */
        GetUncommittedChangesVisitor visitor = new GetUncommittedChangesVisitor(ontology);
        List<OWLOntologyChange> changes = visitor.getChanges();
        RevisionMetadata metadata = new RevisionMetadata(
                getAdmin().getUserInfo().getId(),
                getAdmin().getUserInfo().getName(),
                getAdmin().getUserInfo().getEmailAddress(),
                "First commit");
        CommitBundle commitBundle = new CommitBundleImpl(R0, new Commit(metadata, changes));
        getAdmin().createProject(projectId, projectName, description, owner,
                Optional.ofNullable(options), Optional.ofNullable(commitBundle));
    }

    private VersionedOWLOntology openProjectAsAdmin() throws Exception {
        return getAdmin().openProject(projectId);
    }

    private VersionedOWLOntology openProjectAsGuest() throws Exception {
        UserId guestId = f.getUserId("guest");
        PlainPassword guestPassword = f.getPlainPassword("guestpwd");
        guest = login(guestId, guestPassword);
        return guest.openProject(projectId);
    }

    @Test
    public void shouldCommitChange() throws Exception {
        VersionedOWLOntology vont = openProjectAsAdmin();
        OWLOntology workingOntology = vont.getOntology();
        
        OWLOntologyChange addCustomerDecl = new AddAxiom(workingOntology, Declaration(CUSTOMER));
        OWLOntologyChange addCustomerSubClassOfDomainConcept = new AddAxiom(workingOntology, SubClassOf(CUSTOMER, DOMAIN_CONCEPT));
        List<OWLOntologyChange> changes = Lists.newArrayList(addCustomerDecl, addCustomerSubClassOfDomainConcept);
        
        RevisionMetadata metadata = new RevisionMetadata(
                getAdmin().getUserInfo().getId(),
                getAdmin().getUserInfo().getName(),
                getAdmin().getUserInfo().getEmailAddress(),
                "Add customer subclass of domain concept");
        CommitBundle commitBundle = new CommitBundleImpl(vont.getHeadRevision(), new Commit(metadata, changes));
        ChangeHistory approvedChanges = getAdmin().commit(projectId, commitBundle);
        vont.update(approvedChanges);

        ChangeHistory changeHistoryFromClient = vont.getChangeHistory();

        // Assert the remote change history
        assertThat("The local change history should not be empty", !changeHistoryFromClient.isEmpty());
        assertThat(changeHistoryFromClient.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromClient.getHeadRevision(), is(R2)    );
        assertThat(changeHistoryFromClient.getMetadata().size(), is(2));
        assertThat(changeHistoryFromClient.getRevisions().size(), is(2));
        assertThat(changeHistoryFromClient.getChangesForRevision(R1).size(), is(945));
        assertThat(changeHistoryFromClient.getChangesForRevision(R2).size(), is(2));
        
        ChangeHistory changeHistoryFromServer = ChangeUtils.getAllChanges(vont.getServerDocument());
        
        // Assert the remote change history
        assertThat("The remote change history should not be empty", !changeHistoryFromServer.isEmpty());
        assertThat(changeHistoryFromServer.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromServer.getHeadRevision(), is(R2));
        assertThat(changeHistoryFromServer.getMetadata().size(), is(2));
        assertThat(changeHistoryFromServer.getRevisions().size(), is(2));
        assertThat(changeHistoryFromServer.getChangesForRevision(R1).size(), is(945));
        assertThat(changeHistoryFromServer.getChangesForRevision(R2).size(), is(2));
    }

    @Test(expected=ClientRequestException.class)
    public void shouldNotCommitChange() throws Exception {
        VersionedOWLOntology vont = openProjectAsGuest();
        OWLOntology workingOntology = vont.getOntology();
        
        OWLOntologyChange addCustomerDecl = new AddAxiom(workingOntology, Declaration(CUSTOMER));
        OWLOntologyChange addCustomerSubClassOfDomainConcept = new AddAxiom(workingOntology, SubClassOf(CUSTOMER, DOMAIN_CONCEPT));
        List<OWLOntologyChange> changes = Lists.newArrayList(addCustomerDecl, addCustomerSubClassOfDomainConcept);
        
        RevisionMetadata metadata = new RevisionMetadata(
                guest.getUserInfo().getId(),
                guest.getUserInfo().getName(),
                guest.getUserInfo().getEmailAddress(),
                "Add customer subclass of domain concept");
        CommitBundle commitBundle = new CommitBundleImpl(vont.getHeadRevision(), new Commit(metadata, changes));
        guest.commit(projectId, commitBundle);
    }

    @After
    public void removeProject() throws Exception {
        getAdmin().deleteProject(projectId, true);
    }
}
