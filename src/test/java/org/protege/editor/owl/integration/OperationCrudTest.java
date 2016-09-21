package org.protege.editor.owl.integration;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.protege.editor.owl.client.LocalHttpClient;

import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.Operation.Scope;
import edu.stanford.protege.metaproject.api.OperationId;
import edu.stanford.protege.metaproject.api.OperationType;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.ServerConfiguration;
import edu.stanford.protege.metaproject.api.exception.UnknownOperationIdException;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class OperationCrudTest extends BaseTest {

    private LocalHttpClient admin;

    private ServerConfiguration initialConfiguration;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        startCleanServer();
        admin = connectAsAdmin();
        initialConfiguration = admin.getCurrentConfig();
    }

    @After
    public void cleanUp() throws Exception {
        stopServer();
    }

    @Test
    public void shouldReadExistingOperation() throws Exception {
        OperationId operationId = TestUtils.createOperationId("add-axiom");
        
        // Perform the action
        Operation addAxiomOperation = initialConfiguration.getOperation(operationId);
        
        // Assert the reading
        assertThat(addAxiomOperation, is(not(nullValue())));
        assertThat(addAxiomOperation.getId().get(), is("add-axiom"));
        assertThat(addAxiomOperation.getName().get(), is("Add axiom"));
        assertThat(addAxiomOperation.getDescription().get(), is("Add an axiom to the ontology"));
        assertThat(addAxiomOperation.getType(), is(OperationType.WRITE));
        assertThat(addAxiomOperation.getScope(), is(Scope.ONTOLOGY));
    }

    @Test
    public void shouldCreateNewOperation() throws Exception {
        // Build
        OperationId operationId = TestUtils.createOperationId("kill-process");
        Operation killProcessOperation = TestUtils.createOperation("kill-process", "Kill process",
                "Terminate server process", OperationType.EXECUTE, Scope.SERVER);
        
        // Assert before the addition
        assertThat(initialConfiguration.containsOperation(operationId), is(false));
        
        // Perform the action
        admin.createOperation(killProcessOperation);
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the addition
        ServerConfiguration configAfterAddition = admin.getCurrentConfig();
        assertThat(configAfterAddition.containsOperation(operationId), is(true));
        assertThat(configAfterAddition.getOperation(operationId), is(killProcessOperation));
        assertThat(configAfterAddition.getOperation(operationId).getName().get(), is("Kill process"));
        assertThat(configAfterAddition.getOperation(operationId).getDescription().get(), is("Terminate server process"));
        assertThat(configAfterAddition.getOperation(operationId).getType(), is(OperationType.EXECUTE));
        assertThat(configAfterAddition.getOperation(operationId).getScope(), is(Scope.SERVER));
    }

    @Test
    public void shoudDeleteExistingOperation() throws Exception {
        OperationId operationId = TestUtils.createOperationId("add-axiom");
        
        // Assert before the deletion
        assertThat(initialConfiguration.containsOperation(operationId), is(true));
        assertThat(initialConfiguration.getOperation(operationId), is(not(nullValue())));
        
        // Perform the deletion
        admin.deleteOperation(operationId);
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the deletion
        ServerConfiguration configAfterDeletion = admin.getCurrentConfig();
        assertThat(configAfterDeletion.containsOperation(operationId), is(false));
        
        // Check if the operation is no longer recorded in every roles
        for (Role role : configAfterDeletion.getRoles()) { // assert the operation does not exist in any roles
            Set<OperationId> operationsBelongToRole = role.getOperations();
            assertThat(operationsBelongToRole.contains(operationId), is(false));
        }
        
        thrown.expect(UnknownOperationIdException.class);
        configAfterDeletion.getOperation(operationId);
    }

    @Test
    public void shouldUpdateExistingOperation() throws Exception {
        OperationId operationId = TestUtils.createOperationId("open-project");
        
        // Assert before the update
        Operation openProjectOperation = initialConfiguration.getOperation(operationId);
        assertThat(openProjectOperation, is(not(nullValue())));
        assertThat(openProjectOperation.getName().get(), is("Open project"));
        assertThat(openProjectOperation.getDescription().get(), is("Open a project in the project registry"));
        assertThat(openProjectOperation.getType(), is(OperationType.READ));
        assertThat(openProjectOperation.getScope(), is(Scope.POLICY));
        
        // Perform the action
        Operation updatedOp = TestUtils.createOperation("open-project",
                "Open existing ontology", "Open an existing ontology from remote server",
                OperationType.EXECUTE, Scope.ONTOLOGY);
        admin.updateOperation(operationId, updatedOp);
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the update
        ServerConfiguration configAfterUpdating = admin.getCurrentConfig();
        Operation updatedOpenProjectOperation = configAfterUpdating.getOperation(operationId);
        assertThat(updatedOpenProjectOperation, is(not(nullValue())));
        assertThat(updatedOpenProjectOperation, is(updatedOp));
        assertThat(updatedOpenProjectOperation.getName().get(), is("Open existing ontology"));
        assertThat(updatedOpenProjectOperation.getDescription().get(), is("Open an existing ontology from remote server"));
        assertThat(updatedOpenProjectOperation.getType(), is(OperationType.EXECUTE));
        assertThat(updatedOpenProjectOperation.getScope(), is(Scope.ONTOLOGY));
    }
}
