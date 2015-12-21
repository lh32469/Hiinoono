package com.hiinoono.persistence;

import com.hiinoono.jaxb.Tenant;
import java.util.Collections;
import java.util.stream.Stream;


/**
 *
 * @author Lyle T Harris
 */
public class ZooKeeperPersistenceManager implements PersistenceManager {

    @Override
    public Stream<Tenant> getTenants() {
        return Collections.EMPTY_LIST.stream();
    }


    @Override
    public Tenant getTenantByName(String name) {
        return null;
    }


}
