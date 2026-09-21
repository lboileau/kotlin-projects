import { useState, type FormEvent } from 'react';
import { Button, Callout, Card, Text, TextField } from '@radix-ui/themes';
import { PageHeader } from '../../components/PageHeader';
import { useAuth } from '../../auth/useAuth';
import { updateUsername } from '../../api/auth';
import { ApiError } from '../../api/http';
import { toast } from '../../lib/toastStore';
import './AccountPage.css';

export function AccountPage() {
  const { user, updateUser, signOut } = useAuth();
  const [username, setUsername] = useState(user?.username ?? '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!user) return null;

  const dirty = username.trim().length > 0 && username.trim() !== (user.username ?? '');

  async function handleSave(event: FormEvent) {
    event.preventDefault();
    if (!user || !dirty) return;
    setError(null);
    setSaving(true);
    try {
      const updated = await updateUsername(user.id, username.trim());
      updateUser(updated);
      toast.info('Name updated.');
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Could not update your name.';
      setError(message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="account-page">
      <PageHeader title="Account" backTo="/plans" backAcrossAreas showAccount={false} />
      <div className="account-page__body">
        <Card size="3" className="account-page__card">
          <Text as="p" size="2" color="gray" className="account-page__email-label">
            Email
          </Text>
          <Text as="p" size="3" className="account-page__email">
            {user.email}
          </Text>

          <form onSubmit={handleSave} className="account-page__form">
            <label className="account-page__field">
              <Text as="span" size="2" weight="medium">
                Name
              </Text>
              <TextField.Root
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                autoComplete="name"
                size="3"
              />
            </label>

            {error && (
              <Callout.Root color="red" variant="surface" size="1">
                <Callout.Text>{error}</Callout.Text>
              </Callout.Root>
            )}

            <Button type="submit" size="3" disabled={!dirty} loading={saving}>
              Save
            </Button>
          </form>
        </Card>

        <Button variant="soft" color="red" size="3" onClick={signOut} className="account-page__sign-out">
          Sign out
        </Button>
      </div>
    </div>
  );
}
