package com.wifi.security;

import javax.swing.*;
import java.awt.*;
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.Properties;
import java.nio.file.*;
import java.io.*;

public class WifiSecurityGUI extends JFrame {
    private JTextField webhookField;
    private JButton saveButton;
    private JButton startButton;
    private JButton stopButton;
    private JTextArea logArea;
    private JLabel wifiLabel;
    private JLabel securityLabel;
    private JLabel riskLabel;
    private AtomicBoolean monitoring = new AtomicBoolean(false);
    private Thread monitorThread;
    private static final String CONFIG_FILE = "config.properties";

    public WifiSecurityGUI() {
        setTitle("WiFi 보안 체커");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top panel for webhook input
        JPanel topPanel = new JPanel(new BorderLayout());
        webhookField = new JTextField();
        loadWebhookUrl();
        saveButton = new JButton("저장");
        topPanel.add(new JLabel("디스코드 웹훅 URL: "), BorderLayout.WEST);
        topPanel.add(webhookField, BorderLayout.CENTER);
        topPanel.add(saveButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Center panel for WiFi info
        JPanel centerPanel = new JPanel(new GridLayout(3, 1));
        wifiLabel = new JLabel("현재 WiFi: -");
        securityLabel = new JLabel("보안 유형: -");
        riskLabel = new JLabel("위험도: -");
        centerPanel.add(wifiLabel);
        centerPanel.add(securityLabel);
        centerPanel.add(riskLabel);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom panel for buttons and log
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        startButton = new JButton("모니터링 시작");
        stopButton = new JButton("모니터링 중지");
        stopButton.setEnabled(false);
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        logArea = new JTextArea(8, 40);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Action listeners
        saveButton.addActionListener(e -> saveWebhookUrl());
        startButton.addActionListener(e -> startMonitoring());
        stopButton.addActionListener(e -> stopMonitoring());
    }

    private void loadWebhookUrl() {
        try {
            if (Files.exists(Paths.get(CONFIG_FILE))) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                    props.load(fis);
                    String url = props.getProperty("webhook.url");
                    if (url != null) webhookField.setText(url);
                }
            }
        } catch (IOException e) {
            appendLog("설정 파일 로드 오류: " + e.getMessage());
        }
    }

    private void saveWebhookUrl() {
        String url = webhookField.getText().trim();
        if (!WifiSecurityChecker.isValidWebhookUrl(url)) {
            appendLog("올바른 디스코드 웹훅 URL을 입력하세요.");
            return;
        }
        try {
            Properties props = new Properties();
            props.setProperty("webhook.url", url);
            try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
                props.store(fos, "Discord Webhook Configuration");
            }
            WifiSecurityChecker.setDiscordWebhookUrl(url);
            appendLog("웹훅 URL이 저장되었습니다.");
        } catch (IOException e) {
            appendLog("저장 오류: " + e.getMessage());
        }
    }

    private void startMonitoring() {
        String url = webhookField.getText().trim();
        if (!WifiSecurityChecker.isValidWebhookUrl(url)) {
            appendLog("올바른 디스코드 웹훅 URL을 입력하세요.");
            return;
        }
        WifiSecurityChecker.setDiscordWebhookUrl(url);
        monitoring.set(true);
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        monitorThread = new Thread(() -> {
            String lastSSID = null;
            while (monitoring.get()) {
                try {
                    String wifiInfo = WifiSecurityChecker.getWifiInfo();
                    if (wifiInfo.isEmpty()) {
                        appendLog("WiFi 정보를 가져올 수 없습니다.");
                        TimeUnit.SECONDS.sleep(10);
                        continue;
                    }
                    String ssid = WifiSecurityChecker.extractSSID(wifiInfo);
                    String securityType = WifiSecurityChecker.analyzeSecurityType(wifiInfo);
                    String riskLevel = WifiSecurityChecker.assessRiskLevel(securityType);
                    wifiLabel.setText("현재 WiFi: " + ssid);
                    securityLabel.setText("보안 유형: " + securityType);
                    riskLabel.setText("위험도: " + riskLevel);
                    if (!ssid.equals(lastSSID)) {
                        appendLog("새로운 WiFi 감지: " + ssid);
                        appendLog("보안 유형: " + securityType);
                        appendLog("위험도: " + riskLevel);
                        try {
                            WifiSecurityChecker.sendDiscordAlert(wifiInfo, securityType, riskLevel);
                            appendLog("디스코드로 알림 전송 완료");
                        } catch (Exception ex) {
                            appendLog("디스코드 전송 오류: " + ex.getMessage());
                        }
                        lastSSID = ssid;
                    }
                    TimeUnit.SECONDS.sleep(10);
                } catch (Exception ex) {
                    appendLog("오류: " + ex.getMessage());
                    try { TimeUnit.SECONDS.sleep(10); } catch (InterruptedException ignored) {}
                }
            }
        });
        monitorThread.start();
        appendLog("모니터링을 시작합니다.");
    }

    private void stopMonitoring() {
        monitoring.set(false);
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        appendLog("모니터링을 중지합니다.");
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WifiSecurityGUI gui = new WifiSecurityGUI();
            gui.setVisible(true);
        });
    }
} 