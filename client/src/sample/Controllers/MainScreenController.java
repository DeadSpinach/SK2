package sample.Controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import sample.Data.Message;
import sample.Tasks.LogoutTask;
import sample.Tasks.ResponseListener;
import sample.Tasks.SendMessageTask;
import sample.Tasks.SubUnsubTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public class MainScreenController implements Initializable {

    private final Stage mainStage;
    private LoginScreenController loginController;
    private String serverAddress;
    private String nickname;

    ObservableList<String> topics = FXCollections.observableArrayList(
            "Linux", "Web Design", "World news", "Astronomy", "Books",
            "Music", "Photography", "Movies", "Minecraft", "Star Wars");

    private List<String> subscribedTopics = new ArrayList<>();

    @FXML
    private AnchorPane anchorPane;
    @FXML
    private Button quitButton;

    @FXML
    private Button sendButton;
    @FXML
    private TextField messageTitle;
    @FXML
    private TextArea messageContent;
    @FXML
    private ChoiceBox<String> messageTopic;
    @FXML
    private Text warningText;

    @FXML
    private Button linuxButton;
    @FXML
    private Button webdesignButton;
    @FXML
    private Button worldnewsButton;
    @FXML
    private Button astronomyButton;
    @FXML
    private Button booksButton;
    @FXML
    private Button musicButton;
    @FXML
    private Button photographyButton;
    @FXML
    private Button moviesButton;
    @FXML
    private Button minecraftButton;
    @FXML
    private Button starwarsButton;


    private ListView listView;
    private ObservableList<Message> messageObservableList;

    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private ResponseListener responseListener;

    public Socket getSocket() { return socket; }

    public void showStage() {
        mainStage.show();
    }

    public MainScreenController(LoginScreenController loginController)  {
        this.loginController = loginController;
        messageObservableList = FXCollections.observableArrayList();

        mainStage = new Stage();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main-screen.fxml"));
            loader.setController(this);
            mainStage.setScene(new Scene(loader.load()));
            mainStage.setTitle("Message Board");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        serverAddress = loginController.getAddress();
        nickname = loginController.getNickname();

        try {
            socket = new Socket(serverAddress, 2317);
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Problem z połączeniem z serwerem");
        }

        listView = new ListView();
        listView.setPrefSize(780,360);
        listView.setLayoutX(19);
        listView.setLayoutY(53);
        listView.getStyleClass().add("messagesListView");
        listView.getStylesheets().add(this.getClass().getResource("/listview-style.css").toExternalForm());
        listView.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                event.consume();
            }
        });
        anchorPane.getChildren().add(listView);
        listView.setItems(messageObservableList);
        listView.setCellFactory(messageListView -> new MessageListViewCell());

        messageTopic.setItems(topics);

        sendButton.setOnAction(event -> sendMessage());
        quitButton.setOnAction(event -> quit());
        linuxButton.setOnAction(event -> subOrUnsub(linuxButton));
        webdesignButton.setOnAction(event -> subOrUnsub(webdesignButton));
        worldnewsButton.setOnAction(event -> subOrUnsub(worldnewsButton));
        astronomyButton.setOnAction(event -> subOrUnsub(astronomyButton));
        booksButton.setOnAction(event -> subOrUnsub(booksButton));
        musicButton.setOnAction(event -> subOrUnsub(musicButton));
        photographyButton.setOnAction(event -> subOrUnsub(photographyButton));
        moviesButton.setOnAction(event -> subOrUnsub(moviesButton));
        minecraftButton.setOnAction(event -> subOrUnsub(minecraftButton));
        starwarsButton.setOnAction(event -> subOrUnsub(starwarsButton));

        mainStage.setOnCloseRequest(e -> {
            LogoutTask logoutTask = new LogoutTask(out);
            new Thread(logoutTask).start();
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            Platform.exit();
            System.exit(0);
        });

        // zaczynamy nasłuchiwanie odpowiedzi od serwera
        listenResponses();
    }

    private void sendMessage() {
        String title = messageTitle.getText();
        String content = messageContent.getText();
        String topic = messageTopic.getValue();
        if(!title.equals("") && !content.equals("") && topic != null) {
            if(title.length()<60 && content.length()<1000) {
                if(!content.contains("//divider@//")) {
                    SendMessageTask sendMessageTask = new SendMessageTask(title, content, topic, nickname, out);
                    new Thread(sendMessageTask).start();

                    warningText.setVisible(false);
                    messageTitle.setText("");
                    messageContent.setText("");
                    messageTopic.setValue(null);
                }
                else {
                    warningText.setText("Text cannot contain string '//divider@//'");
                    warningText.setVisible(true);
                }
            }
            else {
                warningText.setText("Max length of title: 60, Max length of content: 1000 characters");
                warningText.setVisible(true);
            }
        }
        else {
            warningText.setText("Fields cannot be empty");
            warningText.setVisible(true);
        }
    }

    private void subOrUnsub(Button topicButton) {
        String topic = topicButton.getText();

        if(subscribedTopics.contains(topic)) {
            SubUnsubTask subUnsubTask = new SubUnsubTask(topic, "UNSUBSCRIBE", out);
            new Thread(subUnsubTask).start();

            subscribedTopics.remove(topic);
            topicButton.setStyle("-fx-background-color:  #2821ff; -fx-background-radius: 10px;");
        }
        else {
            SubUnsubTask subUnsubTask = new SubUnsubTask(topic, "SUBSCRIBE", out);
            new Thread(subUnsubTask).start();

            subscribedTopics.add(topic);
            topicButton.setStyle("-fx-background-color:  #9e9e9e; -fx-background-radius: 10px;");
        }
    }

    private void quit() {
        LogoutTask logoutTask = new LogoutTask(out);
        new Thread(logoutTask).start();

        LoginScreenController logoutController = new LoginScreenController();
        logoutController.showStage();
        mainStage.close();
    }

    private void receiveMessage(String[] responseParts) {
        messageObservableList.add(0, new Message(responseParts[3], responseParts[1], responseParts[4], responseParts[2]));
        // po otrzymaniu wiadomości kontynuujemy nasłuchiwanie odpowiedzi od serwera
        listenResponses();
    }

    private void listenResponses() {
        responseListener = new ResponseListener(in);
        new Thread(responseListener).start();
        responseListener.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                String response = responseListener.getValue();
                if(response != null) {
                    String[] responseParts = response.split("//divider@//");

                    if (responseParts[0].equals("MESSAGE")) {
                    /*
                        responseParts[0] --> nagłówek 'MESSAGE'
                        responseParts[1] --> tytuł wiadomości
                        responseParts[2] --> treść wiadomości
                        responseParts[3] --> nazwa tematu
                        responseParts[4] --> autor
                    */
                        receiveMessage(responseParts);
                    }
                }
            }
        });
    }

}
