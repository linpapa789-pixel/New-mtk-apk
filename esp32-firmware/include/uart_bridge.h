#pragma once
#include <Arduino.h>
#include "bridge_protocol.h"

class UartBridge {
public:
    static UartBridge& getInstance();
    void begin(uint8_t txPin, uint8_t rxPin, uint32_t defaultBaud = 115200);
    bool startBridge(const uart_config_payload_t& config);
    void stopBridge();
    bool isBridgeActive() const;
    size_t writeToTarget(const uint8_t* data, size_t length);
    size_t readFromTarget(uint8_t* buffer, size_t maxLen);
    void loop(void (*onDataReceived)(const uint8_t* data, size_t length));
    uint32_t getActiveBaud() const;

private:
    UartBridge();
    HardwareSerial m_uart;
    uint8_t m_txPin;
    uint8_t m_rxPin;
    uint32_t m_baudRate;
    bool m_isActive;
    uint8_t m_rxBuffer[512];
};
