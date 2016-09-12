package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.ConfigurationManager;
import edu.stanford.protege.metaproject.api.Password;
import edu.stanford.protege.metaproject.api.PolicyFactory;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.UserId;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class TestUtils {

    private final static PolicyFactory f = ConfigurationManager.getFactory();

    public static UserId createUserId(String userId) {
        return f.getUserId(userId);
    }

    public static User createUser(String userId, String name, String emailAddr) {
        return f.getUser(createUserId(userId),
                f.getName(name),
                f.getEmailAddress(emailAddr));
    }

    public static Password createPassword(String passwd) {
        return f.getPlainPassword(passwd);
    }
}
