package com.hiinoono.persistence;

import com.hiinoono.jaxb.Node;
import com.hiinoono.jaxb.Status;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.User;
import com.hiinoono.jaxb.Value;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    List<Node> nodes = new LinkedList<>();

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

    }


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


    @Override
    public Stream<Node> getNodes() {

        return nodes.stream();
    }


    @Override
    public String getHash(String tenant, String username) {
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


}
