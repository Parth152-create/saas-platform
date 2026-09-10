package com.yourco.saas.domain.user;

/**
 * Test-only factory for building User fixtures from OTHER test packages.
 * User's no-arg constructor is (correctly) protected - fine for Hibernate,
 * not visible outside com.yourco.saas.domain.user. This lives in that same
 * package specifically so it CAN call `new User()`, then exposes a public
 * factory method any test package can use. Reusable by the auth/RBAC
 * integration tests coming up next, not just CrossTenantIsolationTest.
 */
public final class UserTestFactory {

    private UserTestFactory() {}

    public static User localUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setRole(role);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}