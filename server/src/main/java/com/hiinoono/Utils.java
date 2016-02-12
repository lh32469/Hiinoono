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

    /**
     * Actual 256 bit key for encrypting data. Read from a properties file only
     * readable by root on the server at startup.
     */
    private static final String KEY
            = System.getProperty(PropertyKey.AES_KEY.value());

    /**
     * Convert KEY String to byte array for SecretKeySpec if KEY is not null and
     * not empty.
     */
    private static final byte[] KEY_BYTES
            = (KEY == null || KEY.isEmpty()) ? null : KEY.getBytes();


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

        if (KEY_BYTES == null) {
            return clear;
        }

        // Create key and cipher
        Key aesKey = new SecretKeySpec(KEY_BYTES, "AES");
        Cipher cipher = Cipher.getInstance("AES");

        // encrypt the data
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        return cipher.doFinal(clear);

    }


    public static final byte[] decrypt(byte[] encrypted) throws
            GeneralSecurityException {

        if (KEY_BYTES == null) {
            return encrypted;
        }

        // Create key and cipher
        Key aesKey = new SecretKeySpec(KEY_BYTES, "AES");
        Cipher cipher = Cipher.getInstance("AES");

        // decrypt the data
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        return cipher.doFinal(encrypted);

    }


    /**
     * Temporary for development before switching to actual encryption.
     */
    @Deprecated
    public static final byte[] encrypt2(byte[] clear) throws
            GeneralSecurityException {
        return clear;
    }


    /**
     * Temporary for development before switching to actual encryption.
     */
    @Deprecated
    public static final byte[] decrypt2(byte[] clear) throws
            GeneralSecurityException {
        return clear;
    }


    /**
     * Get the Id of this Node.
     *
     * @return
     */
    public static String getNodeId() {
        // Placeholder
        return System.getProperty(PropertyKey.NODE_ID_PROPERTY.value());
    }


}
