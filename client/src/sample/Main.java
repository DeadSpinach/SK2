package sample;

import javafx.application.Application;
import javafx.stage.Stage;
import sample.Controllers.LoginScreenController;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        LoginScreenController loginController = new LoginScreenController();
        loginController.showStage();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
