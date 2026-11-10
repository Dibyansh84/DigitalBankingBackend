package com.digitalbank.customer.repository;
import com.digitalbank.customer.entity.Customer;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class CustomerJdbcRepository
{
    private final NamedParameterJdbcTemplate jdbcTemplate;

    // Spring injects the template automatically through the constructor.
    public CustomerJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate)
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    // RowMapper converts one row of the ResultSet into one Customer object.
    // Spring calls this lambda once for each row, passing the row's data as 'rs'
    // and the row's position as 'rowNum'.
    private final RowMapper<Customer> customerRowMapper
            = (rs, rowNum)->{
                        Customer customer = new Customer();
                        customer.setId(rs.getLong("id")); // Read the "id" column
                        customer.setFullName(rs.getString("full_name"));  // Read the "full_name" column
                        customer.setEmail(rs.getString("email"));// Read the "email" column
                        customer.setPhone(rs.getString("phone"));// Read the "phone" column


        return customer;   // Return the populated object
            };

    // Returns every customer in the table, sorted by id.
    public List<Customer> findAll()
    {
        String sql = " SELECT id, full_name, email, phone\n" +
                " FROM public.customers\n" +
                " ORDER BY id ";

        // An empty parameter source. It holds no values because the SQL has no placeholders.
    //It’s an empty map of named values, and when the SQL has no :name placeholders, there’s nothing to look up in it.
//        MapSqlParameterSource params = new MapSqlParameterSource();
//
//        return jdbcTemplate.query(sql, params, customerRowMapper);
        //Since the code has no parameters, you can drop the empty object of MapSqlParameterSource.

        return jdbcTemplate.query(sql, customerRowMapper);
    }


    public Customer findById(Long customerId)
    {
        String sql = " SELECT id, full_name, email, phone\n" +
                " FROM public.customers " +
                " WHERE id = :customerId ";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("customerId", customerId);

        //Hum yahan query() use kar rahe hain, kyunki agar customer ID exist nahi karti, toh empty list milegi.
        List<Customer> customers = jdbcTemplate.query(sql, params, customerRowMapper);

        return customers.isEmpty() ? null : customers.get(0);
    }

    public List<Customer> findPage(int page, int size, String sortBy, String sortDirection)
    {

        // Reject negative page numbers, since page indexes start at 0
        if(page < 0)
        {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        // Limit page size to 1–100 to prevent overly large queries
        if(size < 1 || size > 100)
        {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        // Whitelist of allowed sort fields: maps the API field name (camelCase)
        // to the actual database column name (snake_case).
        // Only these columns can be used in ORDER BY, which prevents SQL injection.
        //allowedColumns sirf approved database columns ko sorting ke liye allow karta hai.
        Map<String, String> allowedColumns = Map.of("id", "id", "fullName", "full_name",
                "email","email", "createdAt", "created_at");

        // Look up the database column for the requested sort field.
        String sortColumn = allowedColumns.get(sortBy);

        // If the field isn't in the whitelist, reject the request
        if(sortColumn == null)
        {
            throw new IllegalArgumentException("Invalid sort field: "+sortBy);
        }

        //Ensure the direction is exactly ASC or DESC(case-insensitive).
        //Null-Check first so equalsIgnoreCase doesn't throw a NullPointerException
//        if(sortDirection == null || sortDirection.equalsIgnoreCase("ASC")
//        || sortDirection.equalsIgnoreCase("DESC"))
//        {
//            throw new IllegalArgumentException("Sort direction must be ASC or DESC");
//        }

        //Normalize the direction to uppercase for the SQL query.
//        String direction = sortDirection.toUpperCase();

        //Updated logic
        String direction = "ASC";
        if(sortDirection !=null && !sortDirection.isBlank())
        {
            String normalizedDirection = sortDirection.trim().toUpperCase(java.util.Locale.ROOT);

            if(!normalizedDirection.equals("ASC") && !normalizedDirection.equals("DESC"))
            {
                throw new IllegalArgumentException("Sort direction must be ASC or DESC");
            }
            direction = normalizedDirection;
        }
        //Ends here

        //Calculate the number of rows to skip
        //Cast to long first to avoid integer overflow on large numbers.\
        long offset = (long)page * size;

        //Build the query.  The column and direction come from validated values above,
        //so it's safe to insert them directly. LIMIT and OFFSET use bound parameters.
        String sql = """
                SELECT id, full_name, email, phone
                FROM public.customers
                ORDER BY %s %s
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, direction);

        //Bind the pagination values as named parameters
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("limit", size);
        params.addValue("offset", offset);

        //Execute the query and map each row to a Customer object.
        return jdbcTemplate.query(sql, params, customerRowMapper);
    }

    //Add total-count method
    public long countAll()
    {
        // Count every row in the customers table.
        //COUNT(*) includes all rows, even those with NULL values in some columns.
        String sql = " SELECT COUNT(*) FROM public.customers ";

        //Run the query and return the single value as a long.
        // queryForObject expects only one row with one column, which COUNT(*) always returns.
        // MapSqlParameterSource is empty because this query has no parameters.
        // Long.class tells Spring to convert the result to a long.
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
    }

    public int updateCustomer(Long customerId, String fullName, String email, String phone)
    {
        String sql = """
                UPDATE public.customers
                SET full_name = :fullName, email = :email,
                phone = :phone
                WHERE id = :customerId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("customerId", customerId);
        params.addValue("fullName", fullName);
        params.addValue("email", email);
        params.addValue("phone", phone);

        return jdbcTemplate.update(sql, params);
    }


    public int deleteCustomer(Long customerId)
    {
        String sql = """
                DELETE FROM public.customers
                WHERE id = :customerId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("customerId", customerId);

        return jdbcTemplate.update(sql, params);

        /*
        * Return value:
    1 — customer delete hua.
    0 — customer ID nahi mili.*/
    }
}
