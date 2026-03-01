CREATE TABLE course_classes (
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    course_id   UUID         NOT NULL REFERENCES courses(id),
    code        VARCHAR(50)  NOT NULL UNIQUE,
    semester    VARCHAR(50)  NOT NULL,
    teacher_id  UUID         REFERENCES users(id) ON DELETE SET NULL,
    join_code   VARCHAR(7)   NOT NULL UNIQUE,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Covering Index (INCLUDE) để hỗ trợ Index-Only Scans
CREATE INDEX idx_course_classes_course_id ON course_classes(course_id) INCLUDE (is_deleted);
CREATE INDEX idx_course_classes_teacher_id ON course_classes(teacher_id) INCLUDE (is_deleted);
