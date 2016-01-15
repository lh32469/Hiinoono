package com.hiinoono.os.linux;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.State;
import com.hiinoono.os.ContainerDriver;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;


/**
 *
 * @author Lyle T Harris
 */
public class LinuxContainerDriver implements ContainerDriver {

    @Override
    public synchronized Container create(Container container) throws
            IOException {

        List<String> command = new LinkedList<>();
        command.add("lxc-create");
        command.add("-t");
        command.add(container.getTemplate());
        command.add("-n");
        command.add(container.getName());

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }

        try {
            if (process.waitFor() == 0) {
                container.setState(State.STOPPED);
            } else {
                container.setState(State.ERROR);
            }
        } catch (InterruptedException ex) {
            container.setState(State.ERROR);
        }

        return container;
    }


    public static void main(String[] args) throws IOException {
        LinuxContainerDriver ld = new LinuxContainerDriver();
        Container c = new Container();
        c.setName("cn-test");
        c.setTemplate("ubuntu");
        ld.create(c);
    }


}
