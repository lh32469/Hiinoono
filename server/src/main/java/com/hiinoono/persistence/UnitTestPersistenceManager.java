package com.hiinoono.persistence;

import com.hiinoono.Utils;
import static com.hiinoono.Utils.now;
import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.Node;
import com.hiinoono.jaxb.Status;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.Tenants;
import com.hiinoono.jaxb.User;
import com.hiinoono.jaxb.Value;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
public class UnitTestPersistenceManager implements PersistenceManager {

    private static JAXBContext jc;

    private final List<Node> nodes = new LinkedList<>();

    private final List<Container> containers = new LinkedList<>();

    private final List<Tenant> tenants = new LinkedList<>();

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(UnitTestPersistenceManager.class);


    static {

        Class[] classes = {
            Node.class,
            Tenant.class,
            Tenants.class};

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("eclipselink.media-type", "application/json");
            // properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);

            jc = JAXBContextFactory.createContext(classes, properties);

        } catch (JAXBException ex) {
            LOG.error(ex.getErrorCode(), ex);
            throw new IllegalStateException(ex);
        }

    }


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
        t1.setName("hiinoono");
        t1.setJoined(now());
        User u = new User();
        u.setName("admin");
        u.setJoined(now());
        u.setPassword(Utils.hash("hiinoonoadminWelcome1"));
        t1.getUsers().add(u);

        tenants.add(t1);

        User u1 = new User();
        u1.setName("Hiinoono Sample User");
        u1.setEmail("sample@hinoono.com");
        u1.setPassword("welcome1");

        t1.getUsers().add(u1);
        t1.getUsers().add(u1);

        Tenant t2 = new Tenant();
        t2.setName("Hiinoono-2");
        t2.setJoined(now());

        tenants.add(t2);

    }


    @Override
    public synchronized Stream<Tenant> getTenants() {

        // Return copy to simulate ZK implementation.
        try {

            ByteArrayOutputStream mem = new ByteArrayOutputStream();
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(tenants, mem);

            Unmarshaller um = jc.createUnmarshaller();
            StringReader reader = new StringReader(mem.toString());
            List<Tenant> _tenants = (List<Tenant>) um.unmarshal(reader);

            return _tenants.stream();

        } catch (JAXBException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
            return Collections.EMPTY_LIST.stream();
        }

    }


    @Override
    public synchronized Optional<Tenant> getTenantByName(String name) {

        Optional<Tenant> tenant
                = tenants.stream().filter(
                        t -> t.getName().equals(name)).findFirst();

        if (tenant.isPresent()) {
            LOG.debug("Found: " + name);
        } else {
            LOG.debug("Not Found: " + name);
        }

        return tenant;
    }


    @Override
    public synchronized Stream<Node> getNodes() {
        return nodes.stream();
    }


    @Override
    public synchronized String getHash(String tenant, String username) {
        Optional<Tenant> t = getTenantByName(tenant);

        if (t.isPresent()) {
            Optional<User> user = t.get().getUsers().stream()
                    .filter(u -> u.getName().equals(username))
                    .findFirst();

            if (user.isPresent()) {
                // "Password" in this context is hash 
                // of tenant, username and password
                LOG.info("Found: " + user.get().getPassword());
                return user.get().getPassword();
            }
        }

        // No possible match
        LOG.info("No user found: " + tenant + "/" + username);
        return UUID.randomUUID().toString();
    }


    @Override
    public synchronized void addTenant(Tenant t) {
        tenants.add(t);
    }


    @Override
    public synchronized void deleteTenant(String tenantName) {

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
    public synchronized void persist(Object obj) {
        if (obj instanceof Tenant) {
            Tenant t = (Tenant) obj;
            // Delete previous version if it exists.
            deleteTenant(t.getName());
            tenants.add(t);
        } else {
            //To change body of generated methods, choose Tools | Templates.
            Logger.getLogger(getClass().getName()).severe("Not supported yet.");
            throw new UnsupportedOperationException("Not supported yet.:    "
                    + obj.getClass());
        }
    }


    @Override
    public String hash(String string) {
        return Utils.hash(string);
    }


    @Override
    public synchronized Stream<Container> getContainers() {
        return containers.stream();
    }


    @Override
    public synchronized void addContainer(Container t) {
        this.containers.add(t);
    }


}
