-- 일별과 월별 테이블 테스트
BEGIN
    -- 일별 테이블 (파티션일자 사용)
    INSERT INTO DM.`일별매출현황`
    SELECT * FROM DW.`일별거래내역`;

    -- 월별 테이블 (기준일자 사용)
    INSERT INTO DM.`월별매출현황`
    SELECT * FROM DW.`월별거래내역`;

    -- 일이 포함된 테이블 (파티션일자 사용)
    INSERT INTO DM.`카드발급일별현황`
    SELECT * FROM DW.`카드발급일별`;

    -- 일이 없는 테이블 (기준일자 사용)
    INSERT INTO DM.`서비스현황`
    SELECT * FROM DW.`서비스마스터`;
END;

