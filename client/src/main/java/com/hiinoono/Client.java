package com.hiinoono;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.Manager;
import com.hiinoono.jaxb.Node;
import com.hiinoono.jaxb.Nodes;
import com.hiinoono.jaxb.SiteInfo;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.User;
import com.hiinoono.rest.api.model.HClient;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.rs.MOXyJsonProvider;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.LoggerFactory;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.filter.EncodingFilter;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;


/**
 *
 * @author Lyle T Harris
 */
public class Client {

    /**
     * Format for dates presented to the user.
     */
    private static final Format DTF
            = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final String LIST = "list";

    private static final String LOGGING = "log";

    private static final String SERVICE = "service";

    private static final String TENANTS = "tenants";

    private static final String MANAGERS = "managers";

    private static final String NODES = "nodes";

    private static final String CONTAINERS = "containers";

    private static final String USERS = "users";

    private static final String PROXY = "proxy";

    private static final String HELP = "help";

    private static final String VERSION = "version";

    private static final String ADD_TENANT = "addTenant";

    private static final String ADD_USER = "addUser";

    private static final String DELETE_TENANT = "deleteTenant";

    private static final String DELETE_USER = "deleteUser";

    private static final String ADD_CONTAINER = "addContainer";

    private static final String GET_CONTAINER = "getContainer";

    private static final String CLIENT = "HiinoonoClient";

    private static final String API = "HIINOONO_SERVICE";

    private static final String USER = "HIINOONO_USER";

    private static final String PASS = "HIINOONO_PASSWORD";

    private static User user;

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(Client.class);


    public static void main(String[] args) throws IOException, ParseException, java.text.ParseException {

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

        Options options = HiinoonoOptions.getOptions(user);
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException ex) {
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

        String services = "http://localhost:8080/api";

        if (System.getenv(API) != null) {
            services = System.getenv(API);
        }

        if (cmd.hasOption(SERVICE)) {
            services = cmd.getOptionValue(SERVICE);
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

        String pass = System.getenv(PASS);
        if (pass == null) {
            LOG.error(PASS + " environment variable not set");
            System.exit(1);
        }

        HttpAuthenticationFeature authentication
                = HttpAuthenticationFeature.basic(user.getTenant()
                        + "/" + user.getName(), pass);

        javax.ws.rs.client.Client c = HClient.createClient();

        c.register(authentication);

        c.register(MultiPartFeature.class);
        c.register(GZipEncoder.class);
        c.register(EncodingFilter.class);
        c.register(MOXyJsonProvider.class);

        MoxyJsonConfig config = new MoxyJsonConfig();
        config.setFormattedOutput(true);
        config.setIncludeRoot(true);
        c.register(config.resolver());

        if (cmd.hasOption(LOGGING)) {
            c.register(LoggingFilter.class);
        }

        // Get list of possible service URL API endpoints provided and
        // shuffle them for load balancing.
        List<String> serviceList = Arrays.asList(services.split(","));
        Collections.shuffle(serviceList);

        // The resulting service URL that this client will use.
        String svc = null;

        // Find a working service URL.
        SiteInfo info = null;
        Iterator<String> iter = serviceList.iterator();

        while (info == null && iter.hasNext()) {
            try {
                svc = iter.next();
                HClient.Site site = HClient.site(c, URI.create(svc));
                info = site.info().getAsSiteInfo();
            } catch (WebApplicationException | ProcessingException ex) {
                LOG.warn(svc + " " + ex.getLocalizedMessage());
            }
        }

        try {
            // Sanity test service URL to make sure a viable one was selected.
            LOG.info("Connecting to: " + svc + " as "
                    + user.getTenant() + "/" + user.getName());
            HClient.Site site = HClient.site(c, URI.create(svc));
            info = site.info().getAsSiteInfo();
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
            } else if (cmd.hasOption(ADD_CONTAINER)) {
                addContainer(cmd, c, svc);
            } else if (cmd.hasOption(GET_CONTAINER)) {
                getContainer(cmd, c, svc);
            } else if (cmd.hasOption(HiinoonoOptions.SAMPLE_CONTAINER)) {
                getSampleContainer(cmd, c, svc);
            } else if (cmd.hasOption(HiinoonoOptions.STOP_CONTAINER)) {
                stopContainer(cmd, c, svc);
            } else if (cmd.hasOption(HiinoonoOptions.START_CONTAINER)) {
                startContainer(cmd, c, svc);
            } else if (cmd.hasOption(HiinoonoOptions.DELETE_CONTAINER)) {
                deleteContainer(cmd, c, svc);
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
            String svc) throws java.text.ParseException {

        String type = cmd.getOptionValue(LIST);
        if (type.equalsIgnoreCase(TENANTS)) {
            HClient.Tenant t = HClient.tenant(c, URI.create(svc));

            List<Tenant> tenants
                    = t.getAs(new GenericType<List<Tenant>>() {
                    });

            // TODO: Better formatting
            final String format = "%-15s%-25s\n";
            System.out.println("");
            System.out.printf(format,
                    "Tenant", "Joined");
            for (Tenant tenant : tenants) {
                Date joined
                        = tenant.getJoined().toGregorianCalendar().getTime();
                System.out.printf(format,
                        tenant.getName(), DTF.format(joined));
            }
            System.out.println("\nTotal: "
                    + tenants.size() + "\n");

        } else if (type.equalsIgnoreCase(MANAGERS)) {
            listManagers(c, URI.create(svc));
        } else if (type.equalsIgnoreCase(NODES)) {
            HClient.Node t = HClient.node(c, URI.create(svc));
            Nodes nodes = t.getAsNodes();
            final String format = "%-17s%-10s%4s%11s%8s%9s  %-25s\n";
            System.out.println("");
            System.out.printf(format,
                    "Hostname",
                    "NodeId",
                    "Cs",
                    "M-Subs",
                    "M-Tot",
                    "Disk",
                    "Joined");

            final NumberFormat memFormat = new DecimalFormat("'MEG_'#");
            final NumberFormat diskFormat = new DecimalFormat("'GIG_'#");

            for (Node node : nodes.getNode()) {
                Date joined = node.getJoined().toGregorianCalendar().getTime();

                long memUsed = node.getContainer().stream().mapToLong(cn
                        -> cn.getMemUsage() / 1024000).sum();

                int memAllocated = node.getContainer().stream().mapToInt(cn -> {
                    try {
                        String option = cn.getMemory().toString();
                        return memFormat.parse(option).intValue();
                    } catch (java.text.ParseException ex) {
                        return 0;
                    }
                }).sum();

                int diskUsed
                        = (int) (node.getVgTotal() - node.getVgFree()) / 1024;

                int diskTotal = (int) (node.getVgTotal() / 1024);

                System.out.printf(format,
                        node.getHostname(),
                        node.getId().split("-")[0] + "..",
                        node.getContainer().size(),
                        memUsed + "/" + memAllocated,
                        node.getMemTotal() / 1024,
                        diskUsed + "/" + diskTotal,
                        DTF.format(joined));
            }

            System.out.println("");

        } else if (type.equalsIgnoreCase(USERS)) {
            HClient.User.List list = HClient.user(c, URI.create(svc)).list();

            List<User> users
                    = list.getAs(new GenericType<List<User>>() {
                    });

            final String format = "%-15s%-25s\n";

            System.out.println("");
            for (User _user : users) {
                Date joined
                        = _user.getJoined().toGregorianCalendar().getTime();
                if (joined != null) {
                    System.out.printf(format,
                            _user.getName(), DTF.format(joined));
                } else {
                    System.out.printf(format,
                            _user.getName(), "Unknown");
                }
            }
            System.out.println("");

        } else if (type.equalsIgnoreCase(CONTAINERS)) {
            HClient.Container cont = HClient.container(c, URI.create(svc));

            List<Container> containers
                    = cont.list().getAs(new GenericType<List<Container>>() {
                    });

            //Containers containers = cont.list().getAsContainers();
            final String hinoonoAdminFormat
                    = "%-12s%-11s%-11s%-12s%-10s%-12s%-25s\n";
            final String tenantAdminformat
                    = "%-15s%-15s%-15s%-10s%-25s\n";
            final String userFormat
                    = "%-11s%-11s%-9s%10s%6s  %-22s\n";

            final NumberFormat memFormat = new DecimalFormat("'MEG_'#");
            final NumberFormat diskFormat = new DecimalFormat("'GIG_'#");

            System.out.println("");

            if (user.getTenant().equals("hiinoono")) {
                System.out.printf(hinoonoAdminFormat,
                        "Name",
                        "Tenant",
                        "User",
                        "Template",
                        "State",
                        "Node",
                        "Added");
            } else if (user.getName().equals("admin")) {
                System.out.printf(tenantAdminformat,
                        "Name",
                        "User",
                        "Template",
                        "State",
                        "Added");
            } else {
                System.out.printf(userFormat,
                        "Name",
                        "Template",
                        "State",
                        "Memory",
                        "Disk",
                        "Added");
            }

            for (Container container : containers) {

                Date added
                        = container.getAdded().toGregorianCalendar().getTime();

                if (user.getTenant().equals("hiinoono")) {
                    System.out.printf(hinoonoAdminFormat,
                            container.getName(),
                            container.getOwner().getTenant(),
                            container.getOwner().getName(),
                            capitalize(container.getTemplate().toString()),
                            capitalize(container.getState().toString()),
                            container.getNodeId().split("-")[0] + "..",
                            DTF.format(added));
                } else if (user.getName().equals("admin")) {
                    System.out.printf(tenantAdminformat,
                            container.getName(),
                            container.getOwner().getName(),
                            capitalize(container.getTemplate().toString()),
                            capitalize(container.getState().toString()),
                            DTF.format(added));
                } else {
                    long memUsed = container.getMemUsage() / 1024 / 1024;
                    String tmp = container.getMemory().toString();
                    long memAlloc = memFormat.parse(tmp).longValue();

                    tmp = container.getDisk().toString();
                    int diskUsed = diskFormat.parse(tmp).intValue();

                    System.out.printf(userFormat,
                            container.getName(),
                            capitalize(container.getTemplate().toString()),
                            capitalize(container.getState().toString()),
                            memUsed + "/" + memAlloc,
                            diskUsed + "G",
                            DTF.format(added));
                }

            }
            System.out.println("");

        } else {
            LOG.error("Unrecognized --" + LIST + " argument provided\n");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(CLIENT, HiinoonoOptions.getOptions(user));
            System.exit(1);
        }
    }


    private static void listManagers(javax.ws.rs.client.Client c, URI svc) {
        HClient.Node node = HClient.node(c, svc);
        List<Manager> managers
                = node.managers().getAsManagers().getManager();
    }


    private static void addTenant(CommandLine cmd,
            javax.ws.rs.client.Client c,
            String svc) {

        String name = cmd.getOptionValue(ADD_TENANT);

        HClient.Tenant.Add add = HClient.tenant(c, URI.create(svc)).add();

        Tenant newTenant = new Tenant();
        newTenant.setName(name);

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
         * Use the XML option since the XML/JAXB option fails since 
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

        String[] names = cmd.getOptionValues(DELETE_TENANT);
        HClient.Tenant t = HClient.tenant(c, URI.create(svc));

        for (String name : names) {
            LOG.info("Deleting: " + name);

            HClient.Tenant.DeleteName request = t.deleteName(name);
            Response response = request.getAs(Response.class);

            if (response.getStatus() >= 400) {
                throw new WebApplicationException(response);
            }
        }

    }


    private static void deleteUser(CommandLine cmd,
            javax.ws.rs.client.Client c,
            String svc) {

        String[] names = cmd.getOptionValues(DELETE_USER);
        HClient.User u = HClient.user(c, URI.create(svc));

        for (String name : names) {
            LOG.info("Deleting: " + name);
            HClient.User.DeleteName request = u.deleteName(name);

            Response response = request.getAs(Response.class);

            if (response.getStatus() >= 400) {
                throw new WebApplicationException(response);
            }
        }

    }


    private static void addContainer(CommandLine cmd,
            javax.ws.rs.client.Client c,
            String svc) {

        String[] files = cmd.getOptionValues(HiinoonoOptions.ADD_CONTAINER);

        for (String name : files) {

            HClient.Container container = HClient.container(c, URI.create(svc));

            final Class[] classes = {Container.class, Tenant.class, Node.class};
            final Map<String, Object> properties = new HashMap<>();

            if (!cmd.hasOption(HiinoonoOptions.XML)
                    && !name.toLowerCase().endsWith(".xml")) {
                properties.put("eclipselink.media-type", "application/json");
            }

            try {

                JAXBContext jc
                        = JAXBContextFactory.createContext(classes, properties);
                Unmarshaller um = jc.createUnmarshaller();

                FileInputStream fis = new FileInputStream(name);
                // Make this two-step process so if file is not
                // found the FileNotFoundException will be cleanly 
                // thrown versus the uglier version as part of 
                // a JAXBException.
                StreamSource source = new StreamSource(fis);

                JAXBElement<Container> userElement
                        = um.unmarshal(source, Container.class);
                Container tc = userElement.getValue();

                LOG.info("Adding: " + tc.getName());

                container.create().postXmlAsContainer(tc);
            } catch (FileNotFoundException | JAXBException ex) {
                LOG.error(ex.toString());
            }

        }
    }


    private static void getContainer(CommandLine cmd,
            javax.ws.rs.client.Client c,
            String svc) {

        String name = cmd.getOptionValue(GET_CONTAINER);
        List<String> list = Arrays.asList(name.split("/"));

        // Make list [cn-name/user/tenant] 
        Collections.reverse(list);

        String tenantName = user.getTenant();
        String userName = user.getName();

        if (list.size() > 1) {
            // There is a user option
            userName = list.get(1);
        }

        if (list.size() > 2) {
            // There is a tenant option
            tenantName = list.get(2);
        }

        HClient.Container cRequest = HClient.container(c, URI.create(svc));
        HClient.Container.GetTenantUserName get
                = cRequest.getTenantUserName(tenantName, userName, list.get(0));

        if (cmd.hasOption(HiinoonoOptions.XML)) {
            // Default from server is XML
            System.out.print(get.getAs(String.class));
        } else {
            marshallAsJson(get.getAsContainer());
        }

    }


    private static void stopContainer(CommandLine cmd,
            javax.ws.rs.client.Client c,
            String svc) {

        String[] containers
                = cmd.getOptionValues(HiinoonoOptions.STOP_CONTAINER);

        for (String name : containers) {
            LOG.info("Stopping: " + name);

            List<String> list = Arrays.asList(name.split("/"));

            // Make list [cn-name/user/tenant] 
            Collections.reverse(list);

            String tenantName = user.getTenant();
            String userName = user.getName();

            if (list.size() > 1) {
                // There is a user option
                userName = list.get(1);
            }

            if (list.size() > 2) {
                // There is a tenant option
                tenantName = list.get(2);
            }

            HClient.Container container = HClient.container(c, URI.create(svc));
            HClient.Container.StopTenantUserName get
                    = container.stopTenantUserName(tenantName,
                            userName, list.get(0));

            get.getAs(String.class);
        }

    }


    private static void deleteContainer(CommandLine cmd,
            javax.ws.rs.client.Client c,
            String svc) {

        String[] containers
                = cmd.getOptionValues(HiinoonoOptions.DELETE_CONTAINER);

        for (String name : containers) {
            LOG.info("Deleting: " + name);
            List<String> list = Arrays.asList(name.split("/"));

            // Make list [cn-name/user/tenant] 
            Collections.reverse(list);

            String tenantName = user.getTenant();
            String userName = user.getName();

            if (list.size() > 1) {
                // There is a user option
                userName = list.get(1);
            }

            if (list.size() > 2) {
                // There is a tenant option
                tenantName = list.get(2);
            }

            HClient.Container container = HClient.container(c, URI.create(svc));
            HClient.Container.DeleteTenantUserName get
                    = container.deleteTenantUserName(tenantName,
                            userName, list.get(0));

            get.getAs(String.class);
        }

    }


    private static void startContainer(CommandLine cmd,
            javax.ws.rs.client.Client c,
            String svc) {

        String[] containers
                = cmd.getOptionValues(HiinoonoOptions.START_CONTAINER);

        for (String name : containers) {
            LOG.info("Starting: " + name);
            List<String> list = Arrays.asList(name.split("/"));

            // Make list [cn-name/user/tenant] 
            Collections.reverse(list);

            String tenantName = user.getTenant();
            String userName = user.getName();

            if (list.size() > 1) {
                // There is a user option
                userName = list.get(1);
            }

            if (list.size() > 2) {
                // There is a tenant option
                tenantName = list.get(2);
            }

            HClient.Container container = HClient.container(c, URI.create(svc));
            HClient.Container.StartTenantUserName get
                    = container.startTenantUserName(tenantName,
                            userName, list.get(0));

            get.getAs(String.class);
        }

    }


    private static void getSampleContainer(CommandLine cmd,
            javax.ws.rs.client.Client c,
            String svc) {

        HClient.Container cRequest = HClient.container(c, URI.create(svc));

        if (cmd.hasOption(HiinoonoOptions.XML)) {
            System.out.println(cRequest.sample().getAsXml(String.class));
        } else {
            System.out.println(cRequest.sample().getAsJson(String.class));
        }

    }


    private static void marshallAsJson(Object jaxb) {

        final Class[] classes = {Container.class, Tenant.class, Node.class};
        final Map<String, Object> properties = new HashMap<>();
        properties.put("eclipselink.media-type", "application/json");

        try {
            JAXBContext jc
                    = JAXBContextFactory.createContext(classes, properties);

            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(jaxb, System.out);
            System.out.println("");

        } catch (JAXBException ex) {
            LOG.error(ex.toString());
        }
    }


    /**
     * Returns the word with only the 1st letter in uppercase.
     *
     * @param word
     * @return
     */
    static String capitalize(String word) {
        word = word.toLowerCase();
        return word.substring(0, 1).toUpperCase() + word.substring(1);

    }


}
