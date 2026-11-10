package com.digitalbank.account.repository;

import com.digitalbank.account.dto.AccountResponseDto;
import com.digitalbank.account.exception.AccountNotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AccountJdbcRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AccountJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AccountResponseDto findById(Long accountId) {
        String sql = """
                SELECT id, account_number, customer_id, account_type,
                balance, account_status, created_at
                FROM public.accounts
                WHERE id = :accountId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("accountId", accountId);
        try {
            return jdbcTemplate.queryForObject(sql, params, new BeanPropertyRowMapper<>(AccountResponseDto.class));
        } catch (EmptyResultDataAccessException e) {
            throw new AccountNotFoundException("Account not found with ID: " + accountId);
        }
    }

    // Fetch paginated accounts
    public List<AccountResponseDto> findAll(int limit, int offset) {
        String sql = """
                SELECT id, account_number, customer_id, account_type, balance,\s
                account_status, created_at
                FROM public.accounts
                ORDER BY id DESC
                LIMIT :limit OFFSET :offset
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("limit", limit);
        params.addValue("offset", offset);

        return jdbcTemplate.query(sql, params, new BeanPropertyRowMapper<>(AccountResponseDto.class));
    }

    //Count total counts
    public long countAll() {
        String sql = """
                SELECT COUNT(*)
                FROM public.accounts
                """;
        Long count = jdbcTemplate.getJdbcTemplate().queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    //We'll write an UPDATE query using NamedParameterJdbcTemplate.
    public int updateAccount(Long accountId, String accountType, String accountStatus) {
        String sql = """
                UPDATE public.accounts
                SET
                	account_type = :accountType,
                	account_status = :accountStatus
                WHERE id = :accountId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("accountId", accountId);
        params.addValue("accountType", accountType);
        params.addValue("accountStatus", accountStatus);

        return jdbcTemplate.update(sql, params);
    }

    /*Note: Important banking design decision: In real banking systems, accounts generally should not be physically deleted because financial records and audit history must be preserved.
 Instead, accounts are closed.
 We'll implement a soft-delete/account closure approach by changing the account status to CLOSED. */

    public int closeAccount(Long accountId)
    {
        String sql = """
                UPDATE public.accounts
                SET account_status = 'CLOSED'
                WHERE id = :accountId
                AND account_status <> 'CLOSED'
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("accountId", accountId);

        return jdbcTemplate.update(sql, params);
    }
}
