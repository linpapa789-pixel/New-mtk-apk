#include "config_manager.h"
#include <esp_random.h>

ConfigManager::ConfigManager() {}

ConfigManager& ConfigManager::getInstance() {
    static ConfigManager instance;
    return instance;
}

void ConfigManager::begin() {
    prefs.begin("mtk_bridge", false);
    if (!prefs.isKey("initialized")) {
        resetToDefaults();
    } else {
        prefs.getString("ap_ssid", config.ap_ssid, sizeof(config.ap_ssid));
        prefs.getString("ap_pass", config.ap_password, sizeof(config.ap_password));
        prefs.getString("sta_ssid", config.sta_ssid, sizeof(config.sta_ssid));
        prefs.getString("sta_pass", config.sta_password, sizeof(config.sta_password));
        config.sta_enabled = prefs.getBool("sta_en", false);
        config.tcp_port = prefs.getUShort("tcp_port", 8888);
        config.ws_port = prefs.getUShort("ws_port", 8080);
        config.trigger_pin = prefs.getUChar("trig_pin", 4);
        config.relay_pin = prefs.getUChar("relay_pin", 5);
        config.uart_tx_pin = prefs.getUChar("tx_pin", 17);
        config.uart_rx_pin = prefs.getUChar("rx_pin", 18);
        config.default_baud = prefs.getUInt("def_baud", 115200);
        config.default_trigger_ms = prefs.getUShort("def_trig_ms", 500);
    }
}

void ConfigManager::resetToDefaults() {
    uint32_t randChipId = (uint32_t)(ESP.getEfuseMac() & 0xFFFFFF);
    snprintf(config.ap_ssid, sizeof(config.ap_ssid), "MTK-Bridge-%06X", randChipId);
    generateSecureApPassword(config.ap_password, sizeof(config.ap_password));
    
    config.sta_ssid[0] = '\0';
    config.sta_password[0] = '\0';
    config.sta_enabled = false;
    config.tcp_port = 8888;
    config.ws_port = 8080;
    config.trigger_pin = 4;
    config.relay_pin = 5;
    config.uart_tx_pin = 17;
    config.uart_rx_pin = 18;
    config.default_baud = 115200;
    config.default_trigger_ms = 500;
    
    saveConfig();
    prefs.putBool("initialized", true);
}

void ConfigManager::generateSecureApPassword(char* outPassword, size_t maxLen) {
    // Generate an 8-character high entropy alphanumeric WPA2 passphrase
    const char charset[] = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
    size_t charsetLen = sizeof(charset) - 1;
    size_t passLen = 8;
    if (maxLen <= passLen) return;
    
    for (size_t i = 0; i < passLen; i++) {
        uint32_t r = esp_random() % charsetLen;
        outPassword[i] = charset[r];
    }
    outPassword[passLen] = '\0';
}

BridgeConfig& ConfigManager::getConfig() {
    return config;
}

void ConfigManager::saveConfig() {
    prefs.putString("ap_ssid", config.ap_ssid);
    prefs.putString("ap_pass", config.ap_password);
    prefs.putString("sta_ssid", config.sta_ssid);
    prefs.putString("sta_pass", config.sta_password);
    prefs.putBool("sta_en", config.sta_enabled);
    prefs.putUShort("tcp_port", config.tcp_port);
    prefs.putUShort("ws_port", config.ws_port);
    prefs.putUChar("trig_pin", config.trigger_pin);
    prefs.putUChar("relay_pin", config.relay_pin);
    prefs.putUChar("tx_pin", config.uart_tx_pin);
    prefs.putUChar("rx_pin", config.uart_rx_pin);
    prefs.putUInt("def_baud", config.default_baud);
    prefs.putUShort("def_trig_ms", config.default_trigger_ms);
}
