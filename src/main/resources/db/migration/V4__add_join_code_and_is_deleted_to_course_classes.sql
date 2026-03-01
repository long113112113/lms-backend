ALTER TABLE course_classes
    ADD COLUMN join_code VARCHAR(7),
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE course_classes
SET join_code = upper(substr(md5(random()::text), 1, 7))
WHERE join_code IS NULL;

ALTER TABLE course_classes ALTER COLUMN join_code SET NOT NULL;
CREATE UNIQUE INDEX idx_course_classes_join_code ON course_classes(join_code);
