SELECT *, 'MAIN' as record_type, NULL as parent_customer_id
             FROM entities WHERE
             LOWER(full_name) LIKE ? OR
             LOWER(short_name) LIKE ? OR
             LOWER(ultimate_parent) LIKE ?
             ORDER BY CASE
               WHEN LOWER(full_name) = LOWER(?) THEN 1
               WHEN LOWER(short_name) = LOWER(?) THEN 2
               ELSE 3 END
             LIMIT 100
