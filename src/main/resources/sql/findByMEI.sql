SELECT e.*, 'MAIN' as record_type, NULL as parent_customer_id
            FROM entities e WHERE e.mei = ?
            UNION ALL
            SELECT e.*, 'LOCATION' as record_type, l.parent_customer_id
            FROM entity_locations l
            JOIN entities e ON l.location_id = e.entity_id
            WHERE l.mei = ?
