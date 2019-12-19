DROP TABLE comment;
DROP TABLE post_tag;
DROP TABLE tag;
DROP TABLE post;
DROP TABLE blog;
DROP TABLE author;
DROP PROCEDURE selectTwoSetsOfAuthors;
DROP PROCEDURE insertAuthor;
DROP PROCEDURE selectAuthorViaOutParams;

CREATE TABLE author (
id                INT NOT NULL GENERATED BY DEFAULT AS IDENTITY (START WITH 10000),
username          VARCHAR(255) NOT NULL,
password          VARCHAR(255) NOT NULL,
email             VARCHAR(255) NOT NULL,
bio               CLOB,
favourite_section VARCHAR(25),
PRIMARY KEY (id)
);

CREATE TABLE blog (
id          INT NOT NULL GENERATED BY DEFAULT AS IDENTITY,
author_id   INT NOT NULL,
title       VARCHAR(255),
PRIMARY KEY (id)
);

CREATE TABLE post (
id          INT NOT NULL GENERATED BY DEFAULT AS IDENTITY,
blog_id     INT,
author_id   INT NOT NULL,
created_on  TIMESTAMP NOT NULL,
section     VARCHAR(25) NOT NULL,
subject     VARCHAR(255) NOT NULL,
body        CLOB NOT NULL,
draft       INT NOT NULL,
PRIMARY KEY (id),
FOREIGN KEY (blog_id) REFERENCES blog(id)
);

CREATE TABLE tag (
id          INT NOT NULL GENERATED BY DEFAULT AS IDENTITY,
name        VARCHAR(255) NOT NULL,
PRIMARY KEY (id)
);

CREATE TABLE post_tag (
post_id     INT NOT NULL,
tag_id      INT NOT NULL,
PRIMARY KEY (post_id, tag_id)
);

CREATE TABLE comment (
id          INT NOT NULL GENERATED BY DEFAULT AS IDENTITY,
post_id     INT NOT NULL,
name        LONG VARCHAR NOT NULL,
comment     LONG VARCHAR NOT NULL,
PRIMARY KEY (id)
);

CREATE TABLE node (
id  INT NOT NULL,
parent_id INT,
PRIMARY KEY(id)
);
