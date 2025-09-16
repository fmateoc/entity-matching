SELECT *, 'MAIN' as record_type, NULL as parent_customer_id
                     FROM entities WHERE debt_domain_id = ?
