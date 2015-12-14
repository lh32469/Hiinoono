package com.hiinoono;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
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

    private static final String EXCLUDE = "exclude";

    private static final String INCLUDE = "include";

    private static final String RESTORE = "restore";

    private static final String LIST = "list";

    private static final String LOGGING = "log";

    private static final String SERVICE = "service";

    private static final String KEY = "key";

    private static final String GENKEY = "genkey";

    private static final String DIRECTORY = "directory";

    private static final String PROXY = "proxy";

    private static final String HELP = "help";

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(Client.class);


    public static void main(String[] args) throws IOException, ParseException {

        Options options = getOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(HELP) || cmd.getOptions().length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("BackupClient", options);
            System.exit(0);
        }

        if (!cmd.hasOption(KEY)) {
            LOG.error("No key provided");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("BackupClient", options);
            System.exit(0);
        }

        String svc = "http://localhost:7117/";

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

        UriBuilder bldr = UriBuilder.fromUri(svc);
        URI baseUri = bldr.build();

        LOG.info(" Connecting to: " + svc);

        HttpAuthenticationFeature authentication
                = HttpAuthenticationFeature.basic("adminn", "password");

        javax.ws.rs.client.Client c = ClientBuilder.newClient();

        c.register(authentication);

        c.register(MultiPartFeature.class);
        c.register(GZipEncoder.class);
        c.register(EncodingFilter.class);

        if (cmd.hasOption(LOGGING)) {
            c.register(LoggingFilter.class);
        }

        WebTarget target = c.target(baseUri);

        if (cmd.hasOption(DIRECTORY)) {

            if (cmd.hasOption(RESTORE)) {
                String version = cmd.getOptionValue(RESTORE);
                if (version == null) {
                    version = "0";
                }
                LOG.info("Restoring: Version: " + version);
                for (String dir : cmd.getOptionValues(DIRECTORY)) {
                    LOG.info("Restoring: " + dir);

                    InputStream r = target
                            .path("/file/" + dir)
                            .request()
                            .header("Version", version)
                            .get(InputStream.class);

                    ZipInputStream unzip
                            = new ZipInputStream(r);

                    ZipEntry entry;
                    byte[] buffer = new byte[2048];

                    while ((entry = unzip.getNextEntry()) != null) {

                        LOG.info("Restoring: " + entry.getName());
                        String outpath = "./" + entry.getName();
                        new File(outpath).getParentFile().mkdirs();

                        try (FileOutputStream output
                                = new FileOutputStream(outpath)) {
                            int len;
                            while ((len = unzip.read(buffer)) > 0) {
                                output.write(buffer, 0, len);
                            }
                        }
                    }

                }

            } else if (cmd.hasOption(LIST)) {
                for (String dir : cmd.getOptionValues(DIRECTORY)) {
                    LOG.info("Listing: " + dir);

                    File f = new File("");
                    LOG.info("This Dir: " + f.getAbsolutePath());

                    Response r = target
                            .path("/file/list/" + dir)
                            .request()
                            .header("BaseDir", f.getAbsolutePath())
                            .get();
                    LOG.info(r.readEntity(String.class));

                }

            }
        }

    }


    static final Options getOptions() {

        final Options options = new Options();

        options.addOption("x", EXCLUDE, true,
                "Exclude files based on PathMatcher pattern."
                + "\n  Ex: \"glob:**/*.{jar,class,log}\"");

        options.addOption("i", INCLUDE, true,
                "Include files based on PathMatcher pattern."
                + "\n  Ex: \"glob:**/*.{java,py}\"");

        options.addOption("s", SERVICE, true,
                "Backup Service URL.");

        options.addOption("k", KEY, true,
                "Backup Service Key.");

        options.addOption("g", GENKEY, false,
                "Generate Backup Service Key.");

        options.addOption("h", HELP, false,
                "Display this message.");

        options.addOption("l", LIST, false,
                "List the versions of the the directory(ies) listed ");

        options.addOption("L", LOGGING, false,
                "Enable org.glassfish.jersey.filter.LoggingFilter");

        Option directories = Option.builder("d")
                .hasArgs()
                .argName(DIRECTORY)
                .longOpt(DIRECTORY)
                .desc("Directory to backup.  Can occur more than once.")
                .build();
        options.addOption(directories);

        Option proxy = Option.builder("p")
                .hasArg()
                .argName("http://...")
                .longOpt(PROXY)
                .desc("HTTP Proxy (if needed).")
                .build();
        options.addOption(proxy);

        Option restore = Option.builder("r")
                .hasArg()
                .optionalArg(true)
                .argName("version")
                .longOpt(RESTORE)
                .desc("Restore the version of directory(ies) listed using the"
                        + " current directory as the new parent.")
                .build();
        options.addOption(restore);

        return options;
    }


}
