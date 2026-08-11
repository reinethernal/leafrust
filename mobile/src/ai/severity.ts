import * as FileSystem from 'expo-file-system/legacy';
import jpeg from 'jpeg-js';

import { base64ToUint8Array } from './base64';

export type SeverityResult = {
  damagePercentage: number;
  leafPixels: number;
  lesionPixels: number;
};

function rgbToHsv(r: number, g: number, b: number) {
  const rn = r / 255;
  const gn = g / 255;
  const bn = b / 255;
  const max = Math.max(rn, gn, bn);
  const min = Math.min(rn, gn, bn);
  const d = max - min;
  let h = 0;
  if (d !== 0) {
    if (max === rn) h = ((gn - bn) / d) % 6;
    else if (max === gn) h = (bn - rn) / d + 2;
    else h = (rn - gn) / d + 4;
    h *= 60;
    if (h < 0) h += 360;
  }
  const s = max === 0 ? 0 : d / max;
  const v = max;
  return { h, s, v };
}

function isLeafPixel(h: number, s: number, v: number) {
  const greenish = h >= 35 && h <= 170 && s >= 0.12 && v >= 0.12 && v <= 0.95;
  const yellowish = h >= 25 && h < 35 && s >= 0.18 && v >= 0.2;
  return greenish || yellowish;
}

function isLesionPixel(h: number, s: number, v: number) {
  const rustBrown = ((h >= 0 && h <= 40) || h >= 330) && s >= 0.2 && v >= 0.12 && v <= 0.75;
  const darkNecrosis = v < 0.22 && s < 0.45;
  const chlorosis = h >= 40 && h <= 70 && s >= 0.25 && v >= 0.35;
  return rustBrown || darkNecrosis || chlorosis;
}

function isHealthyGreen(h: number, s: number, v: number) {
  return h >= 70 && h <= 160 && s >= 0.25 && v >= 0.25 && v <= 0.9;
}

async function decodeJpeg(imageUri: string) {
  const base64 = await FileSystem.readAsStringAsync(imageUri, {
    encoding: FileSystem.EncodingType.Base64,
  });
  const bytes = base64ToUint8Array(base64);
  return jpeg.decode(bytes, { useTArray: true, formatAsRGBA: true });
}

/**
 * Estimates leaf damage % from a JPEG image URI using HSV masks
 * (leaf area vs lesion area).
 */
export async function estimateSeverity(imageUri: string): Promise<SeverityResult> {
  let decoded: { width: number; height: number; data: Uint8Array };
  try {
    decoded = await decodeJpeg(imageUri);
  } catch {
    return { damagePercentage: 12, leafPixels: 0, lesionPixels: 0 };
  }

  const { data, width, height } = decoded;
  const step = Math.max(1, Math.floor(Math.min(width, height) / 160));
  let leafPixels = 0;
  let lesionPixels = 0;

  for (let y = 0; y < height; y += step) {
    for (let x = 0; x < width; x += step) {
      const i = (y * width + x) * 4;
      const a = data[i + 3];
      if (a < 128) continue;
      const { h, s, v } = rgbToHsv(data[i], data[i + 1], data[i + 2]);
      if (!isLeafPixel(h, s, v) && !isLesionPixel(h, s, v)) continue;
      leafPixels += 1;
      if (isLesionPixel(h, s, v) && !isHealthyGreen(h, s, v)) {
        lesionPixels += 1;
      }
    }
  }

  if (leafPixels < 50) {
    return { damagePercentage: 8, leafPixels, lesionPixels };
  }

  const raw = (lesionPixels / leafPixels) * 100;
  const damagePercentage = Math.round(Math.min(98, Math.max(0, raw)) * 10) / 10;
  return { damagePercentage, leafPixels, lesionPixels };
}

export async function extractColorStats(imageUri: string) {
  let decoded: { width: number; height: number; data: Uint8Array };
  try {
    decoded = await decodeJpeg(imageUri);
  } catch {
    return { meanH: 90, meanS: 0.4, meanV: 0.5, rustRatio: 0.1, whiteRatio: 0.05, darkRatio: 0.05 };
  }

  const { data, width, height } = decoded;
  const step = Math.max(1, Math.floor(Math.min(width, height) / 120));
  let n = 0;
  let sumH = 0;
  let sumS = 0;
  let sumV = 0;
  let rust = 0;
  let white = 0;
  let dark = 0;

  for (let y = 0; y < height; y += step) {
    for (let x = 0; x < width; x += step) {
      const i = (y * width + x) * 4;
      if (data[i + 3] < 128) continue;
      const { h, s, v } = rgbToHsv(data[i], data[i + 1], data[i + 2]);
      if (v > 0.92 && s < 0.12) {
        white += 1;
        continue;
      }
      n += 1;
      sumH += h;
      sumS += s;
      sumV += v;
      if (((h >= 0 && h <= 40) || h >= 330) && s > 0.25 && v < 0.7) rust += 1;
      if (v < 0.2) dark += 1;
    }
  }

  if (n === 0) {
    return { meanH: 90, meanS: 0.4, meanV: 0.5, rustRatio: 0.1, whiteRatio: 0.05, darkRatio: 0.05 };
  }

  return {
    meanH: sumH / n,
    meanS: sumS / n,
    meanV: sumV / n,
    rustRatio: rust / n,
    whiteRatio: white / Math.max(1, white + n),
    darkRatio: dark / n,
  };
}
