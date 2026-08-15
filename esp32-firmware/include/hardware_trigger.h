#pragma once
#include <Arduino.h>
#include "bridge_protocol.h"

class HardwareTrigger {
public:
    static HardwareTrigger& getInstance();
    void begin(uint8_t triggerPin, uint8_t relayPin);
    bool executeTrigger(const trigger_request_payload_t& request);
    void stopTrigger();
    bool isTriggerActive() const;
    void loop();

private:
    HardwareTrigger();
    uint8_t m_triggerPin;
    uint8_t m_relayPin;
    bool m_isActive;
    unsigned long m_triggerStartTime;
    unsigned long m_triggerDuration;
    uint8_t m_pulseCount;
    uint8_t m_currentPulse;
    uint8_t m_polarity;
    bool m_useRelay;
    bool m_pulseInDelay;
    unsigned long m_delayStartTime;
};
