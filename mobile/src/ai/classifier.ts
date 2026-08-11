import { estimateSeverity, extractColorStats } from './severity';
import { PLANT_CLASSES, getClassById, type PlantClass } from './labels';

export type AnalysisResult = {
  plantSpecies: string;
  diseaseName: string;
  confidence: number;
  damagePercentage: number;
  symptoms: string;
  classId: string;
  demoMode: boolean;
  healthy: boolean;
};

/**
 * On-device analyzer.
 *
 * Default Expo path: lightweight local heuristic over PlantVillage labels (demo)
 * + real HSV severity estimation.
 *
 * To enable trained TFLite weights after `eas build` / prebuild:
 * 1. Put `models/plantvillage_mobilenet.tflite` in the project
 * 2. Wire `react-native-fast-tflite` (or similar) inside `runTfliteIfAvailable`
 */
export async function analyzeLeafImage(imageUri: string): Promise<AnalysisResult> {
  const severity = await estimateSeverity(imageUri);
  const tflite = await runTfliteIfAvailable(imageUri);
  if (tflite) {
    return {
      ...tflite,
      damagePercentage: severity.damagePercentage,
      demoMode: false,
    };
  }

  const stats = await extractColorStats(imageUri);
  const picked = pickDemoClass(stats, severity.damagePercentage);
  const confidence = computeDemoConfidence(stats, severity.damagePercentage, picked);

  return {
    plantSpecies: picked.plantRu,
    diseaseName: picked.diseaseRu,
    confidence,
    damagePercentage: picked.healthy ? Math.min(severity.damagePercentage, 5) : severity.damagePercentage,
    symptoms: picked.symptoms,
    classId: picked.id,
    demoMode: true,
    healthy: picked.healthy,
  };
}

async function runTfliteIfAvailable(
  _imageUri: string
): Promise<Omit<AnalysisResult, 'damagePercentage' | 'demoMode'> | null> {
  // Hook for native TFLite runtime — not available in Expo Go managed workflow.
  return null;
}

/** True when a trained model runtime path is configured (see train script). */
export function isModelBundled(): boolean {
  return false;
}

type ColorStats = Awaited<ReturnType<typeof extractColorStats>>;

function pickDemoClass(stats: ColorStats, damage: number): PlantClass {
  if (damage < 6 && stats.rustRatio < 0.05 && stats.whiteRatio < 0.08) {
    return getClassById('Tomato___healthy') ?? PLANT_CLASSES[PLANT_CLASSES.length - 1];
  }
  if (stats.whiteRatio > 0.18) {
    return getClassById('Squash___Powdery_mildew') ?? PLANT_CLASSES[0];
  }
  if (stats.rustRatio > 0.16) {
    return getClassById('Corn___Common_rust') ?? getClassById('Apple___Cedar_apple_rust')!;
  }
  if (stats.darkRatio > 0.14 && damage > 20) {
    return getClassById('Potato___Late_blight') ?? PLANT_CLASSES[0];
  }
  if (stats.meanH >= 40 && stats.meanH <= 75 && damage > 15) {
    return getClassById('Tomato___Early_blight') ?? PLANT_CLASSES[0];
  }
  if (damage > 25) {
    return getClassById('Tomato___Septoria_leaf_spot') ?? PLANT_CLASSES[0];
  }
  return getClassById('Apple___Apple_scab') ?? PLANT_CLASSES[0];
}

function computeDemoConfidence(stats: ColorStats, damage: number, picked: PlantClass) {
  let score = 62;
  if (picked.healthy && damage < 6) score += 18;
  if (!picked.healthy && damage >= 10) score += 12;
  if (stats.rustRatio > 0.12 && picked.id.includes('rust')) score += 10;
  if (stats.whiteRatio > 0.15 && picked.id.toLowerCase().includes('powdery')) score += 10;
  return Math.min(96, Math.max(55, Math.round(score)));
}
