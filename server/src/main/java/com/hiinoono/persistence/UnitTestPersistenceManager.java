package com.hiinoono.persistence;

import com.hiinoono.jaxb.Node;
import com.hiinoono.jaxb.Status;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.User;
import com.hiinoono.jaxb.Value;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
public class UnitTestPersistenceManager implements PersistenceManager {

    final List<Node> nodes = new LinkedList<>();

    final List<Tenant> tenants = new LinkedList<>();

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(UnitTestPersistenceManager.class);


    public UnitTestPersistenceManager() {
        Node node1 = new Node();
        node1.setHostname("localhost");
        node1.setId(UUID.randomUUID().toString());
        Status status = new Status();
        status.setValue(Value.OK);
        status.getMessages().add("Tutto Bene!");
        status.getMessages().add("All good...");
        node1.setStatus(status);

        nodes.add(node1);

        /*
         * Initialize Tenants.
         */
        Tenant t1 = new Tenant();
        t1.setName("Hiinoono");

        tenants.add(t1);

        User u1 = new User();
        u1.setName("Hiinoono Sample User");
        u1.setEmail("sample@hinoono.com");
        u1.setPassword("welcome1");

        t1.getUsers().add(u1);
        t1.getUsers().add(u1);

        Tenant t2 = new Tenant();
        t2.setName("Hiinoono-2");

        tenants.add(t2);

    }


    @Override
    public synchronized Stream<Tenant> getTenants() {
        return tenants.stream();
    }


    @Override
    public synchronized Optional<Tenant> getTenantByName(String name) {

        Optional<Tenant> tenant
                = getTenants().filter(
                        t -> t.getName().equals(name)).findFirst();

        return tenant;
    }


    @Override
    public synchronized Stream<Node> getNodes() {

        return nodes.stream();
    }


    @Override
    public synchronized String getHash(String tenant, String username) {
        return hash(tenant + username + "welcome1");
    }


    @Override
    public synchronized String hash(String str) {

        try {

            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.reset();
            md.update(str.getBytes("UTF-8"));
            byte[] digest = md.digest();

            String hexStr = "";
            for (int i = 0; i < digest.length; i++) {
                hexStr += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1);
            }
            return hexStr;

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        }

        return null;
    }


    @Override
    public synchronized void addTenant(Tenant t) {
        tenants.add(t);
    }


    @Override
    public void deleteTenant(String tenantName) {

        // Nothing faster than an Iterator for removing from List
        Iterator<Tenant> iter = tenants.iterator();

        while (iter.hasNext()) {
            Tenant t = iter.next();
            if (tenantName.equals(t.getName())) {
                iter.remove();
                break;
            }
        }
    }


    @Override
    public void persist(Object obj) {
        // Not really needed here since all objects are in memory.
    }


}
