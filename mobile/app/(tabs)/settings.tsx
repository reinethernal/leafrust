import { useEffect, useState } from 'react';
import {
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { isModelBundled } from '@/src/ai/classifier';
import { PrimaryButton } from '@/src/components/PrimaryButton';
import { listInspections } from '@/src/db/inspections';
import { exportCsvAndShare } from '@/src/services/share';
import { loadSettings, saveSettings } from '@/src/services/settings';
import { colors } from '@/src/theme/colors';

export default function SettingsScreen() {
  const [email, setEmail] = useState('');
  const [telegramHint, setTelegramHint] = useState('');
  const [saving, setSaving] = useState(false);
  const modelReady = isModelBundled();

  useEffect(() => {
    loadSettings().then((s) => {
      setEmail(s.email);
      setTelegramHint(s.telegramHint);
    });
  }, []);

  return (
    <ScrollView contentContainerStyle={styles.content}>
      <Text style={styles.section}>Отправка результатов</Text>
      <Text style={styles.help}>
        Email подставится в письмо. Telegram-подсказка — напоминание, куда отправлять через Share.
      </Text>
      <Text style={styles.label}>Email</Text>
      <TextInput
        style={styles.input}
        value={email}
        onChangeText={setEmail}
        keyboardType="email-address"
        autoCapitalize="none"
        placeholder="agronom@example.com"
        placeholderTextColor={colors.inkMuted}
      />
      <Text style={styles.label}>Telegram (заметка)</Text>
      <TextInput
        style={styles.input}
        value={telegramHint}
        onChangeText={setTelegramHint}
        placeholder="@channel или имя контакта"
        placeholderTextColor={colors.inkMuted}
      />
      <PrimaryButton
        label="Сохранить"
        loading={saving}
        onPress={async () => {
          setSaving(true);
          try {
            await saveSettings({ email, telegramHint });
            Alert.alert('Готово', 'Настройки сохранены на этом телефоне.');
          } finally {
            setSaving(false);
          }
        }}
      />

      <Text style={[styles.section, { marginTop: 28 }]}>Данные</Text>
      <PrimaryButton
        label="Экспортировать все осмотры (CSV)"
        variant="secondary"
        onPress={async () => {
          const items = await listInspections();
          if (!items.length) {
            Alert.alert('Пусто', 'Пока нет осмотров для экспорта.');
            return;
          }
          await exportCsvAndShare(items);
        }}
      />

      <Text style={[styles.section, { marginTop: 28 }]}>ИИ на устройстве</Text>
      <View style={styles.badge}>
        <Text style={styles.badgeText}>
          {modelReady
            ? 'Обнаружен файл TFLite-модели. Для полного inference нужен native runtime (EAS/dev client).'
            : 'Демо-режим: локальный классификатор + HSV-оценка поражения. Обучите веса через scripts/train_mobilenet.py и положите .tflite в mobile/models/.'}
        </Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: {
    padding: 20,
    gap: 10,
    backgroundColor: colors.bg,
  },
  section: {
    fontSize: 18,
    fontWeight: '800',
    color: colors.ink,
    marginBottom: 4,
  },
  help: {
    color: colors.inkMuted,
    marginBottom: 8,
    lineHeight: 20,
  },
  label: {
    fontWeight: '600',
    color: colors.ink,
    marginTop: 6,
  },
  input: {
    backgroundColor: colors.bgElevated,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 14,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 16,
    color: colors.ink,
    marginBottom: 8,
  },
  badge: {
    backgroundColor: colors.rustSoft,
    borderRadius: 14,
    padding: 14,
  },
  badgeText: {
    color: colors.ink,
    lineHeight: 20,
  },
});
