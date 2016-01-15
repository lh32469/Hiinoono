package com.hiinoono;

import com.hiinoono.rest.API;
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
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

        HttpHandler api
                = delegate.createEndpoint(new API(), clazz);

        serverConfig.addHttpHandler(api, "/api");

        // Add Hystrix Servlet for Dashboard
        WebappContext context = new WebappContext("Hystrix", "/hystrix");
        context.addServlet("MetricsStreamServlet",
                HystrixMetricsStreamServlet.class);
        context.deploy(server);

        System.out.println("\nWeb Application Description available at:\n\t"
                + base + "/api/application.wadl");

        return server;
    }


    public static void main(String[] args) throws
            InterruptedException, IOException {

        String port = System.getProperty("port", "8080");

        URI base = URI.create("http://0.0.0.0:" + port);

        final Path nodeIdFile = Paths.get("/etc/hiinoono/nodeId");
        
        try {
            List<String> lines = Files.readAllLines(nodeIdFile);
            if (lines.size() == 1) {
                System.setProperty(Utils.NODE_ID_PROPERTY, lines.get(0));
                System.out.println("Starting Node: " + lines.get(0));
            } else {
                System.err.print("\n\nERROR: ");
                System.err.println(nodeIdFile + " corrupt\n\n");
                System.exit(1);
            }

        } catch (NoSuchFileException ex) {
            String id = UUID.randomUUID().toString() + "\n";
            Files.write(nodeIdFile, id.getBytes(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
        }

        startServer(base);
        Thread.sleep(Long.MAX_VALUE);

    }


}
