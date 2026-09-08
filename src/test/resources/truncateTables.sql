begin
    for r in (select table_name from user_tables where table_name not in ('flyway_schema_history')) loop
        execute immediate 'truncate table ' || r.table_name;
    end loop;
end;