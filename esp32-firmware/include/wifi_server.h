#pragma once
#include <Arduino.h>
#include <WiFi.h>
#include <WebServer.h>
#include <WebSocketsServer.h>
#include "bridge_protocol.h"

typedef void (*FrameCallback)(const uint8_t* frame, size_t length, uint8_t clientNum);

class WifiServerManager {
public:
    static WifiServerManager& getInstance();
    void begin(FrameCallback onFrameReceived);
    void loop();
    bool broadcastFrame(const uint8_t* data, size_t length);
    bool sendFrameToClient(uint8_t clientNum, const uint8_t* data, size_t length);
    bool isClientConnected() const;
    uint8_t getClientCount() const;
    String getApSsid() const;
    String getApIp() const;
    String getApPassword() const;

private:
    WifiServerManager();
    WebServer m_httpServer;
    WebSocketsServer m_wsServer;
    WiFiServer m_tcpServer;
    WiFiClient m_tcpClient;
    FrameCallback m_frameCallback;
    uint8_t m_connectedClients;
    bool m_apRunning;

    void setupCaptivePortal();
    void handleWebSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length);
    static void wsEventWrapper(uint8_t num, WStype_t type, uint8_t * payload, size_t length);
};
