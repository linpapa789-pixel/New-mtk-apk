#include <Arduino.h>
#include "bridge_protocol.h"
#include "config_manager.h"
#include "hardware_trigger.h"
#include "uart_bridge.h"
#include "wifi_server.h"
#include "led_status.h"
#include "protocol_handler.h"

// Ring buffer for USB CDC stream parsing
static uint8_t s_usbRxBuffer[512];
static size_t s_usbRxLen = 0;

static void onWifiFrameReceived(const uint8_t* frame, size_t length, uint8_t clientNum) {
    ProtocolHandler::getInstance().processFrame(frame, length, TRANSPORT_WIFI_AP_WS, clientNum);
}

static void onUartDataReceivedFromTarget(const uint8_t* data, size_t length) {
    // Forward raw UART bytes to active transport (USB or Wi-Fi)
    uint8_t buffer[BRIDGE_PROTO_MAX_PAYLOAD + 16];
    size_t frameLen = ProtocolHandler::getInstance().buildFrame(
        CMD_RAW_UART_DATA, 0, data, length, buffer, sizeof(buffer)
    );
    if (frameLen > 0) {
        if (Serial) {
            Serial.write(buffer, frameLen);
            Serial.flush();
        }
        WifiServerManager::getInstance().broadcastFrame(buffer, frameLen);
    }
}

void setup() {
    // Initialize USB-CDC Serial for primary high-speed communication
    Serial.begin(115200);

    // Initialize Subsystems
    LedStatusManager::getInstance().begin(48);
    ConfigManager::getInstance().begin();
    
    BridgeConfig& cfg = ConfigManager::getInstance().getConfig();
    HardwareTrigger::getInstance().begin(cfg.trigger_pin, cfg.relay_pin);
    UartBridge::getInstance().begin(cfg.uart_tx_pin, cfg.uart_rx_pin, cfg.default_baud);

    // Start Wi-Fi Server (SoftAP + WebSocket + TCP)
    WifiServerManager::getInstance().begin(onWifiFrameReceived);
}

void loop() {
    // Subsystem state machines
    LedStatusManager::getInstance().loop();
    HardwareTrigger::getInstance().loop();
    WifiServerManager::getInstance().loop();
    UartBridge::getInstance().loop(onUartDataReceivedFromTarget);

    // Handle USB-CDC stream
    while (Serial.available() > 0) {
        uint8_t b = (uint8_t)Serial.read();
        
        // Find magic start if buffer empty
        if (s_usbRxLen == 0 && b != BRIDGE_PROTO_MAGIC_0) {
            continue;
        }
        if (s_usbRxLen == 1 && b != BRIDGE_PROTO_MAGIC_1) {
            s_usbRxLen = 0;
            continue;
        }

        s_usbRxBuffer[s_usbRxLen++] = b;

        // Check if we have complete header
        if (s_usbRxLen >= BRIDGE_PROTO_HEADER_SIZE) {
            uint16_t payloadLen = ((uint16_t)s_usbRxBuffer[4] << 8) | s_usbRxBuffer[5];
            size_t expectedTotal = BRIDGE_PROTO_HEADER_SIZE + payloadLen + 2; // +2 for CRC16
            
            if (expectedTotal > sizeof(s_usbRxBuffer)) {
                // Buffer overflow protection: reset
                s_usbRxLen = 0;
                continue;
            }

            if (s_usbRxLen >= expectedTotal) {
                ProtocolHandler::getInstance().processFrame(s_usbRxBuffer, expectedTotal, TRANSPORT_USB_CDC, 0);
                s_usbRxLen = 0;
            }
        }
    }
}
