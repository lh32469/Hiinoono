package com.hiinoono;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import com.hiinoono.jaxb.User;
import org.apache.commons.cli.OptionGroup;


/**
 *
 * @author Lyle T Harris
 */
public class HiinoonoOptions {

    private static final String LIST = "list";

    private static final String LOGGING = "log";

    private static final String SERVICE = "service";

    private static final String PROXY = "proxy";

    private static final String HELP = "help";

    private static final String VERSION = "version";

    private static final String ADD_TENANT = "addTenant";

    private static final String ADD_USER = "addUser";

    private static final String DELETE_TENANT = "deleteTenant";

    private static final String DELETE_USER = "deleteUser";

    private static final String SAMPLE_VM = "sampleVm";

    private static final String ADD_VM = "addVm";

    static final String ADD_CONTAINER = "addContainer";

    static final String START_CONTAINER = "startContainer";

    static final String STOP_CONTAINER = "stopContainer";

    static final String DELETE_CONTAINER = "deleteContainer";

    static final String GET_CONTAINER = "getContainer";

    private static final String API = "HIINOONO_SERVICE";


    static final Options getOptions(User user) {

        final Options options = new Options();

        final boolean hAdmin = user.getTenant().equals("hiinoono");
        final boolean tAdmin = user.getName().equals("admin");

        if (hAdmin) {
            addHiinoonoAdminOptions(options);
        } else if (tAdmin) {
            addTenantAdminOptions(options);
        } else {
            addUserOptions(options);
        }

        options.addOption("h", HELP, false,
                "Display this message.");

        options.addOption("v", VERSION, false,
                "Display version.");

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

        return options;
    }


    static final void addHiinoonoAdminOptions(Options options) {

        options.addOption("l", LIST, true,
                "List tenants, nodes, instances, containers ");

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

        Option getContainer = Option.builder()
                .hasArgs()
                .argName("tenant/user/container")
                .longOpt(GET_CONTAINER)
                .desc("Get info on Container.")
                .build();
        options.addOption(getContainer);

    }


    static final void addTenantAdminOptions(Options options) {

        options.addOption("l", LIST, true,
                "List instances, containers ");

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

        Option getContainer = Option.builder()
                .hasArgs()
                .argName("user/container")
                .longOpt(GET_CONTAINER)
                .desc("Get info on Container.")
                .build();
        options.addOption(getContainer);

        Option stopContainer = Option.builder()
                .hasArgs()
                .argName("container")
                .longOpt(STOP_CONTAINER)
                .desc("Stop a Container.")
                .build();
        options.addOption(stopContainer);

    }


    static final void addUserOptions(Options options) {

        options.addOption("l", LIST, true,
                "List instances, containers ");

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

        Option getContainer = Option.builder()
                .hasArgs()
                .argName("container")
                .longOpt(GET_CONTAINER)
                .desc("Get info on Container.")
                .build();
        options.addOption(getContainer);

        OptionGroup group = new OptionGroup();
               
        Option stopContainer = Option.builder()
                .hasArgs()
                .argName("container")
                .longOpt(STOP_CONTAINER)
                .desc("Stop a Container.")
                .build();
        group.addOption(stopContainer);

        Option startContainer = Option.builder()
                .hasArgs()
                .argName("container")
                .longOpt(START_CONTAINER)
                .desc("Start a Container.")
                .build();
        group.addOption(startContainer);

        Option deleteContainer = Option.builder()
                .hasArgs()
                .argName("container")
                .longOpt(DELETE_CONTAINER)
                .desc("Delete a Container.")
                .build();
        group.addOption(deleteContainer);
        
         options.addOptionGroup(group);

    }


}
