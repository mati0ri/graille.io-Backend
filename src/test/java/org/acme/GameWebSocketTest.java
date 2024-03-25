package org.acme;

import jakarta.websocket.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

class GameWebSocketTest {

    private GameWebSocket gameWebSocket;
    private Session sessionMock;

    @BeforeEach
    void setUp() {
        gameWebSocket = new GameWebSocket();
        sessionMock = mock(Session.class);
        // Mock session ID if necessary
        when(sessionMock.getId()).thenReturn("session1");
    }

    @AfterEach
    void tearDown() {
        // Clean up resources, if necessary
    }

    @Test
    void testOnOpenAddsSessionAndInitialPosition() {
        gameWebSocket.onOpen(sessionMock);
        // Verify if the session was added and an initial position is set
        assertNotNull(GameWebSocket.positions.get("session1"));
        assertTrue(GameWebSocket.sessions.contains(sessionMock));
    }

    @Test
    void testOnMessageMovesPosition() throws Exception {
        // Simulate opening a session to initialize position
        gameWebSocket.onOpen(sessionMock);

        // Simulate receiving a message to move up
        String message = "{\"up\":true}";
        gameWebSocket.onMessage(message, sessionMock);

        // Capture the sent state after movement
        ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
        verify(sessionMock, atLeastOnce()).getAsyncRemote();
        // Verify the position has been updated correctly (assuming initial position and movement logic)
        GameWebSocket.Position position = GameWebSocket.positions.get("session1");
        assertTrue(position.getY() < 100); // Assuming initial Y is 100 and moving up decreases Y value
    }

    @Test
    void testOnCloseRemovesSessionAndPosition() {
        gameWebSocket.onOpen(sessionMock);
        gameWebSocket.onClose(sessionMock);

        // Verify if the session and its position are removed
        assertNull(GameWebSocket.positions.get("session1"));
        assertFalse(GameWebSocket.sessions.contains(sessionMock));
    }

    // Additional tests can be written to cover more scenarios like testing collision handling,
    // testing score increment on collision, testing square generation and movement, etc.
}
