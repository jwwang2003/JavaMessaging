import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextArea;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatController extends LoginController implements Initializable {

    double xOffset, yOffset;

    @FXML
    AnchorPane mainPanel, topBar, bodyPanel, close, minimize, exitPopup;

    @FXML
    JFXTextArea chatArea, msgArea;

    @FXML
    JFXListView<Label> clientList;

    @FXML
    JFXButton btnSend, btnStay, btnLeave;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mainPanel.setOpacity(0);
        animateIn();

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
                exitPopup.setVisible(true);
                Timeline timeline = new Timeline();
                KeyFrame key = new KeyFrame(Duration.millis(350),
                        new KeyValue(exitPopup.opacityProperty(), 1),
                        new KeyValue(bodyPanel.opacityProperty(), 0.2));
                timeline.getKeyFrames().add(key);
                timeline.setOnFinished((ae) -> {
                    bodyPanel.setDisable(true);
                });
                timeline.play();
            }
        });

        btnStay.setOnAction((actionEvent) -> {
            Timeline timeline = new Timeline();
            KeyFrame key = new KeyFrame(Duration.millis(200),
                    new KeyValue(exitPopup.opacityProperty(), 0),
                    new KeyValue(bodyPanel.opacityProperty(), 1));
            timeline.getKeyFrames().add(key);
            timeline.setOnFinished((ae) -> {exitPopup.setVisible(false);
            bodyPanel.setDisable(false);
            });
            timeline.play();
        });

        btnLeave.setOnAction((actionEvent) -> {
            Timeline timeline = new Timeline();
            KeyFrame key = new KeyFrame(Duration.millis(200),
                    new KeyValue(((Stage) btnLeave.getScene().getWindow()).opacityProperty(), 0));
            timeline.getKeyFrames().add(key);
            timeline.setOnFinished((ae) -> System.exit(2));
            timeline.play();
        });

        msgArea.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    msgArea.deletePreviousChar();
                    send();
                }
            }
        });

        btnSend.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Timeline timeline = new Timeline();
                KeyFrame key = new KeyFrame(Duration.millis(100),
                        new KeyValue(btnSend.opacityProperty(), 1));
                timeline.getKeyFrames().add(key);
                timeline.play();
            }
        });

        btnSend.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Timeline timeline = new Timeline();
                KeyFrame key = new KeyFrame(Duration.millis(100),
                        new KeyValue(btnSend.opacityProperty(), 0.5));
                timeline.getKeyFrames().add(key);
                timeline.play();
            }
        });

        btnSend.setOnAction((actionEvent) -> {
           send();
        });

        String clientSize = receiveMsg();
        String clientName;
        for (int i = 0; i < Integer.parseInt(clientSize); ++i) {
            clientName = receiveMsg();
            Label label = new Label();
            label.setTextFill(Color.BLACK);
            label.setText(clientName);
            clientList.getItems().add(label);
        }

        run();
    }

    public class Listener implements Runnable {

        private void printText(String value, int offset) {
            chatArea.appendText(value.substring(offset) + "\n");
        }

        @Override
        public void run() {
            while (true) {
                String input = receiveMsg();
                if (input.startsWith("MESSAGE")) {
                    printText(input, 8);
                } else if (input.startsWith("SYSTEM")) {
                    printText(input, 7);
                    if (input.contains("has joined")) {
                        int offset = input.indexOf("has joined");
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                clientList.getItems().add(new Label(input.substring(7, offset-1)));
                            }
                        });
                    } else if (input.contains("has left")) {
                        int offset = input.indexOf("has left");
                        for (int i = 0; i < clientList.getItems().size(); ++i) {
                            Label temp = clientList.getItems().get(i);
                            String temp1 = temp.getText();
                            System.out.println(temp1);
                            String temp2 = input.substring(7, offset-1);
                            System.out.println(temp2);
                            if(temp1.equals(temp2)) {
                                int finalI = i;
                                Platform.runLater(new Runnable(){
                                    @Override
                                    public void run() {
                                        clientList.getItems().remove(finalI);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
    }

    private void run() {
        // A new thread is created for the class Listener
        ExecutorService pool = Executors.newFixedThreadPool(1);
        pool.execute(new Listener());
    }

    private void send() {
        String temp = msgArea.getText();
        if (!temp.isEmpty()) sendMsg(temp);
        msgArea.setText("");
    }

    private void animateIn() {
        Timeline timeline = new Timeline();
        KeyFrame key = new KeyFrame(Duration.millis(200),
                new KeyValue(this.mainPanel.opacityProperty(), 1));
        timeline.getKeyFrames().add(key);
        timeline.play();
    }
}
