import { Image } from 'expo-image';
import { useFocusEffect, useRouter } from 'expo-router';
import { useCallback, useState } from 'react';
import {
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { PrimaryButton } from '@/src/components/PrimaryButton';
import { listInspections, type Inspection } from '@/src/db/inspections';
import { exportCsvAndShare } from '@/src/services/share';
import { colors } from '@/src/theme/colors';

export default function InspectionsScreen() {
  const router = useRouter();
  const [items, setItems] = useState<Inspection[]>([]);
  const [exporting, setExporting] = useState(false);

  useFocusEffect(
    useCallback(() => {
      let active = true;
      (async () => {
        const rows = await listInspections();
        if (active) setItems(rows);
      })();
      return () => {
        active = false;
      };
    }, [])
  );

  return (
    <View style={styles.container}>
      {items.length > 0 ? (
        <View style={styles.toolbar}>
          <PrimaryButton
            label="Экспорт CSV"
            variant="ghost"
            loading={exporting}
            onPress={async () => {
              setExporting(true);
              try {
                await exportCsvAndShare(items);
              } finally {
                setExporting(false);
              }
            }}
            style={{ flex: 1 }}
          />
        </View>
      ) : null}

      <FlatList
        data={items}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={items.length === 0 ? styles.emptyWrap : styles.list}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text style={styles.emptyTitle}>Пока нет осмотров</Text>
            <Text style={styles.emptyText}>Сделайте первый снимок на вкладке «Съёмка».</Text>
          </View>
        }
        renderItem={({ item }) => (
          <Pressable
            style={({ pressed }) => [styles.card, pressed && { opacity: 0.9 }]}
            onPress={() => router.push(`/result/${item.id}`)}>
            <Image source={{ uri: item.imageUri }} style={styles.thumb} contentFit="cover" />
            <View style={styles.cardBody}>
              <Text style={styles.disease} numberOfLines={1}>
                {item.diseaseName}
              </Text>
              <Text style={styles.meta} numberOfLines={1}>
                {item.plantSpecies} · {item.damagePercentage}% поражения
              </Text>
              <Text style={styles.date}>
                {new Date(item.createdAt).toLocaleString('ru-RU')}
              </Text>
            </View>
          </Pressable>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bg,
  },
  toolbar: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 4,
  },
  list: {
    padding: 16,
    gap: 12,
  },
  emptyWrap: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: 24,
  },
  empty: {
    alignItems: 'center',
    gap: 8,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: colors.ink,
  },
  emptyText: {
    textAlign: 'center',
    color: colors.inkMuted,
  },
  card: {
    flexDirection: 'row',
    backgroundColor: colors.bgElevated,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.border,
    overflow: 'hidden',
    marginBottom: 12,
  },
  thumb: {
    width: 92,
    height: 92,
    backgroundColor: colors.leafSoft,
  },
  cardBody: {
    flex: 1,
    padding: 12,
    justifyContent: 'center',
    gap: 3,
  },
  disease: {
    fontSize: 16,
    fontWeight: '700',
    color: colors.ink,
  },
  meta: {
    color: colors.rust,
    fontWeight: '600',
  },
  date: {
    color: colors.inkMuted,
    fontSize: 12,
  },
});
