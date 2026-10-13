BEGIN;

CREATE SEQUENCE rl_coordinates_seq AS bigint MINVALUE 1;
CREATE SEQUENCE rl_location_seq AS bigint MINVALUE 1;
CREATE SEQUENCE rl_route_seq AS bigint MINVALUE 1;
CREATE SEQUENCE rl_account_seq AS bigint MINVALUE 1;

CREATE TABLE rl_coordinates (
    id bigint PRIMARY KEY DEFAULT nextval('rl_coordinates_seq') CHECK (id > 0),
    x real NOT NULL CHECK (x > '-Infinity'::real AND x < 'Infinity'::real),
    y real NOT NULL CHECK (y > '-Infinity'::real AND y < 'Infinity'::real),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);

CREATE TABLE rl_location (
    id bigint PRIMARY KEY DEFAULT nextval('rl_location_seq') CHECK (id > 0),
    x real NOT NULL CHECK (x > '-Infinity'::real AND x < 'Infinity'::real),
    y real NOT NULL CHECK (y > '-Infinity'::real AND y < 'Infinity'::real),
    z integer NOT NULL,
    name text NOT NULL CHECK (name ~ '[^[:space:]]'),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);

CREATE TABLE rl_route (
    id bigint PRIMARY KEY DEFAULT nextval('rl_route_seq') CHECK (id > 0),
    name text NOT NULL CHECK (name ~ '[^[:space:]]'),
    coordinates_id bigint NOT NULL REFERENCES rl_coordinates(id) ON DELETE RESTRICT,
    creation_date timestamp NOT NULL DEFAULT timezone('UTC', CURRENT_TIMESTAMP),
    from_id bigint REFERENCES rl_location(id) ON DELETE RESTRICT,
    to_id bigint NOT NULL REFERENCES rl_location(id) ON DELETE RESTRICT,
    distance real NOT NULL CHECK (distance > 1 AND distance < 'Infinity'::real),
    rating real NOT NULL CHECK (rating > 0 AND rating < 'Infinity'::real),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);

CREATE INDEX rl_route_coordinates_idx ON rl_route(coordinates_id);
CREATE INDEX rl_route_from_to_idx ON rl_route(from_id, to_id);
CREATE INDEX rl_route_to_idx ON rl_route(to_id);
CREATE INDEX rl_route_name_idx ON rl_route(name);
CREATE INDEX rl_route_rating_idx ON rl_route(rating);

CREATE TABLE rl_account (
    id bigint PRIMARY KEY DEFAULT nextval('rl_account_seq') CHECK (id > 0),
    username varchar(32) NOT NULL UNIQUE CHECK (username ~ '^[a-zA-Z0-9_]{3,32}$'),
    password_hash varchar(255) NOT NULL
);

CREATE TABLE rl_state (
    id integer PRIMARY KEY CHECK (id = 1),
    revision bigint NOT NULL CHECK (revision >= 0)
);

INSERT INTO rl_state VALUES (1, 0);

COMMIT;
