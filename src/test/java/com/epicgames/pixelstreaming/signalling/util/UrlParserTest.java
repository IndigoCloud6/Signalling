package com.epicgames.pixelstreaming.signalling.util;

import com.epicgames.pixelstreaming.signalling.model.ConnectionAttributes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlParserTest {

    @Test
    void testParseStreamerConnection() {
        String path = "/type=streamer";
        ConnectionAttributes attributes = UrlParser.parseConnectionAttributes(path);
        
        assertEquals(ConnectionAttributes.ConnectionType.STREAMER, attributes.getType());
        assertNull(attributes.getInstanceId());
        assertNull(attributes.getProjectId());
    }

    @Test
    void testParsePlayerConnection() {
        String path = "/type=player";
        ConnectionAttributes attributes = UrlParser.parseConnectionAttributes(path);
        
        assertEquals(ConnectionAttributes.ConnectionType.PLAYER, attributes.getType());
        assertNull(attributes.getInstanceId());
        assertNull(attributes.getProjectId());
    }

    @Test
    void testParseComplexStreamerConnection() {
        String path = "/type=streamer&insid=675ba9d5b1796fc2539d3930&projectid=6821C61B48A925890040C3B3849B06C3";
        ConnectionAttributes attributes = UrlParser.parseConnectionAttributes(path);
        
        assertEquals(ConnectionAttributes.ConnectionType.STREAMER, attributes.getType());
        assertEquals("675ba9d5b1796fc2539d3930", attributes.getInstanceId());
        assertEquals("6821C61B48A925890040C3B3849B06C3", attributes.getProjectId());
    }

    @Test
    void testParseWithAdditionalAttributes() {
        String path = "/type=player&insid=123&projectid=456&custom=value&other=test";
        ConnectionAttributes attributes = UrlParser.parseConnectionAttributes(path);
        
        assertEquals(ConnectionAttributes.ConnectionType.PLAYER, attributes.getType());
        assertEquals("123", attributes.getInstanceId());
        assertEquals("456", attributes.getProjectId());
        assertEquals("value", attributes.getAttribute("custom"));
        assertEquals("test", attributes.getAttribute("other"));
    }

    @Test
    void testParseWithoutLeadingSlash() {
        String path = "type=sfu&insid=789";
        ConnectionAttributes attributes = UrlParser.parseConnectionAttributes(path);
        
        assertEquals(ConnectionAttributes.ConnectionType.SFU, attributes.getType());
        assertEquals("789", attributes.getInstanceId());
    }

    @Test
    void testParseInvalidType() {
        String path = "/type=invalid";
        assertThrows(IllegalArgumentException.class, () -> {
            UrlParser.parseConnectionAttributes(path);
        });
    }

    @Test
    void testParseMissingType() {
        String path = "/insid=123&projectid=456";
        assertThrows(IllegalArgumentException.class, () -> {
            UrlParser.parseConnectionAttributes(path);
        });
    }

    @Test
    void testParseEmptyPath() {
        assertThrows(IllegalArgumentException.class, () -> {
            UrlParser.parseConnectionAttributes("");
        });
    }

    @Test
    void testParseNullPath() {
        assertThrows(IllegalArgumentException.class, () -> {
            UrlParser.parseConnectionAttributes(null);
        });
    }
}