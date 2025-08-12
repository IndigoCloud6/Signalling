// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageHelperTest {

    private MessageHelper messageHelper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        messageHelper = new MessageHelper(objectMapper);
    }

    @Test
    void testParseValidMessage() {
        String json = "{\"type\":\"config\",\"id\":\"test-id\",\"data\":{\"test\":\"value\"}}";
        
        BaseMessage message = messageHelper.parseMessage(json);
        
        assertNotNull(message);
        assertEquals("config", message.getType());
        assertEquals("test-id", message.getId());
        assertNotNull(message.getData());
    }

    @Test
    void testParseMessageWithoutId() {
        String json = "{\"type\":\"ping\"}";
        
        BaseMessage message = messageHelper.parseMessage(json);
        
        assertNotNull(message);
        assertEquals("ping", message.getType());
        assertNull(message.getId());
        assertNull(message.getData());
    }

    @Test
    void testParseInvalidMessage() {
        String json = "{\"invalid\":\"json\"}";
        
        BaseMessage message = messageHelper.parseMessage(json);
        
        assertNull(message);
    }

    @Test
    void testParseMalformedJson() {
        String json = "{invalid json";
        
        BaseMessage message = messageHelper.parseMessage(json);
        
        assertNull(message);
    }

    @Test
    void testSerializeMessage() throws Exception {
        JsonNode data = objectMapper.valueToTree("test-data");
        BaseMessage message = new TestMessage("test-type", "test-id", data);
        
        String json = messageHelper.serializeMessage(message);
        
        assertNotNull(json);
        assertTrue(json.contains("test-type"));
        assertTrue(json.contains("test-id"));
    }

    @Test
    void testCreateConfigMessage() throws Exception {
        JsonNode peerOptions = objectMapper.valueToTree("{\"iceServers\":[]}");
        
        BaseMessage message = messageHelper.createConfigMessage(peerOptions);
        
        assertNotNull(message);
        assertEquals(MessageTypes.CONFIG, message.getType());
        assertNotNull(message.getData());
    }

    @Test
    void testCreatePlayerCountMessage() {
        BaseMessage message = messageHelper.createPlayerCountMessage(5);
        
        assertNotNull(message);
        assertEquals(MessageTypes.PLAYER_COUNT, message.getType());
        assertNotNull(message.getData());
    }

    @Test
    void testCreateErrorMessage() {
        BaseMessage message = messageHelper.createErrorMessage("Test error");
        
        assertNotNull(message);
        assertEquals(MessageTypes.ERROR, message.getType());
        assertNotNull(message.getData());
    }

    @Test
    void testCreatePingMessage() {
        BaseMessage message = messageHelper.createPingMessage();
        
        assertNotNull(message);
        assertEquals(MessageTypes.PING, message.getType());
    }

    @Test
    void testCreatePongMessage() {
        BaseMessage message = messageHelper.createPongMessage();
        
        assertNotNull(message);
        assertEquals(MessageTypes.PONG, message.getType());
    }

    // Test message implementation
    private static class TestMessage extends BaseMessage {
        public TestMessage(String type, String id, JsonNode data) {
            super(type, id, data);
        }
    }
}