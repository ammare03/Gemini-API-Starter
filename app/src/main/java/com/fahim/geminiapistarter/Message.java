package com.fahim.geminiapistarter;

public class Message {
    private final String content;
    private final boolean isUser; // true if from the user, false if from the AI

    public Message(String content, boolean isUser) {
        this.content = content;
        this.isUser = isUser;
    }

    public String getContent() {
        return content;
    }

    public boolean isUser() {
        return isUser;
    }
}