CREATE TABLE courses (
    id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    credits     INTEGER      NOT NULL,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE course_classes (
    id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    course_id   UUID         NOT NULL REFERENCES courses(id),
    code        VARCHAR(50)  NOT NULL UNIQUE,
    semester    VARCHAR(50)  NOT NULL,
    teacher_id  UUID,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_course_classes_course_id ON course_classes(course_id);

CREATE INDEX idx_course_classes_teacher_id ON course_classes(teacher_id);
