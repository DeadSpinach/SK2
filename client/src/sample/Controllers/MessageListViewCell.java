package sample.Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import sample.Data.Message;

import java.io.IOException;

public class MessageListViewCell extends ListCell<Message> {

    @FXML
    private Text topicText;
    @FXML
    private Text titleText;
    @FXML
    private Text authorText;
    @FXML
    private TextArea contentTextArea;
    @FXML
    private AnchorPane anchorPane;

    private FXMLLoader mLLoader;

    @Override
    protected void updateItem(Message message, boolean empty) {
        super.updateItem(message, empty);

        if(empty || message == null) {
            setText(null);
            setGraphic(null);
        } else {
            if (mLLoader == null) {
                mLLoader = new FXMLLoader(getClass().getResource("/message-list-cell.fxml"));
                mLLoader.setController(this);

                try {
                    mLLoader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            topicText.setText(message.getTopic());
            titleText.setText(message.getTitle());
            authorText.setText(message.getAuthor());
            contentTextArea.setText(message.getContent());

            setText(null);
            setGraphic(anchorPane);
        }
    }
}
