package org.protege.editor.owl.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Declaration;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.IRI;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.SubClassOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
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
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;

import edu.stanford.protege.metaproject.api.Password;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.impl.ProjectOptionsImpl;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class CommitChangesTest extends ProjectBaseTest {

    private static final String ONTOLOGY_ID = PizzaOntology.getId() + "#";

    private static final OWLClass DOMAIN_CONCEPT = Class(IRI(ONTOLOGY_ID, "DomainConcept"));
    private static final OWLClass CUSTOMER = Class(IRI(ONTOLOGY_ID, "Customer"));
    private static final OWLClass MEAT_TOPPING = Class(IRI(ONTOLOGY_ID, "MeatTopping"));

    private final OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();

    private ProjectId projectId;

    private VersionedOWLOntology versionedOntology;

    private LocalHttpClient admin;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        startCleanServer();
        admin = connectAsAdmin();
        projectId = createPizzaProject();
        createNewUserJohn();
    }

    private ProjectId createPizzaProject() throws Exception {
        Map<String, Set<String>> options = new HashMap<>();
        options.put("key_a", new HashSet<String>(Arrays.asList("value_1", "value_2")));
        options.put("key_b", new HashSet<String>(Arrays.asList("value_3")));
        Project pizzaProject = TestUtils.createProject("pizza-project", "Pizza Project",
                "Creating a useful pizza classification",
                PizzaOntology.getResource(), "guest",
                Optional.of(new ProjectOptionsImpl(options)));
        admin.createProject(pizzaProject);
        return pizzaProject.getId();
    }

    private void createNewUserJohn() throws Exception {
        User user = TestUtils.createUser("john", "John Doe", "john.doe@email.com");
        Password password = TestUtils.createPassword("johnpwd");
        admin.createUser(user, Optional.of(password));
        admin.reallyPutConfig();
    }

    @After
    public void cleanUp() throws Exception {
        stopServer();
        removeDataDirectory();
        removeSnapshotFiles();
    }

    @Test
    public void shouldCommitUserChanges_AddAxioms() throws Exception {
        LocalHttpClient guestUser = connectAsGuest();
        
        OWLOntology workingOntology = openRemoteProject(guestUser);
        List<OWLOntologyChange> userChanges = simulateAddingAxiomInOntology(workingOntology);
        applyUserChanges(userChanges);
        
        CommitBundle commitBundle = createCommitBundle(guestUser,
                userChanges, // This input could be obtained from some history manager in Protege
                "Add customer subclass of domain concept");
        
        ChangeHistory approvedChanges = performCommit(guestUser, commitBundle);
        updateLocalChangeHistory(approvedChanges);

        // Assert the local change history
        ChangeHistory changeHistoryFromClient = getLocalChangeHistory();
        assertThat(changeHistoryFromClient.isEmpty(), is(false));
        assertThat(changeHistoryFromClient.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromClient.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromClient.getMetadata().size(), is(1));
        assertThat(changeHistoryFromClient.getRevisions().size(), is(1));
        assertThat(changeHistoryFromClient.getChangesForRevision(R1).size(), is(2));
        
        // Assert the remote change history
        ChangeHistory changeHistoryFromServer = getRemoteChangeHistory(guestUser);
        assertThat(changeHistoryFromServer.isEmpty(), is(false));
        assertThat(changeHistoryFromServer.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromServer.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromServer.getMetadata().size(), is(1));
        assertThat(changeHistoryFromServer.getRevisions().size(), is(1));
        assertThat(changeHistoryFromServer.getChangesForRevision(R1).size(), is(2));
    }

    @Test
    public void shouldCommitUserChanges_RemoveAxioms() throws Exception {
        LocalHttpClient guestUser = connectAsGuest();
        
        OWLOntology workingOntology = openRemoteProject(guestUser);
        List<OWLOntologyChange> userChanges = simulateRemovingAxiomsFromOntology(workingOntology);
        applyUserChanges(userChanges);
        
        CommitBundle commitBundle = createCommitBundle(guestUser,
                userChanges, // This input could be obtained from some history manager in Protege
                "Remove MeatTopping and its references");
        
        ChangeHistory approvedChanges = performCommit(guestUser, commitBundle);
        updateLocalChangeHistory(approvedChanges);
        
        // Assert the local change history
        ChangeHistory changeHistoryFromClient = getLocalChangeHistory();
        assertThat(changeHistoryFromClient.isEmpty(), is(false));
        assertThat(changeHistoryFromClient.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromClient.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromClient.getMetadata().size(), is(1));
        assertThat(changeHistoryFromClient.getRevisions().size(), is(1));
        assertThat(changeHistoryFromClient.getChangesForRevision(R1).size(), is(16));
        
        // Assert the remote change history
        ChangeHistory changeHistoryFromServer = getRemoteChangeHistory(guestUser);
        assertThat(changeHistoryFromServer.isEmpty(), is(false));
        assertThat(changeHistoryFromServer.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromServer.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromServer.getMetadata().size(), is(1));
        assertThat(changeHistoryFromServer.getRevisions().size(), is(1));
        assertThat(changeHistoryFromServer.getChangesForRevision(R1).size(), is(16));
    }

    @Test
    public void shouldNotCommitUserChanges() throws Exception {
        LocalHttpClient johnUser = connect("john", "johnpwd", SERVER_ADDRESS);
        
        OWLOntology workingOntology = openRemoteProject(johnUser);
        List<OWLOntologyChange> userChanges = simulateAddingAxiomInOntology(workingOntology);
        applyUserChanges(userChanges);
        
        CommitBundle commitBundle = createCommitBundle(johnUser,
                userChanges, // This input could be obtained from some history manager in Protege
                "Add customer subclass of domain concept");
        
        thrown.expect(ClientRequestException.class);
        performCommit(johnUser, commitBundle);
    }

    /*
     * A collection of private helper methods
     */

    private OWLOntology openRemoteProject(LocalHttpClient author) throws Exception {
        ServerDocument serverDocument = author.openProject(projectId);
        setVersionedOntology(author, serverDocument);
        return versionedOntology.getOntology();
    }

    private void setVersionedOntology(LocalHttpClient author, ServerDocument serverDocument) throws Exception {
        versionedOntology = author.buildVersionedOntology(serverDocument, ontologyManager, projectId);
    }

    private List<OWLOntologyChange> simulateAddingAxiomInOntology(OWLOntology workingOntology) {
        List<OWLOntologyChange> userChanges = new ArrayList<OWLOntologyChange>();
        userChanges.add(new AddAxiom(workingOntology, Declaration(CUSTOMER)));
        userChanges.add(new AddAxiom(workingOntology, SubClassOf(CUSTOMER, DOMAIN_CONCEPT)));
        return userChanges;
    }

    private List<OWLOntologyChange> simulateRemovingAxiomsFromOntology(OWLOntology workingOntology) {
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
        List<OWLOntologyChange> userChanges = new ArrayList<>();
        for (OWLAxiom ax : axiomsToRemove) {
            userChanges.add(new RemoveAxiom(workingOntology, ax)); 
        }
        return userChanges;
    }

    private CommitBundle createCommitBundle(Client author, List<OWLOntologyChange> changes, String message) {
        Commit commit = ClientUtils.createCommit(author, message, changes);
        return new CommitBundleImpl(versionedOntology.getHeadRevision(), commit);
    }

    private void applyUserChanges(List<OWLOntologyChange> userChanges) {
        ontologyManager.applyChanges(userChanges);
    }

    private void updateLocalChangeHistory(ChangeHistory changes) {
        versionedOntology.update(changes);
    }

    private ChangeHistory performCommit(LocalHttpClient author, CommitBundle commitBundle) throws Exception {
        return author.commit(projectId, commitBundle);
    }

    private ChangeHistory getLocalChangeHistory() {
        return versionedOntology.getChangeHistory();
    }

    private ChangeHistory getRemoteChangeHistory(LocalHttpClient author) throws Exception {
        return author.getAllChanges(versionedOntology.getServerDocument());
    }
}
