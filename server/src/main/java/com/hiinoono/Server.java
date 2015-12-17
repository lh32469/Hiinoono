package com.hiinoono;

import com.hiinoono.rest.API;
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;
import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import java.net.URI;
import javax.ws.rs.ext.RuntimeDelegate;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ContainerFactory;


/**
 * Main entry point for Java application. Adds the REST Applications to the
 * server and starts it.
 *
 * @author Lyle T Harris
 */
public class Server {

    /**
     * Starts Grizzly HTTP server exposing JAX-RS Applications defined in this
     * project.
     *
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer(URI base) {

        HttpServer server
                = GrizzlyHttpServerFactory.createHttpServer(base);

        ServerConfiguration serverConfig = server.getServerConfiguration();
        RuntimeDelegate delegate = RuntimeDelegate.getInstance();

        Class<HttpHandler> clazz = HttpHandler.class;

        HttpHandler api
                = delegate.createEndpoint(new API(), clazz);
        
        serverConfig.addHttpHandler(api, "/api");

        // Add Hystrix Servlet for Dashboard
        WebappContext context = new WebappContext("Hystrix", "/hystrix");
        context.addServlet("MetricsStreamServlet",
                HystrixMetricsStreamServlet.class);
        context.deploy(server);
        
        return server;
    }


    public static void main(String[] args) throws InterruptedException {
        String port = System.getProperty("port", "8080");

        URI base = URI.create("http://localhost:" + port);

        startServer(base);
        Thread.sleep(Long.MAX_VALUE);

    }


}
