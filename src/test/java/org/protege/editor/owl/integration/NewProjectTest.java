package org.protege.editor.owl.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.util.ChangeUtils;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.integration.BaseTest.PizzaOntology;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.ServerDocument;

import org.junit.After;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import edu.stanford.protege.metaproject.api.Description;
import edu.stanford.protege.metaproject.api.Name;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.ProjectOptions;
import edu.stanford.protege.metaproject.api.UserId;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class NewProjectTest extends BaseTest {

    private ProjectId projectId;

    @Test
    public void createNewProject() throws Exception {
        /*
         * [GUI] The input project properties
         */
        projectId = f.getProjectId("pizza-" + System.currentTimeMillis()); // currentTimeMilis() for uniqueness
        Name projectName = f.getName("Pizza Project");
        Description description = f.getDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        UserId owner = f.getUserId("root");
        
        Optional<ProjectOptions> options = Optional.ofNullable(null);
        
        Project proj = f.getProject(projectId, projectName, description, PizzaOntology.getResource(), owner, options);
       
        ServerDocument serverDocument = getAdmin().createProject(proj);
        
        // Assert the server document
        assertThat(serverDocument, is(notNullValue()));
        assertThat(serverDocument.getServerAddress(), is(URI.create(SERVER_ADDRESS)));
        assertThat(serverDocument.getHistoryFile(), is(notNullValue()));
        assertThat(serverDocument.getHistoryFile().length(), is(greaterThan(new Long(0))));
        
        // Assert the remote change history
        ChangeHistory remoteChangeHistory = getAdmin().getAllChanges(serverDocument);
        assertThat("The remote change history should not be empty", !remoteChangeHistory.isEmpty());
        assertThat(remoteChangeHistory.getBaseRevision(), is(R0));
        assertThat(remoteChangeHistory.getHeadRevision(), is(R1));
        assertThat(remoteChangeHistory.getMetadata().size(), is(1));
        assertThat(remoteChangeHistory.getRevisions().size(), is(1));
        assertThat(remoteChangeHistory.getChangesForRevision(R1).size(), is(0));
    }

    @After
    public void removeProject() throws Exception {
        getAdmin().deleteProject(projectId, true);
    }
}
