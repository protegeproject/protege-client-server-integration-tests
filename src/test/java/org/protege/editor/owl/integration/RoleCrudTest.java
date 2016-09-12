package org.protege.editor.owl.integration;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.protege.editor.owl.client.LocalHttpClient;

import edu.stanford.protege.metaproject.api.OperationId;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.RoleId;
import edu.stanford.protege.metaproject.api.ServerConfiguration;
import edu.stanford.protege.metaproject.api.exception.UnknownRoleIdException;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class RoleCrudTest extends BaseTest {

    private LocalHttpClient admin;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void loginAsAdmin() throws Exception {
        admin = (LocalHttpClient) connectAsAdmin();
    }

    @Test
    public void shouldReadRole() throws Exception {
        
        RoleId roleId = TestUtils.createRoleId("mp-admin");
        
        // Perform the action
        Role role = admin.getRole(roleId);
        
        // Assert the reading
        assertThat(role, is(notNullValue()));
        assertThat(role.getName().get(), is("Administrator"));
        assertThat(role.getDescription().get(), is("A user with this role is allowed to do any operation on the server"));
        assertThat(role.getOperations().size(), is(26));
        assertThat(role.getOperations(), hasItems(
            TestUtils.createOperationId("add-user"),
            TestUtils.createOperationId("remove-import"),
            TestUtils.createOperationId("remove-ontology-annotation"),
            TestUtils.createOperationId("add-project"),
            TestUtils.createOperationId("assign-role"),
            TestUtils.createOperationId("accept-change"))); // sample some of the operations
        assertThat(role.getOperations(), not(hasItems(TestUtils.createOperationId("clone"))));
    }

    @Test
    public void shouldCreateRole() throws Exception {
        
        RoleId roleId = TestUtils.createRoleId("editor");
        Set<OperationId> operations = new HashSet<>();
        Role role = TestUtils.createRole("editor", "Ontology editor", "Working as an editor", operations);
        
        // Assert before the addition
        ServerConfiguration sc = admin.getCurrentConfig();
        assertThat(sc.containsRole(roleId), is(false));
        
        // Perform the action
        admin.createRole(role);
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the addition
        ServerConfiguration nsc = admin.getCurrentConfig();
        assertThat(nsc.containsRole(roleId), is(true));
        assertThat(nsc.getRole(roleId), is(role));
        assertThat(nsc.getRole(roleId).getName().get(), is("Ontology editor"));
        assertThat(nsc.getRole(roleId).getDescription().get(), is("Working as an editor"));
        assertThat(nsc.getRole(roleId).getOperations(), is(operations));
    }

    @Test
    public void shouldDeleteRole() throws Exception {
       
        RoleId roleId = TestUtils.createRoleId("mp-guest");
        
        // Assert before the deletion
        ServerConfiguration sc = admin.getCurrentConfig();
        assertThat(sc.containsRole(roleId), is(true));
        
        // Perform the action
        admin.deleteRole(roleId);
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the addition
        ServerConfiguration nsc = admin.getCurrentConfig();
        assertThat(nsc.containsRole(roleId), is(false));
        
        thrown.expect(UnknownRoleIdException.class);
        nsc.getRole(roleId);
    }

    @Test
    public void shouldUpdateRole() throws Exception {
        
        RoleId roleId = TestUtils.createRoleId("mp-project-manager");
        
        // Assert before the update
        ServerConfiguration sc = admin.getCurrentConfig();
        Role projectManager = sc.getRole(roleId);
        assertThat(projectManager, is(notNullValue()));
        assertThat(projectManager.getName().get(), is("Project Manager"));
        assertThat(projectManager.getDescription().get(), is("A user with this role is allowed to create, "
                + "remove, modify and open a project, as well as to perform any ontology operations"));
        
        // Perform the action
        Set<OperationId> operations = projectManager.getOperations();
        Role updatedRole = TestUtils.createRole("mp-project-manager", "PM", "A project manager", operations);
        admin.updateRole(roleId, updatedRole);
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the update
        ServerConfiguration nsc = admin.getCurrentConfig();
        assertThat(nsc.getRole(roleId), is(updatedRole));
        assertThat(nsc.getRole(roleId).getName().get(), is("PM"));
        assertThat(nsc.getRole(roleId).getDescription().get(), is("A project manager"));
        assertThat(nsc.getRole(roleId).getOperations(), is(operations));
    }
}
