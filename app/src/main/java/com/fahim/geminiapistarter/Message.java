package com.fahim.geminiapistarter;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class Message {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private final String content;
    private final boolean isUser;

    public Message(String content, boolean isUser) {
        this.content = content;
        this.isUser = isUser;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }
    public boolean isUser() {
        return isUser;
    }
}