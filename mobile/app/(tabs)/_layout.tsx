import { SymbolView } from 'expo-symbols';
import { Tabs } from 'expo-router';
import { Platform } from 'react-native';

import { colors } from '@/src/theme/colors';

export default function TabLayout() {
  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: colors.leaf,
        tabBarInactiveTintColor: colors.inkMuted,
        tabBarStyle: {
          backgroundColor: colors.bgElevated,
          borderTopColor: colors.border,
          height: Platform.OS === 'ios' ? 88 : 64,
          paddingBottom: Platform.OS === 'ios' ? 28 : 10,
          paddingTop: 8,
        },
        headerStyle: { backgroundColor: colors.bg },
        headerTintColor: colors.ink,
        headerTitleStyle: { fontWeight: '700' },
      }}>
      <Tabs.Screen
        name="index"
        options={{
          title: 'Съёмка',
          headerShown: false,
          tabBarIcon: ({ color }) => (
            <SymbolView
              name={{ ios: 'camera.fill', android: 'photo_camera', web: 'photo_camera' }}
              tintColor={color}
              size={26}
            />
          ),
        }}
      />
      <Tabs.Screen
        name="inspections"
        options={{
          title: 'Осмотры',
          tabBarIcon: ({ color }) => (
            <SymbolView
              name={{ ios: 'list.bullet', android: 'list', web: 'list' }}
              tintColor={color}
              size={26}
            />
          ),
        }}
      />
      <Tabs.Screen
        name="settings"
        options={{
          title: 'Настройки',
          tabBarIcon: ({ color }) => (
            <SymbolView
              name={{ ios: 'gearshape.fill', android: 'settings', web: 'settings' }}
              tintColor={color}
              size={26}
            />
          ),
        }}
      />
    </Tabs>
  );
}
