import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import java.nio.charset.StandardCharsets;

import pt.gov.cartaodecidadao.*;

/* 
   This code example demonstrates the raw data signing feature of pteid-mw SDK
   Available algorithm is RSA-SHA256 with PKCS#1 padding, in future versions other algorithm options may be offered.
*/

public class SignData {

    //This static block is needed to load the sdk library
    static {
        try {
            System.loadLibrary("pteidlibj");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load. \n" + e);
            System.exit(1);
        }
    }
    
    //Main attributes needed for SDK functionalities
    PTEID_ReaderSet readerSet = null;
    PTEID_ReaderContext readerContext = null;
    PTEID_EIDCard eidCard = null;
    final static String dataToBeSigned = "This is our input data for digital signature";

    /**
     * Initializes the SDK and sets main variables
     * @throws PTEID_Exception when there is some error with the SDK methods
     */
    public void initiate() throws PTEID_Exception {
       
        //Must always be called in the beginning of the program
        PTEID_ReaderSet.initSDK();

        //Gets the set of connected readers
        readerSet = PTEID_ReaderSet.instance();

        //Gets the first reader
        //When multiple readers are connected, you can iterate through the various reader objects with the methods getReaderName and getReaderByName or getReaderByNum
        //Any reader can be checked for an inserted card with PTEID_ReaderContext.isCardPresent()
        readerContext = readerSet.getReader();

        //Gets the card instance
        eidCard = readerContext.getEIDCard();
    }
    
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    private PTEID_ByteArray getSignatureInput() {
        MessageDigest digest;
        
        try {
            digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 digest algorithm not present in this JVM!");
            return null;
        }
        byte[] data_hash = digest.digest(dataToBeSigned.getBytes(StandardCharsets.UTF_8));
        return new PTEID_ByteArray(data_hash, data_hash.length);
    }

    private void verifySignature(Certificate signature_certificate, byte[] card_signature) {
        try {

            PublicKey pk = signature_certificate.getPublicKey();
            String signatureAlgo = pk instanceof RSAPublicKey ? "SHA256withRSA": "SHA256withECDSA";
            Signature sig = Signature.getInstance(signatureAlgo);
            sig.initVerify(signature_certificate.getPublicKey());
            sig.update(dataToBeSigned.getBytes(StandardCharsets.UTF_8));
            boolean verified = sig.verify(card_signature);

            System.out.format("%s signature is: %s\n", signatureAlgo, (verified ? "OK": "NOK"));
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Non-conforming JVM, this algo is present in the Java Security Standard Algorithm Names Specification!");
            e.printStackTrace();
        }
        catch (InvalidKeyException e) {
            System.err.println("Can't use provided public key: "+e.getMessage());
        }
        catch (SignatureException e) {
            System.err.println("Failed to verify signature!: "+ e.getMessage());
        }
    }

    private Certificate loadCertificateFromDEREncoding(byte[] data) {
        ByteArrayInputStream ba_is;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ba_is = new ByteArrayInputStream(data);
            return cf.generateCertificate(ba_is);
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        return null;
    }

    
    public void start() {
        
        try {
            initiate();
            //Input byte array must be the SHA-256 digest of the input data in binary format
            PTEID_ByteArray input_ba = getSignatureInput();
            
            if (input_ba != null) {

                //Change to false for Authentication
                boolean isSignature_key = true;
                
                //The following method call is equivalent to eidCard.Sign() with the same input: eidCard.SignSHA256
                PTEID_ByteArray signature = eidCard.Sign(input_ba, isSignature_key);

                System.out.println(String.format("Signature generated by CC card %s key: %s\nInput data: \"%s\"", 
                                   isSignature_key ? "signature" : "authentication", bytesToHex(signature.GetBytes()), dataToBeSigned));

                PTEID_ByteArray cert;

                if (isSignature_key)
                    cert = eidCard.getCert(PTEID_CertifType.PTEID_CERTIF_TYPE_SIGNATURE).getCertData();
                else 
                    cert = eidCard.getCert(PTEID_CertifType.PTEID_CERTIF_TYPE_AUTHENTICATION).getCertData();

                //Read matching certificate from card
                Certificate java_cert = loadCertificateFromDEREncoding(cert.GetBytes());
                if (java_cert != null) {

                    //Verify RSA-SHA256 signature using Java security classes
                    verifySignature(java_cert, signature.GetBytes());
                }
            }

        }
        catch (PTEID_ExNoReader ex) {
            System.out.println("No reader found.");
        } 
        catch (PTEID_ExNoCardPresent ex) {
            System.out.println("No card inserted.");
        } 
        catch (PTEID_Exception ex) {
            System.out.println("Caught exception in some SDK method. Error: " + ex.GetMessage());
        }
        catch (Exception ex) {
            System.out.println("Exception caught: " + ex.getMessage());
        }
        finally {
            release();
        }
    }

     /**
     * Releases the SDK (must always be done at the end of the program)
     */
    public void release() {

        try {
            PTEID_ReaderSet.releaseSDK();
        } catch (PTEID_Exception ex) {
            System.out.println("Caught exception in some SDK method. Error: " + ex.GetMessage());
        }
    }

    public static void main(String[] args) {
        
        new SignData().start();
    }
}
