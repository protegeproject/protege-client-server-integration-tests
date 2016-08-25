package org.protege.editor.owl.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Declaration;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.IRI;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.SubClassOf;

import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.server.api.exception.OperationNotAllowedException;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.RemoveAxiom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
public class CommitChangesTest extends BaseTest {

    private static final String ONTOLOGY_ID = PizzaOntology.getId() + "#";

    private static final OWLClass DOMAIN_CONCEPT = Class(IRI(ONTOLOGY_ID, "DomainConcept"));
    private static final OWLClass CUSTOMER = Class(IRI(ONTOLOGY_ID, "Customer"));
    private static final OWLClass MEAT_TOPPING = Class(IRI(ONTOLOGY_ID, "MeatTopping"));

    private ProjectId projectId;

    private LocalHttpClient guest;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void createProject() throws Exception {
        /*
         * User inputs part
         */
    	this.connectToServer(ADMIN_SERVER_ADDRESS);
        projectId = f.getProjectId("pizza-" + System.currentTimeMillis()); // currentTimeMilis() for uniqueness
        Name projectName = f.getName("Pizza Project");
        Description description = f.getDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        UserId owner = f.getUserId("root");
        Optional<ProjectOptions> options = Optional.ofNullable(null);
        
        Project proj = f.getProject(projectId, projectName, description, PizzaOntology.getResource(), owner, options);
       
        getAdmin().createProject(proj);
    }

    private VersionedOWLOntology openProjectAsAdmin() throws Exception {
    	connectToServer(SERVER_ADDRESS);
        ServerDocument serverDocument = getAdmin().openProject(projectId);
        return getAdmin().buildVersionedOntology(serverDocument, owlManager, projectId);
    }

    private VersionedOWLOntology openProjectAsGuest() throws Exception {
        UserId guestId = f.getUserId("guest");
        PlainPassword guestPassword = f.getPlainPassword("guestpwd");
        guest = login(guestId, guestPassword, SERVER_ADDRESS);
        ServerDocument serverDocument = guest.openProject(projectId);
        return guest.buildVersionedOntology(serverDocument, owlManager, projectId);
    }

    @Test
    public void shouldCommitAddition() throws Exception {
        VersionedOWLOntology vont = openProjectAsAdmin();
        OWLOntology workingOntology = vont.getOntology();
        histManager = new SessionRecorder(workingOntology);
        
        /*
         * Simulates user edits over a working ontology (add axioms)
         */
        List<OWLOntologyChange> cs = new ArrayList<OWLOntologyChange>();
        cs.add(new AddAxiom(workingOntology, Declaration(CUSTOMER)));
        cs.add(new AddAxiom(workingOntology, SubClassOf(CUSTOMER, DOMAIN_CONCEPT)));
        
        owlManager.applyChanges(cs);
        
        histManager.logChanges(cs);
        
        
        
        /*
         * Prepare the commit bundle
         */
        List<OWLOntologyChange> changes = histManager.getUncommittedChanges();
        Commit commit = ClientUtils.createCommit(getAdmin(), "Add customer subclass of domain concept", changes);
        DocumentRevision commitBaseRevision = vont.getHeadRevision();
        CommitBundle commitBundle = new CommitBundleImpl(commitBaseRevision, commit);
        
        /*
         * Do commit
         */
        ChangeHistory approvedChanges = getAdmin().commit(projectId, commitBundle);
        
        /*
         * Update local history
         */
        vont.update(approvedChanges);

        ChangeHistory changeHistoryFromClient = vont.getChangeHistory();

        // Assert the local change history
        assertThat("The local change history should not be empty", !changeHistoryFromClient.isEmpty());
        assertThat(changeHistoryFromClient.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromClient.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromClient.getMetadata().size(), is(1));
        assertThat(changeHistoryFromClient.getRevisions().size(), is(1));
        assertThat(changeHistoryFromClient.getChangesForRevision(R1).size(), is(2));
        
        ChangeHistory changeHistoryFromServer = ((LocalHttpClient)getAdmin()).getAllChanges(vont.getServerDocument());
        
        // Assert the remote change history
        assertThat("The remote change history should not be empty", !changeHistoryFromServer.isEmpty());
        assertThat(changeHistoryFromServer.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromServer.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromServer.getMetadata().size(), is(1));
        assertThat(changeHistoryFromServer.getRevisions().size(), is(1));
        assertThat(changeHistoryFromServer.getChangesForRevision(R1).size(), is(2));
    }

    @Test
    public void shouldCommitDeletion() throws Exception {
        VersionedOWLOntology vont = openProjectAsAdmin();
        OWLOntology workingOntology = vont.getOntology();
        histManager = new SessionRecorder(workingOntology);
        
        /*
         * Simulates user edits over a working ontology (remove a class and its references)
         */
        Set<OWLAxiom> axiomsToRemove = new HashSet<>();
        for (OWLAxiom ax : workingOntology.getAxioms()) {
            if (ax.getSignature().contains(MEAT_TOPPING)) {
                axiomsToRemove.add(ax);
            }
            if (ax instanceof OWLAnnotationAssertionAxiom) {
                OWLAnnotationAssertionAxiom asa = (OWLAnnotationAssertionAxiom) ax;
                OWLAnnotationSubject subject = asa.getSubject();
                if (subject instanceof IRI && subject.equals(MEAT_TOPPING.getIRI())) {
                    axiomsToRemove.add(ax);
                }
                OWLAnnotationValue value = asa.getValue();
                if (value instanceof IRI && value.equals(MEAT_TOPPING.getIRI())) {
                    axiomsToRemove.add(ax);
                }
            }
        }
        owlManager.removeAxioms(workingOntology, axiomsToRemove);
        
        List<OWLOntologyChange> cs = new ArrayList<OWLOntologyChange>();
        for (OWLAxiom ax : axiomsToRemove) {
        	cs.add(new RemoveAxiom(workingOntology, ax));
        	
        }
        
        histManager.logChanges(cs);
        
        /*
         * Prepare the commit bundle
         */
        List<OWLOntologyChange> changes = histManager.getUncommittedChanges();
        Commit commit = ClientUtils.createCommit(getAdmin(), "Remove MeatTopping and its references", changes);
        DocumentRevision commitBaseRevision = vont.getHeadRevision();
        CommitBundle commitBundle = new CommitBundleImpl(commitBaseRevision, commit);
        
        /*
         * Do commit
         */
        ChangeHistory approvedChanges = getAdmin().commit(projectId, commitBundle);
        
        /*
         * Update local history
         */
        vont.update(approvedChanges);

        ChangeHistory changeHistoryFromClient = vont.getChangeHistory();

        // Assert the local change history
        assertThat("The local change history should not be empty", !changeHistoryFromClient.isEmpty());
        assertThat(changeHistoryFromClient.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromClient.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromClient.getMetadata().size(), is(1));
        assertThat(changeHistoryFromClient.getRevisions().size(), is(1));
        assertThat(changeHistoryFromClient.getChangesForRevision(R1).size(), is(16));
        
        ChangeHistory changeHistoryFromServer = ((LocalHttpClient)getAdmin()).getAllChanges(vont.getServerDocument());
        
        // Assert the remote change history
        assertThat("The remote change history should not be empty", !changeHistoryFromServer.isEmpty());
        assertThat(changeHistoryFromServer.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromServer.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromServer.getMetadata().size(), is(1));
        assertThat(changeHistoryFromServer.getRevisions().size(), is(1));
        assertThat(changeHistoryFromServer.getChangesForRevision(R1).size(), is(16));
    }

    @Test
    public void shouldNotCommitChange() throws Exception {
        VersionedOWLOntology vont = openProjectAsGuest();
        OWLOntology workingOntology = vont.getOntology();
        histManager = new SessionRecorder(workingOntology);
        
        /*
         * Simulates user edits over a working ontology (add axioms)
         */
        List<OWLOntologyChange> cs = new ArrayList<OWLOntologyChange>();
        cs.add(new AddAxiom(workingOntology, Declaration(CUSTOMER)));
        cs.add(new AddAxiom(workingOntology, SubClassOf(CUSTOMER, DOMAIN_CONCEPT)));
        
        owlManager.applyChanges(cs);
        
        histManager.logChanges(cs);
       
        
        /*
         * Prepare the commit bundle
         */
        List<OWLOntologyChange> changes = histManager.getUncommittedChanges();
        Commit commit = ClientUtils.createCommit(guest, "Add customer subclass of domain concept", changes);
        DocumentRevision commitBaseRevision = vont.getHeadRevision();
        CommitBundle commitBundle = new CommitBundleImpl(commitBaseRevision, commit);
        
        thrown.expect(ClientRequestException.class);
        //thrown.expectCause(new CauseMatcher(OperationNotAllowedException.class,
               // "User has no permission for 'Add axiom' operation"));
        
        /*
         * Do commit
         */
        guest.commit(projectId, commitBundle);
    }

    @After
    public void removeProject() throws Exception {
    	this.connectToServer(ADMIN_SERVER_ADDRESS);
        getAdmin().deleteProject(projectId, true);
    }
}
