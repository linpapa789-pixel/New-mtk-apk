#pragma once
#include <Arduino.h>
#include <Preferences.h>

struct BridgeConfig {
    char ap_ssid[32];
    char ap_password[64];
    char sta_ssid[32];
    char sta_password[64];
    bool sta_enabled;
    uint16_t tcp_port;
    uint16_t ws_port;
    uint8_t trigger_pin;
    uint8_t relay_pin;
    uint8_t uart_tx_pin;
    uint8_t uart_rx_pin;
    uint32_t default_baud;
    uint16_t default_trigger_ms;
};

class ConfigManager {
public:
    static ConfigManager& getInstance();
    void begin();
    BridgeConfig& getConfig();
    void saveConfig();
    void resetToDefaults();
    void generateSecureApPassword(char* outPassword, size_t maxLen);

private:
    ConfigManager();
    Preferences prefs;
    BridgeConfig config;
};
