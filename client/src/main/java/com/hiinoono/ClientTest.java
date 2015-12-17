package com.hiinoono;

import com.hiinoono.jaxb.Status;
import com.hiinoono.jaxb.SiteInfo;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.Tenants;
import com.hiinoono.jaxb.Value;
import com.hiinoono.rest.api.model.HClient;
import java.net.URI;
import javax.ws.rs.client.Client;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import org.eclipse.persistence.jaxb.MarshallerProperties;


/**
 *
 * @author ltharris
 */
public class ClientTest {

    public static void main(String[] args) throws JAXBException {

        Client c = HClient.createClient();

        HClient.Node node
                = HClient.node(c, URI.create("http://localhost:8080/api"));

        Status status = node.status().getAsStatus();

        JAXBContext jaxbContext = JAXBContext.newInstance(Status.class);
        Marshaller marshaller = jaxbContext.createMarshaller();

        QName qName = new QName("com.hiinoono.jaxb.model", "status");
        JAXBElement<Status> root
                = new JAXBElement<>(qName, Status.class, status);

        // format the XML and JSON output
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        System.out.println("===== XML =====\n");
        marshaller.marshal(root, System.out);
        System.out.println("");

        System.out.println("===== JSON =====\n");
        marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
        marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
        marshaller.marshal(root, System.out);
        System.out.println("\n");

        Value value = status.getValue();
        System.out.println("StatusV: " + value);

        SiteInfo info = HClient.site().info().getAs(SiteInfo.class);

        System.out.println("SiteInfo: " + info);

        Object text = HClient.site().info().getAs(String.class);

        System.out.println("SiteInfo: \n" + text);

        // ============ Tenants ====================== //
        HClient.Tenant tenantClient
                = HClient.tenant();

        Tenants tenants = tenantClient.getAsTenants();

        jaxbContext = JAXBContext.newInstance(Tenants.class);
        marshaller = jaxbContext.createMarshaller();

        qName = new QName("com.hiinoono.jaxb.model", "tenant");
        JAXBElement<Tenants> tenantJaxb
                = new JAXBElement<>(qName, Tenants.class, tenants);

        System.out.println("===== Tenant =====\n");
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
        marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
        marshaller.marshal(tenantJaxb, System.out);
        System.out.println("\n");

    }


}
