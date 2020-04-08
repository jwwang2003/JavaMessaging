import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Random;

public class Login extends Application {
    Parent login;
    String[] bg = {"css/BG1.css", "css/BG2.css", "css/BG3.css", "css/BG4.css", "css/BG5.css", "css/BG6.css"};

    public void start(Stage primaryStage) throws Exception {
        try {
            Random random = new Random();
            int rand = random.nextInt(5);
            login = FXMLLoader.load(getClass().getClassLoader().getResource("Login.fxml"));
            Scene loginScene = new Scene(login);
            loginScene.getStylesheets().add(bg[rand]);
            primaryStage.initStyle(StageStyle.TRANSPARENT);
            primaryStage.setScene(loginScene);
            primaryStage.getIcons().add(new Image("icon.png"));
            primaryStage.show();
            loginScene.setFill(Color.TRANSPARENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void run() {
        launch();
    }
}
