package com.hiinoono.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 *
 * @author Lyle T Harris
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "logEvent", propOrder = {
    "timeStamp",
    "loggerName",
    "hostName",
    "nodeId",
    "message",
    "level"
})
@XmlRootElement(name = "logEvent")
public class LogEvent {

    protected long timeStamp;

    @XmlElement(required = true)
    protected String loggerName;

    @XmlElement()
    protected String hostName;

    @XmlElement(required = true)
    protected String nodeId;

    @XmlElement(required = true)
    protected String message;

    @XmlElement(required = true)
    protected String level;


    public long getTimeStamp() {
        return timeStamp;
    }


    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }


    public String getLoggerName() {
        return loggerName;
    }


    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }


    public String getMessage() {
        return message;
    }


    public void setMessage(String message) {
        this.message = message;
    }


    public String getHostName() {
        return hostName;
    }


    public void setHostName(String hostName) {
        this.hostName = hostName;
    }


    public String getNodeId() {
        return nodeId;
    }


    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }


    public String getLevel() {
        return level;
    }


    public void setLevel(String level) {
        this.level = level;
    }


}
