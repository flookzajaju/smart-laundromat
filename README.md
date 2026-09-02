# Smart Laundromat — IoT Firmware

ESP32 firmware for the Smart Laundromat monitoring system. This device attaches externally to a washing machine — no internal circuitry modification required — and streams vibration data used to detect machine availability.

> Looking for the backend API and web dashboard? See [smart-laundromat](https://github.com/flookzajaju/smart-laundromat).

## Overview

This firmware runs on an ESP32 microcontroller connected to an MPU6050 accelerometer/gyroscope sensor. The sensor is mounted on the outside of a washing machine to pick up vibration patterns produced while it's running. Those readings are sent over the network to the [backend service](https://github.com/flookzajaju/smart-laundromat), which classifies the machine's state as Idle, Running, or Done.

## Hardware

| Component | Purpose |
|---|---|
| ESP32 | Microcontroller — reads sensor data and sends it to the backend |
| MPU6050 | Accelerometer + gyroscope — captures vibration from the washing machine |
| Reed Switch | Detects door open/close state (e.g. lid or door status) |

### Wiring

| Component Pin | ESP32 Pin |
|---|---|
| MPU6050 VCC | 3.3V |
| MPU6050 GND | GND |
| MPU6050 SCL | GPIO 22 |
| MPU6050 SDA | GPIO 21 |
| Reed Switch | GPIO 12 |

## Features

- Reads real-time vibration data from the MPU6050 via I2C
- Detects door open/close state using a reed switch
- Sends sensor readings to the backend REST API over Wi-Fi
- Non-invasive setup — attaches externally, no modification to the washing machine's internal circuitry
- Lightweight enough to run continuously for ongoing monitoring

## Getting Started

### Prerequisites

- ESP32 development board
- MPU6050 sensor module
- Arduino IDE or PlatformIO
- Wi-Fi network credentials
- The [backend service](https://github.com/flookzajaju/smart-laundromat) running and reachable on the network

### Setup

```bash
git clone https://github.com/flookzajaju/smart-laundromat-iot.git
```

1. Open the project in Arduino IDE or PlatformIO.
2. Update your Wi-Fi credentials and backend API endpoint in the config section of the code.
3. Wire the MPU6050 to the ESP32 as described above.
4. Flash the firmware to the ESP32.
5. Power on the device — it will begin reading vibration data and sending it to the backend.

## How It Works

1. The ESP32 continuously reads accelerometer/gyroscope values from the MPU6050.
2. The reed switch reports whether the washing machine door is open or closed.
3. Readings are packaged and sent to the backend API at regular intervals.
4. The backend interprets these vibration and door-state patterns to determine whether the machine is idle, running, or finished.

## Related Repository

- **Backend & Dashboard:** [smart-laundromat](https://github.com/flookzajaju/smart-laundromat) — Spring Boot API, MongoDB storage, and the web dashboard

## Author

**Peeraphat Lampoothong**
GitHub: [@flookzajaju](https://github.com/flookzajaju)

## License

Academic project — created for coursework/portfolio purposes.
