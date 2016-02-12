package com.hiinoono.persistence.zk;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Tenant;
import static com.hiinoono.persistence.zk.ZooKeeperPersistenceManager.TENANTS;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

    private static final HystrixCommandGroupKey GROUP_KEY
            = HystrixCommandGroupKey.Factory.asKey("ZK-Persistence");

    /**
     * Monitor failures but don't currently open CircuitBreaker for DDoS attacks
     */
    private static final HystrixCommandProperties.Setter CB_DISABLED
            = HystrixCommandProperties.Setter()
            .withExecutionIsolationSemaphoreMaxConcurrentRequests(100)
            .withExecutionTimeoutInMilliseconds(5000)
            .withCircuitBreakerEnabled(false);

    private static final HystrixThreadPoolProperties.Setter THREAD_PROPERTIES
            = HystrixThreadPoolProperties.Setter()
            .withQueueSizeRejectionThreshold(10000)
            .withMaxQueueSize(10000);

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
     * @param name Tenant name.
     */
    public GetTenantByName(ZooKeeper zk, String name) {
        super(Setter
                .withGroupKey(GROUP_KEY)
                .andThreadPoolPropertiesDefaults(THREAD_PROPERTIES)
                .andCommandPropertiesDefaults(CB_DISABLED)
        );

        this.zk = zk;
        this.name = name;

    }


    @Override
    protected Optional<Tenant> run() throws Exception {
        try {
            Unmarshaller unMarshaller = jc.createUnmarshaller();
            byte[] data = zk.getData(TENANTS + "/" + name, false, null);
            String json = new String(Utils.decrypt(data));
            Tenant t = (Tenant) unMarshaller.unmarshal(new StringReader(json));
            return Optional.of(t);
        } catch (Exception ex) {
            LOG.error(ex.toString());
            throw ex;
        }
    }


    @Override
    protected Optional<Tenant> getFallback() {
        return Optional.empty();
    }


}
