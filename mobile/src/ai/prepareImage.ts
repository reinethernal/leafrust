import * as FileSystem from 'expo-file-system/legacy';
import * as ImageManipulator from 'expo-image-manipulator';

/**
 * Compress and normalize any picked/captured image to a local JPEG for AI + storage.
 */
export async function prepareLeafJpeg(sourceUri: string): Promise<string> {
  const manipulated = await ImageManipulator.manipulateAsync(
    sourceUri,
    [{ resize: { width: 1024 } }],
    { compress: 0.85, format: ImageManipulator.SaveFormat.JPEG }
  );

  const dir = `${FileSystem.documentDirectory}leafrust-photos/`;
  const info = await FileSystem.getInfoAsync(dir);
  if (!info.exists) {
    await FileSystem.makeDirectoryAsync(dir, { intermediates: true });
  }

  const dest = `${dir}leaf_${Date.now()}.jpg`;
  await FileSystem.copyAsync({ from: manipulated.uri, to: dest });
  return dest;
}
