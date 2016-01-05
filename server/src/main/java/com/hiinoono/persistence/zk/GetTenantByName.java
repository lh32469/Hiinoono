package com.hiinoono.persistence.zk;

import com.hiinoono.jaxb.Tenant;
import static com.hiinoono.persistence.zk.ZooKeeperPersistenceManager.TENANTS;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.zookeeper.ZooKeeper;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.LoggerFactory;


/**
 * Hystrix Command to get the Tenant named in the constructor. The initial
 * purpose of this is to only monitor requests and failures so the
 * CircuitBreaker is disabled by default.
 *
 * @author Lyle T Harris
 */
public class GetTenantByName extends HystrixCommand<Optional<Tenant>> {

    private static JAXBContext jc;

    private final ZooKeeper zk;

    private final String name;

    private final String key;

    private static final HystrixCommandGroupKey GROUP_KEY
            = HystrixCommandGroupKey.Factory.asKey("ZK-Persistence");

    /**
     * Monitor failures but don't currently open CircuitBreaker for DDoS attacks
     */
    private static final HystrixCommandProperties.Setter CB_DISABLED
            = HystrixCommandProperties.Setter().withCircuitBreakerEnabled(false);

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(GetTenantByName.class);


    static {

        Class[] classes = {Tenant.class};

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("eclipselink.media-type", "application/json");
            jc = JAXBContextFactory.createContext(classes, properties);

        } catch (JAXBException ex) {
            LOG.error(ex.getErrorCode(), ex);
        }

    }


    /**
     * Get the named Tenant from ZooKeeper using the decryption key provided.
     *
     * @param zk
     * @param name  Tenant name. 
     * @param key Key used to initially encrypt the Tenant.
     */
    public GetTenantByName(ZooKeeper zk, String name, String key) {
        super(Setter
                .withGroupKey(GROUP_KEY)
                .andCommandPropertiesDefaults(CB_DISABLED)
        );

        this.zk = zk;
        this.name = name;
        this.key = key;

    }


    @Override
    protected Optional<Tenant> run() throws Exception {
        Unmarshaller unMarshaller = jc.createUnmarshaller();
        byte[] data = zk.getData(TENANTS + "/" + name, false, null);
        String json = new String(decrypt(data));
        Tenant t = (Tenant) unMarshaller.unmarshal(new StringReader(json));
        return Optional.of(t);
    }


    @Override
    protected Optional<Tenant> getFallback() {
        return Optional.empty();
    }


    byte[] decrypt(byte[] encrypted) throws GeneralSecurityException {

        // Create key and cipher
        Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");

        // decrypt the data
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        return cipher.doFinal(encrypted);

    }


}
