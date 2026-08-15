#pragma once
#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

#define BRIDGE_PROTO_MAGIC_0        0x55
#define BRIDGE_PROTO_MAGIC_1        0xAA
#define BRIDGE_PROTO_HEADER_SIZE    6
#define BRIDGE_PROTO_MAX_PAYLOAD    4096

// Command Identifiers
enum BridgeCommandId : uint8_t {
    CMD_PING                = 0x01,
    CMD_PONG                = 0x02,
    CMD_GET_STATUS          = 0x03,
    CMD_STATUS_RESPONSE     = 0x04,
    CMD_START_TRIGGER       = 0x10,
    CMD_STOP_TRIGGER        = 0x11,
    CMD_TRIGGER_RESPONSE    = 0x12,
    CMD_START_UART_BRIDGE   = 0x20,
    CMD_STOP_UART_BRIDGE    = 0x21,
    CMD_UART_BRIDGE_STATE   = 0x22,
    CMD_RAW_UART_DATA       = 0x30,
    CMD_SET_WIFI_CONFIG     = 0x40,
    CMD_GET_WIFI_CONFIG     = 0x41,
    CMD_SET_GPIO_CONFIG     = 0x42,
    CMD_REBOOT              = 0xFE,
    CMD_ERROR_RESPONSE      = 0xFF
};

// Transport Modes
enum BridgeTransportMode : uint8_t {
    TRANSPORT_USB_CDC       = 0x01,
    TRANSPORT_WIFI_AP_WS    = 0x02,
    TRANSPORT_WIFI_STA_WS   = 0x03,
    TRANSPORT_WIFI_RAW_TCP  = 0x04
};

// Operating Mode (Mode a vs Mode b)
enum BridgeOperationalRole : uint8_t {
    ROLE_TEST_POINT_TRIGGER = 0x01, // Mode (a): ESP32 triggers test-point; phone communicates via direct USB-OTG
    ROLE_UART_BRIDGE        = 0x02  // Mode (b): ESP32 proxies raw UART BROM lines
};

// Bridge Status Flags
enum BridgeStatusFlags : uint16_t {
    FLAG_TRIGGER_ACTIVE     = (1 << 0),
    FLAG_UART_BRIDGE_ACTIVE = (1 << 1),
    FLAG_USB_CONNECTED      = (1 << 2),
    FLAG_WIFI_CONNECTED     = (1 << 3),
    FLAG_RELAY_ENGAGED      = (1 << 4),
    FLAG_ERROR_STATE        = (1 << 15)
};

// Binary Frame Header
#pragma pack(push, 1)
typedef struct {
    uint8_t magic[2];      // 0x55, 0xAA
    uint8_t cmd_id;        // BridgeCommandId
    uint8_t seq;           // Sequence number
    uint16_t length;       // Big-Endian payload length
} bridge_header_t;

typedef struct {
    uint32_t uptime_sec;
    uint8_t role_mode;     // BridgeOperationalRole
    uint8_t transport;     // BridgeTransportMode
    uint16_t status_flags; // BridgeStatusFlags
    uint32_t uart_baud;
    uint16_t trigger_duration_ms;
    uint8_t active_clients;
    uint8_t wifi_channel;
    char device_name[24];
    char firmware_version[12];
} bridge_status_payload_t;

typedef struct {
    uint16_t duration_ms;  // Pulse length in ms (e.g. 500ms)
    uint8_t pulse_count;   // 1 = single pulse, N = multi-pulse
    uint8_t polarity;      // 0 = active LOW (pull TP to GND), 1 = active HIGH
    uint8_t use_relay;     // 1 = toggle relay, 0 = direct MOSFET/GPIO
} trigger_request_payload_t;

typedef struct {
    uint32_t baud_rate;    // e.g. 115200, 921600
    uint8_t data_bits;     // 8
    uint8_t parity;        // 0 = None, 1 = Odd, 2 = Even
    uint8_t stop_bits;     // 1
} uart_config_payload_t;

typedef struct {
    uint8_t status_code;   // 0 = Success, >0 = Error Code
    uint16_t error_detail;
    char message[48];
} bridge_generic_response_t;
#pragma pack(pop)

// CRC-16-CCITT calculation for robust framing
uint16_t bridge_crc16(const uint8_t *data, size_t length);

#ifdef __cplusplus
}
#endif
