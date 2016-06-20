package org.protege.editor.owl.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Test;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.util.ChangeUtils;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.integration.BaseTest.PizzaOntology;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import edu.stanford.protege.metaproject.api.Description;
import edu.stanford.protege.metaproject.api.Name;
import edu.stanford.protege.metaproject.api.PlainPassword;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.ProjectOptions;
import edu.stanford.protege.metaproject.api.UserId;

public class LargeProjectTest extends BaseTest {
	private ProjectId projectId;

    @Test
    public void createLargeProject() throws Exception {
        /*
         * [GUI] The input project properties
         */
        projectId = f.getProjectId("BiomedGT-" + System.currentTimeMillis());
        Name projectName = f.getName("NCI Thesaurus" );
        Description description = f.getDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        UserId owner = f.getUserId("root");
                
        Optional<ProjectOptions> options = Optional.ofNullable(null);
        
        Project proj = f.getProject(projectId, projectName, description, LargeOntology.getResource(), owner, options);
       
        ServerDocument serverDocument = getAdmin().createProject(proj);
        
        
        // Assert the server document
        assertThat(serverDocument, is(notNullValue()));
        assertThat(serverDocument.getServerAddress(), is(URI.create(SERVER_ADDRESS)));
        assertThat(serverDocument.getRegistryPort(), is(REGISTRY_PORT));
        assertThat(serverDocument.getHistoryFile(), is(notNullValue()));
        assertThat(serverDocument.getHistoryFile().length(), is(greaterThan(new Long(0))));
        
        // Assert the remote change history
        ChangeHistory remoteChangeHistory = ((LocalHttpClient) getAdmin()).getAllChanges(serverDocument);
        assertThat("The remote change history should not be empty", !remoteChangeHistory.isEmpty());
        assertThat(remoteChangeHistory.getBaseRevision(), is(R0));
        assertThat(remoteChangeHistory.getHeadRevision(), is(R1));
        assertThat(remoteChangeHistory.getMetadata().size(), is(1));
        assertThat(remoteChangeHistory.getRevisions().size(), is(1));
        assertThat(remoteChangeHistory.getChangesForRevision(R1).size(), is(0));
    }
    
    @After
    public void shouldDownloadRemoteChanges() throws Exception {
        /*
         * Login as Guest
         */
        UserId guestId = f.getUserId("guest");
        PlainPassword guestPassword = f.getPlainPassword("guestpwd");
        Client guest = login(guestId, guestPassword);
        
        ServerDocument serverDocument = guest.openProject(projectId);
        VersionedOWLOntology vont = ((LocalHttpClient) guest).buildVersionedOntology(serverDocument, owlManager, projectId);
        ChangeHistory changeHistoryFromClient = vont.getChangeHistory();
        
        // Assert the remote change history
        assertThat("The local change history should not be empty", !changeHistoryFromClient.isEmpty());
        assertThat(changeHistoryFromClient.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromClient.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromClient.getMetadata().size(), is(1));
        assertThat(changeHistoryFromClient.getRevisions().size(), is(1));
        assertThat(changeHistoryFromClient.getChangesForRevision(R1).size(), is(495358));
        
        ChangeHistory changeHistoryFromServer = ((LocalHttpClient) guest).getAllChanges(vont.getServerDocument());
        
        // Assert the remote change history
        assertThat("The remote change history should not be empty", !changeHistoryFromServer.isEmpty());
        assertThat(changeHistoryFromServer.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromServer.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromServer.getMetadata().size(), is(1));
        assertThat(changeHistoryFromServer.getRevisions().size(), is(1));
        assertThat(changeHistoryFromServer.getChangesForRevision(R1).size(), is(0));
        
        //assertThat(changeHistoryFromClient.getChangesForRevision(R1).size(), 
        		//is(changeHistoryFromServer.getChangesForRevision(R1).size()));
        
        getAdmin().deleteProject(projectId, true);
    }
    
   

    

}
