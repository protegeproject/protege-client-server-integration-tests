package org.protege.editor.owl.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Declaration;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.IRI;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.SubClassOf;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.integration.BaseTest.PizzaOntology;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.api.exception.AuthorizationException;
import org.protege.editor.owl.server.api.exception.OutOfSyncException;
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

import edu.stanford.protege.metaproject.api.Description;
import edu.stanford.protege.metaproject.api.Name;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.ProjectOptions;
import edu.stanford.protege.metaproject.api.UserId;
import io.undertow.client.ClientRequest;

public class ConcurentCommitChangesTest extends BaseTest {
	
	 private static final String ONTOLOGY_ID = PizzaOntology.getId() + "#";

	    private static final OWLClass DOMAIN_CONCEPT = Class(IRI(ONTOLOGY_ID, "DomainConcept"));
	    private static final OWLClass CUSTOMER = Class(IRI(ONTOLOGY_ID, "Customer"));
	    private static final OWLClass CUSTOMER1 = Class(IRI(ONTOLOGY_ID, "Customer1"));
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
	        projectId = f.getProjectId("pizza-" + System.currentTimeMillis()); // currentTimeMilis() for uniqueness
	        Name projectName = f.getName("Pizza Project");
	        Description description = f.getDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
	        UserId owner = f.getUserId("root");
	        Optional<ProjectOptions> options = Optional.ofNullable(null);
	        
	        Project proj = f.getProject(projectId, projectName, description, PizzaOntology.getResource(), owner, options);
	       
	        getAdmin().createProject(proj);
	    }
	    
	    private VersionedOWLOntology openProjectAsAdmin() throws Exception {
	        ServerDocument serverDocument = getAdmin().openProject(projectId);
	        return getAdmin().buildVersionedOntology(serverDocument, owlManager, projectId);
	    }
	    
	    @Test
	    public void shouldCommitAddition() throws Exception {
	        VersionedOWLOntology vont = openProjectAsAdmin();
	        OWLOntology workingOntology = vont.getOntology();
	        
	        
	        
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
	        List<OWLOntologyChange> changes = ClientUtils.getUncommittedChanges(histManager, vont.getOntology(), vont.getChangeHistory());
	        Commit commit = ClientUtils.createCommit(getAdmin(), "Add customer subclass of domain concept", changes);
	        DocumentRevision commitBaseRevision = vont.getHeadRevision();
	        CommitBundle commitBundle = new CommitBundleImpl(commitBaseRevision, commit);
	        
	        ExecutorService ex_serv = Executors.newFixedThreadPool(2);
	        
	        Future<ChangeHistory> fch1 =  ex_serv.submit(new Callable<ChangeHistory>() {

				@Override
				public ChangeHistory call() throws Exception {
					// TODO Auto-generated method stub
					return getAdmin().commit(projectId, commitBundle);
				}
	        	
	        });
	        
	         
	        
	        
	        
	        Future<ChangeHistory> fch2 =  ex_serv.submit(new Callable<ChangeHistory>() {

				@Override
				public ChangeHistory call() throws Exception {
					// TODO Auto-generated method stub
					return getAdmin().commit(projectId, commitBundle);
				}
	        	
	        });
	        
	        ExecutionException expected = null;
	        
	        ChangeHistory h1 = null;
	        ChangeHistory h2 = null;
	        
	        try {
	        	h1 = fch1.get(); 
	        	vont.update(h1);
	        } catch (ExecutionException ex) {
	        	expected = ex;
	        	
	        	
	        }
	        
	        try {
	        	h2 = fch2.get();
	        	vont.update(h2);
	        } catch (ExecutionException ex) {
	        	expected = ex;
	        	
	        	
	        }
	        
	        
	        
	        if (expected != null) {
	        	assertThat(expected.getCause().getMessage(), is("The local copy is outdated. Please do update."));
	        } else {
	        	// we should have gotten an exception
	        	assertThat(1 + 1, is(3));
	        }
	        
	                 
	        
	        
	       // thrown.expect(ExecutionException.class);
	        //thrown.expectCause(new CauseMatcher(ClientRequestException.class, "Out of sync error"));
	        
	        
	        
	        //ChangeHistory approvedChanges
	        
	        //vont.update(approvedChanges);

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
	    
	    @After
	    public void removeProject() throws Exception {
	        getAdmin().deleteProject(projectId, true);
	    }



}
