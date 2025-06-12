package com.wifi.security;

import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.nio.file.*;
import java.util.Properties;

public class WifiSecurityChecker {
    private static String discordWebhookUrl;
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String CONFIG_FILE = "config.properties";

    public static void setDiscordWebhookUrl(String url) {
        discordWebhookUrl = url;
    }

    public static void main(String[] args) {
        try {
            if (!isWindows()) {
                System.out.println("이 프로그램은 Windows 환경에서만 실행 가능합니다.");
                return;
            }

            // 웹훅 URL 로드 또는 입력 받기
            loadOrRequestWebhookUrl();

            String wifiInfo = getWifiInfo();
            if (wifiInfo.isEmpty()) {
                System.out.println("WiFi 정보를 가져올 수 없습니다. 관리자 권한으로 실행해보세요.");
                return;
            }

            String ssid = extractSSID(wifiInfo);
            String securityType = analyzeSecurityType(wifiInfo);
            String riskLevel = assessRiskLevel(securityType);
            
            System.out.println("현재 WiFi : " + ssid);
            System.out.println("보안 유형: " + securityType);
            System.out.println("위험도: " + riskLevel);
            
            if (!riskLevel.equals("LOW")) {
                sendDiscordAlert(wifiInfo, securityType, riskLevel);
                System.out.println("디스코드로 경고 메시지를 전송했습니다.(위헙합니다.)");
            }else if(!riskLevel.equals("HIGH")){
                sendDiscordAlert(wifiInfo, securityType, riskLevel);
                System.out.println("디스코드로 경고 메시지를 전송했습니다.(안전합니다.)");
            }else{
                System.out.println("디스코드로 경고 메시지를 전송했습니다.(주의가 필요합니다.)");
            }

        } catch (Exception e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public static String getWifiInfo() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "netsh", "wlan", "show", "interfaces");
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("WiFi 정보를 가져오는데 실패했습니다. (종료 코드: " + exitCode + ")");
        }
        
        return output.toString();
    }

    public static String analyzeSecurityType(String wifiInfo) {
        if (wifiInfo.contains("WPA2")) {
            return "WPA2";
        } else if (wifiInfo.contains("WPA")) {
            return "WPA";
        } else if (wifiInfo.contains("WEP")) {
            return "WEP";
        } else {
            return "OPEN";
        }
    }

    public static String assessRiskLevel(String securityType) {
        switch (securityType) {
            case "WPA2":
                return "LOW";
            case "WPA":
                return "MEDIUM";
            case "WEP":
            case "OPEN":
                return "HIGH";
            default:
                return "UNKNOWN";
        }
    }

    public static void sendDiscordAlert(String wifiInfo, String securityType, String riskLevel) throws Exception {
        String ssid = extractSSID(wifiInfo);
        String message = "";
        if(riskLevel.equals("HIGH")){
            message = String.format(
                "⚠️ WiFi 보안 경고 ⚠️\n" +
                "네트워크: %s\n" +
                "보안 유형: %s\n" +
                "위험도: %s\n" +
                "이 네트워크는 보안이 취약할 수 있습니다. 민감한 정보 전송을 피하세요.",
                ssid, securityType, riskLevel
            );}else if(riskLevel.equals("LOW")){
                message = String.format(
                "🎉 WiFi 보안 경고 🎉\n" +
                "네트워크: %s\n" +
                "보안 유형: %s\n" +
                "위험도: %s\n" +
                "이 네트워크는 안전합니다. 즐거운 하루 되세요.",
                ssid, securityType, riskLevel
            );}else{
                message = String.format(
                "🫤 WiFi 보안 경고 🫤\n" +
                "네트워크: %s\n" +
                "보안 유형: %s\n" +
                "위험도: %s\n" +
                "이 네트워크는 주의가 필요합니다. 조심하세요.",
                ssid, securityType, riskLevel
            );}

        String json = objectMapper.writeValueAsString(new DiscordMessage(message));
        
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        
        Request request = new Request.Builder()
            .url(discordWebhookUrl)
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Failed to send Discord message: " + response);
            }
        }
    }

    public static String extractSSID(String wifiInfo) {
        Pattern pattern = Pattern.compile("SSID\\s+:\\s+(.+)");
        Matcher matcher = pattern.matcher(wifiInfo);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Unknown";
    }

    public static boolean isValidWebhookUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return url.matches("^https://discord\\.com/api/webhooks/\\d+/[\\w-]+$");
    }

    private static void loadOrRequestWebhookUrl() {
        try {
            // 설정 파일에서 웹훅 URL 로드
            if (Files.exists(Paths.get(CONFIG_FILE))) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                    props.load(fis);
                    discordWebhookUrl = props.getProperty("webhook.url");
                }
            }

            // 웹훅 URL이 없거나 유효하지 않은 경우 새로 입력 받기
            if (discordWebhookUrl == null || !isValidWebhookUrl(discordWebhookUrl)) {
                Scanner scanner = new Scanner(System.in);
                System.out.println("디스코드 웹훅 URL을 입력하세요:");
                discordWebhookUrl = scanner.nextLine().trim();
                scanner.close();
                
                if (!isValidWebhookUrl(discordWebhookUrl)) {
                    System.out.println("올바른 디스코드 웹훅 URL을 입력해주세요.");
                    System.exit(1);
                }

                // 입력받은 웹훅 URL 저장
                 saveWebhookUrl();
            }
        } catch (IOException e) {
            System.err.println("설정 파일 처리 중 오류 발생: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void saveWebhookUrl() throws IOException {
        Properties props = new Properties();
        props.setProperty("webhook.url", discordWebhookUrl);
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "Discord Webhook Configuration");
        }
    }

    static class DiscordMessage {
        private String content;

        public DiscordMessage(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
} 