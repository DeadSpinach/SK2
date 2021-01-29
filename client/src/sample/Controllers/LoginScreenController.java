package sample.Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginScreenController implements Initializable {

    private final Stage loginStage;
    private String nickname;
    private String address;

    @FXML
    private Button loginButton;
    @FXML
    private TextField nicknameTextField;
    @FXML
    private TextField serverTextField;

    public String getNickname() {
        return nickname;
    }
    public String getAddress() { return address; }

    public LoginScreenController() {
        loginStage = new Stage();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login-screen.fxml"));
            loader.setController(this);
            loginStage.setScene(new Scene(loader.load()));
            loginStage.setTitle("Message Board");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loginButton.setOnAction(event -> goToMainScreen());
    }

    public void showStage() {
        loginStage.show();
    }

    private void goToMainScreen() {
        if(!nicknameTextField.getText().equals("") && nicknameTextField.getText().length() < 20) {
            nickname = nicknameTextField.getText();
            address = serverTextField.getText();
            if(address.equals(""))
                address = "localhost";

            MainScreenController mainController = new MainScreenController(this);
            mainController.showStage();
            loginStage.close();
        }
    }

}
