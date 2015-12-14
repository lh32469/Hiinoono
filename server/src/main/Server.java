package com.oracle.el.bkeeper;

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;
import com.oracle.el.bkeeper.rest.master.MasterApp;
import com.oracle.el.bkeeper.rest.ServerApplication;
import java.net.URI;
import javax.ws.rs.ext.RuntimeDelegate;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;


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

        HttpHandler masterApp
                = delegate.createEndpoint(new MasterApp(), clazz);

        serverConfig.addHttpHandler(masterApp, "/v1");

        HttpHandler srvr
                = delegate.createEndpoint(new ServerApplication(), clazz);
        serverConfig.addHttpHandler(srvr, "/svr");

        
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
