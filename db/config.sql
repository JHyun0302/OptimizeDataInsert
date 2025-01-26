-- SYSTEM 테이블스페이스 정보 확인
SELECT
    ddf.TABLESPACE_NAME,
    ddf.FILE_NAME,
    ddf.BYTES / 1024 / 1024 AS SIZE_MB,
    ddf.MAXBYTES / 1024 / 1024 AS MAX_SIZE_MB,
    (SELECT SUM(dfs.BYTES) / 1024 / 1024
     FROM DBA_FREE_SPACE dfs
     WHERE dfs.TABLESPACE_NAME = ddf.TABLESPACE_NAME) AS FREE_SPACE_MB
FROM DBA_DATA_FILES ddf
WHERE ddf.TABLESPACE_NAME = 'SYSTEM';


-- SYSTEM 테이블스페이스 자동 확장 설정 (oracleinanutshell/oracle-xe-11g)
ALTER DATABASE DATAFILE '/u01/app/oracle/oradata/XE/system.dbf' AUTOEXTEND ON MAXSIZE UNLIMITED;

-- SYSTEM 테이블스페이스 자동 확장 설정 (oracle/database:19.3.0-ee)
      -- Oracle Database 19c Enterprise Edition Release 19.0.0.0.0 - Production
ALTER DATABASE DATAFILE '/opt/oracle/oradata/ORCL/system01.dbf' AUTOEXTEND ON MAXSIZE UNLIMITED;
