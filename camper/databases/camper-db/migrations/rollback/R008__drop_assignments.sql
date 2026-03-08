DROP TRIGGER IF EXISTS trg_transfer_assignment_ownership ON users;
DROP FUNCTION IF EXISTS transfer_assignment_ownership_on_user_delete();
DROP TABLE IF EXISTS assignments CASCADE;
