package com.hiinoono;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.GregorianCalendar;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
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
     * Default 256 bit key for encrypting data.
     */
    private static final String DEFAULT_KEY
            = "5A6BE0127FE74038919E0DA921D8EC78";

    /**
     * Actual 256 bit key for encrypting data. Will eventually be read from a
     * file only readable by root on the server at startup.
     */
    private static final byte[] key
            = System.getProperty("KEY", DEFAULT_KEY).getBytes();


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


    public static final byte[] encrypt(byte[] clear) throws
            GeneralSecurityException {

        // Create key and cipher
        Key aesKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");

        // encrypt the data
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        return cipher.doFinal(clear);

    }


    public static final byte[] decrypt(byte[] encrypted) throws
            GeneralSecurityException {

        // Create key and cipher
        Key aesKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");

        // decrypt the data
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        return cipher.doFinal(encrypted);

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
