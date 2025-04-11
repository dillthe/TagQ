CREATE SCHEMA tagQ;
USE tagQ;

CREATE TABLE tag (
    tag_id INT PRIMARY KEY AUTO_INCREMENT,
    tag_name VARCHAR(255) UNIQUE NOT NULL
    );
    
    CREATE TABLE question (
    question_id INT AUTO_INCREMENT PRIMARY KEY,
    question TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE question_tag (
    question_id INT,
    tag_id INT,
    PRIMARY KEY (question_id, tag_id),
    FOREIGN KEY (question_id) REFERENCES question(question_id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(tag_id) ON DELETE CASCADE
);

-- CREATE TABLE subtag (
--     subtag_id INT PRIMARY KEY AUTO_INCREMENT,
--     subtag_name VARCHAR(255) UNIQUE NOT NULL
-- );

-- CREATE TABLE question_subtag (
--     question_id INT,
--     subtag_id INT,
--     PRIMARY KEY (question_id, subtag_id),
--     FOREIGN KEY (question_id) REFERENCES question(question_id) ON DELETE CASCADE,
--     FOREIGN KEY (subtag_id) REFERENCES subtag(subtag_id) ON DELETE CASCADE
-- );

-- CREATE TABLE tag_subtag (
--     tag_id INT,
--     subtag_id INT,
--     PRIMARY KEY (tag_id, subtag_id),
--     FOREIGN KEY (tag_id) REFERENCES tag(tag_id) ON DELETE CASCADE,
--     FOREIGN KEY (subtag_id) REFERENCES subtag(subtag_id) ON DELETE CASCADE
-- );