#!/bin/bash

# Installing curl
apt update
apt install curl -y

# Waiting for kafka connect to be ready
echo "Waiting for Kafka Connect to be ready..."
while ! curl -s http://kafka-connect:8083/connectors > /dev/null; do
    echo "Kafka Connect not ready yet, retrying..."
    sleep 5
done

# Creating connector velib-http-source
echo "[WRAPPER] Creating connector velib-http-source..."
curl -v -X POST http://kafka-connect:8083/connectors \
     -H "Content-Type: application/json" \
     --data @/tmp/velib-http-source.json
