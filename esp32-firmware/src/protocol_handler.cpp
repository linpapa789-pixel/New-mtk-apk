#include "protocol_handler.h"
#include "config_manager.h"
#include "hardware_trigger.h"
#include "uart_bridge.h"
#include "wifi_server.h"
#include "led_status.h"

// CRC-16-CCITT implementation
uint16_t bridge_crc16(const uint8_t *data, size_t length) {
    uint16_t crc = 0xFFFF;
    for (size_t i = 0; i < length; i++) {
        crc ^= (uint16_t)data[i] << 8;
        for (int j = 0; j < 8; j++) {
            if (crc & 0x8000) {
                crc = (crc << 1) ^ 0x1021;
            } else {
                crc <<= 1;
            }
        }
    }
    return crc;
}

ProtocolHandler& ProtocolHandler::getInstance() {
    static ProtocolHandler instance;
    return instance;
}

size_t ProtocolHandler::buildFrame(uint8_t cmdId, uint8_t seq, const uint8_t* payload, size_t payloadLen, uint8_t* outBuffer, size_t maxLen) {
    if (payloadLen > BRIDGE_PROTO_MAX_PAYLOAD || maxLen < (BRIDGE_PROTO_HEADER_SIZE + payloadLen + 2)) {
        return 0;
    }

    outBuffer[0] = BRIDGE_PROTO_MAGIC_0;
    outBuffer[1] = BRIDGE_PROTO_MAGIC_1;
    outBuffer[2] = cmdId;
    outBuffer[3] = seq;
    outBuffer[4] = (uint8_t)((payloadLen >> 8) & 0xFF);
    outBuffer[5] = (uint8_t)(payloadLen & 0xFF);

    if (payload && payloadLen > 0) {
        memcpy(&outBuffer[BRIDGE_PROTO_HEADER_SIZE], payload, payloadLen);
    }

    size_t frameLenWithoutCrc = BRIDGE_PROTO_HEADER_SIZE + payloadLen;
    uint16_t crc = bridge_crc16(outBuffer, frameLenWithoutCrc);
    outBuffer[frameLenWithoutCrc] = (uint8_t)((crc >> 8) & 0xFF);
    outBuffer[frameLenWithoutCrc + 1] = (uint8_t)(crc & 0xFF);

    return frameLenWithoutCrc + 2;
}

void ProtocolHandler::sendResponse(uint8_t cmdId, uint8_t seq, const uint8_t* payload, size_t payloadLen, uint8_t sourceTransport, uint8_t clientNum) {
    uint8_t buffer[BRIDGE_PROTO_MAX_PAYLOAD + 16];
    size_t totalLen = buildFrame(cmdId, seq, payload, payloadLen, buffer, sizeof(buffer));
    if (totalLen == 0) return;

    if (sourceTransport == TRANSPORT_USB_CDC) {
        Serial.write(buffer, totalLen);
        Serial.flush();
    } else {
        WifiServerManager::getInstance().sendFrameToClient(clientNum, buffer, totalLen);
    }
}

void ProtocolHandler::sendGenericStatus(uint8_t statusCode, uint16_t detail, const char* msg, uint8_t seq, uint8_t sourceTransport, uint8_t clientNum) {
    bridge_generic_response_t resp;
    resp.status_code = statusCode;
    resp.error_detail = detail;
    strncpy(resp.message, msg ? msg : "", sizeof(resp.message));
    sendResponse(CMD_ERROR_RESPONSE, seq, (const uint8_t*)&resp, sizeof(resp), sourceTransport, clientNum);
}

void ProtocolHandler::sendBridgeStatus(uint8_t seq, uint8_t sourceTransport, uint8_t clientNum) {
    bridge_status_payload_t status;
    status.uptime_sec = millis() / 1000;
    status.role_mode = UartBridge::getInstance().isBridgeActive() ? ROLE_UART_BRIDGE : ROLE_TEST_POINT_TRIGGER;
    status.transport = sourceTransport;
    
    uint16_t flags = 0;
    if (HardwareTrigger::getInstance().isTriggerActive()) flags |= FLAG_TRIGGER_ACTIVE;
    if (UartBridge::getInstance().isBridgeActive()) flags |= FLAG_UART_BRIDGE_ACTIVE;
    if (sourceTransport == TRANSPORT_USB_CDC) flags |= FLAG_USB_CONNECTED;
    if (WifiServerManager::getInstance().isClientConnected()) flags |= FLAG_WIFI_CONNECTED;
    status.status_flags = flags;
    
    status.uart_baud = UartBridge::getInstance().getActiveBaud();
    status.trigger_duration_ms = ConfigManager::getInstance().getConfig().default_trigger_ms;
    status.active_clients = WifiServerManager::getInstance().getClientCount();
    status.wifi_channel = 1;
    strncpy(status.device_name, ConfigManager::getInstance().getConfig().ap_ssid, sizeof(status.device_name));
    strncpy(status.firmware_version, MTK_BRIDGE_VERSION, sizeof(status.firmware_version));

    sendResponse(CMD_STATUS_RESPONSE, seq, (const uint8_t*)&status, sizeof(status), sourceTransport, clientNum);
}

void ProtocolHandler::processFrame(const uint8_t* frame, size_t length, uint8_t sourceTransport, uint8_t clientNum) {
    if (length < (BRIDGE_PROTO_HEADER_SIZE + 2)) return;
    if (frame[0] != BRIDGE_PROTO_MAGIC_0 || frame[1] != BRIDGE_PROTO_MAGIC_1) return;

    uint8_t cmdId = frame[2];
    uint8_t seq = frame[3];
    uint16_t payloadLen = ((uint16_t)frame[4] << 8) | frame[5];

    if (length < (size_t)(BRIDGE_PROTO_HEADER_SIZE + payloadLen + 2)) return;

    // Check CRC
    uint16_t calculatedCrc = bridge_crc16(frame, BRIDGE_PROTO_HEADER_SIZE + payloadLen);
    uint16_t receivedCrc = ((uint16_t)frame[BRIDGE_PROTO_HEADER_SIZE + payloadLen] << 8) |
                           frame[BRIDGE_PROTO_HEADER_SIZE + payloadLen + 1];

    if (calculatedCrc != receivedCrc) {
        sendGenericStatus(1, 0xCRC0, "CRC Check Failed", seq, sourceTransport, clientNum);
        return;
    }

    const uint8_t* payload = &frame[BRIDGE_PROTO_HEADER_SIZE];

    switch (cmdId) {
        case CMD_PING:
            sendResponse(CMD_PONG, seq, nullptr, 0, sourceTransport, clientNum);
            break;

        case CMD_GET_STATUS:
            sendBridgeStatus(seq, sourceTransport, clientNum);
            break;

        case CMD_START_TRIGGER: {
            if (payloadLen >= sizeof(trigger_request_payload_t)) {
                const trigger_request_payload_t* req = (const trigger_request_payload_t*)payload;
                HardwareTrigger::getInstance().executeTrigger(*req);
                sendGenericStatus(0, 0, "Trigger Fired", seq, sourceTransport, clientNum);
            } else {
                sendGenericStatus(2, 0, "Invalid Trigger Payload", seq, sourceTransport, clientNum);
            }
            break;
        }

        case CMD_STOP_TRIGGER:
            HardwareTrigger::getInstance().stopTrigger();
            sendGenericStatus(0, 0, "Trigger Stopped", seq, sourceTransport, clientNum);
            break;

        case CMD_START_UART_BRIDGE: {
            if (payloadLen >= sizeof(uart_config_payload_t)) {
                const uart_config_payload_t* cfg = (const uart_config_payload_t*)payload;
                UartBridge::getInstance().startBridge(*cfg);
                sendGenericStatus(0, 0, "UART Bridge Active", seq, sourceTransport, clientNum);
            } else {
                sendGenericStatus(3, 0, "Invalid UART Payload", seq, sourceTransport, clientNum);
            }
            break;
        }

        case CMD_STOP_UART_BRIDGE:
            UartBridge::getInstance().stopBridge();
            sendGenericStatus(0, 0, "UART Bridge Inactive", seq, sourceTransport, clientNum);
            break;

        case CMD_RAW_UART_DATA:
            if (UartBridge::getInstance().isBridgeActive()) {
                UartBridge::getInstance().writeToTarget(payload, payloadLen);
            }
            break;

        case CMD_REBOOT:
            sendGenericStatus(0, 0, "Rebooting...", seq, sourceTransport, clientNum);
            delay(200);
            ESP.restart();
            break;

        default:
            sendGenericStatus(4, cmdId, "Unknown Command", seq, sourceTransport, clientNum);
            break;
    }
}
