CREATE TABLE enrollments (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id      UUID         NOT NULL REFERENCES users(id),
    course_class_id UUID         NOT NULL REFERENCES course_classes(id),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (student_id, course_class_id)
);

CREATE INDEX idx_enrollments_student_id ON enrollments(student_id);
CREATE INDEX idx_enrollments_course_class_id ON enrollments(course_class_id);
CREATE INDEX idx_enrollments_status ON enrollments(status);
