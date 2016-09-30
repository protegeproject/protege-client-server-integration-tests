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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import edu.stanford.protege.metaproject.api.Password;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.impl.ConfigurationUtils;
import edu.stanford.protege.metaproject.impl.ProjectOptionsImpl;

public class ConcurentCommitChangesTest extends ProjectBaseTest {

    private static final String ONTOLOGY_ID = PizzaOntology.getId() + "#";

    private static final OWLClass DOMAIN_CONCEPT = Class(IRI(ONTOLOGY_ID, "DomainConcept"));
    private static final OWLClass CUSTOMER = Class(IRI(ONTOLOGY_ID, "Customer"));

    private final ExecutorService executionService = Executors.newFixedThreadPool(3);

    private ProjectId projectId;

    private LocalHttpClient admin;

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
        admin.assignRole(user.getId(), projectId, ConfigurationUtils.getProjectManagerRole().getId());
        admin.reallyPutConfig();
    }

    @After
    public void cleanUp() throws Exception {
        stopServer();
        removeDataDirectory();
        removeSnapshotFiles();
    }

    @Test
    public void shouldCommitUserChangesConcurrently() throws Exception {
        /*
         * Construct commit bundles from 3 different users
         */
        LocalHttpClient adminUser = connect("root", "rootpwd", SERVER_ADDRESS);
        VersionedOWLOntology adminVersionedOntology = openRemoteProjectAndGetVersionedOntology(adminUser);
        CommitBundle adminCommitBundle = simulateMakingChangesAndCreateCommitBundle(adminUser,
                "Add a subclass axiom", adminVersionedOntology);
        
        LocalHttpClient guestUser = connect("guest", "guestpwd", SERVER_ADDRESS);
        VersionedOWLOntology guestVersionedOntology = openRemoteProjectAndGetVersionedOntology(guestUser);
        CommitBundle guestCommitBundle = simulateMakingChangesAndCreateCommitBundle(guestUser,
                "Add a new classification to the ontology", guestVersionedOntology);
        
        LocalHttpClient johnUser = connect("john", "johnpwd", SERVER_ADDRESS);
        VersionedOWLOntology johnVersionedOntology = openRemoteProjectAndGetVersionedOntology(guestUser);
        CommitBundle johnCommitBundle = simulateMakingChangesAndCreateCommitBundle(guestUser,
                "Add customer subclass of domain concept", guestVersionedOntology);
        
        /*
         * Submit the commits to the server (almost) at the same time
         */
        Future<ChangeHistory> adminSubmitCommitTask = executionService.submit(new Callable<ChangeHistory>() {
            @Override
            public ChangeHistory call() throws Exception {
                return guestUser.commit(projectId, adminCommitBundle);
            }
        });

        Future<ChangeHistory> guestSubmitCommitTask = executionService.submit(new Callable<ChangeHistory>() {
            @Override
            public ChangeHistory call() throws Exception {
                return guestUser.commit(projectId, guestCommitBundle);
            }
        });

        Future<ChangeHistory> johnSubmitCommitTask = executionService.submit(new Callable<ChangeHistory>() {
            @Override
            public ChangeHistory call() throws Exception {
                return johnUser.commit(projectId, johnCommitBundle);
            }
        });
        
        /*
         * Assert which user who got his changes accepted by the server
         */
        try {
            ChangeHistory approvedChanges = adminSubmitCommitTask.get();
            updateLocalChangeHistory(adminVersionedOntology, approvedChanges);
            assertAcceptedCommit(adminUser, adminVersionedOntology);
        } catch (ExecutionException ex) {
            System.err.println("User Admin failed to perform the commit");
            assertThat(ex.getCause().getMessage(), is("Commit failed, please update your local copy first"));
        }

        try {
            ChangeHistory approvedChanges = guestSubmitCommitTask.get();
            updateLocalChangeHistory(guestVersionedOntology, approvedChanges);
            assertAcceptedCommit(guestUser, guestVersionedOntology);
        } catch (ExecutionException ex) {
            System.err.println("User Guest failed to perform the commit");
            assertThat(ex.getCause().getMessage(), is("Commit failed, please update your local copy first"));
        }
        
        try {
            ChangeHistory approvedChanges = johnSubmitCommitTask.get();
            updateLocalChangeHistory(johnVersionedOntology, approvedChanges);
            assertAcceptedCommit(johnUser, johnVersionedOntology);
        } catch (ExecutionException ex) {
            System.err.println("User John failed to perform the commit");
            assertThat(ex.getCause().getMessage(), is("Commit failed, please update your local copy first"));
        }
    }

    private void assertAcceptedCommit(LocalHttpClient client, VersionedOWLOntology versionedOntology) throws Exception {
        // Assert the local change history
        ChangeHistory changeHistoryFromClient = getLocalChangeHistory(versionedOntology);
        assertThat(changeHistoryFromClient.isEmpty(), is(false));
        assertThat(changeHistoryFromClient.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromClient.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromClient.getMetadata().size(), is(1));
        assertThat(changeHistoryFromClient.getRevisions().size(), is(1));
        assertThat(changeHistoryFromClient.getChangesForRevision(R1).size(), is(2));
        
        // Assert the remote change history
        ChangeHistory changeHistoryFromServer = getRemoteChangeHistory(client, versionedOntology);
        assertThat(changeHistoryFromServer.isEmpty(), is(false));
        assertThat(changeHistoryFromServer.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromServer.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromServer.getMetadata().size(), is(1));
        assertThat(changeHistoryFromServer.getRevisions().size(), is(1));
        assertThat(changeHistoryFromServer.getChangesForRevision(R1).size(), is(2));
    }

    /*
     * A collection of private helper methods
     */

    private VersionedOWLOntology openRemoteProjectAndGetVersionedOntology(LocalHttpClient client) throws Exception {
        ServerDocument serverDocument = client.openProject(projectId);
        return client.buildVersionedOntology(serverDocument, projectId);
    }

    private CommitBundle simulateMakingChangesAndCreateCommitBundle(LocalHttpClient client,
            String commitMessage, VersionedOWLOntology versionedOntology) throws Exception {
        OWLOntology workingOntology = versionedOntology.getOntology();
        List<OWLOntologyChange> userChanges = simulateAddingAxiomInOntology(workingOntology);
        workingOntology.getOWLOntologyManager().applyChanges(userChanges);
        return createCommitBundle(client,
                userChanges, // This input could be obtained from some history manager in Protege
                commitMessage, versionedOntology.getHeadRevision());
    }

    private List<OWLOntologyChange> simulateAddingAxiomInOntology(OWLOntology workingOntology) {
        List<OWLOntologyChange> userChanges = new ArrayList<OWLOntologyChange>();
        userChanges.add(new AddAxiom(workingOntology, Declaration(CUSTOMER)));
        userChanges.add(new AddAxiom(workingOntology, SubClassOf(CUSTOMER, DOMAIN_CONCEPT)));
        return userChanges;
    }

    private CommitBundle createCommitBundle(Client author, List<OWLOntologyChange> changes, String message, DocumentRevision headRevision) {
        Commit commit = ClientUtils.createCommit(author, message, changes);
        return new CommitBundleImpl(headRevision, commit);
    }

    private void updateLocalChangeHistory(VersionedOWLOntology versionedOntology, ChangeHistory changes) {
        versionedOntology.update(changes);
    }

    private ChangeHistory getLocalChangeHistory(VersionedOWLOntology versionedOntology) {
        return versionedOntology.getChangeHistory();
    }

    private ChangeHistory getRemoteChangeHistory(LocalHttpClient author, VersionedOWLOntology versionedOntology) throws Exception {
        return author.getAllChanges(versionedOntology.getServerDocument());
    }
}
