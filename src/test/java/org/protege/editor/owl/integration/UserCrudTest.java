package org.protege.editor.owl.integration;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.protege.editor.owl.client.LocalHttpClient;

import edu.stanford.protege.metaproject.api.Password;
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
    public void shouldReadExistingUser() throws Exception {
        UserId userId = TestUtils.createUserId("root");
        
        // Perform the action
        User rootUser = initialConfiguration.getUser(userId);
        
        // Assert the reading
        assertThat(rootUser, is(not(nullValue())));
        assertThat(rootUser.getId().get(), is("root"));
        assertThat(rootUser.getName().get(), is("Root User"));
        assertThat(rootUser.getEmailAddress().get(), is(""));
    }

    @Test
    public void shouldCreateNewUserWithPassword() throws Exception {
        // Build
        UserId userId = TestUtils.createUserId("john");
        User user = TestUtils.createUser("john", "John Doe", "john.doe@email.com");
        Password password = TestUtils.createPassword("johnpwd");
        
        // Assert before the addition
        assertThat(initialConfiguration.containsUser(userId), is(false));
        
        // Perform the action
        admin.createUser(user, Optional.of(password));
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the addition
        ServerConfiguration configAfterAddition = admin.getCurrentConfig();
        assertThat(configAfterAddition.containsUser(user), is(true));
        assertThat(configAfterAddition.getUser(userId), is(user));
        assertThat(configAfterAddition.getUser(userId).getName().get(), is("John Doe"));
        assertThat(configAfterAddition.getUser(userId).getEmailAddress().get(), is("john.doe@email.com"));
    }

    @Test
    public void shouldCreateNewUserWithoutPassword() throws Exception {
        UserId userId = TestUtils.createUserId("mary");
        User user = TestUtils.createUser("mary", "Mary Doe", "mary.doe@email.com");
        
        // Assert before the addition
        assertThat(initialConfiguration.containsUser(userId), is(false));
        
        // Perform the action
        admin.createUser(user, Optional.empty());
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the addition
        ServerConfiguration configAfterAddition = admin.getCurrentConfig();
        assertThat(configAfterAddition.containsUser(user), is(true));
        assertThat(configAfterAddition.getUser(userId), is(user));
        assertThat(configAfterAddition.getUser(userId).getName().get(), is("Mary Doe"));
        assertThat(configAfterAddition.getUser(userId).getEmailAddress().get(), is("mary.doe@email.com"));
    }

    @Test
    public void shouldUpdateExistingUser() throws Exception {
        UserId userId = TestUtils.createUserId("guest");
        
        // Assert before the update
        User userBob = initialConfiguration.getUser(userId);
        assertThat(userBob, is(not(nullValue())));
        assertThat(userBob.getName().get(), is("Guest User"));
        assertThat(userBob.getEmailAddress().get(), is(""));
        
        // Perform the action
        User updatedUser = TestUtils.createUser("guest", "VIP Guest", "guest@email.com");
        admin.updateUser(userId, updatedUser, Optional.empty());
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the update
        ServerConfiguration configAfterUpdating = admin.getCurrentConfig();
        User updatedUserBob = configAfterUpdating.getUser(userId);
        assertThat(updatedUserBob, is(not(nullValue())));
        assertThat(updatedUserBob, is(updatedUser));
        assertThat(updatedUserBob.getName().get(), is("VIP Guest"));
        assertThat(updatedUserBob.getEmailAddress().get(), is("guest@email.com"));
    }

    @Test
    public void shouldDeleteExistingUser() throws Exception {
        UserId userId = TestUtils.createUserId("guest");
        
        // Assert before the deletion
        assertThat(initialConfiguration.containsUser(userId), is(true));
        assertThat(initialConfiguration.getPolicyMap().keySet().contains(userId), is(true));
        assertThat(initialConfiguration.getAuthenticationDetails(userId), is(not(nullValue())));
        
        // Perform the action
        admin.deleteUser(userId);
        admin.reallyPutConfig(); // upload changes to server
        
        // Assert after the deletion
        ServerConfiguration configAfterDeletion = admin.getCurrentConfig();
        assertThat(configAfterDeletion.containsUser(userId), is(false));
        assertThat(configAfterDeletion.getPolicyMap().keySet().contains(userId), is(false));
        
        thrown.expect(UserNotRegisteredException.class);
        configAfterDeletion.getAuthenticationDetails(userId);
    }
}
