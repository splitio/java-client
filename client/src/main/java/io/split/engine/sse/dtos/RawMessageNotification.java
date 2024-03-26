package io.split.engine.sse.dtos;

public class RawMessageNotification {
    private String id;
    private String clientId;
    private long timestamp;
    private String encoding;
    private String channel;
    private String data;

    public RawMessageNotification(String id,
                                  String clientId,
                                  long timestamp,
                                  String encoding,
                                  String channel,
                                  String data) {
        this.id = id;
        this.clientId = clientId;
        this.timestamp = timestamp;
        this.encoding = encoding;
        this.channel = channel;
        this.data = data;
    }

    public String getChannel() {
        return channel;
    }

    public String getData() { return data; }
}
