# Smart Laundromat — Backend API

A real-time washing machine monitoring system that detects machine availability (Idle / Running / Done) using vibration sensor data, without modifying the internal circuitry of the washing machine.

This repository contains the **backend service**, built with Java and Spring Boot, which processes sensor data sent from the companion IoT device and exposes it through a REST API and web dashboard.

> Looking for the ESP32 firmware? See [smart-laundromat-iot](https://github.com/flookzajaju/smart-laundromat-iot).

## Overview

Many shared laundromats don't tell you whether a machine is actually free or how long you'll have to wait. This project solves that by attaching a low-cost vibration sensor (MPU6050) to the outside of existing washing machines, streaming that data to this backend, and classifying each machine's state in real time — no hardware modification required.

## Features

- Classifies each machine's state as **Idle**, **Running**, or **Done** based on incoming vibration data
- Estimates remaining cycle time from collected sensor patterns
- Stores vibration logs and historical wash cycles in MongoDB for monitoring and preventive maintenance analysis
- Exposes a RESTful API for the frontend dashboard and other clients
- Serves a responsive web dashboard so users can check machine availability, and admins can visualize historical vibration trends

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java, Spring Boot |
| Database | MongoDB |
| Frontend | HTML, CSS, JavaScript |
| Build Tool | Maven |
| Hardware (companion repo) | ESP32, MPU6050, Reed Switch |

## Project Structure

```
smart-laundromat/
├── src/
│   ├── main/
│   │   ├── java/...      # Spring Boot application (controllers, services, models)
│   │   └── resources/    # application config, static dashboard assets
│   └── test/
├── .mvn/wrapper/
├── mvnw / mvnw.cmd
└── pom.xml
```

## Getting Started

### Prerequisites

- Java 17+ (adjust to whatever version you targeted)
- Maven (or use the included `mvnw` wrapper)
- MongoDB instance (local or cloud, e.g. MongoDB Atlas)

### Setup

```bash
# Clone the repository
git clone https://github.com/flookzajaju/smart-laundromat.git
cd smart-laundromat

# Configure your MongoDB connection in
# src/main/resources/application.properties (or application.yml)

# Run with the Maven wrapper
./mvnw spring-boot:run
```

The API will start on `http://localhost:8080` by default.

## How It Works

1. The ESP32 device (see the [IoT repo](https://github.com/flookzajaju/smart-laundromat-iot)) reads vibration data from the MPU6050 sensor attached to a washing machine.
2. Sensor readings are sent to this backend's REST API.
3. The backend classifies the current machine state (Idle / Running / Done) and estimates remaining cycle time.
4. Each reading and state change is logged to MongoDB for historical analysis.
5. The web dashboard queries the API to show live machine status and vibration trend history.

## Related Repository

- **IoT Firmware:** [smart-laundromat-iot](https://github.com/flookzajaju/smart-laundromat-iot) — ESP32 code for reading and transmitting sensor data

## Author

**Peeraphat Lampoothong**
GitHub: [@flookzajaju](https://github.com/flookzajaju)

## License

Academic project — created for coursework/portfolio purposes.
