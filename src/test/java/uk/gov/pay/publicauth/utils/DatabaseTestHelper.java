package uk.gov.pay.publicauth.utils;

import io.dropwizard.jdbi.args.ZonedDateTimeMapper;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.util.StringColumnMapper;
import uk.gov.pay.publicauth.model.TokenHash;
import uk.gov.pay.publicauth.model.TokenLink;
import uk.gov.pay.publicauth.model.TokenPaymentType;
import uk.gov.pay.publicauth.model.TokenSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import static uk.gov.pay.publicauth.model.TokenPaymentType.CARD;
import static uk.gov.pay.publicauth.model.TokenSource.API;

public class DatabaseTestHelper {

    private final DBI jdbi;

    DatabaseTestHelper(DBI jdbi) {
        this.jdbi = jdbi;
    }

    public void insertAccount(TokenHash tokenHash, TokenLink randomTokenLink, String accountId, String description, String createdBy) {
        insertAccount(tokenHash, randomTokenLink, accountId, description, null, createdBy, ZonedDateTime.now(ZoneOffset.UTC));
    }

    public void insertAccount(TokenHash tokenHash, TokenLink randomTokenLink, String accountId, String description, ZonedDateTime revoked, String createdBy) {
        insertAccount(tokenHash, randomTokenLink, accountId, description, revoked, createdBy, ZonedDateTime.now(ZoneOffset.UTC));
    }

    public void insertAccount(TokenHash tokenHash, TokenLink randomTokenLink, String accountId, String description, ZonedDateTime revoked, String createdBy, ZonedDateTime lastUsed) {
        insertAccount(tokenHash, randomTokenLink, API, accountId, description, revoked, createdBy, lastUsed, CARD);
    }

    public void insertAccount(TokenHash tokenHash, TokenLink randomTokenLink, TokenSource tokenSource, String accountId, String description, ZonedDateTime revoked, String createdBy, ZonedDateTime lastUsed) {
        insertAccount(tokenHash, randomTokenLink, tokenSource, accountId, description, revoked, createdBy, lastUsed, CARD);
    }

    public void insertAccount(TokenHash tokenHash, TokenLink randomTokenLink, TokenSource tokenSource, String accountId, String description, ZonedDateTime revoked, String createdBy, ZonedDateTime lastUsed, TokenPaymentType tokenPaymentType) {
        jdbi.withHandle(handle ->
        handle.insert("INSERT INTO tokens(token_hash, token_link, type, account_id, description, token_type, revoked, created_by, last_used) VALUES (?,?,?,?,?,?,(? at time zone 'utc'),?,(? at time zone 'utc'))",
                tokenHash.getValue(), randomTokenLink.getValue(), tokenSource, accountId, description, tokenPaymentType,
                revoked, createdBy, lastUsed));
    }

    public ZonedDateTime issueTimestampForAccount(String accountId) {
        return getDateTimeColumn("issued", accountId);
    }

    public java.util.Optional<String> lookupColumnForTokenTable(String column, String idKey, String idValue) {
        return java.util.Optional.ofNullable(jdbi.withHandle(handle ->
                handle.createQuery("SELECT " + column + " FROM tokens WHERE " + idKey + "=:placeholder")
                        .bind("placeholder", idValue)
                        .map(StringColumnMapper.INSTANCE)
                        .first()));

    }

    public Map<String, Object> getTokenByHash(TokenHash tokenHash) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT token_id, type, token_type, token_hash, account_id, issued, revoked, token_link, description, created_by " +
                        "FROM tokens t " +
                        "WHERE token_hash = :token_hash")
                        .bind("token_hash", tokenHash.getValue())
                        .first());
    }

    public ZonedDateTime getDateTimeColumn(String column, String accountId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT " + column + " FROM tokens WHERE account_id=:accountId")
                        .bind("accountId", accountId)
                        .map(new ZonedDateTimeMapper(Optional.of(TimeZone.getTimeZone("UTC"))))
                        .first());
    }

    public ZonedDateTime getCurrentTime() {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT (now() at time zone 'utc')")
                        .map(new ZonedDateTimeMapper(Optional.of(TimeZone.getTimeZone("UTC"))))
                        .first());
    }
}
