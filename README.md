# 🍞 Yuruppang Backend / 유루빵 백엔드 🍞

베이킹 재고 및 레시피 관리 웹 서비스 **Yuruppang**의 백엔드 레포지터리입니다.  
유루빵은 재고 파악, 레시피 기록, 생산 계획 등을 돕는 홈베이킹 전용 서비스입니다.

**프론트엔드 리포 :** https://github.com/iaminwoo/YuruppangFrontend

## 🛠️ 기술 스택

### 1. 백엔드
언어: Java

프레임워크: Spring Boot

데이터베이스: PostgreSQL

API 설계: RESTful API

보안: Spring Security (JWT 인증 적용)

빌드 도구: Gradle

### 2. 배포 및 인프라
서버 배포: Render

데이터베이스: Neon

도메인: yuruppang.store (구매 및 연결 완료)

## 🧩 주요 기능 요약

- **생산 계획 관리**: 베이킹 플랜 생성, 상세 조회, 레시피 목표 수량 조정 등
- **레시피 관리** : 레시피 추가/수정, 재료 관리, 카테고리별 필터링
- **재고 관리** : 보유 중인 재료의 수량, 단위, 상세 정보 열람
- **기록 관리** : 구매/소비 이력 관리, 개별 기록 수정/삭제
- **홈 화면** : 간단한 요약 및 진입점 제공
