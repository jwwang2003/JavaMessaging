import java.io.*;

import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.util.*;

import java.net.ServerSocket;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Server {

    private static Set<String> names = new HashSet<>();
    private static List<Writer> writers = new ArrayList<Writer>();
    private static final int port = 59001;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static void main(String args[]) throws GeneralSecurityException {
        System.out.println("Server running...");

        // The server is threaded so that the application can
        // "serve" multiple users at the same time
        // Theoretically, 500 users can connect at once
        ExecutorService pool = Executors.newFixedThreadPool(500);

        // Open ServerSocket on "port"
        // Whenever a connection, most likely from the
        // client is established, a new thread is
        // created dedicated to that client
        try(ServerSocket listener = new ServerSocket(port))    {
            System.out.println("Listening on port: " + listener.getLocalPort());
            while(true) {
                pool.execute(new Handler(listener.accept()));
            }
        } catch (Exception e)   {
            // Usually if another server is running using
            // the same port an error would occur
            System.out.println(ANSI_RED+"SETUP FAILURE"+ANSI_WHITE);
            // Print error message
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static class Handler implements Runnable {
        // Encryption stuff
        private SecureRandom srandom = new SecureRandom();
        private PublicKey publicKey;
        private SecretKey sKey;
        private IvParameterSpec ivspec;
        private String verifyE = "JIMMY";
        private Writer w;

        // Managing socket input output...
        private ObjectInputStream ois;
        private ObjectOutputStream oos;
        private Socket socket;

        private String name;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                System.out.println(Thread.currentThread().getName());
                /**
                 Brief description on how the encryption works
                 - First of all, a client makes connection with the socket
                 and then, a new thread is created for that client
                 - Client sends over the RSA public key
                 - Why public key?
                 - Once encrypted by the public key, it's not possible
                 to decrypt it with the public key, one must use a
                 private key to get any useful information out of it
                 - So if there was a middle man for got both the public
                 key and the encrypted info, he/she can't do anything with it
                 - Server side generates a AES key and an IV which gets sent
                 back to the client
                 - NOTE: the AES key is encrypted by RSA public key but
                 the IV is not
                 - Client takes the encrypted AES key and decrypts it using its
                 private key and now both the server and client share the same
                 AES key and we can start encrypting and decrypting messages
                 using it
                 */
                // Receiving PUBLIC RSA key from Client
                ois = new ObjectInputStream(socket.getInputStream());
                oos = new ObjectOutputStream(socket.getOutputStream());
                String output = (String) ois.readObject();
                publicKey = getPublicKey(output);
                System.out.println(publicKey.toString());
                // Public RSA key is used to encrypt the AES256 key that would be
                // generated below, then sent back to the client to be decrypted
                // using the private RSA key

                // Generating a AES256 key
                KeyGenerator kGen = KeyGenerator.getInstance("AES");
                kGen.init(256);
                sKey = kGen.generateKey();
                byte[] iv = new byte[128/8];
                srandom.nextBytes(iv);
                ivspec = new IvParameterSpec(iv);

                // Encrypting AES key with the public RSA key
                byte[] encrypted = encryptRSA(sKey.getEncoded(), publicKey);

                // Then send both the encrypted AES (first) then the
                // generated IV (second) to the client
                oos.writeObject(encrypted);
                oos.writeObject(iv);
                oos.flush();

                while (true) {
                    sendMsg("SUBMITNAME");
                    name = receiveMsg();
                    System.out.println(name);
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!name.isEmpty() && !names.contains(name)) {
                            names.add(name);
                            break;
                        }
                    }
                }
                sendMsg("NAMEACCEPTED " + name);
                sendMsg(Integer.toString(names.size()));
                for (String name:names) {
                    sendMsg(name);
                }
                for (Writer writer: writers)   {
                    sendMsg("SYSTEM " + name + " has joined", writer.oos, writer.sKey, writer.ivspec);
                }
                w = new Writer(oos, sKey, ivspec);
                writers.add(w);

                while (true) {
                    String input = receiveMsg();
                    if (input.toLowerCase().startsWith("/quit")) {
                        return;
                    }
                    for (Writer writer: writers)   {
                        sendMsg("MESSAGE " + name + ": " + input, writer.oos, writer.sKey, writer.ivspec);
                        System.out.println("MESSAGE " + name + ": " + input);
                    }
                }

            } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            } finally {
                System.out.println("CHECKPOINT2");
                synchronized (names) {
                    names.remove(name);
                    writers.remove(w);
                    for (Writer writer: writers)   {
                        sendMsg("SYSTEM " + name + " has left", writer.oos, writer.sKey, writer.ivspec);
                        //System.out.println("MESSAGE " + name + ": " + input);
                    }
                }
                Thread.currentThread().interrupt();
            }
        }

        public void sendMsg(String data) {
            try {
                oos.writeObject(encryptAES(data, sKey, ivspec));
                oos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMsg(String data, ObjectOutputStream out, SecretKey sKey, IvParameterSpec ivspec) {
            try {
                out.writeObject(encryptAES(data, sKey, ivspec));
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String receiveMsg() throws IOException, ClassNotFoundException {
            return decryptAES((byte[])ois.readObject(),sKey, ivspec);
        }

        // AES ----------------------------------------------------------------------------
        public static byte[] encryptAES(String value, SecretKey sKey, IvParameterSpec iv) {
            byte[] encrypted = new byte[0];
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                cipher.init(Cipher.ENCRYPT_MODE, sKey, iv);
                encrypted = cipher.doFinal(value.getBytes(UTF_8));

            } catch (NoSuchPaddingException | NoSuchAlgorithmException |
                    InvalidAlgorithmParameterException | InvalidKeyException
                    | BadPaddingException | IllegalBlockSizeException e) {
                e.printStackTrace();
            }
            finally {
                return encrypted;
            }
        }

        public static String decryptAES(byte[] encrypted, SecretKey sKey, IvParameterSpec iv) {
            byte[] decrypted = new byte[0];
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                cipher.init(Cipher.DECRYPT_MODE, sKey, iv);
                decrypted = cipher.doFinal(encrypted);
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException |
                    IllegalBlockSizeException | InvalidAlgorithmParameterException | InvalidKeyException e) {
                e.printStackTrace();
            }
            finally {
                return new String(decrypted);
            }
        }
        // AES ----------------------------------------------------------------------------
        // RSA ----------------------------------------------------------------------------
        public static byte[] encryptRSA(byte[] data, PublicKey publicKey) {
            byte[] encrypt = new byte[0];
            try {
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                encrypt = cipher.doFinal(data);
            } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException |
                    NoSuchPaddingException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            finally {
                return encrypt;
            }
        }

        public static byte[] decryptRSA(byte[] data, PrivateKey privateKey) {
            byte[] decrypted = new byte[0];
            try {
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, privateKey);
                decrypted = cipher.doFinal(data);
            } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            finally {
                return decrypted;
            }
        }

        public static PublicKey getPublicKey(String base64PublicKey){
            PublicKey publicKey = null;
            try{
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKey.getBytes()));
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                publicKey = keyFactory.generatePublic(keySpec);
                return publicKey;
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
            }
            return publicKey;
        }
        // RSA ----------------------------------------------------------------------------

    }

    public static class Writer {
        private ObjectOutputStream oos;
        private SecretKey sKey;
        private IvParameterSpec ivspec;
        public Writer (ObjectOutputStream oos, SecretKey sKey, IvParameterSpec ivspec) {
            this.oos = oos;
            this.sKey = sKey;
            this.ivspec = ivspec;
        }
    }
}