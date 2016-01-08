package com.hiinoono.rest.vm;

import com.hiinoono.jaxb.VM;
import com.hiinoono.os.VirtualMachineDriver;
import com.hiinoono.persistence.PersistenceManager;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
@Path(VirtualMachineResource.PATH)
public class VirtualMachineResource {

    public static final String PATH = "/vm";

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(VirtualMachineResource.class);

    @Inject
    private PersistenceManager pm;

    @Inject
    private VirtualMachineDriver vmDriver;

    @Context
    private SecurityContext sc;


    @GET
    @Path("sample")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public VM getSample() {
        
        LOG.info("VM Driver: " + vmDriver);

        VM vm = new VM();
        vm.setName("sample");
        vm.setRam("256M");
        vm.setMachineType("pc,accel=kvm");
        vm.setDrive("50G");
        vm.setCpu("host");
        return vm;
    }


}
