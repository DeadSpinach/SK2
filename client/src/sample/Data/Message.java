package sample.Data;

public class Message {
    private String topic;
    private String title;
    private String author;
    private String content;

    public Message(String topic, String title, String author, String content) {
        this.topic = topic;
        this.title = title;
        this.author = author;
        this.content = content;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}