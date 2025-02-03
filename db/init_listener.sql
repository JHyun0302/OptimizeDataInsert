-- 리스너의 LOCAL_LISTENER 설정
ALTER SYSTEM SET LOCAL_LISTENER='(ADDRESS=(PROTOCOL=TCP)(HOST=0.0.0.0)(PORT=1521))';

-- 데이터베이스 리스너 다시 등록
ALTER SYSTEM REGISTER;

-- 리스너 상태 확인용 출력
SELECT * FROM v$listener_network;