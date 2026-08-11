import { Link, Stack } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';

import { colors } from '@/src/theme/colors';

export default function NotFoundScreen() {
  return (
    <>
      <Stack.Screen options={{ title: 'Не найдено' }} />
      <View style={styles.container}>
        <Text style={styles.title}>Экран не найден</Text>
        <Link href="/" style={styles.link}>
          <Text style={styles.linkText}>На главную LeafRust</Text>
        </Link>
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
    backgroundColor: colors.bg,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
    color: colors.ink,
  },
  link: {
    marginTop: 15,
    paddingVertical: 15,
  },
  linkText: {
    fontSize: 15,
    color: colors.leaf,
    fontWeight: '600',
  },
});
