import * as ImagePicker from 'expo-image-picker';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import {
  Alert,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { analyzeLeafImage } from '@/src/ai/classifier';
import { prepareLeafJpeg } from '@/src/ai/prepareImage';
import { LeafBrand } from '@/src/components/LeafBrand';
import { PrimaryButton } from '@/src/components/PrimaryButton';
import { insertInspection } from '@/src/db/inspections';
import { colors } from '@/src/theme/colors';

export default function ScanScreen() {
  const router = useRouter();
  const [busy, setBusy] = useState(false);

  async function runPipeline(uri: string) {
    setBusy(true);
    try {
      const jpegUri = await prepareLeafJpeg(uri);
      const result = await analyzeLeafImage(jpegUri);
      const id = await insertInspection({
        imageUri: jpegUri,
        plantSpecies: result.plantSpecies,
        diseaseName: result.diseaseName,
        confidence: result.confidence,
        damagePercentage: result.damagePercentage,
        symptoms: result.symptoms,
        classId: result.classId,
        demoMode: result.demoMode,
      });
      router.push(`/result/${id}`);
    } catch (error) {
      console.error(error);
      Alert.alert('Ошибка анализа', 'Не удалось обработать фото. Попробуйте ещё раз.');
    } finally {
      setBusy(false);
    }
  }

  async function takePhoto() {
    const permission = await ImagePicker.requestCameraPermissionsAsync();
    if (!permission.granted) {
      Alert.alert('Нужна камера', 'Разрешите доступ к камере в настройках телефона.');
      return;
    }
    const shot = await ImagePicker.launchCameraAsync({
      quality: 0.9,
      exif: false,
      allowsEditing: false,
      cameraType: ImagePicker.CameraType.back,
    });
    if (!shot.canceled && shot.assets[0]?.uri) {
      await runPipeline(shot.assets[0].uri);
    }
  }

  async function pickGallery() {
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) {
      Alert.alert('Нужна галерея', 'Разрешите доступ к фото в настройках телефона.');
      return;
    }
    const picked = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      quality: 0.9,
      exif: false,
    });
    if (!picked.canceled && picked.assets[0]?.uri) {
      await runPipeline(picked.assets[0].uri);
    }
  }

  return (
    <SafeAreaView style={styles.safe}>
      <ScrollView contentContainerStyle={styles.content} bounces={false}>
        <View style={styles.hero}>
          <LeafBrand size={72} />
          <Text style={styles.tagline}>
            Сфотографируйте лист — локальный ИИ оценит болезнь и процент поражения.
          </Text>
        </View>

        <View style={styles.panel}>
          <PrimaryButton
            label="Сфотографировать лист"
            onPress={takePhoto}
            loading={busy}
            style={styles.cta}
          />
          <PrimaryButton
            label="Выбрать из галереи"
            onPress={pickGallery}
            variant="secondary"
            disabled={busy}
          />
        </View>

        <View style={styles.hintCard}>
          <Text style={styles.hintTitle}>Как лучше снимать</Text>
          <Text style={styles.hintText}>
            Один лист на светлом фоне, без сильных бликов. Камера держите параллельно листу.
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: colors.bg,
  },
  content: {
    flexGrow: 1,
    paddingHorizontal: 22,
    paddingTop: 28,
    paddingBottom: 36,
    justifyContent: 'space-between',
    gap: 28,
  },
  hero: {
    alignItems: 'center',
    paddingTop: 36,
    gap: 16,
  },
  tagline: {
    textAlign: 'center',
    color: colors.inkMuted,
    fontSize: 16,
    lineHeight: 24,
    maxWidth: 320,
  },
  panel: {
    gap: 12,
  },
  cta: {
    minHeight: 62,
  },
  hintCard: {
    backgroundColor: colors.leafSoft,
    borderRadius: 18,
    padding: 16,
    gap: 6,
  },
  hintTitle: {
    fontWeight: '700',
    color: colors.ink,
    fontSize: 15,
  },
  hintText: {
    color: colors.inkMuted,
    fontSize: 14,
    lineHeight: 20,
  },
});
