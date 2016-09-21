package org.protege.editor.owl.integration;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.ProjectOptions;
import edu.stanford.protege.metaproject.api.ServerConfiguration;
import edu.stanford.protege.metaproject.impl.ProjectOptionsImpl;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ProjectCrudTest extends ProjectBaseTest {

    private final OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();

    private ProjectId projectId;

    private LocalHttpClient admin;

    private ServerConfiguration initialConfiguration;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        startCleanServer();
        admin = connectAsAdmin();
        projectId = createPizzaProject();
        initialConfiguration = admin.getCurrentConfig();
    }

    @After
    public void cleanUp() throws Exception {
        stopServer();
        removeDataDirectory();
        removeSnapshotFiles();
    }

    private ProjectId createPizzaProject() throws Exception {
        ProjectOptions projectOptions = createProjectOptions();
        Project pizzaProject = TestUtils.createProject("pizza-project", "Pizza Project",
                "Creating a useful pizza classification",
                PizzaOntology.getResource(), // XXX: the specification for input file parameter in
                                             // project is not for the ontology file but instead
                                             // for the history file
                "guest", Optional.of(projectOptions));
        admin.createProject(pizzaProject);
        return pizzaProject.getId();
    }

    private ProjectOptions createProjectOptions() {
        Map<String, Set<String>> options = new HashMap<>();
        options.put("key_a", new HashSet<String>(Arrays.asList("value_1", "value_2")));
        options.put("key_b", new HashSet<String>(Arrays.asList("value_3")));
        return new ProjectOptionsImpl(options);
    }

    @Test
    public void shouldOpenRemoteOntology() throws Exception {
        LocalHttpClient guest = connectAsGuest();
        
        ServerDocument serverDocument = guest.openProject(projectId);
        OWLOntology downloadedOntology = getDownloadedOntology(guest, serverDocument);
        
        // Assert the downloaded ontology
        assertThat(downloadedOntology, is(not(nullValue())));
        assertThat(downloadedOntology.getOntologyID().getOntologyIRI(),
                is(com.google.common.base.Optional.of(IRI.create(PizzaOntology.getId()))));
        assertThat(downloadedOntology.getAxiomCount(), is(940));
        assertThat(downloadedOntology.getLogicalAxiomCount(), is(712));
        assertThat(downloadedOntology.getClassesInSignature().size(), is(100));
        assertThat(downloadedOntology.getObjectPropertiesInSignature().size(), is(8));
        assertThat(downloadedOntology.getDataPropertiesInSignature().size(), is(0));
    }

    private OWLOntology getDownloadedOntology(LocalHttpClient client, ServerDocument serverDocument)
            throws Exception {
        return client.buildVersionedOntology(serverDocument, ontologyManager, projectId).getOntology();
    }

    @Test
    public void shouldUpdateExistingProject() throws Exception {
        // Perform the action
        Project pizzaProject = initialConfiguration.getProject(projectId);
        ProjectOptions anotherProjectOptions = createAnotherProjectOptions();
        Project updatedProject = TestUtils.createProject("pizza-project", // can't be replaced
                "The Pizza Project",
                "Creating a better taste of pizza classification",
                pizzaProject.getFile(), // can't be replaced
                "admin",
                Optional.of(anotherProjectOptions));
        admin.updateProject(projectId, updatedProject);
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert the update
        ServerConfiguration configAfterUpdating = admin.getCurrentConfig();
        Project updatedPizzaProject = configAfterUpdating.getProject(projectId);
        assertThat(updatedPizzaProject, is(not(nullValue())));
        assertThat(updatedPizzaProject.getName().get(), is("The Pizza Project"));
        assertThat(updatedPizzaProject.getDescription().get(), is("Creating a better taste of pizza classification"));
        assertThat(updatedPizzaProject.getOwner().get(), is("admin"));
        assertThat(updatedPizzaProject.getOptions(), is(Optional.of(anotherProjectOptions)));
    }

    private ProjectOptions createAnotherProjectOptions() {
        Map<String, Set<String>> options = new HashMap<>();
        options.put("key_a", new HashSet<String>(Arrays.asList("value_1", "value_2")));
        options.put("key_c", new HashSet<String>(Arrays.asList("value_4", "value_5", "value_6")));
        options.put("key_d", new HashSet<String>(Arrays.asList("value_7")));
        return new ProjectOptionsImpl(options);
    }

    @Test
    public void shouldDeleteExistingProject() throws Exception {
        // Assert before the deletion
        assertThat(initialConfiguration.containsProject(projectId), is(true));
        
        // Perform the action
        admin.deleteProject(projectId, true);
        admin.reallyPutConfig();
        
        // Assert after the deleteion
        ServerConfiguration configAfterDeletion = admin.getCurrentConfig();
        assertThat(configAfterDeletion.containsProject(projectId), is(false));
    }
}
