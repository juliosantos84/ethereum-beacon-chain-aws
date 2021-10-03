#! /bin/bash

set -x

set -e

echo "Installing prometheus..."

echo "Creating users..." \
&& sudo useradd --no-create-home --shell /bin/false prometheus \
&& sudo useradd --no-create-home --shell /bin/false node_exporter

echo "Creating directories..." \
&& sudo mkdir -p /etc/prometheus \
&& sudo chown -R prometheus:prometheus /etc/prometheus \
&& sudo mkdir -p /var/lib/prometheus \
&& sudo chown -R prometheus:prometheus /var/lib/prometheus

echo "Downloading and installing binaries" \
&& curl -LO https://github.com/prometheus/prometheus/releases/download/v2.20.0/prometheus-2.20.0.linux-amd64.tar.gz \
&& tar xvf prometheus-2.20.0.linux-amd64.tar.gz \
&& sudo cp prometheus-2.20.0.linux-amd64/{prometheus,promtool} /usr/local/bin/ \
&& sudo chown -R prometheus:prometheus /usr/local/bin/{prometheus,promtool} \
&& sudo cp -r prometheus-2.20.0.linux-amd64/consoles /etc/prometheus \
&& sudo cp -r prometheus-2.20.0.linux-amd64/console_libraries /etc/prometheus \
&& sudo chown -R prometheus:prometheus /etc/prometheus/consoles \
&& sudo chown -R prometheus:prometheus /etc/prometheus/console_libraries \
&& rm -rf prometheus-2.20.0.linux-amd64.tar.gz prometheus-2.20.0.linux-amd64

sudo cat <<PROM_YML >> ./prometheus.yml \
&& sudo mv ./prometheus.yml /etc/prometheus/prometheus.yml \
&& sudo chown -R prometheus:prometheus /etc/prometheus/prometheus.yml
global:
  scrape_interval:     15s # Set the scrape interval to every 15 seconds. Default is every 1 minute.
  evaluation_interval: 15s # Evaluate rules every 15 seconds. The default is every 1 minute.
  # scrape_timeout is set to the global default (10s).
# Alertmanager configuration
alerting:
  alertmanagers:
  - static_configs:
    - targets:
      # - alertmanager:9093
# Load rules once and periodically evaluate them according to the global 'evaluation_interval'.
rule_files:
  # - "first_rules.yml"
  # - "second_rules.yml"
# A scrape configuration containing exactly one endpoint to scrape:
# Here it's Prometheus itself.
scrape_configs:
  - job_name: 'nodes'
    metrics_path: /metrics    
    static_configs:
      - targets: ['localhost:5054']
  - job_name: 'node_exporter'
    static_configs:
      - targets: ['localhost:9100']
PROM_YML

cat <<PROM_UNIT >> ./prometheus.service \
&& sudo mv ./prometheus.service /etc/systemd/system/prometheus.service \
&& sudo systemctl daemon-reload && sudo systemctl start prometheus
[Unit]
Description=Prometheus
Wants=network-online.target
After=network-online.target

[Service]
Type=simple
User=prometheus
Group=prometheus
Restart=always
RestartSec=30
ExecStart=/usr/local/bin/prometheus \
    --config.file /etc/prometheus/prometheus.yml \
    --storage.tsdb.path /var/lib/prometheus/ \
    --web.console.templates=/etc/prometheus/consoles \
    --web.console.libraries=/etc/prometheus/console_libraries

[Install]
WantedBy=multi-user.target
PROM_UNIT