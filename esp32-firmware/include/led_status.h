#pragma once
#include <Arduino.h>

enum class LedState {
    IDLE_BREATHING,
    USB_CONNECTED,
    WIFI_CONNECTED,
    TRIGGER_ACTIVE,
    UART_BRIDGE_ACTIVE,
    ERROR_STATE
};

class LedStatusManager {
public:
    static LedStatusManager& getInstance();
    void begin(uint8_t pin = 48);
    void setState(LedState state);
    void loop();

private:
    LedStatusManager();
    uint8_t m_pin;
    LedState m_currentState;
    unsigned long m_lastUpdate;
    uint16_t m_step;
    void updateRgb(uint8_t r, uint8_t g, uint8_t b);
};
