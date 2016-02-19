package com.hiinoono.jsf.beans;

import com.hiinoono.jaxb.LogEvent;
import java.io.StringReader;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;


/**
 *
 * @author Lyle T Harris
 */
@ManagedBean(name = "logBean")
@RequestScoped()
public class LogBean {

    private static volatile int count;

    final static private Logger LOG
            = LoggerFactory.getLogger(LogBean.class);

    @ManagedProperty("#{rPool}")
    private RedisPool pool;

    @ManagedProperty("#{sBean}")
    private SessionBean sBean;

    private JAXBContext jc;

    private Jedis jedis;

    private final Set<String> levels = new HashSet<>();

    private final Set<String> loggers = new HashSet<>();

    private final Set<String> nodes = new HashSet<>();

    private List<LogEvent> entries = new LinkedList<>();


    public LogBean() {
        count++;
        LOG.info("Construct: (" + count + ") " + this.toString());

    }


    @Override
    protected void finalize() throws Throwable {
        count--;
        LOG.info("finalize: (" + count + ") " + this.toString());
    }


    @PostConstruct
    public void postConstruct() {
        LOG.info("postConstruct: (" + count + ") " + this.toString());
        jedis = pool.get();

        try {
            jc = JAXBContext.newInstance(LogEvent.class);
        } catch (JAXBException ex) {
            LOG.error(ex.toString());
        }

        jedis.select(5);
        Set<String> keys = jedis.keys("*");
        List<String> foo = new LinkedList(keys);
        Collections.sort(foo);

        try {

            for (String key : foo) {
                String xml = jedis.get(key);
                Unmarshaller um = jc.createUnmarshaller();
                StringReader reader = new StringReader(xml);
                StreamSource source = new StreamSource(reader);
                JAXBElement<LogEvent> jaxb
                        = um.unmarshal(source, LogEvent.class);
                LogEvent entry = jaxb.getValue();

                levels.add(entry.getLevel());

                String node = entry.getNodeId();
                node = node.split("-")[0];
                entry.setNodeId(node);
                nodes.add(node);

                String logger = entry.getLoggerName();
                logger = logger.replaceFirst("com.hiinoono.", "...");
                entry.setLoggerName(logger);
                loggers.add(logger);

                entries.add(entry);
            }

        } catch (Exception ex) {

        }

    }


    @PreDestroy
    public void preDestroy() {
        LOG.info("preDestroy: (" + count + ") " + this.toString());
        pool.release(jedis);
    }


    public void setPool(RedisPool pool) {
        LOG.info(pool.toString());
        this.pool = pool;
    }


    public void setsBean(SessionBean sBean) {
        this.sBean = sBean;
    }


    public List<LogEvent> getEntries() throws JAXBException {
        //  LOG.info("getEntries: " + entries.size());

        String[] filteredNodes = sBean.getFilteredNodes();
        if (filteredNodes != null) {
            for (String node : filteredNodes) {
                LOG.info("filteredNode: " + node);
                entries.removeIf(e -> e.getNodeId().contains(node));
            }
        }

        String[] filteredLoggers = sBean.getFilteredLoggers();
        if (filteredLoggers != null) {
            for (String logger : filteredLoggers) {
                LOG.info("filteredLogger: " + logger);
                entries.removeIf(e -> e.getLoggerName().contains(logger));
            }
        }

        String[] filteredLevels = sBean.getFilteredLevels();
        if (filteredLevels != null) {
            for (String level : filteredLevels) {
                LOG.info("filteredLevel: " + level);
                entries.removeIf(e -> e.getLevel().contains(level));
            }
        }

        return entries;
    }


    public Set<String> getLevels() {
        LOG.info("getLevels");
        return levels;
    }


    public Set<String> getLoggers() {
        LOG.info("getLoggers");
        return loggers;
    }


    public Set<String> getNodes() {
        return nodes;
    }


    public String convertTime(long time) {
        Date date = new Date(time);
        Format format = new SimpleDateFormat("HH:mm:ss.SSS");
        return format.format(date);
    }


}
