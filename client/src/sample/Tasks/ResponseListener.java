package sample.Tasks;

import javafx.concurrent.Task;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ResponseListener extends Task<String> {

    private InputStream in;

    public ResponseListener(InputStream in) {
        this.in = in;
    }

    @Override
    protected String call() throws Exception {
        // Najpierw odczytujemy długość wiadomości
        byte[] responseLength = new byte[4];
        StringBuilder responseLengthString = new StringBuilder();
        int currentCount = 0;
        int len = 0;
        while((currentCount = in.read(responseLength, 0, 4)) > 0) {
            len = len + currentCount;
            responseLengthString.append(new String(responseLength,0,currentCount, StandardCharsets.US_ASCII));
            if (len == 4) break;
        }
        if(currentCount < 0) {
            System.out.println("Błąd podczas odczytu wiadomości od serwera.");
            return null;
        }

        Integer length = Integer.valueOf(new String(responseLengthString)); // ilość bajtów do odczytania

        // Teraz odczytujemy wiadomość o danej długości
        byte[] response = new byte[length];
        StringBuilder responseString = new StringBuilder();
        currentCount = 0;
        int totalCount = 0;
        while((currentCount = in.read(response, 0, length)) > 0) {
            totalCount = totalCount + currentCount;
            responseString.append(new String(response, 0, currentCount, StandardCharsets.US_ASCII));
            if(totalCount == length) break;
        }
        if(currentCount < 0) {
            System.out.println("Błąd podczas odczytu wiadomości od serwera.");
            return null;
        }

        return new String(responseString);
    }
}
