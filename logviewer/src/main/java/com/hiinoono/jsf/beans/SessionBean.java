package com.hiinoono.jsf.beans;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
@ManagedBean(name = "sBean")
@RequestScoped
public class SessionBean {

    final static private Logger LOG
            = LoggerFactory.getLogger(SessionBean.class);

    private final Set<String> nodes = new HashSet<>();

    private String[] filteredNodes;

    private String[] filteredLoggers;

    private String[] filteredLevels;


    public String getDate() {
        return new Date().toString();
    }


    public Set<String> getNodes() {
        return nodes;
    }


    public String[] getFilteredNodes() {
        return filteredNodes;
    }


    public void setFilteredNodes(String[] filteredNodes) {
        this.filteredNodes = filteredNodes;
    }


    public String[] getFilteredLoggers() {
        return filteredLoggers;
    }


    public void setFilteredLoggers(String[] filteredLoggers) {
        this.filteredLoggers = filteredLoggers;
    }


    public String[] getFilteredLevels() {
        return filteredLevels;
    }


    public void setFilteredLevels(String[] filteredLevels) {
        this.filteredLevels = filteredLevels;
    }


}
