import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ChatLauncher extends Application {
    Parent chat;

    public void start(Stage primaryStage) throws Exception {
        try {
            chat = FXMLLoader.load(getClass().getClassLoader().getResource("Chat.fxml"));
            Scene chatScene = new Scene(chat);
            chatScene.getStylesheets().add("css/chat.css");
            primaryStage.initStyle(StageStyle.TRANSPARENT);
            primaryStage.setScene(chatScene);
            primaryStage.show();
            chatScene.setFill(Color.TRANSPARENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main (String args[]) {
        launch();
    }
}
