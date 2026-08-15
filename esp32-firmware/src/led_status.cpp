#include "led_status.h"
#include <FastLED.h>

#define NUM_LEDS 1
static CRGB s_leds[NUM_LEDS];

LedStatusManager::LedStatusManager()
    : m_pin(48), m_currentState(LedState::IDLE_BREATHING), m_lastUpdate(0), m_step(0) {}

LedStatusManager& LedStatusManager::getInstance() {
    static LedStatusManager instance;
    return instance;
}

void LedStatusManager::begin(uint8_t pin) {
    m_pin = pin;
    FastLED.addLeds<WS2812, 48, GRB>(s_leds, NUM_LEDS);
    FastLED.setBrightness(40);
    setState(LedState::IDLE_BREATHING);
}

void LedStatusManager::setState(LedState state) {
    m_currentState = state;
    m_step = 0;
}

void LedStatusManager::updateRgb(uint8_t r, uint8_t g, uint8_t b) {
    s_leds[0] = CRGB(r, g, b);
    FastLED.show();
}

void LedStatusManager::loop() {
    unsigned long now = millis();
    if (now - m_lastUpdate < 30) return;
    m_lastUpdate = now;
    m_step++;

    switch (m_currentState) {
        case LedState::IDLE_BREATHING: {
            // Gentle breathing Cyan (0, 180, 255)
            float val = (sin(m_step * 0.05f) + 1.0f) * 0.5f; // 0.0 to 1.0
            uint8_t g = (uint8_t)(val * 140);
            uint8_t b = (uint8_t)(val * 255);
            updateRgb(0, g, b);
            break;
        }
        case LedState::USB_CONNECTED:
            updateRgb(0, 220, 255); // Solid Bright Cyan
            break;
        case LedState::WIFI_CONNECTED:
            updateRgb(0, 80, 255); // Solid Deep Blue
            break;
        case LedState::TRIGGER_ACTIVE: {
            // Fast strobe Amber/Orange (255, 120, 0)
            bool blink = (m_step % 6) < 3;
            updateRgb(blink ? 255 : 0, blink ? 120 : 0, 0);
            break;
        }
        case LedState::UART_BRIDGE_ACTIVE: {
            // Purple pulse (180, 0, 255)
            bool blink = (m_step % 10) < 5;
            updateRgb(blink ? 180 : 20, 0, blink ? 255 : 30);
            break;
        }
        case LedState::ERROR_STATE:
            updateRgb(255, 0, 0); // Solid Red
            break;
    }
}
