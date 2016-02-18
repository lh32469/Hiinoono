package com.hiinoono.log;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.LogEvent;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


/**
 * Publish LogEvent via various channels in Redis server.
 *
 * @author Lyle T Harris
 */
public class RedisLogPublisher extends HystrixCommand<String> {

    private static JAXBContext jc;

    private final LogEvent event;

    private final JedisPool pool;

    private Jedis jedis;

    private static final HystrixCommandGroupKey GROUP_KEY
            = HystrixCommandGroupKey.Factory.asKey("Redis");

    private static final HystrixCommandProperties.Setter CMD_PROPERTIES
            = HystrixCommandProperties.Setter()
            .withExecutionTimeoutInMilliseconds(15000)
            .withCircuitBreakerEnabled(true);

    private static final HystrixThreadPoolProperties.Setter THREAD_PROPERTIES
            = HystrixThreadPoolProperties.Setter()
            .withQueueSizeRejectionThreshold(10000)
            .withMaxQueueSize(10000);


    static {

        Class[] classes = {LogEvent.class};

        try {
            Map<String, Object> properties = new HashMap<>();
            //properties.put("eclipselink.media-type", "application/json");
            jc = JAXBContextFactory.createContext(classes, properties);

        } catch (JAXBException ex) {

        }

    }


    public RedisLogPublisher(LogEvent event, JedisPool pool) {
        super(Setter
                .withGroupKey(GROUP_KEY)
                .andThreadPoolPropertiesDefaults(THREAD_PROPERTIES)
                .andCommandPropertiesDefaults(CMD_PROPERTIES)
        );
        this.event = event;
        this.pool = pool;
    }


    @Override
    protected String run() throws Exception {
        
        Marshaller m = jc.createMarshaller();
        ByteArrayOutputStream mem = new ByteArrayOutputStream();
        m.marshal(event, mem);

        // Experimental for now..   Publishing and saving for dev/testing.
        jedis = pool.getResource();
        jedis.publish("Common.xml", mem.toString());

        final String node = Utils.getNodeId().split("-")[0];

        StringBuilder sb = new StringBuilder();
        sb.append(node);
        sb.append("  ");
        sb.append(event.getLoggerName());
        sb.append("  ");
        sb.append(event.getMessage());

        jedis.publish("Common.txt", sb.toString());

        jedis.select(5);
        String key = event.getTimeStamp() + ".log";
        jedis.set(key, mem.toString());
        jedis.expire(key, (int) TimeUnit.MINUTES.toSeconds(10));

        pool.returnResource(jedis);
        return "";
    }


    @Override
    protected String getFallback() {
        pool.returnResource(jedis);
        return "";
    }


}
