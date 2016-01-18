package com.hiinoono.rest.auth;

/**
 *
 * @author ltharris
 */
public interface Roles {

    /**
     * Hiinoono Admin User Role. User to create Tenants and administer cloud
     * platform in general.
     */
    String H_ADMIN = "HiinoonoAdmin";

    /**
     * Administrator Role for a given Tenant.
     */
    String T_ADMIN = "TenantAdmin";

    /**
     * Role for a non-privileged user.
     */
    String USER = "User";

}
