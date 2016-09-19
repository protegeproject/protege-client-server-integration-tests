package org.protege.editor.owl.integration;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.protege.editor.owl.client.LocalHttpClient;

import edu.stanford.protege.metaproject.api.Password;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.RoleId;
import edu.stanford.protege.metaproject.api.ServerConfiguration;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.UserId;
import edu.stanford.protege.metaproject.api.exception.UserNotRegisteredException;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class UserCrudTest extends BaseTest {

    private LocalHttpClient admin;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void loginAsAdmin() throws Exception {
        admin = (LocalHttpClient) connectAsAdmin();
    }

    @Test
    public void shouldCreateUserWithPassword() throws Exception {
        
        UserId userId = TestUtils.createUserId("johndoe");
        User user = TestUtils.createUser("johndoe", "John Doe", "john.doe@email.com");
        Password password = TestUtils.createPassword("johndoepasswd");
        
        // Assert before the addition
        ServerConfiguration sc = admin.getCurrentConfig();
        assertThat(sc.containsUser(userId), is(false));
        
        // Perform the action
        admin.createUser(user, Optional.of(password));
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the addition
        ServerConfiguration nsc = admin.getCurrentConfig();
        assertThat(nsc.containsUser(user), is(true));
        assertThat(nsc.getUser(userId), is(user));
        assertThat(nsc.getUser(userId).getName().get(), is("John Doe"));
        assertThat(nsc.getUser(userId).getEmailAddress().get(), is("john.doe@email.com"));
    }

    @Test
    public void shouldCreateUserWithoutPassword() throws Exception {
        
        UserId userId = TestUtils.createUserId("marydoe");
        User user = TestUtils.createUser("marydoe", "Mary Doe", "mary.doe@email.com");
        
        // Assert before the addition
        ServerConfiguration sc = admin.getCurrentConfig();
        assertThat(sc.containsUser(userId), is(false));
        
        // Perform the action
        admin.createUser(user, Optional.empty());
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the addition
        ServerConfiguration nsc = admin.getCurrentConfig();
        assertThat(nsc.containsUser(user), is(true));
        assertThat(nsc.getUser(userId), is(user));
        assertThat(nsc.getUser(userId).getName().get(), is("Mary Doe"));
        assertThat(nsc.getUser(userId).getEmailAddress().get(), is("mary.doe@email.com"));
    }

    @Test
    public void shouldDeleteUser() throws Exception {
        
        UserId userId = TestUtils.createUserId("alice");
        
        // Assert before the deletion
        ServerConfiguration sc = admin.getCurrentConfig();
        assertThat(sc.containsUser(userId), is(true));
        assertThat(sc.getUserRoleMap(userId).isEmpty(), is(false));
        
        // Perform the action
        admin.deleteUser(userId);
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the deletion
        ServerConfiguration nsc = admin.getCurrentConfig();
        assertThat(nsc.containsUser(userId), is(false));
        
        // Check if the user is no longer recorded in the policy
        Map<UserId, Map<ProjectId, Set<RoleId>>> policyMap = nsc.getPolicyMap();
        Set<UserId> existingUsersInPolicy = policyMap.keySet();
        assertThat(existingUsersInPolicy.contains(userId), is(false));
        
        thrown.expect(UserNotRegisteredException.class);
        nsc.getAuthenticationDetails(userId);
    }

    @Test
    public void shouldUpdateUser() throws Exception {
        
        UserId userId = TestUtils.createUserId("bob");
        
        // Assert before the update
        ServerConfiguration sc = admin.getCurrentConfig();
        User user = sc.getUser(userId);
        assertThat(user, is(notNullValue()));
        assertThat(sc.getUser(userId).getName().get(), is("bob"));
        assertThat(sc.getUser(userId).getEmailAddress().get(), is("bob"));
        
        // Perform the action
        User updatedUser = TestUtils.createUser("bob", "Bob Underwood", "bob.underwood@email.com");
        admin.updateUser(userId, updatedUser, Optional.empty());
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the update
        ServerConfiguration nsc = admin.getCurrentConfig();
        assertThat(nsc.getUser(userId), is(updatedUser));
        assertThat(nsc.getUser(userId).getName().get(), is("Bob Underwood"));
        assertThat(nsc.getUser(userId).getEmailAddress().get(), is("bob.underwood@email.com"));
    }
}
