package org.protege.editor.owl.integration;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.protege.editor.owl.client.LocalHttpClient;

import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.Operation.Scope;
import edu.stanford.protege.metaproject.api.OperationId;
import edu.stanford.protege.metaproject.api.OperationType;
import edu.stanford.protege.metaproject.api.ServerConfiguration;
import edu.stanford.protege.metaproject.api.exception.UnknownOperationIdException;

public class OperationCrudTest extends BaseTest {

    private LocalHttpClient admin;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void loginAsAdmin() throws Exception {
        admin = (LocalHttpClient) connectAsAdmin();
    }

    @Test
    public void shouldCreateOperation() throws Exception {
        
        OperationId operationId = TestUtils.createOperationId("kill-process");
        Operation op = TestUtils.createOperation("kill-process", "Kill process", "Terminate server process",
                OperationType.EXECUTE, Scope.SERVER);
        
        // Assert before the addition
        ServerConfiguration sc = admin.getCurrentConfig();
        assertThat(sc.containsOperation(operationId), is(false));
        
        // Perform the action
        admin.createOperation(op);
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the addition
        ServerConfiguration nsc = admin.getCurrentConfig();
        assertThat(nsc.containsOperation(operationId), is(true));
        assertThat(nsc.getOperation(operationId), is(op));
        assertThat(nsc.getOperation(operationId).getName().get(), is("Kill process"));
        assertThat(nsc.getOperation(operationId).getDescription().get(), is("Terminate server process"));
        assertThat(nsc.getOperation(operationId).getType(), is(OperationType.EXECUTE));
        assertThat(nsc.getOperation(operationId).getScope(), is(Scope.SERVER));
    }

    @Test
    public void shoudDeleteOperation() throws Exception {
        
        OperationId operationId = TestUtils.createOperationId("clone");
        
        // Assert before the deletion
        ServerConfiguration sc = admin.getCurrentConfig();
        assertThat(sc.containsOperation(operationId), is(true));
        
        // Perform the deletion
        admin.deleteOperation(operationId);
        admin.reallyPutConfig();
        
        // Assert after the deletion
        ServerConfiguration nsc = admin.getCurrentConfig();
        assertThat(nsc.containsOperation(operationId), is(false));
        
        thrown.expect(UnknownOperationIdException.class);
        nsc.getOperation(operationId);
    }

    @Test
    public void shouldUpdateOperation() throws Exception {
        
        OperationId operationId = TestUtils.createOperationId("open-project");
        
        // Assert before the update
        ServerConfiguration sc = admin.getCurrentConfig();
        Operation op = sc.getOperation(operationId);
        assertThat(op, is(notNullValue()));
        assertThat(op.getName().get(), is("Open project"));
        assertThat(op.getDescription().get(), is("Open a project in the project registry"));
        
        // Perform the action
        Operation updatedOp = TestUtils.createOperation("open-project", "Open ontology",
                "Open an ontology from remote server", op.getType(), op.getScope());
        admin.updateOperation(operationId, updatedOp);
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the update
        ServerConfiguration nsc = admin.getCurrentConfig();
        assertThat(nsc.getOperation(operationId), is(updatedOp));
        assertThat(nsc.getOperation(operationId).getName().get(), is("Open ontology"));
        assertThat(nsc.getOperation(operationId).getDescription().get(), is("Open an ontology from remote server"));
        assertThat(nsc.getOperation(operationId).getType(), is(OperationType.READ));
        assertThat(nsc.getOperation(operationId).getScope(), is(Scope.POLICY));
    }
}
