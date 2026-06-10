CREATE DATABASE IF NOT EXISTS smart_code_note DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_code_note;

CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(64),
    email VARCHAR(128),
    avatar VARCHAR(255),
    openid VARCHAR(128),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_app_user_username (username),
    UNIQUE KEY uk_app_user_email (email),
    UNIQUE KEY uk_app_user_openid (openid)
);

CREATE TABLE IF NOT EXISTS note (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    category VARCHAR(64),
    tags VARCHAR(255),
    file_url VARCHAR(255),
    file_type VARCHAR(16),
    original_content LONGTEXT,
    clean_content LONGTEXT,
    parse_status TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_note_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS knowledge_point (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    note_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    type VARCHAR(32),
    summary TEXT,
    difficulty VARCHAR(32),
    mastery_level INT DEFAULT 0,
    next_review_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_knowledge_user_id (user_id),
    INDEX idx_knowledge_note_id (note_id)
);

CREATE TABLE IF NOT EXISTS question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    note_id BIGINT,
    knowledge_id BIGINT NOT NULL,
    question_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    standard_answer TEXT,
    analysis TEXT,
    difficulty VARCHAR(32),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_question_user_id (user_id),
    INDEX idx_question_knowledge_id (knowledge_id)
);

CREATE TABLE IF NOT EXISTS question_option (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    option_key VARCHAR(8) NOT NULL,
    option_content TEXT NOT NULL,
    is_correct TINYINT DEFAULT 0,
    INDEX idx_question_option_question_id (question_id)
);

CREATE TABLE IF NOT EXISTS answer_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    user_answer TEXT,
    score INT DEFAULT 0,
    is_correct TINYINT DEFAULT 0,
    ai_comment TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_answer_user_id (user_id),
    INDEX idx_answer_question_id (question_id)
);

CREATE TABLE IF NOT EXISTS wrong_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    wrong_count INT DEFAULT 1,
    last_wrong_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    mastered TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_wrong_user_question (user_id, question_id),
    INDEX idx_wrong_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS chat_session (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    title       VARCHAR(128) DEFAULT '新对话',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0,
    INDEX idx_chat_session_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS chat_message (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id  BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    role        VARCHAR(16) NOT NULL COMMENT 'user/assistant',
    content     TEXT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0,
    INDEX idx_chat_message_session_id (session_id),
    INDEX idx_chat_message_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS review_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    knowledge_id BIGINT NOT NULL,
    question_id BIGINT,
    review_result VARCHAR(32),
    score INT DEFAULT 0,
    next_review_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_review_user_id (user_id),
    INDEX idx_review_knowledge_id (knowledge_id)
);

CREATE TABLE IF NOT EXISTS note_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    note_id BIGINT NOT NULL,
    knowledge_id BIGINT,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding MEDIUMBLOB NOT NULL,
    token_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_note_chunk_user_id (user_id),
    INDEX idx_note_chunk_note_id (note_id)
);
