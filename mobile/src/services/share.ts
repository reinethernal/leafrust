import * as FileSystem from 'expo-file-system/legacy';
import * as MailComposer from 'expo-mail-composer';
import * as Sharing from 'expo-sharing';
import { Linking, Platform, Share } from 'react-native';

import type { Inspection } from '@/src/db/inspections';
import { inspectionsToCsv } from '@/src/db/inspections';

export function formatInspectionText(item: Inspection) {
  const when = new Date(item.createdAt).toLocaleString('ru-RU');
  return [
    'LeafRust — результат осмотра',
    `Дата: ${when}`,
    `Культура: ${item.plantSpecies}`,
    `Диагноз: ${item.diseaseName}`,
    `Уверенность: ${item.confidence}%`,
    `Поражение: ${item.damagePercentage}%`,
    `Симптомы: ${item.symptoms}`,
    item.notes ? `Заметки: ${item.notes}` : '',
    item.demoMode ? 'Режим: демо-классификатор (локальный)' : 'Режим: TFLite-модель',
  ]
    .filter(Boolean)
    .join('\n');
}

export async function shareInspection(item: Inspection) {
  const message = formatInspectionText(item);
  const canShareFiles = await Sharing.isAvailableAsync();
  if (canShareFiles && item.imageUri) {
    try {
      await Sharing.shareAsync(item.imageUri, {
        dialogTitle: 'LeafRust — отправить осмотр',
        mimeType: 'image/jpeg',
        UTI: 'public.jpeg',
      });
      // Also offer text via system share on some platforms after file share.
      if (Platform.OS === 'ios') {
        await Share.share({ message });
      }
      return;
    } catch {
      // fall through to text share
    }
  }
  await Share.share({ message, title: 'LeafRust' });
}

export async function shareText(message: string) {
  await Share.share({ message, title: 'LeafRust' });
}

export async function openTelegramWithText(message: string) {
  const url = `https://t.me/share/url?url=${encodeURIComponent('LeafRust')}&text=${encodeURIComponent(message)}`;
  const supported = await Linking.canOpenURL(url);
  if (supported) {
    await Linking.openURL(url);
    return;
  }
  await Share.share({ message });
}

export async function emailInspection(item: Inspection, toAddress?: string) {
  const body = formatInspectionText(item);
  const available = await MailComposer.isAvailableAsync();
  if (available) {
    await MailComposer.composeAsync({
      recipients: toAddress ? [toAddress] : undefined,
      subject: `LeafRust: ${item.plantSpecies} — ${item.diseaseName}`,
      body,
      attachments: item.imageUri ? [item.imageUri] : undefined,
    });
    return;
  }
  const mailto = `mailto:${encodeURIComponent(toAddress ?? '')}?subject=${encodeURIComponent(
    `LeafRust: ${item.diseaseName}`
  )}&body=${encodeURIComponent(body)}`;
  await Linking.openURL(mailto);
}

export async function exportCsvAndShare(items: Inspection[]) {
  const csv = inspectionsToCsv(items);
  const dir = FileSystem.cacheDirectory ?? FileSystem.documentDirectory;
  if (!dir) throw new Error('Нет доступа к файловой системе');
  const path = `${dir}leafrust-inspections.csv`;
  await FileSystem.writeAsStringAsync(path, csv, {
    encoding: FileSystem.EncodingType.UTF8,
  });
  if (await Sharing.isAvailableAsync()) {
    await Sharing.shareAsync(path, {
      mimeType: 'text/csv',
      dialogTitle: 'Экспорт осмотров LeafRust',
      UTI: 'public.comma-separated-values-text',
    });
  } else {
    await Share.share({ message: csv, title: 'LeafRust CSV' });
  }
}
