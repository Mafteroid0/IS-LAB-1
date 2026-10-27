CREATE OR REPLACE FUNCTION rl_average_rating()
RETURNS double precision
LANGUAGE sql STABLE
SET search_path FROM CURRENT
AS $$
    SELECT avg(rating::double precision) FROM rl_route
$$;

CREATE OR REPLACE FUNCTION rl_count_rating_less(p_rating real)
RETURNS bigint
LANGUAGE sql STABLE
SET search_path FROM CURRENT
AS $$
    SELECT count(*) FROM rl_route WHERE rating < p_rating
$$;

CREATE OR REPLACE FUNCTION rl_routes_name_contains(p_part text)
RETURNS SETOF rl_route
LANGUAGE sql STABLE
SET search_path FROM CURRENT
AS $$
    SELECT * FROM rl_route WHERE strpos(name, p_part) > 0 ORDER BY id
$$;

CREATE OR REPLACE FUNCTION rl_routes_between(p_from_id bigint, p_to_id bigint, p_sort text, p_desc boolean)
RETURNS SETOF rl_route
LANGUAGE plpgsql STABLE
SET search_path FROM CURRENT
AS $$
DECLARE
    sort_column text;
BEGIN
    sort_column := CASE p_sort
        WHEN 'id' THEN 'id'
        WHEN 'name' THEN 'name'
        WHEN 'creationDate' THEN 'creation_date'
        WHEN 'distance' THEN 'distance'
        WHEN 'rating' THEN 'rating'
        ELSE NULL
    END;
    IF sort_column IS NULL OR p_desc IS NULL OR p_from_id IS NULL OR p_to_id IS NULL THEN
        RAISE EXCEPTION 'Укажите две локации, допустимую сортировку и направление' USING ERRCODE = '22023';
    END IF;
    RETURN QUERY EXECUTE format(
        'SELECT r.* FROM rl_route r WHERE r.from_id = $1 AND r.to_id = $2 ORDER BY r.%I %s, r.id ASC',
        sort_column, CASE WHEN p_desc THEN 'DESC' ELSE 'ASC' END
    ) USING p_from_id, p_to_id;
END
$$;

CREATE OR REPLACE FUNCTION rl_add_between(
    p_name text,
    p_coordinates_id bigint,
    p_from_id bigint,
    p_to_id bigint,
    p_distance real,
    p_rating real
)
RETURNS rl_route
LANGUAGE plpgsql VOLATILE
SET search_path FROM CURRENT
AS $$
DECLARE
    result rl_route;
BEGIN
    IF p_from_id IS NULL THEN
        RAISE EXCEPTION 'Для этой операции укажите локацию отправления' USING ERRCODE = '22023';
    END IF;
    INSERT INTO rl_route(name, coordinates_id, from_id, to_id, distance, rating)
    VALUES (p_name, p_coordinates_id, p_from_id, p_to_id, p_distance, p_rating)
    RETURNING * INTO result;
    RETURN result;
END
$$;
