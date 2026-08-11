import { Image } from 'expo-image';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import {
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { PrimaryButton } from '@/src/components/PrimaryButton';
import { deleteInspection, getInspection, type Inspection } from '@/src/db/inspections';
import {
  emailInspection,
  formatInspectionText,
  openTelegramWithText,
  shareInspection,
} from '@/src/services/share';
import { loadSettings } from '@/src/services/settings';
import { colors } from '@/src/theme/colors';

export default function ResultScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const [item, setItem] = useState<Inspection | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const num = Number(id);
    if (!Number.isFinite(num)) return;
    getInspection(num).then(setItem);
  }, [id]);

  if (!item) {
    return (
      <View style={styles.center}>
        <Text style={styles.muted}>Загрузка…</Text>
      </View>
    );
  }

  return (
    <View style={styles.root}>
      <ScrollView contentContainerStyle={styles.content}>
        <Image source={{ uri: item.imageUri }} style={styles.photo} contentFit="cover" />

        {item.demoMode ? (
          <View style={styles.demoBadge}>
            <Text style={styles.demoText}>Демо-классификатор · поражение считается локально</Text>
          </View>
        ) : null}

        <Text style={styles.disease}>{item.diseaseName}</Text>
        <Text style={styles.plant}>{item.plantSpecies}</Text>

        <View style={styles.metrics}>
          <View style={styles.metric}>
            <Text style={styles.metricValue}>{item.damagePercentage}%</Text>
            <Text style={styles.metricLabel}>Поражение</Text>
          </View>
          <View style={styles.metric}>
            <Text style={styles.metricValue}>{item.confidence}%</Text>
            <Text style={styles.metricLabel}>Уверенность</Text>
          </View>
        </View>

        <Text style={styles.section}>Симптомы</Text>
        <Text style={styles.body}>{item.symptoms}</Text>
        <Text style={styles.date}>{new Date(item.createdAt).toLocaleString('ru-RU')}</Text>
      </ScrollView>

      <View style={styles.actions}>
        <PrimaryButton
          label="Отправить"
          loading={busy}
          onPress={async () => {
            setBusy(true);
            try {
              await shareInspection(item);
            } finally {
              setBusy(false);
            }
          }}
        />
        <View style={styles.row}>
          <PrimaryButton
            label="Telegram"
            variant="secondary"
            style={{ flex: 1 }}
            onPress={() => openTelegramWithText(formatInspectionText(item))}
          />
          <PrimaryButton
            label="Email"
            variant="ghost"
            style={{ flex: 1 }}
            onPress={async () => {
              const settings = await loadSettings();
              await emailInspection(item, settings.email || undefined);
            }}
          />
        </View>
        <View style={styles.row}>
          <PrimaryButton
            label="Ещё снимок"
            variant="ghost"
            style={{ flex: 1 }}
            onPress={() => router.replace('/(tabs)')}
          />
          <PrimaryButton
            label="Удалить"
            variant="danger"
            style={{ flex: 1 }}
            onPress={() => {
              Alert.alert('Удалить осмотр?', 'Запись исчезнет с этого телефона.', [
                { text: 'Отмена', style: 'cancel' },
                {
                  text: 'Удалить',
                  style: 'destructive',
                  onPress: async () => {
                    await deleteInspection(item.id);
                    router.replace('/(tabs)/inspections');
                  },
                },
              ]);
            }}
          />
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: colors.bg,
  },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  muted: {
    color: colors.inkMuted,
  },
  content: {
    paddingBottom: 24,
  },
  photo: {
    width: '100%',
    height: 280,
    backgroundColor: colors.leafSoft,
  },
  demoBadge: {
    marginHorizontal: 16,
    marginTop: 12,
    backgroundColor: colors.rustSoft,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  demoText: {
    color: colors.ink,
    fontSize: 13,
    fontWeight: '600',
  },
  disease: {
    marginTop: 16,
    marginHorizontal: 16,
    fontSize: 28,
    fontWeight: '800',
    color: colors.ink,
  },
  plant: {
    marginHorizontal: 16,
    marginTop: 4,
    fontSize: 16,
    color: colors.leaf,
    fontWeight: '700',
  },
  metrics: {
    flexDirection: 'row',
    gap: 12,
    marginHorizontal: 16,
    marginTop: 18,
  },
  metric: {
    flex: 1,
    backgroundColor: colors.bgElevated,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.border,
    padding: 14,
  },
  metricValue: {
    fontSize: 26,
    fontWeight: '800',
    color: colors.rust,
  },
  metricLabel: {
    color: colors.inkMuted,
    marginTop: 2,
  },
  section: {
    marginTop: 20,
    marginHorizontal: 16,
    fontWeight: '800',
    color: colors.ink,
    fontSize: 16,
  },
  body: {
    marginHorizontal: 16,
    marginTop: 6,
    color: colors.inkMuted,
    lineHeight: 22,
  },
  date: {
    marginHorizontal: 16,
    marginTop: 14,
    color: colors.inkMuted,
    fontSize: 12,
  },
  actions: {
    borderTopWidth: 1,
    borderTopColor: colors.border,
    backgroundColor: colors.bgElevated,
    paddingHorizontal: 14,
    paddingTop: 12,
    paddingBottom: 18,
    gap: 10,
  },
  row: {
    flexDirection: 'row',
    gap: 10,
  },
});
