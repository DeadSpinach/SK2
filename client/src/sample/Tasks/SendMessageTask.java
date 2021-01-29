package sample.Tasks;

import javafx.concurrent.Task;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class SendMessageTask extends Task<Void> {

    private String title;
    private String content;
    private String topic;
    private String username;
    private OutputStream out;

    public SendMessageTask(String title, String content, String topic, String username, OutputStream out) {
        this.title = title;
        this.content = content;
        this.topic = topic;
        this.username = username;
        this.out = out;
    }

    @Override
    protected Void call() throws Exception {
        try {
            String request = "SENDMESSAGE//divider@//" + title + "//divider@//" + content + "//divider@//" + topic + "//divider@//" + username;
            int requestSize = request.length();
            if(requestSize>1000)
                request = requestSize + request;
            else if(requestSize>100)
                request = "0" + requestSize + request;
            else
                request = "00" + requestSize + request;

            out.write(request.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("Błąd podczas wysyłania wiadomości typu SENDMESSAGE do serwera.");
            //e.printStackTrace();
        }

        return null;
    }
}