package com.hiinoono.jsf.beans;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


/**
 *
 * @author lh32469
 */
@ManagedBean(name = "rPool")
@ApplicationScoped
public class RedisPool {

    private static final String REDIS_HOST = "redis.host";

    private static final String REDIS_PORT = "redis.port";

    private static final String REDIS_PASS = "redis.pass";

    private final JedisPool pool;

    final static private Logger LOG
            = LoggerFactory.getLogger(RedisPool.class);


    /**
     * Creates a new instance of RedisPool
     */
    public RedisPool() {

        String redisHost
                = System.getProperty(REDIS_HOST, "localhost");
        String redisPass
                = System.getProperty(REDIS_PASS);
        String redisPort
                = System.getProperty(REDIS_PORT, "6379");

        LOG.info(this + ":  " + redisHost + ":" + redisPort);
        int port = Integer.parseInt(redisPort);

        JedisPoolConfig cfg = new JedisPoolConfig();
        cfg.setMaxTotal(100);
        cfg.setTestWhileIdle(true);

        if (redisPass != null) {
            pool = new JedisPool(cfg, redisHost, port, 0, redisPass);
        } else {
            pool = new JedisPool(cfg, redisHost, port);
        }
    }


    public Jedis get() {
        return pool.getResource();
    }


    public void release(Jedis jedis) {
        pool.returnResource(jedis);
    }


}
