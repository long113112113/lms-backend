CREATE TABLE users (
    id         UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    email      VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    full_name  VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE course_classes
ADD CONSTRAINT fk_course_classes_teacher
FOREIGN KEY (teacher_id) REFERENCES users(id);