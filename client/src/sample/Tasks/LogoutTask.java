package sample.Tasks;

import javafx.concurrent.Task;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


public class LogoutTask extends Task<Void> {
    private OutputStream out;

    public LogoutTask(OutputStream out) {
        this.out = out;
    }

    @Override
    protected Void call() throws Exception {
        try {
            String request = "LOGOUT";
            int requestSize = request.length();
            request = "000" + requestSize + request;
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            System.out.println("Błąd podczas wylogowywania.");
            //e.printStackTrace();
        }

        return null;
    }
}
