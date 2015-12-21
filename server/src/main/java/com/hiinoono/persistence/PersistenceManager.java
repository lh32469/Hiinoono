package com.hiinoono.persistence;

import com.hiinoono.jaxb.Tenant;
import java.util.stream.Stream;


/**
 *
 * @author Lyle T Harris
 */
public interface PersistenceManager {

    /**
     * Get Stream of all Tenants and their Users.
     *
     * @return
     */
    Stream<Tenant> getTenants();


    Tenant getTenantByName(String name);


}
