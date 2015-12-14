package com.hiinoono.rest.zk;

import javax.inject.Singleton;
import javax.ws.rs.Path;


/**
 *
 * @author Lyle T Harris
 */
@Path(ZooKeeperResource.PATH)
@Singleton
public class ZooKeeperResource {

    public static final String PATH = "/zk";

}
