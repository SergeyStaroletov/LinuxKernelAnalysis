package ru.altstu;

public interface IMsgMatcher {
    String closestMessage(String newMsg);
    void addNewMsg(String newMsg);
    public void buildMsgDistances() throws InterruptedException;

    }
