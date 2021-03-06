package com.hiinoono.persistence;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.Node;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.Manager;
import java.util.Optional;
import java.util.stream.Stream;


/**
 *
 * @author Lyle T Harris
 */
public interface PersistenceManager {

    /**
     * Get Stream of all Tenants and their Users.
     *
     * @return
     */
    Stream<Tenant> getTenants();


    void addTenant(Tenant t);


    void deleteTenant(String tenantName);


    /**
     * Get the named Tenant object if it exists.
     */
    Optional<Tenant> getTenantByName(String name);


    /**
     * Get Stream of all Nodes.
     *
     * @return
     */
    Stream<Node> getNodes();


    /**
     * Add the Container to the cluster which implies creating and starting it
     * as well.
     *
     * @param container
     */
    void addContainer(Container container);


    void startContainer(Container container);


    void stopContainer(Container container);


    void deleteContainer(Container container);


    /**
     * Get Stream of all Containers.
     *
     * @return
     */
    Stream<Container> getContainers();


    Stream<Manager> getManagers();


    /**
     * Save the object to persistent storage.
     *
     * @param obj
     */
    void persist(Object obj);


    /**
     * Get password hash for named User of given Tenant name.
     *
     * @param tenant Name of the Tenant User belongs to.
     * @param username Name of the User.
     *
     * @return Hash for the User.
     */
    String getHash(String tenant, String username);


    /**
     * Utility method that calculates a hash for the String provided.
     *
     * @param string
     * @return
     */
    String hash(String string);


}
