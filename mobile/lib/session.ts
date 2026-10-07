import * as SecureStore from 'expo-secure-store';

export type Session = {
  token: string;
  name: string;
  email: string;
  role: 'STUDENT' | 'FACULTY' | 'THERAPIST';
  expiresInSeconds: number;
  expiresAtEpochSeconds: number;
};

const SESSION_KEY = 'brainbot.session';

export async function clearSession(): Promise<void> {
  await SecureStore.deleteItemAsync(SESSION_KEY).catch(() => undefined);
}

export async function readValidSession(): Promise<Session | null> {
  try {
    const raw = await SecureStore.getItemAsync(SESSION_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw) as Partial<Session>;
    const validRole = ['STUDENT', 'FACULTY', 'THERAPIST'].includes(session.role ?? '');
    const valid = typeof session.token === 'string'
      && session.token.length > 0
      && typeof session.name === 'string'
      && validRole
      && typeof session.expiresAtEpochSeconds === 'number'
      && session.expiresAtEpochSeconds > Date.now() / 1000;
    if (valid) return session as Session;
  } catch {
    // Invalid or unavailable secure storage is treated as a signed-out session.
  }
  await clearSession();
  return null;
}
