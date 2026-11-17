-- Fresh database only: reproduce the original application's persisted entities.
-- Existing (pre-Flyway) databases need an explicit, reviewed baseline at version 1.
-- Version 2 applies new ordering tables and inventory fields.
CREATE TABLE roles (
    id INTEGER NOT NULL AUTO_INCREMENT,
    name VARCHAR(20),
    PRIMARY KEY (id)
);
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(20) NOT NULL,
    email VARCHAR(50) NOT NULL,
    password VARCHAR(120) NOT NULL,
    fullname VARCHAR(255),
    orgname VARCHAR(255),
    empno BIGINT NOT NULL,
    mobileno BIGINT NOT NULL,
    date DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id INTEGER NOT NULL,
    PRIMARY KEY (user_id,role_id),
    CONSTRAINT fk_user_roles_users FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_roles FOREIGN KEY (role_id) REFERENCES roles (id)
);
CREATE TABLE food_menu (
    id VARCHAR(255) NOT NULL,
    foodname VARCHAR(255),
    rates INTEGER NOT NULL,
    time VARCHAR(255),
    description VARCHAR(255),
    category VARCHAR(255),
    name VARCHAR(255),
    type VARCHAR(255),
    data LONGBLOB,
    PRIMARY KEY (id)
);
CREATE TABLE files (
    id VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    type VARCHAR(255),
    data LONGBLOB,
    PRIMARY KEY (id)
);
CREATE TABLE user_pic (
    id BIGINT NOT NULL,
    pic_id VARCHAR(255),
    PRIMARY KEY (id)
);
