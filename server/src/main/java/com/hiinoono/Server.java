package com.hiinoono;

import com.hiinoono.rest.API;
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
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

    private static final String PROPERTIES_FILE = "/etc/hiinoono/config.props";

    private static final String LOGDIR = "/var/log/hiinoono";


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

        try {

            // Create Log directory.
            Path logDir = Paths.get(LOGDIR);
            if (!Files.exists(logDir, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(logDir);
            }

            File propsFile = new File(PROPERTIES_FILE);

            Path parent = Paths.get(propsFile.toURI()).getParent();
            if (!Files.exists(parent, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(parent);
            }

            // Load Properies
            Properties props = new Properties();
            FileReader reader = new FileReader(propsFile);
            props.load(reader);

            System.getProperties().putAll(props);

        } catch (FileNotFoundException ex) {
            System.err.println("Creating Default Properties file.");
            Properties props = new Properties();
            String id = UUID.randomUUID().toString();
            props.setProperty(PropertyKey.NODE_ID_PROPERTY.value(), id);
            props.put(PropertyKey.AES_KEY.value(),
                    "5A6BE0127FE74038919E0DA921D8EC78");
            System.getProperties().putAll(props);
            FileWriter propsFile = new FileWriter(PROPERTIES_FILE);
            props.store(propsFile, "Default Properties");

        }

        startServer(base);
        Thread.sleep(Long.MAX_VALUE);

    }


}
