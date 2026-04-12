-- Allow duplicate email values across users (non-unique column).
-- Application may still reject duplicates via service checks until those are relaxed.
DROP INDEX IF EXISTS ux_users_email;
