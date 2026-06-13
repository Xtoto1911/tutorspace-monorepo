DO $$
BEGIN
    IF to_regclass('subjects') IS NOT NULL THEN
        ALTER TABLE subjects
            ADD COLUMN IF NOT EXISTS description varchar(1000);

        UPDATE subjects
        SET description = 'Индивидуальные занятия по предмету «'
            || COALESCE(NULLIF(BTRIM(name), ''), 'Без названия')
            || '» с учетом целей и уровня ученика.'
        WHERE description IS NULL OR BTRIM(description) = '';

        ALTER TABLE subjects
            ALTER COLUMN description SET NOT NULL;
    END IF;
END
$$;
