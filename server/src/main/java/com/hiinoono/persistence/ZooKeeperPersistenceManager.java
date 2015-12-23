package com.hiinoono.persistence;

import com.hiinoono.jaxb.Node;
import com.hiinoono.jaxb.Tenant;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.stream.Stream;


/**
 *
 * @author Lyle T Harris
 */
public class ZooKeeperPersistenceManager implements PersistenceManager {

    @Override
    public Stream<Tenant> getTenants() {
        return Collections.EMPTY_LIST.stream();
    }


    @Override
    public Tenant getTenantByName(String name) {
        return null;
    }


    @Override
    public Stream<Node> getNodes() {
        //To change body of generated methods, choose Tools | Templates.
        Logger.getLogger(this.getClass().getName()).severe("Not supported yet.");
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public String getHash(String tenant, String username) {
        //To change body of generated methods, choose Tools | Templates.
        Logger.getLogger(this.getClass().getName()).severe("Not supported yet.");
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public synchronized String hash(String string) {
        //To change body of generated methods, choose Tools | Templates.
        Logger.getLogger(this.getClass().getName()).severe("Not supported yet.");
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public void addTenant(Tenant t) {
        //To change body of generated methods, choose Tools | Templates.
        Logger.getLogger(this.getClass().getName()).severe("Not supported yet.");
        throw new UnsupportedOperationException("Not supported yet.");
    }


}
