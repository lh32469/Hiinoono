package com.hiinoono;

import com.hiinoono.rest.api.model.HClient;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Scanner;
import javax.ws.rs.WebApplicationException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.LoggerFactory;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.filter.EncodingFilter;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.message.GZipEncoder;


/**
 *
 * @author Lyle T Harris
 */
public class Client {

    private static final String LIST = "list";

    private static final String LOGGING = "log";

    private static final String SERVICE = "service";

    private static final String TENANTS = "tenants";

    private static final String PROXY = "proxy";

    private static final String HELP = "help";

    private static final String VERSION = "version";

    private static final String CLIENT = "HiinoonoClient";

    private static final String USER = "HIINOONO_USER";

    private static final String PASS = "HIINOONO_PASSWORD";

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(Client.class);


    public static void main(String[] args) throws IOException, ParseException {

        Options options = getOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(HELP) || cmd.getOptions().length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(CLIENT, options);
            System.exit(0);
        }

        if (cmd.hasOption(VERSION)) {

            Properties props = new Properties();

            Enumeration<URL> manifest
                    = ClassLoader.getSystemResources("META-INF/MANIFEST.MF");

            while (manifest.hasMoreElements()) {
                URL nextElement = manifest.nextElement();
                // System.out.println("Manifest: " + nextElement.getFile());
                if (nextElement.getFile().contains("Hc.jar")) {
                    props.load(nextElement.openStream());
                    break;
                }
            }

            //  System.out.println("Props: " + prop);
            LOG.info("Version: " + props.getProperty(VERSION)
                    + "  (" + props.getProperty("date") + ")");
            System.exit(0);
        }

        String svc = "http://localhost:8080/api";

        if (cmd.hasOption(SERVICE)) {
            svc = cmd.getOptionValue(SERVICE);
        }

        if (cmd.hasOption(PROXY)) {

            LOG.info("Setting proxy to: " + cmd.getOptionValue(PROXY));
            String option = cmd.getOptionValue(PROXY);
            String proxy = option.replaceFirst("http://", "");
            Scanner sc = new Scanner(proxy).useDelimiter(":");

            String host = sc.next();
            // LOG.info(host);

            // Default proxy port
            String port = "8080";
            if (sc.hasNext()) {
                port = sc.next();
                //  LOG.info(port);
            }

            System.setProperty("http.proxyHost", host);
            System.setProperty("http.proxyPort", port);
        }

        String user = System.getenv(USER);
        if (user == null) {
            LOG.error(USER + " environment variable not set");
            System.exit(1);
        }

        String pass = System.getenv(PASS);
        if (pass == null) {
            LOG.error(PASS + " environment variable not set");
            System.exit(1);
        }

        LOG.info("Connecting to: " + svc);

        HttpAuthenticationFeature authentication
                = HttpAuthenticationFeature.basic(user, pass);

        javax.ws.rs.client.Client c = HClient.createClient();

        c.register(authentication);

        c.register(MultiPartFeature.class);
        c.register(GZipEncoder.class);
        c.register(EncodingFilter.class);

        if (cmd.hasOption(LOGGING)) {
            c.register(LoggingFilter.class);
        }

        try {

            if (cmd.hasOption(LIST)) {
                list(cmd, c, svc);
            }

        } catch (WebApplicationException ex) {
            LOG.error(ex.getLocalizedMessage());
            LOG.error(ex.getResponse().readEntity(String.class));
        }

    }


    /**
     * Implements the --list option functionality.
     */
    private static void list(
            CommandLine cmd,
            javax.ws.rs.client.Client c,
            String svc) {

        String type = cmd.getOptionValue(LIST);
        if (type.equalsIgnoreCase(TENANTS)) {
            HClient.Tenant t = HClient.tenant(c, URI.create(svc));
            System.out.println(t.getAs(String.class) + "\n");
        } else {
            LOG.error("Unrecognized --" + LIST + " argument provided\n");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(CLIENT, getOptions());
            System.exit(1);
        }
    }


    static final Options getOptions() {

        final Options options = new Options();

        options.addOption("h", HELP, false,
                "Display this message.");

        options.addOption("v", VERSION, false,
                "Display version.");

        options.addOption("l", LIST, true,
                "List tenants, nodes, instances ");

        options.addOption("L", LOGGING, false,
                "Enable org.glassfish.jersey.filter.LoggingFilter");

        Option proxy = Option.builder("p")
                .hasArg()
                .argName("http://...")
                .longOpt(PROXY)
                .desc("HTTP Proxy (if needed).")
                .build();
        options.addOption(proxy);

        Option service = Option.builder("s")
                .hasArg()
                .argName("http://...")
                .longOpt(SERVICE)
                .desc("Hiinoono Service API URL.")
                .build();
        options.addOption(service);

        Option addTenant = Option.builder("a")
                .hasArgs()
                .argName("name")
                .longOpt("addTenant")
                .desc("Add a new Tenant.")
                .build();
        options.addOption(addTenant);

        return options;
    }


}
