package org.protege.editor.owl.integration;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.ServerDocument;

import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.ProjectOptions;
import edu.stanford.protege.metaproject.api.ServerConfiguration;
import edu.stanford.protege.metaproject.impl.ProjectOptionsImpl;

/**
 * @author Bob Dionne <dionne@dionne-associates.com> <br>
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class LargeProjectTest extends ProjectBaseTest {

    private ProjectId projectId;

    private LocalHttpClient admin;

    private ServerDocument projectDocument;

    @Before
    public void setUp() throws Exception {
        startCleanServer();
        admin = connectAsAdmin();
        projectId = createThesaurusProject();
    }

    @After
    public void cleanUp() throws Exception {
        stopServer();
        removeDataDirectory();
        removeSnapshotFiles();
    }

    private ProjectId createThesaurusProject() throws Exception {
        ProjectOptions projectOptions = createProjectOptions();
        Project pizzaProject = TestUtils.createProject("biomedgt-project", "NCI Thesaurus Project",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
                PizzaOntology.getResource(), // XXX: the specification for input file parameter in
                                             // project is not for the ontology file but instead
                                             // for the history file
                "guest", Optional.of(projectOptions));
        ServerDocument serverDocument = admin.createProject(pizzaProject);
        setServerDocument(serverDocument);
        return pizzaProject.getId();
    }

    private ProjectOptions createProjectOptions() {
        Map<String, Set<String>> options = new HashMap<>();
        options.put("key_a", new HashSet<String>(Arrays.asList("value_1", "value_2")));
        options.put("key_b", new HashSet<String>(Arrays.asList("value_3")));
        return new ProjectOptionsImpl(options);
    }

    private void setServerDocument(ServerDocument projectDocument) {
        this.projectDocument = projectDocument;
    }

    @Test
    public void shouldReadProjectProperties() throws Exception {
        ServerConfiguration configAfterCreation = admin.getCurrentConfig();
        
        // Assert the server configuration
        assertThat(configAfterCreation.containsProject(projectId), is(true));
        assertThat(configAfterCreation.getProject(projectId), is(not(nullValue())));
        assertThat(configAfterCreation.getProject(projectId).getId().get(), is("biomedgt-project"));
        assertThat(configAfterCreation.getProject(projectId).getName().get(), is("NCI Thesaurus Project"));
        assertThat(configAfterCreation.getProject(projectId).getDescription().get(),
                is("Lorem ipsum dolor sit amet, consectetur adipiscing elit"));
        assertThat(configAfterCreation.getProject(projectId).getOwner().get(), is("guest"));
        assertThat(configAfterCreation.getProject(projectId).getOptions(),
                is(Optional.of(createProjectOptions())));
    }

    @Test
    public void shouldReadServerDocumentAfterCreatingNewProject() throws Exception {
        // Assert the returned server document
        assertThat(projectDocument, is(not(nullValue())));
        assertThat(projectDocument.getServerAddress(), is(URI.create(SERVER_ADDRESS)));
        assertThat(projectDocument.getRegistryPort(), is(8081));
        assertThat(projectDocument.getHistoryFile(), is(not(nullValue())));
        assertThat(projectDocument.getHistoryFile().getName(), is("NCI_Thesaurus_Project.history"));
    }

    @Test
    public void shouldReadInitialChangeHistory() throws Exception {
        // Retrieve the initial changes as a guest user
        LocalHttpClient guestUser = connectAsGuest();
        ChangeHistory initialHistory = guestUser.getAllChanges(projectDocument);
        
        // Assert the returned server document
        assertThat(initialHistory, is(not(nullValue())));
        assertThat(initialHistory.isEmpty(), is(true));
        assertThat(initialHistory.getBaseRevision(), is(R0)); // Revision 0
        assertThat(initialHistory.getHeadRevision(), is(R0)); // Revision 0
        assertThat(initialHistory.getMetadata(), is(not(nullValue())));
        assertThat(initialHistory.getMetadata().isEmpty(), is(true));
        assertThat(initialHistory.getRevisions(), is(not(nullValue())));
        assertThat(initialHistory.getRevisions().isEmpty(), is(true));
    }
}
