package com.hiinoono.persistence.zk;

import com.hiinoono.jaxb.Node;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.persistence.PersistenceManager;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
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

    private ZooKeeper zk;

    public static final String TENANTS = "/tenants";

    /**
     * ACL to create nodes with.
     *
     * For private: Ids.CREATOR_ALL_ACL
     *
     * For development: Ids.OPEN_ACL_UNSAFE
     */
    private final ArrayList<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;

    String key128 = "Bar12345Bar12345"; // 128 bit key

    String key256 = "5A6BE0127FE74038919E0DA921D8EC78"; // 256 bit key

    String key = key256;

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ZooKeeperPersistenceManager.class);


    static {

        Class[] classes = {
            Node.class,
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


    @Inject
    private ZooKeeperClient zooKeeperClient;


    @PostConstruct
    void postConstruct() throws KeeperException, InterruptedException {
        System.out.println(this + "\n" + zooKeeperClient);
        this.zk = zooKeeperClient.getZookeeper();
//        if (zk.exists(TENANTS, null) == null) {
//            zk.create(TENANTS, "Initialized".getBytes(),
//                    acl, CreateMode.PERSISTENT);
//        }
    }


    @Override
    public Stream<Tenant> getTenants() {
        List<Tenant> tenants = new LinkedList<>();
        try {
            List<String> names = zk.getChildren(TENANTS, false);
            LOG.info(names.toString());
            for (String name : names) {
                tenants.add(getTenantByName(name));
            }
            return tenants.stream();
        } catch (KeeperException | InterruptedException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
            return Collections.EMPTY_LIST.stream();
        }
    }


    @Override
    public Tenant getTenantByName(String name) {
        LOG.info(name);
        try {

            Unmarshaller unMarshaller = jc.createUnmarshaller();
            byte[] data = zk.getData(TENANTS + "/" + name, false, null);
            String json = new String(decrypt(data));
            LOG.info("Getting Tenant: " + "\n" + json);
            Tenant t = (Tenant) unMarshaller.unmarshal(new StringReader(json));
            return t;
        } catch (KeeperException | InterruptedException |
                JAXBException | GeneralSecurityException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
            return null;
        }
    }


    @Override
    public Stream<Node> getNodes() {
        //To change body of generated methods, choose Tools | Templates.
        Logger.getLogger(this.getClass().getName()).severe("Not supported yet.");
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public void addTenant(Tenant t) {
        final String tenantPath = TENANTS + "/" + t.getName();
        try {
            ByteArrayOutputStream mem = new ByteArrayOutputStream();
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(t, mem);

            zk.create(tenantPath, encrypt(mem.toByteArray()),
                    acl, CreateMode.PERSISTENT);
            LOG.info("Adding Tenant: " + t.getName() + "\n" + mem);

        } catch (KeeperException | InterruptedException |
                GeneralSecurityException | JAXBException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        }
    }


    @Override
    public void deleteTenant(String tenantName) {
        LOG.info(tenantName);

        try {
            zk.delete(TENANTS + "/" + tenantName, -1);
        } catch (KeeperException | InterruptedException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        }
    }


    @Override
    public String getHash(String tenant, String username) {
        return hash(tenant + username + "welcome1");
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
                hexStr += Integer.toString((digest[i] & 0xff) + 
                        0x100, 16).substring(1);
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
        Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");

        // encrypt the data
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        return cipher.doFinal(clear);

    }


    byte[] decrypt(byte[] encrypted) throws GeneralSecurityException {

        // Create key and cipher
        Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");

        // decrypt the data
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        return cipher.doFinal(encrypted);

    }


    public static void main(String[] args) throws GeneralSecurityException {
        System.out.println("Running..");
        ZooKeeperPersistenceManager zpm = new ZooKeeperPersistenceManager();

        byte[] encrypted = zpm.encrypt("Hello World@".getBytes());

        byte[] decrypted = zpm.decrypt(encrypted);
        System.out.println(new String(decrypted));
    }


}
