CREATE SEQUENCE rl_import_seq AS bigint MINVALUE 1;

CREATE TABLE rl_import (
    id bigint PRIMARY KEY DEFAULT nextval('rl_import_seq') CHECK (id > 0),
    username varchar(32) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED')),
    added_count integer CHECK (added_count >= 0),
    message text,
    created_at timestamp NOT NULL DEFAULT timezone('UTC', CURRENT_TIMESTAMP),
    CHECK ((status = 'SUCCESS') = (added_count IS NOT NULL))
);

CREATE INDEX rl_import_username_idx ON rl_import(username, id);
