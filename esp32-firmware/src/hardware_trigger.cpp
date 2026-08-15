#include "hardware_trigger.h"
#include "led_status.h"

HardwareTrigger::HardwareTrigger()
    : m_triggerPin(4), m_relayPin(5), m_isActive(false),
      m_triggerStartTime(0), m_triggerDuration(500),
      m_pulseCount(1), m_currentPulse(0), m_polarity(0),
      m_useRelay(false), m_pulseInDelay(false), m_delayStartTime(0) {}

HardwareTrigger& HardwareTrigger::getInstance() {
    static HardwareTrigger instance;
    return instance;
}

void HardwareTrigger::begin(uint8_t triggerPin, uint8_t relayPin) {
    m_triggerPin = triggerPin;
    m_relayPin = relayPin;

    pinMode(m_triggerPin, OUTPUT);
    digitalWrite(m_triggerPin, HIGH); // Default HIGH (inactive for active LOW test-point pull)

    pinMode(m_relayPin, OUTPUT);
    digitalWrite(m_relayPin, LOW); // Relay off
}

bool HardwareTrigger::executeTrigger(const trigger_request_payload_t& request) {
    m_triggerDuration = (request.duration_ms > 0) ? request.duration_ms : 500;
    m_pulseCount = (request.pulse_count > 0) ? request.pulse_count : 1;
    m_currentPulse = 0;
    m_polarity = request.polarity;
    m_useRelay = (request.use_relay == 1);
    m_pulseInDelay = false;
    m_isActive = true;
    m_triggerStartTime = millis();

    // Activate GPIO / Relay
    if (m_polarity == 0) {
        // Active LOW: pull test-point to GND
        digitalWrite(m_triggerPin, LOW);
    } else {
        // Active HIGH
        digitalWrite(m_triggerPin, HIGH);
    }

    if (m_useRelay) {
        digitalWrite(m_relayPin, HIGH);
    }

    LedStatusManager::getInstance().setState(LedState::TRIGGER_ACTIVE);
    return true;
}

void HardwareTrigger::stopTrigger() {
    // Release trigger back to default state
    digitalWrite(m_triggerPin, HIGH);
    digitalWrite(m_relayPin, LOW);
    m_isActive = false;
    m_pulseInDelay = false;
}

bool HardwareTrigger::isTriggerActive() const {
    return m_isActive;
}

void HardwareTrigger::loop() {
    if (!m_isActive) return;

    unsigned long now = millis();
    if (!m_pulseInDelay) {
        if (now - m_triggerStartTime >= m_triggerDuration) {
            // End active pulse
            digitalWrite(m_triggerPin, (m_polarity == 0) ? HIGH : LOW);
            if (m_useRelay) digitalWrite(m_relayPin, LOW);

            m_currentPulse++;
            if (m_currentPulse < m_pulseCount) {
                // Inter-pulse delay (150ms)
                m_pulseInDelay = true;
                m_delayStartTime = now;
            } else {
                stopTrigger();
            }
        }
    } else {
        if (now - m_delayStartTime >= 150) {
            m_pulseInDelay = false;
            m_triggerStartTime = now;
            digitalWrite(m_triggerPin, (m_polarity == 0) ? LOW : HIGH);
            if (m_useRelay) digitalWrite(m_relayPin, HIGH);
        }
    }
}
