import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import java.util.Arrays;
import pt.gov.cartaodecidadao.*;
import java.util.Scanner;

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
    PTEID_CardType cardType = null;
    PTEID_CardContactInterface contactInterface = null;


    /**
     * Initializes the SDK and sets main variables
     * @throws PTEID_Exception when there is some error with the SDK methods
     */
    public void initiate() throws PTEID_Exception {
       
        //Must always be called in the beginning of the program
        PTEID_ReaderSet.initSDK();

        //Sets test mode to true so that CC2 can be tested
        PTEID_Config.SetTestMode(true);

        //Gets the set of connected readers
        readerSet = PTEID_ReaderSet.instance();

        //Gets the first reader
        //When multiple readers are connected, you can iterate through the various reader objects with the methods getReaderName and getReaderByName or getReaderByNum
        //Any reader can be checked for an inserted card with PTEID_ReaderContext.isCardPresent()
        readerContext = readerSet.getReader();

        //Gets the Card Contact Interface and type
        if(readerContext.isCardPresent()){
            contactInterface = readerContext.getCardContactInterface();
            cardType = readerContext.getCardType();
            System.out.println("Contact Interface:" + (contactInterface == PTEID_CardContactInterface.PTEID_CARD_CONTACTLESS ? "CONTACTLESS" : "CONTACT"));
        }

        //Gets the card instance
        eidCard = readerContext.getEIDCard();

        //If the contactInterface is contactless and the card supports contactless then authenticate with PACE
        if (contactInterface == PTEID_CardContactInterface.PTEID_CARD_CONTACTLESS && cardType ==  PTEID_CardType.PTEID_CARDTYPE_IAS5){
            Scanner in = new Scanner(System.in);
            System.out.print("Insert the CAN for this EIDCard: ");
            String can_str = in.nextLine();
            eidCard.initPaceAuthentication(can_str, can_str.length(),  PTEID_CardPaceSecretType.PTEID_CARD_SECRET_CAN);
        }
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
