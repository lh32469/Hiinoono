package com.hiinoono.persistence;

import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.User;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;


/**
 *
 * @author Lyle T Harris
 */
public class UnitTestPersistenceManager implements PersistenceManager {

    @Override
    public Stream<Tenant> getTenants() {

        List<Tenant> tenants = new LinkedList<>();

        Tenant t1 = new Tenant();
        t1.setName("Hiinoono");

        User admin = new User();
        admin.setName("Hiinoono Administrator");
        admin.setEmail("hiinoono@hinoono.com");
        admin.setPassword("welcome1");

        t1.setAdmin(admin);
        tenants.add(t1);

        User u1 = new User();
        u1.setName("Hiinoono Sample User");
        u1.setEmail("sample@hinoono.com");
        u1.setPassword("welcome1");

        t1.getUsers().add(u1);
        t1.getUsers().add(u1);

        Tenant t2 = new Tenant();
        t2.setName("Hiinoono-2");
        admin = new User();
        admin.setName("Hiinoono-2 Administrator");
        admin.setEmail("hiinoono-2@hinoono.com");
        admin.setPassword("welcome1");
        t2.setAdmin(admin);

        tenants.add(t2);

        return tenants.stream();
    }


    @Override
    public Tenant getTenantByName(String name) {

        Optional<Tenant> tenant
                = getTenants().filter(
                        t -> t.getName().equals(name)).findFirst();

        return tenant.orElse(null);
    }


}
