#include "uart_bridge.h"
#include "led_status.h"

UartBridge::UartBridge()
    : m_uart(1), m_txPin(17), m_rxPin(18), m_baudRate(115200), m_isActive(false) {}

UartBridge& UartBridge::getInstance() {
    static UartBridge instance;
    return instance;
}

void UartBridge::begin(uint8_t txPin, uint8_t rxPin, uint32_t defaultBaud) {
    m_txPin = txPin;
    m_rxPin = rxPin;
    m_baudRate = defaultBaud;
}

bool UartBridge::startBridge(const uart_config_payload_t& config) {
    m_baudRate = (config.baud_rate > 0) ? config.baud_rate : 115200;
    
    uint32_t configBits = SERIAL_8N1;
    if (config.data_bits == 8 && config.parity == 1) configBits = SERIAL_8O1;
    else if (config.data_bits == 8 && config.parity == 2) configBits = SERIAL_8E1;

    m_uart.end();
    // Configure hardware UART with ultra-low latency rx FIFO threshold
    m_uart.begin(m_baudRate, configBits, m_rxPin, m_txPin);
    m_uart.setRxFIFOFull(1); // Flush immediately for BROM sync timing
    m_isActive = true;

    LedStatusManager::getInstance().setState(LedState::UART_BRIDGE_ACTIVE);
    return true;
}

void UartBridge::stopBridge() {
    if (m_isActive) {
        m_uart.end();
        m_isActive = false;
    }
}

bool UartBridge::isBridgeActive() const {
    return m_isActive;
}

size_t UartBridge::writeToTarget(const uint8_t* data, size_t length) {
    if (!m_isActive || !data || length == 0) return 0;
    size_t written = m_uart.write(data, length);
    m_uart.flush();
    return written;
}

size_t UartBridge::readFromTarget(uint8_t* buffer, size_t maxLen) {
    if (!m_isActive || !buffer || maxLen == 0) return 0;
    size_t count = 0;
    while (m_uart.available() && count < maxLen) {
        buffer[count++] = (uint8_t)m_uart.read();
    }
    return count;
}

void UartBridge::loop(void (*onDataReceived)(const uint8_t* data, size_t length)) {
    if (!m_isActive || !onDataReceived) return;

    size_t availableBytes = m_uart.available();
    if (availableBytes > 0) {
        size_t toRead = min(availableBytes, sizeof(m_rxBuffer));
        size_t bytesRead = readFromTarget(m_rxBuffer, toRead);
        if (bytesRead > 0) {
            onDataReceived(m_rxBuffer, bytesRead);
        }
    }
}

uint32_t UartBridge::getActiveBaud() const {
    return m_baudRate;
}
