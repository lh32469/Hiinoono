package com.hiinoono.rest.tenant;

import com.hiinoono.Server;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.rest.API;
import java.net.URI;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 *
 * @author ltharris
 */
public class TenantResourceTest extends JerseyTest {

    private static HttpServer server;

    private final static String TENANT_NAME = "JUnit Tenant";


    @Override
    protected Application configure() {
        return new API();
    }


    @BeforeClass
    public static void setUpClass() {
        URI base = URI.create("http://localhost:9000");
        server = Server.startServer(base);

    }


    @AfterClass
    public static void tearDownClass() throws InterruptedException {
        // Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        server.shutdownNow();
    }


    @Override
    protected Client getClient() {
        Client c = ClientBuilder.newClient();
        HttpAuthenticationFeature authentication
                = HttpAuthenticationFeature.basic("/hiinoono/admin", "Welcome1");
        c.register(authentication);
        return c;
    }


    /**
     * Test of getTenants method, of class TenantResource.
     */
    @Test
    public void testGetTenants() {
        Response response = target("/tenant").request().get();
        List<Tenant> tenants
                = response.readEntity(new GenericType<List<Tenant>>() {
                });
        assertEquals(2, tenants.size());
    }


    /**
     * Test of addTenant method, of class TenantResource.
     */
    @Test
    public void addTenant() {

        GenericType<List<Tenant>> gt = new GenericType<List<Tenant>>() {
        };
        Response response = target("/tenant").request().get();
        List<Tenant> tenants = response.readEntity(gt);
        boolean found = false;
        for (Tenant tenant : tenants) {
            if (tenant.getName().equals(TENANT_NAME)) {
                found = true;
            }
        }

        assertFalse("Tenant already exists", found);

        Tenant t = new Tenant();
        t.setName(TENANT_NAME);
        Entity<Tenant> e = Entity.entity(t, MediaType.APPLICATION_XML);
        response = target("/tenant/add").request().post(e);
        String reply = response.readEntity(String.class);
        assertTrue(reply.startsWith("Password: "));

        response = target("/tenant").request().get();
        tenants = response.readEntity(new GenericType<List<Tenant>>() {
        });
        found = false;
        for (Tenant tenant : tenants) {
            if (tenant.getName().equals(TENANT_NAME)) {
                found = true;
            }
        }

        assertTrue("Tenant not added", found);

    }


    /**
     * Test of deleteTenant method, of class TenantResource.
     */
    @Test
    public void testDeleteTenant() {
        addTenant();
        target("/tenant/delete/" + TENANT_NAME).request().get();
        // Adding the Tenant again should pass.
        addTenant();
    }


    /**
     * Test of getTenant method, of class TenantResource.
     */
    @Test
    public void testGetTenant() {
        final String name = "Hiinoono-2";
        Response response = target("/tenant/" + name).request().get();
        Tenant tenant = response.readEntity(Tenant.class);
        assertEquals(tenant.getName(), name);
    }


}
