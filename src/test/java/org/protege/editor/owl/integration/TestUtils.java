package org.protege.editor.owl.integration;

import java.util.Set;

import edu.stanford.protege.metaproject.ConfigurationManager;
import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.Operation.Scope;
import edu.stanford.protege.metaproject.api.OperationId;
import edu.stanford.protege.metaproject.api.OperationType;
import edu.stanford.protege.metaproject.api.Password;
import edu.stanford.protege.metaproject.api.PolicyFactory;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.RoleId;
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

    public static RoleId createRoleId(String roleId) {
        return f.getRoleId(roleId);
    }

    public static Role createRole(String roleId, String name, String description, Set<OperationId> operations) {
        return f.getRole(createRoleId(roleId),
                f.getName(name),
                f.getDescription(description),
                operations);
    }

    public static OperationId createOperationId(String operationId) {
        return f.getOperationId(operationId);
    }

    public static Operation createOperation(String operationId, String name, String description,
            OperationType operationType, Scope scope) {
        return f.getCustomOperation(createOperationId(operationId),
                f.getName(name),
                f.getDescription(description),
                operationType,
                scope);
    }
}
