SELECT *, 'MAIN' as record_type, NULL as parent_customer_id
                         FROM entities WHERE
                         REGEXP_REPLACE(LOWER(short_name), '[^a-z0-9]', '') = ?
