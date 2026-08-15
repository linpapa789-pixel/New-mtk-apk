#include "wifi_server.h"
#include "config_manager.h"
#include "led_status.h"
#include <ESPmDNS.h>

static WifiServerManager* s_instance = nullptr;

WifiServerManager::WifiServerManager()
    : m_httpServer(80), m_wsServer(8080), m_tcpServer(8888),
      m_frameCallback(nullptr), m_connectedClients(0), m_apRunning(false) {
    s_instance = this;
}

WifiServerManager& WifiServerManager::getInstance() {
    static WifiServerManager instance;
    return instance;
}

void WifiServerManager::wsEventWrapper(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
    if (s_instance) {
        s_instance->handleWebSocketEvent(num, type, payload, length);
    }
}

void WifiServerManager::begin(FrameCallback onFrameReceived) {
    m_frameCallback = onFrameReceived;
    BridgeConfig& cfg = ConfigManager::getInstance().getConfig();

    // Configure and start Wi-Fi SoftAP
    WiFi.mode(WIFI_AP_STA);
    WiFi.softAP(cfg.ap_ssid, cfg.ap_password);
    m_apRunning = true;

    // Optional Station connection
    if (cfg.sta_enabled && strlen(cfg.sta_ssid) > 0) {
        WiFi.begin(cfg.sta_ssid, cfg.sta_password);
    }

    // Start mDNS
    if (MDNS.begin("mtkbridge")) {
        MDNS.addService("http", "tcp", 80);
        MDNS.addService("ws", "tcp", cfg.ws_port);
        MDNS.addService("mtk-bridge", "tcp", cfg.tcp_port);
    }

    // Start WebSocket Server
    m_wsServer.begin();
    m_wsServer.onEvent(wsEventWrapper);

    // Start TCP Server
    m_tcpServer.begin(cfg.tcp_port);
    m_tcpServer.setNoDelay(true);

    setupCaptivePortal();
    m_httpServer.begin();
}

void WifiServerManager::setupCaptivePortal() {
    m_httpServer.on("/", HTTP_GET, [this]() {
        BridgeConfig& cfg = ConfigManager::getInstance().getConfig();
        String html = "<!DOCTYPE html><html><head><title>ESP32-S3 MTK Flash Bridge</title>";
        html += "<meta name='viewport' content='width=device-width, initial-scale=1'>";
        html += "<style>body{font-family:sans-serif;background:#0d1b2a;color:#fff;padding:20px;}";
        html += ".card{background:#1b263b;padding:15px;border-radius:8px;margin-bottom:15px;}";
        html += "h1{color:#38bdf8;}input{width:100%;padding:8px;margin:5px 0;box-sizing:border-box;}";
        html += "button{background:#0284c7;color:#fff;border:none;padding:10px 20px;border-radius:4px;cursor:pointer;}";
        html += "</style></head><body>";
        html += "<h1>MTK BROM Flash Bridge</h1>";
        html += "<div class='card'><h3>Device Status</h3>";
        html += "<p>SoftAP SSID: <b>" + String(cfg.ap_ssid) + "</b></p>";
        html += "<p>SoftAP IP: <b>" + WiFi.softAPIP().toString() + "</b></p>";
        html += "<p>WebSocket Port: <b>" + String(cfg.ws_port) + "</b></p>";
        html += "<p>Raw TCP Port: <b>" + String(cfg.tcp_port) + "</b></p>";
        html += "<p>Clients Connected: <b>" + String(m_connectedClients) + "</b></p>";
        html += "</div>";
        html += "<div class='card'><h3>Update Wi-Fi Settings</h3>";
        html += "<form method='POST' action='/save'>";
        html += "AP SSID: <input name='ap_ssid' value='" + String(cfg.ap_ssid) + "'><br>";
        html += "AP Password: <input name='ap_pass' type='password' value='" + String(cfg.ap_password) + "'><br>";
        html += "Join Station SSID: <input name='sta_ssid' value='" + String(cfg.sta_ssid) + "'><br>";
        html += "Station Password: <input name='sta_pass' type='password' value='" + String(cfg.sta_password) + "'><br>";
        html += "<br><button type='submit'>Save & Reboot</button>";
        html += "</form></div></body></html>";
        m_httpServer.send(200, "text/html", html);
    });

    m_httpServer.on("/save", HTTP_POST, [this]() {
        BridgeConfig& cfg = ConfigManager::getInstance().getConfig();
        if (m_httpServer.hasArg("ap_ssid")) strncpy(cfg.ap_ssid, m_httpServer.arg("ap_ssid").c_str(), sizeof(cfg.ap_ssid));
        if (m_httpServer.hasArg("ap_pass")) strncpy(cfg.ap_password, m_httpServer.arg("ap_pass").c_str(), sizeof(cfg.ap_password));
        if (m_httpServer.hasArg("sta_ssid")) {
            strncpy(cfg.sta_ssid, m_httpServer.arg("sta_ssid").c_str(), sizeof(cfg.sta_ssid));
            cfg.sta_enabled = (strlen(cfg.sta_ssid) > 0);
        }
        if (m_httpServer.hasArg("sta_pass")) strncpy(cfg.sta_password, m_httpServer.arg("sta_pass").c_str(), sizeof(cfg.sta_password));
        ConfigManager::getInstance().saveConfig();
        m_httpServer.send(200, "text/plain", "Settings saved! Rebooting device...");
        delay(1000);
        ESP.restart();
    });
}

void WifiServerManager::handleWebSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
    switch (type) {
        case WStype_CONNECTED:
            m_connectedClients++;
            LedStatusManager::getInstance().setState(LedState::WIFI_CONNECTED);
            break;
        case WStype_DISCONNECTED:
            if (m_connectedClients > 0) m_connectedClients--;
            if (m_connectedClients == 0) LedStatusManager::getInstance().setState(LedState::IDLE_BREATHING);
            break;
        case WStype_BIN:
            if (m_frameCallback && payload && length > 0) {
                m_frameCallback(payload, length, num);
            }
            break;
        default:
            break;
    }
}

void WifiServerManager::loop() {
    m_httpServer.handleClient();
    m_wsServer.loop();

    // Check incoming TCP clients
    if (m_tcpServer.hasClient()) {
        if (!m_tcpClient || !m_tcpClient.connected()) {
            m_tcpClient = m_tcpServer.available();
            m_connectedClients++;
            LedStatusManager::getInstance().setState(LedState::WIFI_CONNECTED);
        }
    }

    if (m_tcpClient && m_tcpClient.connected() && m_tcpClient.available()) {
        uint8_t buffer[512];
        size_t bytes = m_tcpClient.read(buffer, sizeof(buffer));
        if (bytes > 0 && m_frameCallback) {
            m_frameCallback(buffer, bytes, 0xFF); // 0xFF denotes TCP client
        }
    }
}

bool WifiServerManager::broadcastFrame(const uint8_t* data, size_t length) {
    if (!data || length == 0) return false;
    m_wsServer.broadcastBIN(data, length);
    if (m_tcpClient && m_tcpClient.connected()) {
        m_tcpClient.write(data, length);
        m_tcpClient.flush();
    }
    return true;
}

bool WifiServerManager::sendFrameToClient(uint8_t clientNum, const uint8_t* data, size_t length) {
    if (!data || length == 0) return false;
    if (clientNum == 0xFF) {
        if (m_tcpClient && m_tcpClient.connected()) {
            m_tcpClient.write(data, length);
            m_tcpClient.flush();
            return true;
        }
        return false;
    }
    return m_wsServer.sendBIN(clientNum, data, length);
}

bool WifiServerManager::isClientConnected() const {
    return (m_connectedClients > 0);
}

uint8_t WifiServerManager::getClientCount() const {
    return m_connectedClients;
}

String WifiServerManager::getApSsid() const {
    return String(ConfigManager::getInstance().getConfig().ap_ssid);
}

String WifiServerManager::getApIp() const {
    return WiFi.softAPIP().toString();
}

String WifiServerManager::getApPassword() const {
    return String(ConfigManager::getInstance().getConfig().ap_password);
}
