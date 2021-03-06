package com.hiinoono.os;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import org.slf4j.LoggerFactory;


/**
 * HystrixCommand for running basic OS commands.
 *
 * @author Lyle T Harris
 */
public class ShellCommand extends HystrixCommand<String> {

    private final List<String> command;

    /**
     * Optional response to provide in getFallback() method.
     */
    private final String fallbackResponse;

    /**
     * For capturing command stdout.
     */
    private final StringBuilder stdout = new StringBuilder();

    /**
     * Default fallbackResponse if not provided in constructor.
     */
    public static final String ERROR = "ERROR";

    private static final HystrixCommandGroupKey GROUP_KEY
            = HystrixCommandGroupKey.Factory.asKey("ShellCommand");

    private static final HystrixCommandProperties.Setter COMMAND_PROPS
            = HystrixCommandProperties.Setter()
            .withExecutionTimeoutInMilliseconds(300000);  // Should be a param

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ShellCommand.class);


    /**
     * Construct a command to execute.
     *
     * @param command OS Command to execute
     */
    public ShellCommand(String command) {
        this(Arrays.asList(command.split(" ")));
    }


    /**
     * Construct a command to execute.
     *
     * @param command OS Command to execute
     * @param fallbackResponse Optional response to provide in getFallback()
     * method otherwise the string ShellCommand.ERROR is provided.
     */
    public ShellCommand(String command, String fallbackResponse) {
        this(Arrays.asList(command.split(" ")), fallbackResponse);
    }


    /**
     * Construct a command to execute.
     *
     * @param command OS Command and args as List to execute
     */
    public ShellCommand(List<String> command) {
        this(command, null);
    }


    /**
     * Construct a command to execute.
     *
     * @param command OS Command and args as List to execute
     * @param fallbackResponse Optional response to provide in getFallback()
     * method otherwise the string ShellCommand.ERROR is provided.
     */
    public ShellCommand(List<String> command, String fallbackResponse) {
        super(Setter
                .withGroupKey(GROUP_KEY)
                .andCommandPropertiesDefaults(COMMAND_PROPS));

        this.command = command;
        this.fallbackResponse = fallbackResponse;
    }


    @Override
    protected String run() throws Exception {

        LOG.debug(command.toString());

        try {

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                LOG.trace(line);
                stdout.append(line);
                stdout.append("\n");
            }

            int status = process.waitFor();
            if (status != 0) {
                String msg = "Exit status = " + status
                        + " for command: " + command;
                LOG.warn(msg);
                // So Hystrix will register failure.
                throw new IllegalStateException(msg);
            }

            return stdout.toString().trim();

        } catch (Exception ex) {
            LOG.error(stdout.toString().trim());
            LOG.debug(ex.toString(), ex);
            throw ex;
        }

    }


    @Override
    protected String getFallback() {
        if (fallbackResponse == null) {
            return ERROR;
        } else {
            return fallbackResponse;
        }
    }


    /**
     * Get the stdout from the command execution. Used when there is an error.
     *
     * @return
     */
    public String getStdout() {
        return stdout.toString();
    }


}
