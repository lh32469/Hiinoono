package com.hiinoono;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * Common utilities Class.
 *
 * @author Lyle T Harris
 */
public class Utils {

    public static final String NODE_ID_PROPERTY = "NodeId";


    /**
     * Get current date and time.
     *
     * @return
     */
    public static XMLGregorianCalendar now() {

        try {
            GregorianCalendar date = new GregorianCalendar();
            DatatypeFactory dtf = DatatypeFactory.newInstance();
            return dtf.newXMLGregorianCalendar(date);
        } catch (DatatypeConfigurationException ex) {
            // Impossible?
            throw new RuntimeException(ex);
        }

    }


    /**
     * Get the SHA1 hash of the String provided.
     */
    public static String hash(String str) {

        Formatter formatter = new Formatter();

        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.reset();
            md.update(str.getBytes("UTF-8"));

            for (byte b : md.digest()) {
                formatter.format("%02x", b);
            }
        } catch (NoSuchAlgorithmException ex) {
            // SHA1 should always be present.
            throw new RuntimeException(ex);
        } catch (UnsupportedEncodingException ex) {
            // UTF-8 should always be supported.
            throw new RuntimeException(ex);
        }

        return formatter.toString();
    }


    /**
     * Get the Id of this Node.
     *
     * @return
     */
    public static String getNodeId() {
        // Placeholder
        return System.getProperty(NODE_ID_PROPERTY);
    }


}
