package com.eborovik.streamer.models;

public class LiveVideoModel {

    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    private String url;
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    private String streamId;
    public String getStreamId() { return streamId; }
    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }
}
