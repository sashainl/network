package com.wifi.security;

import java.io.*;
// import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.Properties;
import java.nio.file.*;

public class WifiSecurityService {
    private static final String CONFIG_FILE = "config.properties";
    private static String lastSSID = null;
    private static boolean isRunning = true;
    private static String discordWebhookUrl;

    public static void main(String[] args) {
        try {
            if (!isWindows()) {
                System.out.println("이 프로그램은 Windows 환경에서만 실행 가능합니다.");
                return;
            }

            // 웹훅 URL 로드
            loadWebhookUrl();
            if (discordWebhookUrl == null || !WifiSecurityChecker.isValidWebhookUrl(discordWebhookUrl)) {
                System.out.println("유효한 웹훅 URL이 설정되어 있지 않습니다. 프로그램을 먼저 실행하여 웹훅 URL을 설정해주세요.");
                return;
            }

            // WifiSecurityChecker에 웹훅 URL 설정
            WifiSecurityChecker.setDiscordWebhookUrl(discordWebhookUrl);

            System.out.println("WiFi 보안 모니터링 서비스를 시작합니다.");
            
            // 네트워크 모니터링 시작
            while (isRunning) {
                checkWifiSecurity();
                TimeUnit.SECONDS.sleep(10); // 10초마다 체크
            }

        } catch (Exception e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void checkWifiSecurity() {
        try {
            String wifiInfo = WifiSecurityChecker.getWifiInfo();
            if (wifiInfo.isEmpty()) {
                System.out.println("WiFi 정보를 가져올 수 없습니다.");
                return;
            }

            String ssid = WifiSecurityChecker.extractSSID(wifiInfo);
            
            // SSID가 변경된 경우에만 알림 전송
            if (!ssid.equals(lastSSID)) {
                String securityType = WifiSecurityChecker.analyzeSecurityType(wifiInfo);
                String riskLevel = WifiSecurityChecker.assessRiskLevel(securityType);
                
                System.out.println("새로운 WiFi 감지: " + ssid);
                System.out.println("보안 유형: " + securityType);
                System.out.println("위험도: " + riskLevel);
                
                WifiSecurityChecker.sendDiscordAlert(wifiInfo, securityType, riskLevel);
                lastSSID = ssid;
            }
        } catch (Exception e) {
            System.err.println("WiFi 체크 중 오류 발생: " + e.getMessage());
        }
    }

    private static void loadWebhookUrl() {
        try {
            if (Files.exists(Paths.get(CONFIG_FILE))) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                    props.load(fis);
                    discordWebhookUrl = props.getProperty("webhook.url");
                }
            }
        } catch (IOException e) {
            System.err.println("설정 파일 로드 중 오류 발생: " + e.getMessage());
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public static void shutdown() {
        isRunning = false;
    }
} 