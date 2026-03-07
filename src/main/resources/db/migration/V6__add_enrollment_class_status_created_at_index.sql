CREATE INDEX idx_enrollments_class_status_created_at
    ON enrollments(course_class_id, status, created_at DESC);
