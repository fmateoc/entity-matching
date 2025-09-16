SELECT *, 'MAIN' as record_type, NULL as parent_customer_id
           FROM entities WHERE entity_id = ?
