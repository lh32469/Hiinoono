package com.hiinoono.persistence.zk;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.Node;
import com.hiinoono.jaxb.State;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.User;
import com.hiinoono.os.container.ContainerConstants;
import com.hiinoono.os.container.GetContainersForNode;
import com.hiinoono.persistence.PersistenceManager;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.ws.rs.NotAcceptableException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
public class ZooKeeperPersistenceManager implements PersistenceManager {

    private static JAXBContext jc;

    public static final String TENANTS = "/tenants";

    /**
     * Whether or not the initial setup of /tenants and hiinoono admin user has
     * been attempted. Need a better way of doing this at some point..
     */
    private static boolean initialized = false;

    /**
     * ACL to create nodes with.
     *
     * For private: Ids.CREATOR_ALL_ACL
     *
     * For development: Ids.OPEN_ACL_UNSAFE
     */
    private final ArrayList<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ZooKeeperPersistenceManager.class);

    /**
     * Default 256 bit key for encrypting data.
     */
    private static final String DEFAULT_KEY
            = "5A6BE0127FE74038919E0DA921D8EC78";

    /**
     * Actual 256 bit key for encrypting data. Will eventually be read from a
     * file only readable by root on the server at startup.
     */
    private static final byte[] key
            = System.getProperty("KEY", DEFAULT_KEY).getBytes();


    static {

        Class[] classes = {
            Node.class,
            Container.class,
            Tenant.class};

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("eclipselink.media-type", "application/json");
            // properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);

            jc = JAXBContextFactory.createContext(classes, properties);

        } catch (JAXBException ex) {
            LOG.error(ex.getErrorCode(), ex);
        }

    }


    /**
     * ZooKeeper (zk) cannot be a field in case of session timeouts so should be
     * fetched from ZooKeeperClient whenever needed. Session reconnecting is
     * handled in ZooKeeperClient.
     */
    @Inject
    private ZooKeeperClient zooKeeperClient;


    /*
     * Note that preDestroy annotated methods are not called in this Class
     * as it is not a Resource.
     */
    @PostConstruct
    public void postConstruct() throws KeeperException,
            InterruptedException, GeneralSecurityException {

        if (!initialized) {
            System.out.println("Initializing...");
            ZooKeeper zk = zooKeeperClient.getZookeeper();

            if (zk.exists(TENANTS, null) == null) {
                zk.create(TENANTS, "Initialized".getBytes(),
                        acl, CreateMode.PERSISTENT);
            }

            try {
                encrypt("Test encryption key".getBytes());
            } catch (GeneralSecurityException ex) {
                LOG.error(ex.getLocalizedMessage());
                System.err.println("\n\tError with encryption key "
                        + "provided: '\n\t\t" + ex.getLocalizedMessage()
                        + "'\n\n");
                throw ex;
            }

            if (zk.exists((TENANTS + "/hiinoono"), null) == null) {
                Tenant t = new Tenant();
                t.setName("hiinoono");
                t.setJoined(Utils.now());
                User u = new User();
                u.setName("admin");
                u.setJoined(Utils.now());
                u.setPassword(hash("hiinoonoadminWelcome1"));
                t.getUsers().add(u);

                this.addTenant(t);
            }

            initialized = true;
        }

    }


    @Override
    public Stream<Tenant> getTenants() {
        ZooKeeper zk = zooKeeperClient.getZookeeper();
        List<Tenant> tenants = new LinkedList<>();
        try {
            List<String> names = zk.getChildren(TENANTS, false);
            LOG.info(names.toString());

//            final ForkJoinPool pool = new ForkJoinPool(10);
//            final List<ForkJoinTask<Optional<Tenant>>> q = new LinkedList<>();
//
//            names.stream().forEach((String name) -> {
//                ForkJoinTask<Optional<Tenant>> task
//                        = pool.submit(() -> getTenantByName(name));
//                q.add(task);
//            });
//
//            pool.shutdown();
//            pool.awaitTermination(10, TimeUnit.SECONDS);
//
//            for (ForkJoinTask<Optional<Tenant>> task : q) {
//                Optional<Tenant> optional = task.get();
//                if (optional.isPresent()) {
//                    tenants.add(optional.get());
//                }
//            }
//            
//            
            // Submit requests to queue.  This implementation is slightly 
            // faster than the ForkJoinPool method above and in addition
            // to being simpler it allows for thread/queue tuning in the
            // HystrixCommand class itself in case it is used elsewhere.
            List<Future<Optional<Tenant>>> queue = new LinkedList<>();

            for (String name : names) {
                queue.add(getTenantByNameQueued(name));
            }

            // Gather the results.  This implementation is just as fast
            // and simpler than the gather results implementation below.
            for (Future<Optional<Tenant>> tenant : queue) {
                Optional<Tenant> optional = tenant.get();
                if (optional.isPresent()) {
                    tenants.add(optional.get());
                }
            }

            // Gather the results.
//            while (!queue.isEmpty()) {
//                Iterator<Future<Optional<Tenant>>> iter = queue.iterator();
//                while (iter.hasNext()) {
//                    Future<Optional<Tenant>> future = iter.next();
//                    if (future.isDone()) {
//                        iter.remove();
//                        Optional<Tenant> optional = future.get();
//                        if (optional.isPresent()) {
//                            tenants.add(optional.get());
//                        }
//                    }
//                }
//            }
//
//
//
            // Slowest implementation.  Sequential processing.
//            for (String name : names) {
//                tenants.add(getTenantByName(name).get());
//            }
//
            LOG.info("Tenants count: " + tenants.size());
            return tenants.stream();
        } catch (KeeperException |
                InterruptedException | ExecutionException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
            return Collections.EMPTY_LIST.stream();
        }
    }


    @Override
    public Optional<Tenant> getTenantByName(String name) {
        LOG.info(name);
        ZooKeeper zk = zooKeeperClient.getZookeeper();
        GetTenantByName get = new GetTenantByName(zk, name, key);
        return get.execute();
    }


    public Future<Optional<Tenant>> getTenantByNameQueued(String name) {
        LOG.info(name);
        ZooKeeper zk = zooKeeperClient.getZookeeper();
        GetTenantByName get = new GetTenantByName(zk, name, key);
        return get.queue();
    }


    @Override
    public Stream<Node> getNodes() {
        //To change body of generated methods, choose Tools | Templates.
        Logger.getLogger(this.getClass().getName()).severe("Not supported yet.");
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public void addTenant(Tenant t) {
        ZooKeeper zk = zooKeeperClient.getZookeeper();
        final String tenantPath = TENANTS + "/" + t.getName();
        try {
            ByteArrayOutputStream mem = new ByteArrayOutputStream();
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(t, mem);

            zk.create(tenantPath, encrypt(mem.toByteArray()),
                    acl, CreateMode.PERSISTENT);
            LOG.info("Adding Tenant: " + t.getName() + "\n" + mem);
        } catch (NodeExistsException ex) {
            throw new NotAcceptableException("Tenant " + t.getName()
                    + " already exists.");
        } catch (KeeperException | InterruptedException |
                GeneralSecurityException | JAXBException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        }
    }


    @Override
    public void deleteTenant(String tenantName) {
        LOG.info(tenantName);
        ZooKeeper zk = zooKeeperClient.getZookeeper();

        try {
            zk.delete(TENANTS + "/" + tenantName, -1);
        } catch (KeeperException | InterruptedException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        }
    }


    @Override
    public String getHash(String tenant, String username) {
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
    public String hash(String str) {

        try {

            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.reset();
            md.update(str.getBytes("UTF-8"));
            byte[] digest = md.digest();

            String hexStr = "";
            for (int i = 0; i < digest.length; i++) {
                hexStr += Integer.toString((digest[i] & 0xff)
                        + 0x100, 16).substring(1);
            }
            return hexStr;

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        }

        return null;
    }


    @Override
    public void persist(Object obj) {
        if (obj instanceof Tenant) {
            persist((Tenant) obj);
        } else {
            //To change body of generated methods, choose Tools | Templates.
            Logger.getLogger(this.getClass().getName()).severe("Not supported yet.");
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }


    public void persist(Tenant t) {
        long start = System.currentTimeMillis();
        deleteTenant(t.getName());
        addTenant(t);
        long end = System.currentTimeMillis();
        LOG.info(t.getName() + " (" + (end - start) + "ms)");
    }


    byte[] encrypt(byte[] clear) throws GeneralSecurityException {

        // Create key and cipher
        Key aesKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");

        // encrypt the data
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        return cipher.doFinal(clear);

    }


    /**
     * Get current date and time.
     *
     * @return
     * @throws DatatypeConfigurationException
     */
    XMLGregorianCalendar now() throws DatatypeConfigurationException {
        GregorianCalendar date = new GregorianCalendar();
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        return dtf.newXMLGregorianCalendar(date);
    }


    public static void main(String[] args) throws GeneralSecurityException {
        System.out.println("Running..");
        ZooKeeperPersistenceManager zpm = new ZooKeeperPersistenceManager();

        byte[] encrypted = zpm.encrypt("Hello World@".getBytes());

        byte[] decrypted = Utils.decrypt(encrypted);
        System.out.println(new String(decrypted));
    }


    @Override
    public Stream<Container> getContainers() {
        List<Container> containers = new LinkedList<>();
        Container c = new Container();
        c.setName("cn-00");
        c.setTemplate("ubuntu");
        c.setAdded(Utils.now());
        c.setState(State.RUNNING);
        containers.add(c);

        ZooKeeper zk = zooKeeperClient.getZookeeper();

        try {
            List<String> nodes
                    = zk.getChildren(ContainerConstants.CONTAINERS, null);

            List<Future<List<Container>>> futures = new LinkedList<>();

            for (String node : nodes) {
                String path = ContainerConstants.CONTAINERS + "/" + node;
                futures.add(new GetContainersForNode(zk, path, key).queue());
            }

            for (Future<List<Container>> future : futures) {
                containers.addAll(future.get());
            }

        } catch (KeeperException |
                InterruptedException |
                ExecutionException ex) {
            LOG.error(ex.toString(), ex);
            throw new NotAcceptableException(ex.toString());
        }

        return containers.stream();
    }


    @Override
    public void addContainer(Container c) {

        // TODO: Need to check all paths to see if 
        // this Container already exists.
        // Different Tenants/Users should be able to create
        // Containers of the same name.
        ZooKeeper zk = zooKeeperClient.getZookeeper();

        List<String> nodes;

        try {
            nodes = zk.getChildren(ContainerConstants.CONTAINERS, null);
        } catch (KeeperException | InterruptedException ex) {
            throw new NotAcceptableException(ex.getLocalizedMessage());
        }

        LOG.info("Known Nodes: " + nodes);

        // Check availablity (Node up?) and utilization and assign a Node
        // For now, just pick first one.
        final String nodeID = nodes.get(0);

        final String path = ContainerConstants.CONTAINERS
                + "/" + nodeID
                + ContainerConstants.NEW
                + "/" + c.getName();

        c.setState(State.STOPPED);
        c.setAdded(Utils.now());

        try {
            ByteArrayOutputStream mem = new ByteArrayOutputStream();
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(c, mem);

            zk.create(path, mem.toByteArray(),
                    acl, CreateMode.PERSISTENT);

//            zk.create(tenantPath, encrypt(mem.toByteArray()),
//                    acl, CreateMode.PERSISTENT);
            LOG.info("Adding Container: " + c.getName() + "\n" + mem);

        } catch (NodeExistsException ex) {
            throw new NotAcceptableException("Container " + c.getName()
                    + " already exists.");
        } catch (KeeperException | InterruptedException |
                JAXBException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        }
    }


}
