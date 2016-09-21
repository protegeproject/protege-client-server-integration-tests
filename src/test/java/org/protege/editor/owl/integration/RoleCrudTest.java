package org.protege.editor.owl.integration;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.protege.editor.owl.client.LocalHttpClient;

import edu.stanford.protege.metaproject.api.OperationId;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.RoleId;
import edu.stanford.protege.metaproject.api.ServerConfiguration;
import edu.stanford.protege.metaproject.api.UserId;
import edu.stanford.protege.metaproject.api.exception.UnknownRoleIdException;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class RoleCrudTest extends BaseTest {

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
    public void shouldReadExistingRole() throws Exception {
        RoleId roleId = TestUtils.createRoleId("admin");
        
        // Perform the action
        Role adminRole = initialConfiguration.getRole(roleId);
        
        // Assert the reading
        assertThat(adminRole, is(not(nullValue())));
        assertThat(adminRole.getName().get(), is("Administrator"));
        assertThat(adminRole.getDescription().get(), is("A user with this role is allowed to do any operation on the server"));
        assertThat(adminRole.getOperations().size(), is(36));
        assertThat(adminRole.getOperations(), hasItems(
                TestUtils.createOperationId("use-classes-tab"),
                TestUtils.createOperationId("add-user"),
                TestUtils.createOperationId("remove-role"),
                TestUtils.createOperationId("remove-axiom"),
                TestUtils.createOperationId("use-data-properties-tab"),
                TestUtils.createOperationId("add-axiom"),
                TestUtils.createOperationId("use-server-admin-tab"),
                TestUtils.createOperationId("remove-import"),
                TestUtils.createOperationId("add-ontology-annotation"),
                TestUtils.createOperationId("remove-user"),
                TestUtils.createOperationId("modify-project"),
                TestUtils.createOperationId("reject-change"),
                TestUtils.createOperationId("add-role"),
                TestUtils.createOperationId("use-active-ontology-tab"),
                TestUtils.createOperationId("remove-operation"),
                TestUtils.createOperationId("use-revision-log-tab"),
                TestUtils.createOperationId("use-object-properties-tab"),
                TestUtils.createOperationId("use-entities-tab"),
                TestUtils.createOperationId("use-individuals-tab"),
                TestUtils.createOperationId("stop-server"),
                TestUtils.createOperationId("use-annotation-properties-tab"),
                TestUtils.createOperationId("remove-ontology-annotation"),
                TestUtils.createOperationId("modify-user"),
                TestUtils.createOperationId("modify-ontology-iri"),
                TestUtils.createOperationId("remove-project"),
                TestUtils.createOperationId("add-import"),
                TestUtils.createOperationId("open-project"),
                TestUtils.createOperationId("modify-operation"),
                TestUtils.createOperationId("retract-role"),
                TestUtils.createOperationId("modify-settings"),
                TestUtils.createOperationId("add-project"),
                TestUtils.createOperationId("assign-role"),
                TestUtils.createOperationId("use-views-menu"),
                TestUtils.createOperationId("add-operation"),
                TestUtils.createOperationId("modify-role"),
                TestUtils.createOperationId("accept-change")));
        assertThat(adminRole.getOperations(), not(hasItems(
                TestUtils.createOperationId("retire"),
                TestUtils.createOperationId("merge"),
                TestUtils.createOperationId("clone"))));
    }

    @Test
    public void shouldCreateNewRole() throws Exception {
        RoleId roleId = TestUtils.createRoleId("editor");
        
        Set<OperationId> operations = new HashSet<>();
        operations.add(TestUtils.createOperationId("retire"));
        operations.add(TestUtils.createOperationId("merge"));
        operations.add(TestUtils.createOperationId("clone"));
        Role editorRole = TestUtils.createRole("editor", "Ontology editor", "Working as an editor", operations);
        
        // Assert before the addition
        assertThat(initialConfiguration.containsRole(roleId), is(false));
        
        // Perform the action
        admin.createRole(editorRole);
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the addition
        ServerConfiguration configAfterAddition = admin.getCurrentConfig();
        assertThat(configAfterAddition.containsRole(roleId), is(true));
        assertThat(configAfterAddition.getRole(roleId), is(editorRole));
        assertThat(configAfterAddition.getRole(roleId).getName().get(), is("Ontology editor"));
        assertThat(configAfterAddition.getRole(roleId).getDescription().get(), is("Working as an editor"));
        assertThat(configAfterAddition.getRole(roleId).getOperations().size(), is(3));
        assertThat(configAfterAddition.getRole(roleId).getOperations(), hasItems(
                TestUtils.createOperationId("retire"),
                TestUtils.createOperationId("merge"),
                TestUtils.createOperationId("clone")));
    }

    @Test
    public void shouldDeleteExistingRole() throws Exception {
        RoleId roleId = TestUtils.createRoleId("guest");
        
        // Assert before the deletion
        assertThat(initialConfiguration.containsRole(roleId), is(true));
        assertThat(initialConfiguration.getRole(roleId), is(not(nullValue())));
        
        // Perform the action
        admin.deleteRole(roleId);
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the deletion
        ServerConfiguration configAfterDeletion = admin.getCurrentConfig();
        assertThat(configAfterDeletion.containsRole(roleId), is(false));
        
        // Check if the role is no longer recorded in the policy
        Map<UserId, Map<ProjectId, Set<RoleId>>> policyMap = configAfterDeletion.getPolicyMap();
        for (UserId userId : policyMap.keySet()) {
            Map<ProjectId, Set<RoleId>> roleAssignments = policyMap.get(userId);
            for (Set<RoleId> assignedRoles : roleAssignments.values()) {
                assertThat(assignedRoles.contains(roleId), is(false));
            }
        }
        
        thrown.expect(UnknownRoleIdException.class);
        configAfterDeletion.getRole(roleId);
    }

    @Test
    public void shouldUpdateExistingRole() throws Exception {
        RoleId roleId = TestUtils.createRoleId("project-manager");
        
        // Assert before the update
        Role projectManager = initialConfiguration.getRole(roleId);
        assertThat(projectManager, is(not(nullValue())));
        assertThat(projectManager.getName().get(), is("Project Manager"));
        assertThat(projectManager.getDescription().get(), is("A user with this role is allowed to create, "
                + "remove, modify and open a project, as well as to perform any ontology operations"));
        assertThat(projectManager.getOperations().size(), is(13));
        assertThat(projectManager.getOperations(), hasItems(
                TestUtils.createOperationId("remove-axiom"),
                TestUtils.createOperationId("remove-ontology-annotation"),
                TestUtils.createOperationId("add-axiom"),
                TestUtils.createOperationId("modify-ontology-iri"),
                TestUtils.createOperationId("add-import"),
                TestUtils.createOperationId("open-project"),
                TestUtils.createOperationId("remove-import"),
                TestUtils.createOperationId("remove-project"),
                TestUtils.createOperationId("add-ontology-annotation"),
                TestUtils.createOperationId("add-project"),
                TestUtils.createOperationId("modify-project"),
                TestUtils.createOperationId("reject-change"),
                TestUtils.createOperationId("accept-change")));
        
        // Perform the action
        Set<OperationId> operations = new HashSet<>();
        operations.add(TestUtils.createOperationId("remove-axiom"));
        operations.add(TestUtils.createOperationId("remove-ontology-annotation"));
        operations.add(TestUtils.createOperationId("add-axiom"));
        Role updatedRole = TestUtils.createRole("project-manager", "PM", "A project manager", operations);
        admin.updateRole(roleId, updatedRole);
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the update
        ServerConfiguration configAfterUpdating = admin.getCurrentConfig();
        Role updatedProjectManager = configAfterUpdating.getRole(roleId);
        assertThat(updatedProjectManager, is(not(nullValue())));
        assertThat(updatedProjectManager, is(updatedRole));
        assertThat(updatedProjectManager.getName().get(), is("PM"));
        assertThat(updatedProjectManager.getDescription().get(), is("A project manager"));
        assertThat(updatedProjectManager.getOperations(), hasItems(
                TestUtils.createOperationId("remove-axiom"),
                TestUtils.createOperationId("remove-ontology-annotation"),
                TestUtils.createOperationId("add-axiom")));
        assertThat(updatedProjectManager.getOperations(), not(hasItems(
                TestUtils.createOperationId("modify-ontology-iri"),
                TestUtils.createOperationId("add-import"),
                TestUtils.createOperationId("open-project"),
                TestUtils.createOperationId("remove-import"),
                TestUtils.createOperationId("remove-project"),
                TestUtils.createOperationId("add-ontology-annotation"),
                TestUtils.createOperationId("add-project"),
                TestUtils.createOperationId("modify-project"),
                TestUtils.createOperationId("reject-change"),
                TestUtils.createOperationId("accept-change"))));
    }
}
