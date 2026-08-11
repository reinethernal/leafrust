#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Build offline plant/disease knowledge base for LeafRust.

Output: android/app/src/main/assets/kb/plants_diseases.sqlite

Usage:
  python scripts/build_plant_kb.py
"""
from __future__ import annotations

import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LABELS = ROOT / "android" / "app" / "src" / "main" / "assets" / "models" / "labels.txt"
ORNAMENTAL = ROOT / "android" / "app" / "src" / "main" / "java" / "com" / "leafrust" / "data" / "ai" / "OrnamentalDiagnoser.kt"
OUT_DIR = ROOT / "android" / "app" / "src" / "main" / "assets" / "kb"
OUT_DB = OUT_DIR / "plants_diseases.sqlite"

# PlantVillage English crop → Russian
PV_PLANT_RU = {
    "Apple": ("Яблоня", "Плодовое дерево"),
    "Background": ("—", "—"),
    "Blueberry": ("Голубика", "Ягодная культура"),
    "Cherry": ("Вишня", "Плодовое дерево"),
    "Corn": ("Кукуруза", "Полевая культура"),
    "Grape": ("Виноград", "Садовая культура"),
    "Orange": ("Апельсин", "Плодовое дерево"),
    "Peach": ("Персик", "Плодовое дерево"),
    "Pepper": ("Перец", "Овощная культура"),
    "Potato": ("Картофель", "Овощная культура"),
    "Raspberry": ("Малина", "Ягодная культура"),
    "Soybean": ("Соя", "Полевая культура"),
    "Squash": ("Тыква", "Овощная культура"),
    "Strawberry": ("Клубника", "Ягодная культура"),
    "Tomato": ("Томат", "Овощная культура"),
}

# Disease key templates: kind, name_ru, name_en, symptoms, treatment
DISEASE_TEMPLATES: dict[str, tuple[str, str, str, str, str]] = {
    "healthy": (
        "healthy",
        "Здоровый лист",
        "Healthy",
        "Равномерный цвет, упругая ткань, без налёта, пятен и некрозов.",
        "Поддерживайте подходящий свет и полив. Осматривайте растение раз в неделю. Профилактика: чистота кроны и проветривание.",
    ),
    "Powdery_mildew": (
        "fungal",
        "Мучнистая роса",
        "Powdery mildew",
        "Белый или сероватый мучнистый налёт на листьях и побегах, возможна деформация.",
        "Удалите сильно поражённые листья. Примените фунгицид от мучнистой росы или биопрепарат. Улучшите проветривание, не опрыскивайте на ночь.",
    ),
    "Downy_mildew": (
        "fungal",
        "Ложная мучнистая роса",
        "Downy mildew",
        "Жёлтые пятна сверху, серо-фиолетовый налёт снизу листа, быстрое увядание.",
        "Снизьте влажность на листе, удалите больные части, фунгицид против пероноспороза. Поливайте под корень.",
    ),
    "Rust": (
        "fungal",
        "Ржавчина",
        "Rust",
        "Ржаво-оранжевые или бурые пустулы, чаще на нижней стороне листа.",
        "Удалите листья с пустулами, утилизируйте опад. Фунгицид, улучшите проветривание кроны.",
    ),
    "Early_blight": (
        "fungal",
        "Ранняя пятнистость / альтернариоз",
        "Early blight",
        "Концентрические «мишени» бурого цвета, чаще на старых листьях.",
        "Удалите нижние больные листья. Фунгицид (манкоцеб/триазолы по культуре). Мульча, полив под корень.",
    ),
    "Late_blight": (
        "fungal",
        "Фитофтороз / поздняя пятнистость",
        "Late blight",
        "Водянистые тёмные пятна, быстро разрастаются, возможны белёсые споры.",
        "Срочно фунгицид против фитофторы. Удалите поражённые части. Улучшите проветривание, избегайте смачивания кроны.",
    ),
    "Black_spot": (
        "fungal",
        "Чёрная пятнистость",
        "Black spot",
        "Тёмные округлые пятна, часто с жёлтым ореолом, лист может опадать.",
        "Удалите больные листья. Фунгицид, сухое содержание кроны вечером. Собирайте опад.",
    ),
    "Anthracnose": (
        "fungal",
        "Антракноз",
        "Anthracnose",
        "Крупные некротические пятна с тёмной каймой, иногда вдавленные.",
        "Санитарная обрезка до здоровой ткани, фунгицид, утилизация поражённых частей.",
    ),
    "Leaf_spot": (
        "fungal",
        "Пятнистость листьев",
        "Leaf spot",
        "Мелкие или средние пятна разного цвета с каймой, возможна дырчатость.",
        "Удалите поражённые листья, фунгицид контактный/системный. Не работайте по мокрой кроне.",
    ),
    "Septoria": (
        "fungal",
        "Септориоз",
        "Septoria leaf spot",
        "Много мелких круглых пятен с тёмной каймой и светлым центром.",
        "Удалите нижние листья, фунгицид, мульча, полив без брызг на крону.",
    ),
    "Leaf_mold": (
        "fungal",
        "Бурая пятнистость / плесень листа",
        "Leaf mold",
        "Жёлтые пятна сверху, оливковый или бурый налёт снизу.",
        "Снизьте влажность воздуха, усильте вентиляцию. Удалите поражённые листья, примените фунгицид.",
    ),
    "Scab": (
        "fungal",
        "Парша",
        "Scab",
        "Оливково-бурые бархатистые пятна, деформация листа.",
        "Удалите поражённые листья и опад. Фунгицид на основе меди или системный по схеме культуры.",
    ),
    "Black_rot": (
        "fungal",
        "Чёрная гниль",
        "Black rot",
        "Концентрические тёмные пятна, усыхание ткани, иногда мумификация плодов.",
        "Вырежьте сухие части, уничтожьте мумии. Медьсодержащие обработки в период риска.",
    ),
    "Bacterial_spot": (
        "bacterial",
        "Бактериальная пятнистость",
        "Bacterial spot",
        "Мелкие тёмные водянистые пятна с жёлтым ореолом, возможна дырчатость.",
        "Удалите больные листья стерильным инструментом. Медьсодержащий препарат. Не смачивайте крону.",
    ),
    "Bacterial_blight": (
        "bacterial",
        "Бактериальный ожог / бактериоз",
        "Bacterial blight",
        "Водянистые зоны, почернение жилок, быстрое увядание участков листа.",
        "Удалите поражённые побеги. Медь, дезинфекция инструмента. Избегайте травм и переувлажнения.",
    ),
    "Citrus_greening": (
        "bacterial",
        "Позеленение цитрусовых (HLB)",
        "Huanglongbing",
        "Асимметричный хлороз, желтоватые прожилки, деформация листа.",
        "Контроль переносчиков (листоблошка). Удалите сильно больные растения. Используйте здоровый посадочный материал.",
    ),
    "Virus_mosaic": (
        "viral",
        "Вирусная мозаика",
        "Mosaic virus",
        "Мозаичный светло/тёмно-зелёный узор, деформация, измельчение листа.",
        "Удалите больные растения. Дезинфицируйте инструмент. Контролируйте сосущих вредителей-переносчиков.",
    ),
    "Virus_curl": (
        "viral",
        "Вирусная курчавость / желтуха",
        "Leaf curl / yellow leaf curl",
        "Пожелтение, скручивание и измельчение листьев, угнетённый рост.",
        "Удалите сильно больные кусты. Контроль белокрылки/тли. Устойчивые сорта при посадке.",
    ),
    "Spider_mites": (
        "pest",
        "Паутинный клещ",
        "Spider mites",
        "Мелкая крапчатость, бронзовость, тонкая паутина у черешков.",
        "Обмойте листья, повысьте влажность. Акарицид или масло нима 2–3 раза с интервалом 5–7 дней.",
    ),
    "Aphids": (
        "pest",
        "Тля",
        "Aphids",
        "Скручивание молодых листьев, липкая медвяная роса, колонии на побегах.",
        "Смойте струёй воды, обработайте инсектицидом/мыльным раствором. Повторите через несколько дней.",
    ),
    "Scale": (
        "pest",
        "Щитовка / червец",
        "Scale / mealybug",
        "Липкость, светлые или бурые щитки на листьях и черешках, ослабление растения.",
        "Снимите вредителей вручную. Масло нима / системный инсектицид, повторите обработку.",
    ),
    "Whitefly": (
        "pest",
        "Белокрылка",
        "Whitefly",
        "Мелкие белые насекомые при встряхивании, пожелтение, липкость.",
        "Жёлтые ловушки, инсектицид, обработка нижней стороны листьев. Повторите цикл.",
    ),
    "Chlorosis": (
        "abiotic",
        "Хлороз",
        "Chlorosis",
        "Межжилковое пожелтение или общее побледнение при зелёных жилках.",
        "Проверьте дренаж и pH. Подкормите хелатным железом / комплексным удобрением.",
    ),
    "Sunburn": (
        "abiotic",
        "Солнечный ожог",
        "Sunburn",
        "Сухие белёсые или бурые пятна на стороне, обращённой к солнцу.",
        "Притените от полуденного солнца. Приучайте к свету постепенно. Обрежьте сильно повреждённые участки.",
    ),
    "Leaf_scorch": (
        "abiotic",
        "Ожог / краевой некроз",
        "Leaf scorch / tip burn",
        "Сухие бурые края и кончики листа.",
        "Смягчите воду, не пересушивайте воздух, полив под корень. Уберите от батарей/иссушающего ветра.",
    ),
    "Overwatering": (
        "abiotic",
        "Переувлажнение",
        "Overwatering",
        "Мягкие желтеющие листья при влажном субстрате, вялость.",
        "Дайте субстрату просохнуть. Проверьте дренаж. При гнили корней — пересадка и обрезка корней.",
    ),
    "Edema": (
        "abiotic",
        "Отёк листьев",
        "Edema",
        "Пробковые бугорки на нижней стороне листа.",
        "Поливайте реже и равномернее. Больше света и воздуха. Не опрыскивайте при прохладе.",
    ),
    "Needle_cast": (
        "fungal",
        "Усыхание хвои / шютте",
        "Needle cast",
        "Пожелтение и осыпание хвои, побурение кончиков.",
        "Соберите опад. Фунгицид для хвойных весной и осенью. Улучшите проветривание посадок.",
    ),
    "Esca": (
        "fungal",
        "Эска (чёрная корь винограда)",
        "Esca",
        "Межжилковый хлороз и некроз «тигровый» узор на листьях.",
        "Вырежьте поражённую древесину. Обработайте срезы. Избегайте крупных ран при обрезке.",
    ),
    "Target_spot": (
        "fungal",
        "Коричневая / целевая пятнистость",
        "Target spot",
        "Пятна с концентрическими кольцами на листьях.",
        "Удалите больные листья. Фунгицид. Снизьте влажность на листе.",
    ),
    "Gray_leaf_spot": (
        "fungal",
        "Серая пятнистость",
        "Gray leaf spot",
        "Прямоугольные серо-бурые пятна вдоль жилок.",
        "Севооборот, устойчивые гибриды. При сильном развитии — фунгицид по листу.",
    ),
    "Northern_blight": (
        "fungal",
        "Северный гельминтоспориоз",
        "Northern leaf blight",
        "Удлинённые серо-зелёные или бурые поражения.",
        "Заделайте растительные остатки, севооборот. Фунгицид в фазу риска.",
    ),
    "Leaf_blight": (
        "fungal",
        "Ожог / пятнистость листьев",
        "Leaf blight",
        "Тёмные угловатые пятна, краевой некроз.",
        "Санитарная обрезка, фунгицид, улучшение проветривания.",
    ),
    "Leaf_scorch_strawberry": (
        "fungal",
        "Ожог листьев земляники",
        "Leaf scorch",
        "Пурпурно-бурые пятна, ожог края листа.",
        "Удалите старые листья. Фунгицид во влажную погоду. Не загущайте посадки.",
    ),
    "Cedar_rust": (
        "fungal",
        "Ржавчина (яблоня–можжевельник)",
        "Cedar apple rust",
        "Ярко-оранжевые/ржавые пятна на верхней стороне листа.",
        "Удалите можжевельник-хозяина по возможности. Фунгицид в фазу розового бутона.",
    ),
    "Sooty_mold": (
        "fungal",
        "Чернь (сажистый гриб)",
        "Sooty mold",
        "Чёрный сажистый налёт, часто поверх медвяной росы вредителей.",
        "Смойте налёт. Уничтожьте тлю/щитовку/белокрылку. Поддерживайте чистоту листьев.",
    ),
}

# Which disease templates apply to which habitat categories
HABITAT_DISEASES = {
    "Овощная / полевая культура": [
        "healthy", "Powdery_mildew", "Downy_mildew", "Early_blight", "Late_blight",
        "Bacterial_spot", "Bacterial_blight", "Septoria", "Leaf_spot", "Spider_mites",
        "Aphids", "Chlorosis", "Overwatering", "Virus_mosaic", "Whitefly",
    ],
    "Ягодная культура": [
        "healthy", "Powdery_mildew", "Leaf_spot", "Anthracnose", "Rust",
        "Spider_mites", "Aphids", "Chlorosis", "Leaf_scorch", "Virus_mosaic",
    ],
    "Плодовое дерево": [
        "healthy", "Scab", "Black_rot", "Cedar_rust", "Powdery_mildew",
        "Bacterial_spot", "Rust", "Chlorosis", "Leaf_scorch", "Aphids", "Scale",
    ],
    "Дерево": [
        "healthy", "Leaf_spot", "Anthracnose", "Powdery_mildew", "Rust",
        "Chlorosis", "Leaf_scorch", "Needle_cast", "Scale", "Aphids",
    ],
    "Кустарник": [
        "healthy", "Powdery_mildew", "Leaf_spot", "Rust", "Aphids",
        "Spider_mites", "Chlorosis", "Scale", "Anthracnose",
    ],
    "Садовое декоративное": [
        "healthy", "Black_spot", "Powdery_mildew", "Rust", "Spider_mites",
        "Aphids", "Chlorosis", "Leaf_scorch", "Botrytis_like",
    ],
    "Пряная / лекарственная трава": [
        "healthy", "Powdery_mildew", "Leaf_spot", "Rust", "Aphids",
        "Spider_mites", "Chlorosis", "Overwatering",
    ],
    "Тропическое / экзотическое": [
        "healthy", "Leaf_spot", "Anthracnose", "Spider_mites", "Scale",
        "Chlorosis", "Sunburn", "Overwatering", "Bacterial_spot", "Sooty_mold",
        "Powdery_mildew", "Whitefly",
    ],
    "Комнатное растение": [
        "healthy", "Powdery_mildew", "Spider_mites", "Scale", "Chlorosis",
        "Sunburn", "Overwatering", "Leaf_scorch", "Edema", "Bacterial_spot",
        "Sooty_mold", "Whitefly",
    ],
    "Суккулент / кактус": [
        "healthy", "Overwatering", "Sunburn", "Scale", "Edema", "Chlorosis", "Leaf_spot",
    ],
    "Папоротник / влаголюбивое": [
        "healthy", "Leaf_scorch", "Chlorosis", "Scale", "Overwatering", "Leaf_spot",
    ],
    "Лиана / вьющееся": [
        "healthy", "Powdery_mildew", "Spider_mites", "Aphids", "Leaf_spot",
        "Chlorosis", "Scale", "Anthracnose",
    ],
    "Садовая культура": [
        "healthy", "Powdery_mildew", "Black_rot", "Esca", "Leaf_blight",
        "Downy_mildew", "Spider_mites", "Chlorosis",
    ],
    "Полевая культура": [
        "healthy", "Rust", "Gray_leaf_spot", "Northern_blight", "Leaf_spot",
        "Chlorosis", "Aphids",
    ],
    "—": ["healthy"],
}

# Alias for garden Botrytis-like using Leaf_mold text
DISEASE_TEMPLATES["Botrytis_like"] = (
    "fungal",
    "Серая гниль / ботритис",
    "Botrytis / gray mold",
    "Бурые мокнущие зоны, серый пушистый налёт во влажную погоду.",
    "Удалите поражённые части. Улучшите проветривание. Фунгицид против серой гнили.",
)

HABITAT_PREFIX = {
    "CROP": ("Овощная / полевая культура", "Crop"),
    "BERRY": ("Ягодная культура", "Berry"),
    "FRUIT": ("Плодовое дерево", "Fruit"),
    "TREE": ("Дерево", "Tree"),
    "SHRUB": ("Кустарник", "Shrub"),
    "GARDEN": ("Садовое декоративное", "Garden"),
    "HERB": ("Пряная / лекарственная трава", "Herb"),
    "TROPICAL": ("Тропическое / экзотическое", "Tropical"),
    "HOUSE": ("Комнатное растение", "House"),
    "SUCCULENT": ("Суккулент / кактус", "Succulent"),
    "FERN": ("Папоротник / влаголюбивое", "Fern"),
    "VINE": ("Лиана / вьющееся", "Vine"),
}


def slug(s: str) -> str:
    return re.sub(r"[^A-Za-z0-9_]+", "_", s).strip("_")


def parse_ornamental_species(path: Path) -> list[tuple[str, str, str]]:
    """Return list of (species_id, name_ru, habitat_enum)."""
    text = path.read_text(encoding="utf-8")
    rows = []
    for m in re.finditer(
        r'Species\("([^"]+)",\s*"([^"]+)",\s*Habitat\.([A-Z_]+)\)',
        text,
    ):
        rows.append((m.group(1), m.group(2), m.group(3)))
    return rows


def parse_plantvillage(path: Path) -> list[tuple[str, str, str]]:
    """Return (full_label, plant_en, disease_en)."""
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or "___" not in line:
            if line == "Background":
                rows.append(("Background", "Background", "Background"))
            continue
        plant, disease = line.split("___", 1)
        rows.append((line, plant, disease))
    return rows


def map_pv_disease(disease_en: str) -> str:
    d = disease_en.lower()
    if "healthy" in d:
        return "healthy"
    if "powdery" in d:
        return "Powdery_mildew"
    if "scab" in d:
        return "Scab"
    if "black_rot" in d or "black rot" in d:
        return "Black_rot"
    if "cedar" in d or "rust" in d and "common" not in d:
        if "cedar" in d:
            return "Cedar_rust"
        if "common_rust" in d or "common rust" in d:
            return "Rust"
        return "Rust"
    if "cercospora" in d or "gray_leaf" in d:
        return "Gray_leaf_spot"
    if "northern" in d:
        return "Northern_blight"
    if "esca" in d:
        return "Esca"
    if "isariopsis" in d or "leaf_blight" in d:
        return "Leaf_blight"
    if "haunglongbing" in d or "greening" in d:
        return "Citrus_greening"
    if "bacterial" in d:
        return "Bacterial_spot"
    if "early_blight" in d:
        return "Early_blight"
    if "late_blight" in d:
        return "Late_blight"
    if "leaf_mold" in d:
        return "Leaf_mold"
    if "septoria" in d:
        return "Septoria"
    if "spider" in d or "mite" in d:
        return "Spider_mites"
    if "target" in d:
        return "Target_spot"
    if "yellow_leaf_curl" in d or "curl" in d:
        return "Virus_curl"
    if "mosaic" in d:
        return "Virus_mosaic"
    if "scorch" in d:
        return "Leaf_scorch_strawberry"
    return "Leaf_spot"


def build() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    if OUT_DB.exists():
        OUT_DB.unlink()

    conn = sqlite3.connect(OUT_DB)
    cur = conn.cursor()
    cur.executescript(
        """
        CREATE TABLE plants (
            id TEXT PRIMARY KEY,
            name_ru TEXT NOT NULL,
            name_en TEXT NOT NULL,
            category TEXT NOT NULL
        );
        CREATE TABLE diseases (
            id TEXT PRIMARY KEY,
            name_ru TEXT NOT NULL,
            name_en TEXT NOT NULL,
            kind TEXT NOT NULL
        );
        CREATE TABLE plant_diseases (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            plant_id TEXT NOT NULL,
            disease_id TEXT NOT NULL,
            symptoms_ru TEXT NOT NULL,
            treatment_ru TEXT NOT NULL,
            aliases TEXT NOT NULL DEFAULT '',
            UNIQUE(plant_id, disease_id),
            FOREIGN KEY(plant_id) REFERENCES plants(id),
            FOREIGN KEY(disease_id) REFERENCES diseases(id)
        );
        CREATE TABLE class_map (
            class_key TEXT PRIMARY KEY,
            plant_disease_id INTEGER NOT NULL,
            FOREIGN KEY(plant_disease_id) REFERENCES plant_diseases(id)
        );
        CREATE TABLE meta (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL
        );
        """
    )

    # diseases table
    for did, (kind, name_ru, name_en, _, _) in DISEASE_TEMPLATES.items():
        cur.execute(
            "INSERT INTO diseases(id, name_ru, name_en, kind) VALUES (?,?,?,?)",
            (did, name_ru, name_en, kind),
        )

    plants: dict[str, tuple[str, str, str]] = {}
    # plant_id -> (name_ru, name_en, category)

    def ensure_plant(pid: str, name_ru: str, name_en: str, category: str) -> None:
        if pid not in plants:
            plants[pid] = (name_ru, name_en, category)
            cur.execute(
                "INSERT INTO plants(id, name_ru, name_en, category) VALUES (?,?,?,?)",
                (pid, name_ru, name_en, category),
            )

    def ensure_pd(plant_id: str, disease_id: str, aliases: str = "") -> int:
        kind, name_ru, name_en, symptoms, treatment = DISEASE_TEMPLATES[disease_id]
        # Customize slightly with plant name
        plant_ru = plants[plant_id][0]
        symptoms_full = symptoms
        treatment_full = f"{treatment} Культура: {plant_ru}."
        cur.execute(
            """
            INSERT OR IGNORE INTO plant_diseases(plant_id, disease_id, symptoms_ru, treatment_ru, aliases)
            VALUES (?,?,?,?,?)
            """,
            (plant_id, disease_id, symptoms_full, treatment_full, aliases),
        )
        cur.execute(
            "SELECT id FROM plant_diseases WHERE plant_id=? AND disease_id=?",
            (plant_id, disease_id),
        )
        return int(cur.fetchone()[0])

    def map_class(class_key: str, pd_id: int) -> None:
        cur.execute(
            "INSERT OR REPLACE INTO class_map(class_key, plant_disease_id) VALUES (?,?)",
            (class_key, pd_id),
        )

    # --- PlantVillage labels ---
    for full, plant_en, disease_en in parse_plantvillage(LABELS):
        if plant_en == "Background":
            ensure_plant("pv_Background", "—", "Background", "—")
            pd = ensure_pd("pv_Background", "healthy", aliases=full)
            # special background text
            cur.execute(
                "UPDATE plant_diseases SET symptoms_ru=?, treatment_ru=? WHERE id=?",
                (
                    "На фото нет листа растения. Снимите лист ближе, в рамке, при ровном свете.",
                    "Переснимите лист крупнее, в рамке, без сильных бликов.",
                    pd,
                ),
            )
            map_class(full, pd)
            map_class("Background", pd)
            continue

        name_ru, category = PV_PLANT_RU.get(plant_en, (plant_en, "Растение"))
        pid = f"pv_{plant_en}"
        ensure_plant(pid, name_ru, plant_en, category)
        dkey = map_pv_disease(disease_en)
        if dkey not in DISEASE_TEMPLATES:
            dkey = "Leaf_spot"
        # Prefer plant-specific disease name for PV where we have exact templates
        pd = ensure_pd(pid, dkey, aliases=full)
        # Override disease display via joining — also store PV-specific Russian disease title in aliases
        # Update plant_diseases symptoms with more specific note
        cur.execute(
            "UPDATE plant_diseases SET aliases=? WHERE id=?",
            (full, pd),
        )
        map_class(full, pd)

    # Enrich PV with extra common diseases per crop category
    for plant_en, (name_ru, category) in PV_PLANT_RU.items():
        if plant_en == "Background":
            continue
        pid = f"pv_{plant_en}"
        ensure_plant(pid, name_ru, plant_en, category)
        for dkey in HABITAT_DISEASES.get(category, ["healthy", "Leaf_spot"]):
            if dkey in DISEASE_TEMPLATES:
                ensure_pd(pid, dkey)

    # --- Ornamental species ---
    for sid, name_ru, habitat in parse_ornamental_species(ORNAMENTAL):
        cat, prefix = HABITAT_PREFIX[habitat]
        pid = f"{prefix}_{sid}"
        ensure_plant(pid, name_ru, sid, cat)
        for dkey in HABITAT_DISEASES.get(cat, ["healthy", "Leaf_spot", "Chlorosis"]):
            if dkey not in DISEASE_TEMPLATES:
                continue
            pd = ensure_pd(pid, dkey)
            # ornamental class keys used by diagnoser: Prefix_Species___diseaseKey
            map_class(f"{prefix}_{sid}___{dkey}", pd)
            # also map common ornamental disease keys that differ
            if dkey == "Black_spot":
                map_class(f"{prefix}_{sid}___Black_spot", pd)

    # Map House___* legacy ids if any
    for dkey in HABITAT_DISEASES["Комнатное растение"]:
        if dkey not in DISEASE_TEMPLATES:
            continue
        # generic house plant
        ensure_plant("House_Generic", "Комнатное растение", "Houseplant", "Комнатное растение")
        pd = ensure_pd("House_Generic", dkey)
        map_class(f"House___{dkey}", pd)

    # Counts
    n_plants = cur.execute("SELECT COUNT(*) FROM plants").fetchone()[0]
    n_diseases = cur.execute("SELECT COUNT(*) FROM diseases").fetchone()[0]
    n_pd = cur.execute("SELECT COUNT(*) FROM plant_diseases").fetchone()[0]
    n_map = cur.execute("SELECT COUNT(*) FROM class_map").fetchone()[0]

    for k, v in {
        "plants": str(n_plants),
        "diseases": str(n_diseases),
        "plant_diseases": str(n_pd),
        "class_map": str(n_map),
        "version": "1",
    }.items():
        cur.execute("INSERT INTO meta(key, value) VALUES (?,?)", (k, v))

    conn.commit()
    conn.close()
    print(f"Wrote {OUT_DB}")
    print(f"plants={n_plants} diseases={n_diseases} plant_diseases={n_pd} class_map={n_map}")


if __name__ == "__main__":
    build()
