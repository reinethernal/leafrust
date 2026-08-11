import AsyncStorage from '@react-native-async-storage/async-storage';

const KEYS = {
  email: 'leafrust.settings.email',
  telegramHint: 'leafrust.settings.telegram',
};

export type AppSettings = {
  email: string;
  telegramHint: string;
};

export async function loadSettings(): Promise<AppSettings> {
  const [email, telegramHint] = await Promise.all([
    AsyncStorage.getItem(KEYS.email),
    AsyncStorage.getItem(KEYS.telegramHint),
  ]);
  return {
    email: email ?? '',
    telegramHint: telegramHint ?? '',
  };
}

export async function saveSettings(settings: AppSettings) {
  await AsyncStorage.multiSet([
    [KEYS.email, settings.email],
    [KEYS.telegramHint, settings.telegramHint],
  ]);
}
