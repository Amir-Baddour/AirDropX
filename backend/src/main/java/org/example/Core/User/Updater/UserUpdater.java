package org.example.Core.User.Updater;

import org.example.Core.User.Model.User;
import org.example.Core.User.Provider.UserProvider;
import org.example.Infra.Persistence.Role.RoleRepository;
import org.example.Infra.Persistence.User.UserRepository;
import org.example.Core.Role.Model.Role;
import org.example.Core.Authentication.Exception.AuthenticationCustomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;

public class UserUpdater {
    private static final Logger logger = LoggerFactory.getLogger(UserUpdater.class.getName());
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    public UserUpdater() {
        this.userRepository = new UserRepository();
        this.roleRepository = new RoleRepository();
    }
    public void updateUserRole(String userId, String roleName) throws Exception {
        try {
            validateUpdateInput(userId, roleName);
            Optional<Role> possibleRole = roleRepository.findRoleByName(roleName);
            if (possibleRole.isEmpty()) {
                throw new IllegalArgumentException("Role not found: " + roleName);
            }
            userRepository.updateUserRole(userId, possibleRole.get().id());
            logger.info("User role updated successfully for user ID: {}", userId);
        } catch (SQLException e) {
            logger.error("Error updating user role: {}", e.getMessage());
            throw new AuthenticationCustomException("Failed to update user role", e);
        }
    }
    private void validateUpdateInput(String userId, String roleName) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Role name cannot be null or empty");
        }
    }
}