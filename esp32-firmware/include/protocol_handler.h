#pragma once
#include <Arduino.h>
#include "bridge_protocol.h"

class ProtocolHandler {
public:
    static ProtocolHandler& getInstance();
    void processFrame(const uint8_t* frame, size_t length, uint8_t sourceTransport, uint8_t clientNum = 0);
    size_t buildFrame(uint8_t cmdId, uint8_t seq, const uint8_t* payload, size_t payloadLen, uint8_t* outBuffer, size_t maxLen);
    void sendResponse(uint8_t cmdId, uint8_t seq, const uint8_t* payload, size_t payloadLen, uint8_t sourceTransport, uint8_t clientNum = 0);
    void sendGenericStatus(uint8_t statusCode, uint16_t detail, const char* msg, uint8_t seq, uint8_t sourceTransport, uint8_t clientNum = 0);
    void sendBridgeStatus(uint8_t seq, uint8_t sourceTransport, uint8_t clientNum = 0);
};
