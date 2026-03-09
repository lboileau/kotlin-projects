-- Normalize existing user emails: lowercase and strip dots from local part
UPDATE users
SET email = LOWER(REPLACE(SPLIT_PART(email, '@', 1), '.', '') || '@' || SPLIT_PART(email, '@', 2)),
    updated_at = NOW()
WHERE email != LOWER(REPLACE(SPLIT_PART(email, '@', 1), '.', '') || '@' || SPLIT_PART(email, '@', 2));
