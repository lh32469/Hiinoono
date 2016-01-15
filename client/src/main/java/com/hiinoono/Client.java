package com.hiinoono;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.Node;
import com.hiinoono.jaxb.Nodes;
import com.hiinoono.jaxb.SiteInfo;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.User;
import com.hiinoono.jaxb.Tenants;
import com.hiinoono.rest.api.model.HClient;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.Format;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
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

    /**
     * Format for dates presented to the user.
     */
    private static final Format DTF
            = DateTimeFormatter.RFC_1123_DATE_TIME.toFormat();

    private static final String LIST = "list";

    private static final String LOGGING = "log";

    private static final String SERVICE = "service";

    private static final String TENANTS = "tenants";

    private static final String NODES = "nodes";

    private static final String USERS = "users";

    private static final String PROXY = "proxy";

    private static final String HELP = "help";

    private static final String VERSION = "version";

    private static final String ADD_TENANT = "addTenant";

    private static final String ADD_USER = "addUser";

    private static final String DELETE_TENANT = "deleteTenant";

    private static final String DELETE_USER = "deleteUser";

    private static final String SAMPLE_VM = "sampleVm";

    private static final String ADD_VM = "addVm";

    private static final String ADD_CONTAINER = "addContainer";

    private static final String CLIENT = "HiinoonoClient";

    private static final String API = "HIINOONO_SERVICE";

    private static final String USER = "HIINOONO_USER";

    private static final String PASS = "HIINOONO_PASSWORD";

    private static User user;

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(Client.class);


    public static void main(String[] args) throws IOException, ParseException {

        Options options = getOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (MissingArgumentException ex) {
            System.out.println("");
            LOG.error(ex.getLocalizedMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(CLIENT, options);
            System.exit(1);
        }

        LOG.info("Client Version: " + getVersion());

        if (cmd.hasOption(HELP) || cmd.getOptions().length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(CLIENT, options);
            System.exit(0);
        }

        if (cmd.hasOption(VERSION)) {
            // Version always printed.
            System.exit(0);
        }

        String svc = "http://localhost:8080/api";

        if (System.getenv(API) != null) {
            svc = System.getenv(API);
        }

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

        String _user = System.getenv(USER);
        if (_user == null) {
            LOG.error(USER + " environment variable not set");
            System.exit(1);
        }

        _user = _user.replaceAll("/", " ").trim();

        String[] values = _user.split(" ");
        if (values.length < 2) {
            LOG.error("Invalid syntax for login:  " + _user);
            return;
        }

        user = new User();
        user.setTenant(values[0]);
        user.setName(values[1]);

        String pass = System.getenv(PASS);
        if (pass == null) {
            LOG.error(PASS + " environment variable not set");
            System.exit(1);
        }

        LOG.info("Connecting to: " + svc + " as "
                + user.getTenant() + "/" + user.getName());

        HttpAuthenticationFeature authentication
                = HttpAuthenticationFeature.basic(user.getTenant()
                        + "/" + user.getName(), pass);

        javax.ws.rs.client.Client c = HClient.createClient();

        c.register(authentication);

        c.register(MultiPartFeature.class);
        c.register(GZipEncoder.class);
        c.register(EncodingFilter.class);

        if (cmd.hasOption(LOGGING)) {
            c.register(LoggingFilter.class);
        }

        try {
            // Sanity test service URL
            HClient.Site site = HClient.site(c, URI.create(svc));
            SiteInfo info = site.info().getAsSiteInfo();
            LOG.info("Connected to:  " + info.getName()
                    + ", Version: " + info.getVersion());
        } catch (WebApplicationException ex) {
            if (ex.getResponse().getStatus() == 404) {
                LOG.error("Invalid Hiinoono Service API URL.");
            } else {
                String entity = ex.getResponse().readEntity(String.class);
                if (entity != null && !entity.isEmpty()) {
                    LOG.error(entity);
                } else {
                    LOG.error(ex.getLocalizedMessage());
                }
            }
            return;
        } catch (ProcessingException ex) {
            LOG.error(ex.getLocalizedMessage());
            return;
        }

        try {

            if (cmd.hasOption(LIST)) {
                list(cmd, c, svc);
            } else if (cmd.hasOption(ADD_TENANT)) {
                addTenant(cmd, c, svc);
            } else if (cmd.hasOption(DELETE_TENANT)) {
                deleteTenant(cmd, c, svc);
            } else if (cmd.hasOption(ADD_USER)) {
                addUser(cmd, c, svc);
            } else if (cmd.hasOption(DELETE_USER)) {
                deleteUser(cmd, c, svc);
            } else if (cmd.hasOption(SAMPLE_VM)) {
                sampleVm(cmd, c, svc);
            } else if (cmd.hasOption(ADD_CONTAINER)) {
                addContainer(cmd, c, svc);
            }

        } catch (WebApplicationException ex) {
            String entity = ex.getResponse().readEntity(String.class);
            if (entity != null && !entity.isEmpty()) {
                LOG.error(entity);
            } else {
                LOG.error(ex.getLocalizedMessage());
            }
        } catch (ProcessingException ex) {
            LOG.error("Invalid Hiinoono Service API URL.");
            LOG.error(ex.getLocalizedMessage());
        }

    }


    private static String getVersion() throws IOException {
        Properties props = new Properties();

        Enumeration<URL> manifest
                = ClassLoader.getSystemResources("META-INF/MANIFEST.MF");

        while (manifest.hasMoreElements()) {
            URL nextElement = manifest.nextElement();
            if (nextElement.getFile().contains("Hc.jar")) {
                props.load(nextElement.openStream());
                break;
            }
        }

        return props.getProperty(VERSION)
                + " (" + props.getProperty("date") + ")";
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
            Tenants tenants = t.getAsTenants();
            // TODO: Better formatting
            final String format = "%-15s%-25s\n";
            System.out.println("");
            System.out.printf(format,
                    "Tenant", "Joined");
            for (Tenant tenant : tenants.getTenant()) {
                XMLGregorianCalendar joined = tenant.getJoined();
                ZonedDateTime zdt
                        = joined.toGregorianCalendar().toZonedDateTime();
                System.out.printf(format,
                        tenant.getName(), DTF.format(zdt));
            }
            System.out.println("\nTotal: "
                    + tenants.getTenant().size() + "\n");

        } else if (type.equalsIgnoreCase(NODES)) {
            HClient.Node t = HClient.node(c, URI.create(svc));
            Nodes nodes = t.getAsNodes();
            System.out.println("");
            for (Node node : nodes.getNode()) {
                System.out.println(node.getId() + "  " + node.getHostname());
            }
            System.out.println("");

        } else if (type.equalsIgnoreCase(USERS)) {
            HClient.User u = HClient.user(c, URI.create(svc));
            List<User> users = u.list().getAsUsers().getUser();
            final String format = "%-15s%-25s\n";

            System.out.println("");
            for (User _user : users) {
                XMLGregorianCalendar joined = _user.getJoined();
                if (joined != null) {
                    ZonedDateTime zdt
                            = joined.toGregorianCalendar().toZonedDateTime();
                    System.out.printf(format,
                            _user.getName(), DTF.format(zdt));
                } else {
                    System.out.printf(format,
                            _user.getName(), "Unknown");
                }
            }
            System.out.println("");

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
                .desc("Hiinoono Service API URL or set " + API
                        + " environment variable")
                .build();
        options.addOption(service);

        Option addTenant = Option.builder()
                .hasArgs()
                .argName("name")
                .longOpt(ADD_TENANT)
                .desc("Add a new Tenant.")
                .build();
        options.addOption(addTenant);

        Option deleteTenant = Option.builder()
                .hasArgs()
                .argName("name")
                .longOpt(DELETE_TENANT)
                .desc("Delete a Tenant.")
                .build();
        options.addOption(deleteTenant);

        Option addUser = Option.builder()
                .hasArgs()
                .argName("name")
                .longOpt(ADD_USER)
                .desc("Add a new User.  (Must be Tenant Admin)")
                .build();
        options.addOption(addUser);

        Option deleteUser = Option.builder()
                .hasArgs()
                .argName("name")
                .longOpt(DELETE_USER)
                .desc("Delete a User.  (Must be Tenant Admin)")
                .build();
        options.addOption(deleteUser);

        Option addVm = Option.builder()
                .hasArgs()
                .argName("fileName")
                .longOpt(ADD_VM)
                .desc("Add a new Virtual Machine.")
                .build();
        options.addOption(addVm);

        Option sampleVm = Option.builder()
                .hasArgs()
                .argName("xml|json")
                .longOpt(SAMPLE_VM)
                .desc("Display Sample Virtual Machine.")
                .build();
        options.addOption(sampleVm);

        Option addContainer = Option.builder()
                .hasArgs()
                .argName("fileName")
                .longOpt(ADD_CONTAINER)
                .desc("Add a new Container.")
                .build();
        options.addOption(addContainer);

        return options;
    }


    private static void addTenant(CommandLine cmd,
            javax.ws.rs.client.Client c,
            String svc) {

        String name = cmd.getOptionValue(ADD_TENANT);

        HClient.Tenant.Add add = HClient.tenant(c, URI.create(svc)).add();

        Tenant newTenant = new Tenant();
        newTenant.setName(name);

        /* 
         * Use the JSON option since the XML/JAXB option fails since 
         * when the WADL is compiled the @XmlRootElement(name = "tenant")
         * doesn't get added to the generated class.
         */
        Response response = add.postJsonAsTextPlain(newTenant, Response.class);

        if (response.getStatus() >= 400) {
            throw new WebApplicationException(response);
        } else {
            // Display the new password
            LOG.info(response.readEntity(String.class));
        }

    }


    private static void addUser(CommandLine cmd,
            javax.ws.rs.client.Client c,
            String svc) {

        String newUserName = cmd.getOptionValue(ADD_USER);
        HClient.User.Add add = HClient.user(c, URI.create(svc)).add();

        User _user = new User();
        // Set Tenant name the same as logged-in User.
        _user.setTenant(user.getTenant());
        _user.setName(newUserName);

        /* 
         * Use the JSON option since the XML/JAXB option fails since 
         * when the WADL is compiled the @XmlRootElement(name = "user")
         * doesn't get added to the generated class.
         */
        Response response = add.postJsonAsTextPlain(_user, Response.class);

        if (response.getStatus() >= 400) {
            throw new WebApplicationException(response);
        } else {
            // Display the new password
            LOG.info(response.readEntity(String.class));
        }
    }


    private static void deleteTenant(CommandLine cmd,
            javax.ws.rs.client.Client c,
            String svc) {

        String name = cmd.getOptionValue(DELETE_TENANT);
        HClient.Tenant t = HClient.tenant(c, URI.create(svc));
        HClient.Tenant.DeleteName request = t.deleteName(name);

        Response response = request.getAs(Response.class);

        if (response.getStatus() >= 400) {
            throw new WebApplicationException(response);
        }
    }


    private static void deleteUser(CommandLine cmd,
            javax.ws.rs.client.Client c,
            String svc) {

        String userName = cmd.getOptionValue(DELETE_USER);
        HClient.User u = HClient.user(c, URI.create(svc));
        HClient.User.DeleteName request = u.deleteName(userName);

        Response response = request.getAs(Response.class);

        if (response.getStatus() >= 400) {
            throw new WebApplicationException(response);
        }

    }


    private static void sampleVm(CommandLine cmd,
            javax.ws.rs.client.Client c,
            String svc) {

        String format = cmd.getOptionValue(SAMPLE_VM);
        HClient.Vm vm = HClient.vm(c, URI.create(svc));
        Response response;

        if ("xml".equalsIgnoreCase(format)) {
            response = vm.sample().getAsXml(Response.class);
        } else {
            response = vm.sample().getAsJson(Response.class);
        }

        if (response.getStatus() >= 400) {
            throw new WebApplicationException(response);
        } else {
            System.out.println("\n" + response.readEntity(String.class));
        }

    }


    private static void addContainer(CommandLine cmd,
            javax.ws.rs.client.Client c,
            String svc) {

        HClient.Container container = HClient.container(c, URI.create(svc));

        Container testC = new Container();
        testC.setName("cn-test");
        testC.setTemplate("ubuntu");
        container.create().postXmlAsContainer(testC);

    }


}
