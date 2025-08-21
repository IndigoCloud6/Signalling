package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;

/**
 * Base class for all signalling messages.
 * Corresponds to the BaseMessage interface in the TypeScript implementation.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ConfigMessage.class, name = "config"),
    @JsonSubTypes.Type(value = SubscribeMessage.class, name = "subscribe"),
    @JsonSubTypes.Type(value = UnsubscribeMessage.class, name = "unsubscribe"),
    @JsonSubTypes.Type(value = PlayerConnectedMessage.class, name = "playerConnected"),
    @JsonSubTypes.Type(value = PlayerDisconnectedMessage.class, name = "playerDisconnected"),
    @JsonSubTypes.Type(value = StreamerListMessage.class, name = "streamerList"),
    @JsonSubTypes.Type(value = ListStreamersMessage.class, name = "listStreamers"),
    @JsonSubTypes.Type(value = SubscribeFailedMessage.class, name = "subscribeFailed"),
    @JsonSubTypes.Type(value = StreamerIdChangedMessage.class, name = "streamerIdChanged"),
    @JsonSubTypes.Type(value = StreamerDisconnectedMessage.class, name = "streamerDisconnected"),
    @JsonSubTypes.Type(value = PingMessage.class, name = "ping"),
    @JsonSubTypes.Type(value = PongMessage.class, name = "pong"),
    @JsonSubTypes.Type(value = OfferMessage.class, name = "offer"),
    @JsonSubTypes.Type(value = AnswerMessage.class, name = "answer"),
    @JsonSubTypes.Type(value = IceCandidateMessage.class, name = "iceCandidate"),
    @JsonSubTypes.Type(value = DataChannelRequestMessage.class, name = "dataChannelRequest"),
    @JsonSubTypes.Type(value = PeerDataChannelsReadyMessage.class, name = "peerDataChannelsReady"),
    @JsonSubTypes.Type(value = LayerPreferenceMessage.class, name = "layerPreference"),
    @JsonSubTypes.Type(value = EndpointIdMessage.class, name = "endpointId"),
    @JsonSubTypes.Type(value = DisconnectPlayerMessage.class, name = "disconnectPlayer"),
    @JsonSubTypes.Type(value = StartStreamingMessage.class, name = "startStreaming"),
    @JsonSubTypes.Type(value = StopStreamingMessage.class, name = "stopStreaming"),
    @JsonSubTypes.Type(value = StreamerDataChannelsMessage.class, name = "streamerDataChannels"),
    @JsonSubTypes.Type(value = PeerDataChannelsMessage.class, name = "peerDataChannels")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseMessage {
    
    @JsonProperty("type")
    private final String type;
    
    @JsonProperty("playerId")
    private String playerId;
    
    @JsonProperty("streamerId")
    private String streamerId;
    
    protected BaseMessage(String type) {
        this.type = Objects.requireNonNull(type, "Message type cannot be null");
    }
    
    public String getType() {
        return type;
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public String getStreamerId() {
        return streamerId;
    }
    
    public void setStreamerId(String streamerId) {
        this.streamerId = streamerId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseMessage that = (BaseMessage) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(playerId, that.playerId) &&
                Objects.equals(streamerId, that.streamerId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, playerId, streamerId);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "type='" + type + '\'' +
                ", playerId='" + playerId + '\'' +
                ", streamerId='" + streamerId + '\'' +
                '}';
    }
}