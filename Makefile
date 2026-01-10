# 로컬 개발용 DB 실행
db-up:
	docker-compose -f docker-compose.local.yml -p dev-db up -d

# 로컬 개발용 DB 중지 및 삭제
db-down:
	docker-compose -f docker-compose.local.yml -p dev-db down

# 로컬 개발용 DB 로그 확인
db-logs:
	docker-compose -f docker-compose.local.yml -p dev-db logs -f

# 로컬 개발용 DB 볼륨까지 완전히 삭제 (데이터 초기화 필요할 때)
db-clean:
	docker-compose -f docker-compose.local.yml -p dev-db down -v