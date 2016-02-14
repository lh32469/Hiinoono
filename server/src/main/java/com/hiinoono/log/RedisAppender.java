package com.hiinoono.log;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;
import com.hiinoono.jaxb.LogEvent;
import com.netflix.hystrix.strategy.HystrixPlugins;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


/**
 *
 * @author Lyle T Harris
 */
public class RedisAppender implements Appender {

    private static final String REDIS_HOST = "redis.host";

    private static final String REDIS_PORT = "redis.port";

    private static final String REDIS_PASS = "redis.pass";

    private final JedisPool pool;

    private Context context;

    private boolean started;

    private final List<Filter> filters = new LinkedList<>();


    public RedisAppender() {

        String redisHost
                = System.getProperty(REDIS_HOST, "localhost");
        String redisPass
                = System.getProperty(REDIS_PASS);
        String redisPort
                = System.getProperty(REDIS_PORT, "6379");

        int port = Integer.parseInt(redisPort);

        JedisPoolConfig cfg = new JedisPoolConfig();
        cfg.setMaxTotal(100);
        cfg.setTestWhileIdle(true);

        if (redisPass == null) {
            pool = new JedisPool(cfg, redisHost, port);
        } else {
            pool = new JedisPool(cfg, redisHost, port, 0, redisPass);
        }
    }


    @Override
    public void doAppend(Object _event) throws LogbackException {

        if (_event instanceof LoggingEvent) {
            LoggingEvent event = (LoggingEvent) _event;

            LogEvent le = new LogEvent();

            le.setLevel(event.getLevel().toString());
            le.setLoggerName(event.getLoggerName());
            le.setMessage(event.getMessage());
            le.setTimeStamp(event.getTimeStamp());
            event.getTimeStamp();

            if (HystrixPlugins.getInstance() != null) {
                new RedisLogPublisher(le, pool).queue();
            }

        }
    }


    @Override
    public void setName(String name) {
        // Name is simple class name
    }


    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }


    @Override
    public void start() {
        System.err.println("start()");
        this.started = true;
    }


    @Override
    public void stop() {
        System.err.println("stop()");
        this.started = false;
    }


    @Override
    public boolean isStarted() {
        System.err.println("isStarted()");
        return started;
    }


    @Override
    public void setContext(Context context) {
        System.err.println("setContext: " + context);
        this.context = context;
    }


    @Override
    public Context getContext() {
        System.err.println("getContext: " + context);
        return this.context;
    }


    @Override
    public void addStatus(Status status) {
        System.err.println("addStatus: " + status);
    }


    @Override
    public void addInfo(String msg) {
        System.err.println("addInfo: " + msg);
    }


    @Override
    public void addInfo(String msg, Throwable ex) {
        System.err.println("addInfo2: " + msg);
        ex.printStackTrace();
    }


    @Override
    public void addWarn(String msg) {
        System.err.println("addWarn: " + msg);
    }


    @Override
    public void addWarn(String msg, Throwable ex) {
        System.err.println("addWarn2: " + msg);
        ex.printStackTrace();
    }


    @Override
    public void addError(String msg) {
        System.err.println("addError: " + msg);
    }


    @Override
    public void addError(String msg, Throwable ex) {
        System.err.println("addError2: " + msg);
        System.out.println(msg);
        ex.printStackTrace();
    }


    @Override
    public void addFilter(Filter newFilter) {
        System.err.println("addFilter: " + newFilter);
        this.filters.add(newFilter);
    }


    @Override
    public void clearAllFilters() {
        System.err.println("clearAllFilters()");
        this.filters.clear();
    }


    @Override
    public List getCopyOfAttachedFiltersList() {
        //To change body of generated methods, choose Tools | Templates.
        Logger.getLogger(this.getClass().getName()).severe("Not supported yet.");
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public FilterReply getFilterChainDecision(Object event) {
        //To change body of generated methods, choose Tools | Templates.
        Logger.getLogger(this.getClass().getName()).severe("Not supported yet.");
        throw new UnsupportedOperationException("Not supported yet.");
    }


}
