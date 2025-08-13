// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.message;

/**
 * Constants for Pixel Streaming signalling protocol message types.
 * These message types maintain compatibility with the existing TypeScript implementation.
 */
public final class MessageTypes {

    // Connection lifecycle messages
    public static final String CONFIG = "config";
    public static final String IDENTIFY = "identify";
    public static final String ID_CHANGED = "id_changed";
    public static final String DISCONNECT = "disconnect";
    public static final String PING = "ping";
    public static final String PONG = "pong";

    // WebRTC signalling messages
    public static final String OFFER = "offer";
    public static final String ANSWER = "answer";
    public static final String ICE_CANDIDATE = "iceCandidate";
    public static final String ICE_CANDIDATE_ERROR = "iceCandidateError";

    // Player management messages
    public static final String PLAYER_COUNT = "playerCount";
    public static final String PLAYER_CONNECTED = "playerConnected";
    public static final String PLAYER_DISCONNECTED = "playerDisconnected";

    // Streamer management messages
    public static final String STREAMER_ID_CHANGED = "streamerIdChanged";
    public static final String STREAMER_DATA_CHANNELS = "streamerDataChannels";
    public static final String STREAMER_DISCONNECTED = "streamerDisconnected";

    // SFU (Selective Forwarding Unit) messages
    public static final String SFU_RECV_DATA_CHANNEL_READY = "sfuRecvDataChannelReady";
    public static final String SFU_PEER_DATA_CHANNELS_READY = "sfuPeerDataChannelsReady";
    public static final String LAYER_PREFERENCE = "layerPreference";

    // Data channel messages
    public static final String DATA_CHANNEL_REQUEST = "dataChannelRequest";
    public static final String DATA_CHANNEL_OPEN = "dataChannelOpen";
    public static final String DATA_CHANNEL_CLOSE = "dataChannelClose";

    // Error messages
    public static final String ERROR = "error";
    public static final String WARNING = "warning";

    private MessageTypes() {
        // Utility class - prevent instantiation
    }
}