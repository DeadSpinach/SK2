package sample.Tasks;

import javafx.concurrent.Task;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class SubUnsubTask extends Task<Void> {
    private String command;
    private String topicName;
    private OutputStream out;

    public SubUnsubTask(String topicName, String command, OutputStream out) {
        this.topicName = topicName;
        this.command = command;
        this.out = out;
    }

    @Override
    protected Void call() throws Exception {
        try {
            String request = command + "//divider@//" + topicName;
            int requestSize = request.length();
            if(requestSize>1000)
                request = requestSize + request;
            else if(requestSize>100)
                request = "0" + requestSize + request;
            else
                request = "00" + requestSize + request;

            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            System.out.println("Błąd podczas wysyłania wiadomości typu SUBSCRIBE/UNSUBSCRIBE do serwera.");
            //e.printStackTrace();
        }

        return null;
    }
}
