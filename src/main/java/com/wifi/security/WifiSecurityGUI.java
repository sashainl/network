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
    private JLabel statusLabel;
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
        JButton helpButton = new JButton("?");
        helpButton.setToolTipText("웹훅 설정 방법 보기");
        
        JPanel webhookButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        webhookButtonPanel.add(helpButton);
        webhookButtonPanel.add(saveButton);
        
        topPanel.add(new JLabel("디스코드 웹훅 URL: "), BorderLayout.WEST);
        topPanel.add(webhookField, BorderLayout.CENTER);
        topPanel.add(webhookButtonPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Center panel for WiFi info
        JPanel centerPanel = new JPanel(new GridLayout(4, 1));
        wifiLabel = new JLabel("현재 WiFi: -");
        securityLabel = new JLabel("보안 유형: -");
        riskLabel = new JLabel("위험도: -");
        statusLabel = new JLabel("상태: 대기 중");
        centerPanel.add(wifiLabel);
        centerPanel.add(securityLabel);
        centerPanel.add(riskLabel);
        centerPanel.add(statusLabel);
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
        helpButton.addActionListener(e -> showWebhookHelp());
    }

    private void showWebhookHelp() {
        JDialog helpDialog = new JDialog(this, "웹훅 설정 방법", true);
        helpDialog.setLayout(new BorderLayout());
        helpDialog.setSize(500, 400);
        helpDialog.setLocationRelativeTo(this);

        JTextArea helpText = new JTextArea();
        helpText.setEditable(false);
        helpText.setLineWrap(true);
        helpText.setWrapStyleWord(true);
        helpText.setText(
            "‼️개인 서버를 추가해야합니다‼️\n" +
            "1. 디스코드 서버 설정\n" +
            "   - 디스코드 서버에서 알림을 받고 싶은 채널을 선택합니다.\n" +
            "   - 채널 설정(⚙️)을 클릭합니다.\n" +
            "   - '연동' 또는 'Integrations' 메뉴를 선택합니다.\n" +
            "   - '웹후크' 또는 'Webhooks'를 클릭합니다.\n" +
            "   - '새 웹후크' 또는 'New Webhook'을 클릭합니다.\n\n" +
            "2. 웹훅 생성\n" +
            "   - 웹훅 이름을 입력합니다 (예: 'WiFi 보안 알림').\n" +
            "   - '웹후크 URL 복사' 또는 'Copy Webhook URL'을 클릭합니다.\n" +
            "   - 복사된 URL을 이 프로그램의 웹훅 URL 입력란에 붙여넣습니다.\n\n" +
            "3. 저장 및 확인\n" +
            "   - '저장' 버튼을 클릭하여 웹훅 URL을 저장합니다.\n" +
            "   - '모니터링 시작' 버튼을 클릭하여 알림이 정상적으로 전송되는지 확인합니다.\n\n" +
            "주의사항:\n" +
            "- 웹훅 URL은 비밀번호와 같은 중요한 정보이므로 다른 사람과 공유하지 마세요.\n" +
            "- 웹훅 URL이 노출된 경우, 디스코드에서 해당 웹훅을 삭제하고 새로운 웹훅을 생성하세요."
        );

        JScrollPane scrollPane = new JScrollPane(helpText);
        helpDialog.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("닫기");
        closeButton.addActionListener(e -> helpDialog.dispose());
        helpDialog.add(closeButton, BorderLayout.SOUTH);

        helpDialog.setVisible(true);
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
                    if (WifiSecurityChecker.isNetworkChanged()) {
                        String wifiInfo = WifiSecurityChecker.getWifiInfo();
                        if (wifiInfo.isEmpty()) {
                            appendLog("WiFi 정보를 가져올 수 없습니다.");
                            TimeUnit.SECONDS.sleep(5);
                            continue;
                        }
                        String ssid = WifiSecurityChecker.extractSSID(wifiInfo);
                        String securityType = WifiSecurityChecker.analyzeSecurityType(wifiInfo);
                        String riskLevel = WifiSecurityChecker.assessRiskLevel(securityType);
                        
                        SwingUtilities.invokeLater(() -> {
                            wifiLabel.setText("현재 WiFi: " + ssid);
                            securityLabel.setText("보안 유형: " + securityType);
                            riskLabel.setText("위험도: " + riskLevel);
                            statusLabel.setText("상태: 모니터링 중");
                        });

                        if (!ssid.equals(lastSSID)) {
                            appendLog("WiFi 감지: " + ssid);
                            
                            try {
                                WifiSecurityChecker.sendDiscordAlert(wifiInfo, securityType, riskLevel);
                                appendLog("디스코드로 알림 전송 완료");
                            } catch (Exception ex) {
                                appendLog("디스코드 전송 오류: " + ex.getMessage());
                            }
                            lastSSID = ssid;
                        }
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("상태: WiFi 연결 없음");
                        });
                    }
                    TimeUnit.SECONDS.sleep(5);
                } catch (Exception ex) {
                    appendLog("오류: " + ex.getMessage());
                    try { TimeUnit.SECONDS.sleep(5); } catch (InterruptedException ignored) {}
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
        statusLabel.setText("상태: 대기 중");
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