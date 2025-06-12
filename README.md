# WiFi 보안 체커

[![Java](https://img.shields.io/badge/Java-11-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Windows](https://img.shields.io/badge/Windows-10%2B-blue)](https://www.microsoft.com/windows)

WiFi 네트워크의 보안 상태를 모니터링하고, 보안이 취약한 경우 디스코드를 통해 경고 메시지를 전송하는 Windows 애플리케이션입니다.

## 주요 기능

- 현재 연결된 WiFi 네트워크 정보 확인
- 보안 프로토콜 분석 (WPA2, WPA, WEP, OPEN)
- 위험도 평가 (LOW, MEDIUM, HIGH)
- 디스코드 웹훅을 통한 보안 경고 메시지 전송
- 웹훅 URL 설정 파일 저장 및 자동 로드
- Windows 서비스로 백그라운드 실행
- GUI 인터페이스 제공

## 시스템 요구사항

- Windows 10 이상
- Java 11 이상
- 관리자 권한 (서비스 설치 시)

## 설치 방법

1. [릴리즈 페이지](https://github.com/yourusername/wifi-security-checker/releases)에서 최신 버전을 다운로드합니다.
2. 다운로드한 ZIP 파일을 원하는 위치에 압축 해제합니다.

## 사용 방법

### GUI 모드 실행
```bash
java -jar wifi-security-checker-1.0-SNAPSHOT.jar
```

### Windows 서비스로 설치
1. 관리자 권한으로 명령 프롬프트를 실행합니다.
2. `install-service.bat` 파일을 실행합니다.

## 빌드 방법

1. 저장소를 클론합니다:
```bash
git clone https://github.com/yourusername/wifi-security-checker.git
cd wifi-security-checker
```

2. Maven으로 빌드합니다:
```bash
mvn clean package
```

3. 배포 파일 생성:
```bash
build.bat
```

## 보안 등급 기준

- LOW: WPA2 보안 프로토콜 사용
- MEDIUM: WPA 보안 프로토콜 사용
- HIGH: WEP 또는 보안이 없는 네트워크

## 주의사항

- 이 프로그램은 Windows 환경에서만 실행 가능합니다.
- 서비스 설치 및 관리에는 관리자 권한이 필요합니다.
- 디스코드 웹훅 URL은 절대로 공개하지 마세요.
- `config.properties` 파일에 저장된 웹훅 URL도 안전하게 보관하세요.

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 기여하기

1. 이 저장소를 포크합니다.
2. 새로운 브랜치를 생성합니다 (`git checkout -b feature/amazing-feature`).
3. 변경사항을 커밋합니다 (`git commit -m 'Add some amazing feature'`).
4. 브랜치에 푸시합니다 (`git push origin feature/amazing-feature`).
5. Pull Request를 생성합니다. 