import * as SQLite from 'expo-sqlite';

export type Inspection = {
  id: number;
  createdAt: string;
  imageUri: string;
  plantSpecies: string;
  diseaseName: string;
  confidence: number;
  damagePercentage: number;
  symptoms: string;
  notes: string;
  classId: string;
  demoMode: number;
};

let dbPromise: Promise<SQLite.SQLiteDatabase> | null = null;

async function getDb() {
  if (!dbPromise) {
    dbPromise = (async () => {
      const db = await SQLite.openDatabaseAsync('leafrust.db');
      await db.execAsync(`
        PRAGMA journal_mode = WAL;
        CREATE TABLE IF NOT EXISTS inspections (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          created_at TEXT NOT NULL,
          image_uri TEXT NOT NULL,
          plant_species TEXT NOT NULL,
          disease_name TEXT NOT NULL,
          confidence REAL NOT NULL,
          damage_percentage REAL NOT NULL,
          symptoms TEXT NOT NULL DEFAULT '',
          notes TEXT NOT NULL DEFAULT '',
          class_id TEXT NOT NULL DEFAULT '',
          demo_mode INTEGER NOT NULL DEFAULT 1
        );
      `);
      return db;
    })();
  }
  return dbPromise;
}

function mapRow(row: Record<string, unknown>): Inspection {
  return {
    id: Number(row.id),
    createdAt: String(row.created_at),
    imageUri: String(row.image_uri),
    plantSpecies: String(row.plant_species),
    diseaseName: String(row.disease_name),
    confidence: Number(row.confidence),
    damagePercentage: Number(row.damage_percentage),
    symptoms: String(row.symptoms ?? ''),
    notes: String(row.notes ?? ''),
    classId: String(row.class_id ?? ''),
    demoMode: Number(row.demo_mode ?? 1),
  };
}

export async function initInspectionsDb() {
  await getDb();
}

export async function insertInspection(input: {
  imageUri: string;
  plantSpecies: string;
  diseaseName: string;
  confidence: number;
  damagePercentage: number;
  symptoms: string;
  notes?: string;
  classId: string;
  demoMode: boolean;
}): Promise<number> {
  const db = await getDb();
  const createdAt = new Date().toISOString();
  const result = await db.runAsync(
    `INSERT INTO inspections (
      created_at, image_uri, plant_species, disease_name, confidence,
      damage_percentage, symptoms, notes, class_id, demo_mode
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    createdAt,
    input.imageUri,
    input.plantSpecies,
    input.diseaseName,
    input.confidence,
    input.damagePercentage,
    input.symptoms,
    input.notes ?? '',
    input.classId,
    input.demoMode ? 1 : 0
  );
  return Number(result.lastInsertRowId);
}

export async function listInspections(): Promise<Inspection[]> {
  const db = await getDb();
  const rows = await db.getAllAsync<Record<string, unknown>>(
    'SELECT * FROM inspections ORDER BY id DESC'
  );
  return rows.map(mapRow);
}

export async function getInspection(id: number): Promise<Inspection | null> {
  const db = await getDb();
  const row = await db.getFirstAsync<Record<string, unknown>>(
    'SELECT * FROM inspections WHERE id = ?',
    id
  );
  return row ? mapRow(row) : null;
}

export async function deleteInspection(id: number) {
  const db = await getDb();
  await db.runAsync('DELETE FROM inspections WHERE id = ?', id);
}

export async function updateNotes(id: number, notes: string) {
  const db = await getDb();
  await db.runAsync('UPDATE inspections SET notes = ? WHERE id = ?', notes, id);
}

export function inspectionsToCsv(items: Inspection[]): string {
  const header = [
    'id',
    'created_at',
    'plant_species',
    'disease_name',
    'confidence',
    'damage_percentage',
    'symptoms',
    'notes',
    'class_id',
    'demo_mode',
  ].join(',');
  const lines = items.map((i) =>
    [
      i.id,
      i.createdAt,
      csvEscape(i.plantSpecies),
      csvEscape(i.diseaseName),
      i.confidence,
      i.damagePercentage,
      csvEscape(i.symptoms),
      csvEscape(i.notes),
      csvEscape(i.classId),
      i.demoMode,
    ].join(',')
  );
  return [header, ...lines].join('\n');
}

function csvEscape(value: string) {
  if (value.includes(',') || value.includes('"') || value.includes('\n')) {
    return `"${value.replace(/"/g, '""')}"`;
  }
  return value;
}
