-- STEP001
BEGIN
    INSERT INTO DM.`서비스1`
    SELECT *
    FROM DW.RED_CARE_SALES
    WHERE `파티션일자` = CURRENT_DATE();
END;

-- STEP002
BEGIN
    MERGE INTO DM.`마스터가입자1` AS T
    USING DW.`서비스멤버1` AS S
    ON T.id = S.id
    WHEN MATCHED THEN UPDATE SET T.name = S.name;
END;

-- STEP003
BEGIN
    UPDATE DM.`서비스1`
    SET status = 'ACTIVE'
    WHERE EXISTS (
        SELECT 1 FROM DW.`마스터가입자1`
        WHERE DW.`마스터가입자1`.id = DM.`서비스1`.id
    );
END;

