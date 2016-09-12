package org.protege.editor.owl.integration;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.Test;
import org.protege.editor.owl.client.LocalHttpClient;

import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.User;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class AdminUserTest extends BaseTest {

    @Test
    public void couldLoginAsAdmin() throws Exception {
        
        LocalHttpClient admin = (LocalHttpClient) connectAsAdmin();
        
        // Assert user admin
        assertThat(admin, is(notNullValue()));
        assertThat(admin.getUserInfo().getId(), is("root"));
        assertThat(admin.getUserInfo().getName(), is("Root User"));
        assertThat(admin.getUserInfo().getEmailAddress(), is(""));
    }

    @Test
    public void couldQueryDefaultPolicyOperations() throws Exception {
        
        LocalHttpClient admin = (LocalHttpClient) connectAsAdmin();
        
        // Assert allowed operations
        assertThat(admin.canAssignRole(), is(true));
        assertThat(admin.canCreateOperation(), is(true));
        assertThat(admin.canCreateProject(), is(true));
        assertThat(admin.canCreateRole(), is(true));
        assertThat(admin.canCreateUser(), is(true));
        assertThat(admin.canDeleteOperation(), is(true));
        assertThat(admin.canDeleteProject(), is(true));
        assertThat(admin.canDeleteRole(), is(true));
        assertThat(admin.canDeleteUser(), is(true));
        assertThat(admin.canOpenProject(), is(true));
        assertThat(admin.canRetractRole(), is(true));
        assertThat(admin.canStopServer(), is(true));
        assertThat(admin.canUpdateOperation(), is(true));
        assertThat(admin.canUpdateProject(), is(true));
        assertThat(admin.canUpdateRole(), is(true));
        assertThat(admin.canUpdateServerConfig(), is(true));
        assertThat(admin.canUpdateUser(), is(true));
    }

    @Test
    public void couldBrowseAllUsers() throws Exception {
        
        LocalHttpClient admin = (LocalHttpClient) connectAsAdmin();
        
        // Perform the action
        List<User> users = admin.getAllUsers();
        
        // Assert user list
        assertThat(users, is(notNullValue()));
        assertThat(users.isEmpty(), is(false));
    }

    @Test
    public void couldBrowseAllProjects() throws Exception {
        
        LocalHttpClient admin = (LocalHttpClient) connectAsAdmin();
        
        // Perform the action
        List<Project> projects = admin.getAllProjects();
        
        // Assert user list
        assertThat(projects, is(notNullValue()));
        assertThat(projects.isEmpty(), is(true));
    }

    @Test
    public void couldBrowseAllRoles() throws Exception {
        
        LocalHttpClient admin = (LocalHttpClient) connectAsAdmin();
        
        // Perform the action
        List<Role> roles = admin.getAllRoles();
        
        // Assert user list
        assertThat(roles, is(notNullValue()));
        assertThat(roles.isEmpty(), is(false));
    }

    @Test
    public void couldBrowseAllOperations() throws Exception {
        
        LocalHttpClient admin = (LocalHttpClient) connectAsAdmin();
        
        // Perform the action
        List<Operation> operations = admin.getAllOperations();
        
        // Assert user list
        assertThat(operations, is(notNullValue()));
        assertThat(operations.isEmpty(), is(false));
    }
}
