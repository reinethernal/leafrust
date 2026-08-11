import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';
import { View } from 'react-native';

import { initInspectionsDb } from '@/src/db/inspections';
import { colors } from '@/src/theme/colors';

export { ErrorBoundary } from 'expo-router';

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const [ready, setReady] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        await initInspectionsDb();
      } finally {
        setReady(true);
        await SplashScreen.hideAsync();
      }
    })();
  }, []);

  if (!ready) {
    return <View style={{ flex: 1, backgroundColor: colors.bg }} />;
  }

  return (
    <>
      <StatusBar style="dark" />
      <Stack
        screenOptions={{
          headerStyle: { backgroundColor: colors.bg },
          headerTintColor: colors.leaf,
          headerTitleStyle: { fontWeight: '700', color: colors.ink },
          contentStyle: { backgroundColor: colors.bg },
        }}>
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="result/[id]" options={{ title: 'Результат осмотра' }} />
        <Stack.Screen name="+not-found" />
      </Stack>
    </>
  );
}
