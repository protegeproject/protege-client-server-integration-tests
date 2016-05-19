package org.protege.editor.owl.integration;

import org.protege.editor.owl.client.LocalClient;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.ui.DefaultUserAuthenticator;
import org.protege.editor.owl.client.util.ServerUtils;
import org.protege.editor.owl.server.transport.rmi.RemoteLoginService;
import org.protege.editor.owl.server.transport.rmi.RmiLoginService;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;

import org.junit.Before;
import org.semanticweb.owlapi.model.OWLRuntimeException;

import java.io.File;
import java.net.URISyntaxException;
import java.util.UUID;

import edu.stanford.protege.metaproject.Manager;
import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.MetaprojectFactory;
import edu.stanford.protege.metaproject.api.PlainPassword;
import edu.stanford.protege.metaproject.api.UserId;

public abstract class BaseTest {

    protected static final String SERVER_ADDRESS = "rmi://localhost:5100";
    protected static final int REGISTRY_PORT = 5200;

    protected static final DocumentRevision R0 = DocumentRevision.START_REVISION;
    protected static final DocumentRevision R1 = DocumentRevision.create(1);

    protected static MetaprojectFactory f = Manager.getFactory();

    private Client localClient;

    protected static final File pizzaOntology() {
        try {
            return new File(NewProjectTest.class.getResource("/pizza.owl").toURI());
        }
        catch (URISyntaxException e) {
            throw new OWLRuntimeException("File not found", e);
        }
    }

    @Before
    public void connectToServer() throws Exception {
        UserId userId = f.getUserId(getUsername());
        PlainPassword password = f.getPlainPassword(getPassword());
        localClient = login(userId, password);
    }

    protected Client getClient() {
        return localClient;
    }

    /**
     * Get the user's id name
     */
    protected abstract String getUsername();

    /**
     * Get the corresponding user's password
     */
    protected abstract String getPassword();

    protected static Client login(UserId userId, PlainPassword password) throws Exception {
        RemoteLoginService loginService = (RemoteLoginService) ServerUtils
                .getRemoteService(SERVER_ADDRESS, REGISTRY_PORT, RmiLoginService.LOGIN_SERVICE);
        DefaultUserAuthenticator authenticator = new DefaultUserAuthenticator(loginService);
        AuthToken authToken = authenticator.hasValidCredentials(userId, password);
        return new LocalClient(authToken, SERVER_ADDRESS, REGISTRY_PORT);
    }

    protected static String uuid8char() {
        final UUID uuid = UUID.randomUUID();
        return uuid.toString().replace("-", "").substring(0, 8);
    }
}
