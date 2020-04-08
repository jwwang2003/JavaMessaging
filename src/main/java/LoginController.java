import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTextField;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.security.*;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;


public class LoginController implements Initializable {

    double xOffset, yOffset;

    @FXML
    JFXTextField address, port, name;

    @FXML
    Text txtStatus, txtError;

    @FXML
    JFXButton btnConnect, btnLogin;

    @FXML
    JFXSpinner spinner;

    @FXML
    AnchorPane mainPanel, mainBG, middlePane, topBar, close, minimize;

    private Socket socket;
    private static ObjectInputStream ois;
    private static ObjectOutputStream oos;
    private static SecretKey sKey;
    private static IvParameterSpec ivSpec;

    public void initialize(URL location, ResourceBundle resources) {
        topBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });
        topBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                ((Stage) topBar.getScene().getWindow()).setX(event.getScreenX()-xOffset);
                ((Stage) topBar.getScene().getWindow()).setY(event.getScreenY()-yOffset);
            }
        });

        minimize.setOnMouseReleased(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                ((Stage) minimize.getScene().getWindow()).setIconified(true);
            }
        });

        close.setOnMouseReleased(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                Timeline timeline = new Timeline();
                KeyFrame key = new KeyFrame(Duration.millis(350),
                        new KeyValue(close.getScene().getRoot().opacityProperty(), 0));
                timeline.getKeyFrames().add(key);
                timeline.setOnFinished((ae) -> System.exit(2));
                timeline.play();
            }
        });

        btnConnect.setOnAction((actionEvent) -> {
            handleConnect();
        });

        btnLogin.setOnAction((actionEvent) -> {
           handleName();
        });
    }

    private void handleConnect() {
        String address = this.address.getText(), port = this.port.getText();

        if (address.isEmpty() || port.isEmpty()) {
            if (address.isEmpty()) {
                this.address.clear();
                this.address.setUnFocusColor(Color.ORANGE);
            }
            if (port.isEmpty()) {
                this.port.clear();
                this.port.setUnFocusColor(Color.ORANGE);
            }
            this.txtError.setText("Missing information!");
            return;
        }

        resetLogin();

        String addressPattern = "\\A(\\d+)[.](\\d+)[.](\\d+)[.](\\d+)\\z";
        String portPattern = "(\\d){1,5}";

        Pattern p1 = Pattern.compile(addressPattern);
        Pattern p2 = Pattern.compile(portPattern);

        Matcher m1 = p1.matcher(address);
        Matcher m2 = p2.matcher(port);

        if (!m1.matches() || !m2.matches()) {
            if (!m1.matches() && !m2.matches()) {
                this.address.setUnFocusColor(Color.RED);
                this.port.setUnFocusColor(Color.RED);
                this.txtError.setText("Check IP & port format!");
            } else if (!m1.matches()) {
                this.address.setUnFocusColor(Color.RED);
                this.txtError.setText("Check IP format!");
            } else if (!m2.matches()) {
                this.port.setUnFocusColor(Color.RED);
                this.txtError.setText("Check port format!");
            }
            return;
        }

        resetLogin();

        ExecutorService pool = Executors.newFixedThreadPool(1);
        pool.execute(new FirstTouch(address,port));
    }

    public class FirstTouch implements Runnable {
        private String address, port;

        public FirstTouch (String a, String p) {
            address = a;
            port = p;
        }

        @Override
        public void run() {
            try {
                socket = new Socket(address, Integer.parseInt(port));
                loading(1);
                txtStatus.setText("Connected to Server");
                oos = new ObjectOutputStream(socket.getOutputStream());
                txtStatus.setText("OutputStream initialized");
                ois = new ObjectInputStream(socket.getInputStream());
                txtStatus.setText("InputStream initialized");

                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                txtStatus.setText("RSA key generated");
                SecureRandom random = SecureRandom.getInstanceStrong();

                keyPairGenerator.initialize(2048, random);
                KeyPair keyPair = keyPairGenerator.generateKeyPair();
                PublicKey publicKey = keyPair.getPublic();
                PrivateKey privateKey = keyPair.getPrivate();

                String key = Base64.getEncoder().encodeToString(publicKey.getEncoded());

                oos.writeObject(key);
                oos.flush();
                txtStatus.setText("Key sent to server");

                byte[] output = (byte[]) ois.readObject();
                byte[] iv = (byte[]) ois.readObject();
                byte[] encoded = decryptRSA(output, privateKey);
                sKey = new SecretKeySpec(encoded, "AES");
                ivSpec = new IvParameterSpec(iv);
                txtStatus.setText("Received AES key");

                if(receiveMsg().equals("SUBMITNAME")) {
                    txtStatus.setText("Connection Successful");
                    Thread.sleep(750);
                    loading(0);
                    transitionName();
                } else {
                    txtStatus.setText("Problem");
                    loading(0);
                    return;
                }

            } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleName() {
        String userName = name.getText();
        if (userName.isEmpty()) {
            txtError.setText("Name field empty!");
            name.setUnFocusColor(Color.ORANGE);
            return;
        }

        txtError.setText("");
        name.setUnFocusColor(Color.BLACK);

        sendMsg(userName);
        String receive = receiveMsg();
        if (receive.equals("NAMEACCEPTED")) {
            txtError.setText("");
            name.setUnFocusColor(Color.BLACK);
        } else if (receive.equals("SUBMITNAME")) {
            txtError.setText("Name taken!");
            name.setUnFocusColor(Color.RED);
            return;
        }
        Platform.runLater(() -> {
            loadChat();
        });
    }

    private void resetLogin() {
        this.address.setUnFocusColor(Color.BLACK);
        this.port.setUnFocusColor(Color.BLACK);
        this.txtError.setText("");
    }

    private void loading (int mode) {
        if (mode == 0) {
            Timeline timeline = new Timeline();
            KeyFrame key = new KeyFrame(Duration.millis(200),
                    new KeyValue (address.opacityProperty(), 1),
                    new KeyValue (port.opacityProperty(), 1),
                    new KeyValue (txtError.opacityProperty(), 1),
                    new KeyValue (btnConnect.opacityProperty(), 1));
            timeline.getKeyFrames().add(key);
            timeline.play();
            txtStatus.setVisible(false);
            spinner.setVisible(false);
        } else if (mode == 1) {
            Timeline timeline = new Timeline();
            KeyFrame key = new KeyFrame(Duration.millis(200),
                    new KeyValue (address.opacityProperty(), 0),
                    new KeyValue (port.opacityProperty(), 0),
                    new KeyValue (txtError.opacityProperty(), 0),
                    new KeyValue (btnConnect.opacityProperty(), 0));
            timeline.getKeyFrames().add(key);
            timeline.play();
            txtStatus.setVisible(true);
            spinner.setVisible(true);
        }
    }

    private void transitionName() {
        this.address.setVisible(false);
        this.address.setDisable(true);
        this.port.setVisible(false);
        this.port.setDisable(true);
        this.btnConnect.setVisible(false);
        this.btnConnect.setDisable(true);
        this.name.setVisible(true);
        this.btnLogin.setVisible(true);

        Timeline timeline = new Timeline();
        KeyFrame key = new KeyFrame(Duration.millis(200),
                new KeyValue (this.btnLogin.opacityProperty(), 1),
                new KeyValue (this.name.opacityProperty(), 1)
                );
        timeline.getKeyFrames().add(key);
        timeline.play();
    }

    private void loadChat() {
        Timeline timeline = new Timeline();
        KeyFrame key = new KeyFrame(Duration.millis(200),
                new KeyValue (this.mainPanel.opacityProperty(), 0));
        timeline.getKeyFrames().add(key);
        timeline.setOnFinished((ae) ->
                openChat());
        timeline.play();
    }

    private void openChat() {
        try {
            ((Stage) mainPanel.getScene().getWindow()).close();
            Parent chat = FXMLLoader.load(getClass().getClassLoader().getResource("Chat.fxml"));
            Scene chatScene = new Scene(chat);
            Stage chatStage = new Stage();
            chatStage.initStyle(StageStyle.TRANSPARENT);
            chatStage.setScene(chatScene);
            chatStage.getIcons().add(new Image("icon.png"));
            chatStage.show();
            chatScene.setFill(Color.TRANSPARENT);
            ((Stage) mainPanel.getScene().getWindow()).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg (String data) {
        try {
            oos.writeObject(encryptAES(data, sKey, ivSpec));
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String receiveMsg() {
        String temp = null;
        try {
            temp = decryptAES((byte[])ois.readObject(),sKey, ivSpec);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            return temp;
        }
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
    // RSA ----------------------------------------------------------------------------
}
