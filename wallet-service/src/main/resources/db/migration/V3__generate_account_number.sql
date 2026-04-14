CREATE OR REPLACE FUNCTION sp_generate_ac_number(
    p_account_id     INT,
    p_account_prefix VARCHAR(2),
    p_exp_length     INT,
    p_julian         BOOLEAN
) RETURNS VARCHAR(50) AS $$
DECLARE
v_partial_no   VARCHAR(50);
    v_year_days    INT;
    v_diff_length  INT;
    v_account_number VARCHAR(50);
BEGIN
    IF p_julian THEN
        -- PostgreSQL equivalent of TO_DAYS(NOW()) + 1721060
        v_year_days := EXTRACT(EPOCH FROM NOW())::INT / 86400 + 2440588;
ELSE
        v_year_days := 0;
END IF;

    v_partial_no  := p_account_prefix || v_year_days::TEXT;
    v_diff_length := p_exp_length - (LENGTH(v_partial_no) + LENGTH(p_account_id::TEXT));

    IF v_diff_length > 0 THEN
        v_account_number := v_partial_no || REPEAT('0', v_diff_length) || p_account_id::TEXT;
ELSE
        v_account_number := v_partial_no || p_account_id::TEXT;
END IF;

RETURN v_account_number;
END;
$$ LANGUAGE plpgsql;
