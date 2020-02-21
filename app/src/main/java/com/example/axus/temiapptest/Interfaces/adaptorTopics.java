package com.example.axus.temiapptest.Interfaces;

public interface adaptorTopics {
    public void generateRobotStatusTopic();
    public void generateRobotTaskGotoRequestTopic(int level, String destinationName);
}
