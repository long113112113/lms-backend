CREATE TABLE enrollments (
    id              UUID PRIMARY KEY DEFAULT uuidv7(),
    student_id      UUID         NOT NULL REFERENCES users(id),
    course_class_id UUID         NOT NULL REFERENCES course_classes(id),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (student_id, course_class_id)
);

-- Covering Index: tối ưu query enrollment kèm status
CREATE INDEX idx_enrollments_course_class_id ON enrollments(course_class_id) INCLUDE (status);
