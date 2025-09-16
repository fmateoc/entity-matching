SELECT *, 'MAIN' as record_type, NULL as parent_customer_id
                    FROM entities WHERE
                    email_domain = ? OR
                    LOWER(full_name) LIKE ? OR
                    LOWER(ultimate_parent) LIKE ?
