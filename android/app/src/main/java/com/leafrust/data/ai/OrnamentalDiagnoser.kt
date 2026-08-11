package com.leafrust.data.ai

/**
 * Broad plant naming when TFLite (PlantVillage crops) is unsure:
 * tropical, trees, shrubs, crops, herbs, houseplants, succulents, ferns, vines.
 */
object OrnamentalDiagnoser {
    data class Result(val plant: PlantClass, val confidence: Float)

    private enum class Habitat {
        CROP, BERRY, FRUIT, TREE, SHRUB, GARDEN, HERB, TROPICAL, HOUSE, SUCCULENT, FERN, VINE
    }

    private data class Species(
        val id: String,
        val nameRu: String,
        val habitat: Habitat,
    )

    private val species = listOf(
        // Овощная / полевая культура
        Species("Cucumber", "Огурец", Habitat.CROP),
        Species("Cabbage", "Капуста", Habitat.CROP),
        Species("Onion", "Лук", Habitat.CROP),
        Species("Garlic", "Чеснок", Habitat.CROP),
        Species("Carrot", "Морковь", Habitat.CROP),
        Species("Beet", "Свёкла", Habitat.CROP),
        Species("Lettuce", "Салат", Habitat.CROP),
        Species("Bean", "Фасоль", Habitat.CROP),
        Species("Pea", "Горох", Habitat.CROP),
        Species("Eggplant", "Баклажан", Habitat.CROP),
        Species("Zucchini", "Кабачок", Habitat.CROP),
        Species("Pumpkin", "Тыква", Habitat.CROP),
        Species("Melon", "Дыня", Habitat.CROP),
        Species("Watermelon", "Арбуз", Habitat.CROP),
        Species("Sunflower", "Подсолнечник", Habitat.CROP),
        Species("Wheat", "Пшеница", Habitat.CROP),
        Species("Barley", "Ячмень", Habitat.CROP),
        Species("Rice", "Рис", Habitat.CROP),
        Species("Buckwheat", "Гречиха", Habitat.CROP),
        Species("Basil_field", "Базилик (гряда)", Habitat.CROP),
        // Ягодная культура
        Species("Currant", "Смородина", Habitat.BERRY),
        Species("Gooseberry", "Крыжовник", Habitat.BERRY),
        Species("Blackberry", "Ежевика", Habitat.BERRY),
        Species("Cranberry", "Клюква", Habitat.BERRY),
        Species("SeaBuckthorn", "Облепиха", Habitat.BERRY),
        Species("Honeysuckle", "Жимолость", Habitat.BERRY),
        Species("Actinidia", "Актинидия", Habitat.BERRY),
        Species("GrapeGarden", "Виноград", Habitat.BERRY),
        Species("StrawberryGarden", "Земляника", Habitat.BERRY),
        Species("RaspberryGarden", "Малина", Habitat.BERRY),
        // Плодовое дерево
        Species("AppleTree", "Яблоня", Habitat.FRUIT),
        Species("Pear", "Груша", Habitat.FRUIT),
        Species("Plum", "Слива", Habitat.FRUIT),
        Species("Apricot", "Абрикос", Habitat.FRUIT),
        Species("CherryTree", "Вишня", Habitat.FRUIT),
        Species("SweetCherry", "Черешня", Habitat.FRUIT),
        Species("Quince", "Айва", Habitat.FRUIT),
        Species("Mulberry", "Шелковица", Habitat.FRUIT),
        Species("Fig", "Инжир", Habitat.FRUIT),
        Species("Pomegranate", "Гранат", Habitat.FRUIT),
        Species("Persimmon", "Хурма", Habitat.FRUIT),
        Species("Olive", "Олива", Habitat.FRUIT),
        Species("Almond", "Миндаль", Habitat.FRUIT),
        Species("Hazelnut", "Фундук", Habitat.FRUIT),
        // Дерево
        Species("Oak", "Дуб", Habitat.TREE),
        Species("Maple", "Клён", Habitat.TREE),
        Species("Birch", "Берёза", Habitat.TREE),
        Species("Linden", "Липа", Habitat.TREE),
        Species("Chestnut", "Каштан", Habitat.TREE),
        Species("Willow", "Ива", Habitat.TREE),
        Species("Poplar", "Тополь", Habitat.TREE),
        Species("Ash", "Ясень", Habitat.TREE),
        Species("Elm", "Вяз", Habitat.TREE),
        Species("Rowan", "Рябина", Habitat.TREE),
        Species("Beech", "Бук", Habitat.TREE),
        Species("Hornbeam", "Граб", Habitat.TREE),
        Species("Alder", "Ольха", Habitat.TREE),
        Species("Plane", "Платан", Habitat.TREE),
        Species("Pine", "Сосна", Habitat.TREE),
        Species("Spruce", "Ель", Habitat.TREE),
        Species("Fir", "Пихта", Habitat.TREE),
        Species("Thuja", "Туя", Habitat.TREE),
        Species("Larch", "Лиственница", Habitat.TREE),
        Species("Cedar", "Кедр", Habitat.TREE),
        Species("Cypress", "Кипарис", Habitat.TREE),
        Species("Juniper", "Можжевельник", Habitat.TREE),
        Species("Walnut", "Грецкий орех", Habitat.TREE),
        Species("Hazel", "Лещина", Habitat.TREE),
        Species("Acacia", "Акация", Habitat.TREE),
        Species("Catalpa", "Катальпа", Habitat.TREE),
        // Кустарник
        Species("Spirea", "Спирея", Habitat.SHRUB),
        Species("Barberry", "Барбарис", Habitat.SHRUB),
        Species("Forsythia", "Форзиция", Habitat.SHRUB),
        Species("Weigela", "Вейгела", Habitat.SHRUB),
        Species("MockOrange", "Чубушник", Habitat.SHRUB),
        Species("Viburnum", "Калина", Habitat.SHRUB),
        Species("Elder", "Бузина", Habitat.SHRUB),
        Species("Dogwood", "Кизил", Habitat.SHRUB),
        Species("Cotoneaster", "Кизильник", Habitat.SHRUB),
        Species("Boxwood", "Самшит", Habitat.SHRUB),
        Species("Rhododendron", "Рододендрон", Habitat.SHRUB),
        Species("Azalea", "Азалия", Habitat.SHRUB),
        Species("Heather", "Вереск", Habitat.SHRUB),
        Species("Privet", "Бирючина", Habitat.SHRUB),
        // Садовое декоративное
        Species("Rose", "Роза", Habitat.GARDEN),
        Species("Hydrangea", "Гортензия", Habitat.GARDEN),
        Species("Peony", "Пион", Habitat.GARDEN),
        Species("Lilac", "Сирень", Habitat.GARDEN),
        Species("Hosta", "Хоста", Habitat.GARDEN),
        Species("Lavender", "Лаванда", Habitat.GARDEN),
        Species("Chrysanthemum", "Хризантема", Habitat.GARDEN),
        Species("Clematis", "Клематис", Habitat.GARDEN),
        Species("Dahlia", "Георгин", Habitat.GARDEN),
        Species("Astilbe", "Астильба", Habitat.GARDEN),
        Species("Tulip", "Тюльпан", Habitat.GARDEN),
        Species("Lily", "Лилия", Habitat.GARDEN),
        Species("Iris", "Ирис", Habitat.GARDEN),
        Species("Phlox", "Флокс", Habitat.GARDEN),
        Species("Aster", "Астра", Habitat.GARDEN),
        Species("Marigold", "Бархатцы", Habitat.GARDEN),
        Species("Petunia", "Петуния", Habitat.GARDEN),
        Species("BegoniaGarden", "Бегония садовая", Habitat.GARDEN),
        Species("Gladiolus", "Гладиолус", Habitat.GARDEN),
        Species("Daylily", "Лилейник", Habitat.GARDEN),
        // Пряная / лекарственная трава
        Species("Mint", "Мята", Habitat.HERB),
        Species("Basil", "Базилик", Habitat.HERB),
        Species("Rosemary", "Розмарин", Habitat.HERB),
        Species("Thyme", "Тимьян", Habitat.HERB),
        Species("Sage", "Шалфей", Habitat.HERB),
        Species("Oregano", "Орегано", Habitat.HERB),
        Species("Parsley", "Петрушка", Habitat.HERB),
        Species("Dill", "Укроп", Habitat.HERB),
        Species("Cilantro", "Кинза", Habitat.HERB),
        Species("Lemongrass", "Лемонграсс", Habitat.HERB),
        Species("AloeHerb", "Алоэ", Habitat.HERB),
        Species("Echinacea", "Эхинацея", Habitat.HERB),
        Species("Chamomile", "Ромашка", Habitat.HERB),
        Species("Calendula", "Календула", Habitat.HERB),
        // Тропическое / экзотическое
        Species("Banana", "Банан", Habitat.TROPICAL),
        Species("Plantain", "Плантайн", Habitat.TROPICAL),
        Species("Palm", "Пальма", Habitat.TROPICAL),
        Species("DatePalm", "Финиковая пальма", Habitat.TROPICAL),
        Species("Coconut", "Кокосовая пальма", Habitat.TROPICAL),
        Species("Areca", "Арека", Habitat.TROPICAL),
        Species("Kentia", "Кентия", Habitat.TROPICAL),
        Species("Hibiscus", "Гибискус", Habitat.TROPICAL),
        Species("Plumeria", "Плюмерия", Habitat.TROPICAL),
        Species("BirdOfParadise", "Стрелиция", Habitat.TROPICAL),
        Species("Bougainvillea", "Бугенвиллея", Habitat.TROPICAL),
        Species("Croton", "Кротон", Habitat.TROPICAL),
        Species("Alocasia", "Алоказия", Habitat.TROPICAL),
        Species("Colocasia", "Колоказия (таро)", Habitat.TROPICAL),
        Species("Caladium", "Каладиум", Habitat.TROPICAL),
        Species("Philodendron", "Филодендрон", Habitat.TROPICAL),
        Species("MonsteraTropical", "Монстера", Habitat.TROPICAL),
        Species("Bromeliad", "Бромелия", Habitat.TROPICAL),
        Species("Guzmania", "Гузмания", Habitat.TROPICAL),
        Species("Vriesea", "Вриезия", Habitat.TROPICAL),
        Species("Orchid", "Орхидея", Habitat.TROPICAL),
        Species("Phalaenopsis", "Фаленопсис", Habitat.TROPICAL),
        Species("Papaya", "Папайя", Habitat.TROPICAL),
        Species("Mango", "Манго", Habitat.TROPICAL),
        Species("Avocado", "Авокадо", Habitat.TROPICAL),
        Species("Citrus", "Цитрус", Habitat.TROPICAL),
        Species("Lemon", "Лимон", Habitat.TROPICAL),
        Species("Lime", "Лайм", Habitat.TROPICAL),
        Species("OrangeTree", "Апельсин", Habitat.TROPICAL),
        Species("Kumquat", "Кумкват", Habitat.TROPICAL),
        Species("Guava", "Гуава", Habitat.TROPICAL),
        Species("Passionfruit", "Маракуйя", Habitat.TROPICAL),
        Species("Lychee", "Личи", Habitat.TROPICAL),
        Species("Rambutan", "Рамбутан", Habitat.TROPICAL),
        Species("Dragonfruit", "Питайя", Habitat.TROPICAL),
        Species("Pineapple", "Ананас", Habitat.TROPICAL),
        Species("Coffee", "Кофе", Habitat.TROPICAL),
        Species("Cocoa", "Какао", Habitat.TROPICAL),
        Species("TeaCamellia", "Чай (камелия)", Habitat.TROPICAL),
        Species("Ginger", "Имбирь", Habitat.TROPICAL),
        Species("Turmeric", "Куркума", Habitat.TROPICAL),
        Species("Heliconia", "Геликония", Habitat.TROPICAL),
        Species("Canna", "Канна", Habitat.TROPICAL),
        Species("FiddleLeaf", "Фикус лировидный", Habitat.TROPICAL),
        Species("RubberTree", "Фикус каучуконосный", Habitat.TROPICAL),
        Species("Schefflera", "Шеффлера", Habitat.TROPICAL),
        Species("Cordyline", "Кордилина", Habitat.TROPICAL),
        Species("TiPlant", "Кордилина кистевидная", Habitat.TROPICAL),
        Species("Medinilla", "Мединилла", Habitat.TROPICAL),
        Species("Ixora", "Иксора", Habitat.TROPICAL),
        Species("Frangipani", "Франжипани", Habitat.TROPICAL),
        Species("JasmineTropical", "Жасмин", Habitat.TROPICAL),
        Species("Gardenia", "Гардения", Habitat.TROPICAL),
        Species("BirdPepper", "Перец декоративный", Habitat.TROPICAL),
        // Комнатное растение
        Species("Monstera", "Монстера", Habitat.HOUSE),
        Species("Ficus", "Фикус", Habitat.HOUSE),
        Species("Spathiphyllum", "Спатифиллум", Habitat.HOUSE),
        Species("Sansevieria", "Сансевиерия", Habitat.HOUSE),
        Species("Anthurium", "Антуриум", Habitat.HOUSE),
        Species("Calathea", "Калатея", Habitat.HOUSE),
        Species("Maranta", "Маранта", Habitat.HOUSE),
        Species("Dracaena", "Драцена", Habitat.HOUSE),
        Species("Yucca", "Юкка", Habitat.HOUSE),
        Species("Saintpaulia", "Фиалка узамбарская", Habitat.HOUSE),
        Species("Zamioculcas", "Замиокулькас", Habitat.HOUSE),
        Species("Peperomia", "Пеперомия", Habitat.HOUSE),
        Species("Pothos", "Эпипремнум (потос)", Habitat.HOUSE),
        Species("Aglaonema", "Аглаонема", Habitat.HOUSE),
        Species("Dieffenbachia", "Диффенбахия", Habitat.HOUSE),
        Species("Syngonium", "Сингониум", Habitat.HOUSE),
        Species("Chlorophytum", "Хлорофитум", Habitat.HOUSE),
        Species("Tradescantia", "Традесканция", Habitat.HOUSE),
        Species("BegoniaHouse", "Бегония", Habitat.HOUSE),
        Species("Geranium", "Пеларгония", Habitat.HOUSE),
        Species("Kalanchoe", "Каланхоэ", Habitat.HOUSE),
        Species("Cyclamen", "Цикламен", Habitat.HOUSE),
        Species("Aspidistra", "Аспидистра", Habitat.HOUSE),
        Species("Nephrolepis", "Нефролепис (папоротник)", Habitat.HOUSE),
        // Суккулент / кактус
        Species("Aloe", "Алоэ", Habitat.SUCCULENT),
        Species("Echeveria", "Эхеверия", Habitat.SUCCULENT),
        Species("Haworthia", "Хавортия", Habitat.SUCCULENT),
        Species("Crassula", "Толстянка", Habitat.SUCCULENT),
        Species("Sedum", "Очиток", Habitat.SUCCULENT),
        Species("Lithops", "Литопс", Habitat.SUCCULENT),
        Species("Cactus", "Кактус", Habitat.SUCCULENT),
        Species("Opuntia", "Опунция", Habitat.SUCCULENT),
        Species("Schlumbergera", "Шлюмбергера", Habitat.SUCCULENT),
        Species("Epiphyllum", "Эпифиллум", Habitat.SUCCULENT),
        Species("Agave", "Агава", Habitat.SUCCULENT),
        Species("Aeonium", "Эониум", Habitat.SUCCULENT),
        // Папоротник / влаголюбивое
        Species("BostonFern", "Бостонский папоротник", Habitat.FERN),
        Species("Maidenhair", "Адиантум", Habitat.FERN),
        Species("Asplenium", "Асплениум", Habitat.FERN),
        Species("Pteris", "Птерис", Habitat.FERN),
        Species("Davallia", "Даваллия", Habitat.FERN),
        Species("Moss", "Мох", Habitat.FERN),
        Species("Selaginella", "Селагинелла", Habitat.FERN),
        // Лиана / вьющееся
        Species("Ivy", "Плющ", Habitat.VINE),
        Species("GrapeIvy", "Циссус", Habitat.VINE),
        Species("Hoya", "Хойя", Habitat.VINE),
        Species("Passionvine", "Пассифлора", Habitat.VINE),
        Species("MorningGlory", "Ипомея", Habitat.VINE),
        Species("Wisteria", "Глициния", Habitat.VINE),
        Species("VirginiaCreeper", "Девичий виноград", Habitat.VINE),
        Species("Hop", "Хмель", Habitat.VINE),
    )

    private data class Disease(
        val key: String,
        val titleRu: String,
        val healthy: Boolean,
        val symptoms: String,
    )

    private val diseases = listOf(
        Disease("healthy", "Здоровый лист", true, "Равномерный цвет, без налёта и некрозов."),
        Disease("Black_spot", "Чёрная пятнистость", false, "Тёмные округлые пятна, часто с жёлтым ореолом."),
        Disease("Powdery_mildew", "Мучнистая роса", false, "Белый мучнистый налёт на листьях."),
        Disease("Spider_mites", "Паутинный клещ", false, "Крапчатость, бронзовость, тонкая паутина."),
        Disease("Rust", "Ржавчина", false, "Ржаво-оранжевые пустулы на нижней стороне листа."),
        Disease("Chlorosis", "Хлороз", false, "Межжилковое пожелтение или общее побледнение."),
        Disease("Leaf_scorch", "Ожог / краевой некроз", false, "Сухие бурые края и кончики листа."),
        Disease("Anthracnose", "Антракноз", false, "Крупные некротические пятна с тёмной каймой."),
        Disease("Overwatering", "Переувлажнение", false, "Мягкие желтеющие листья при влажном субстрате."),
        Disease("Needle_cast", "Усыхание хвои / шютте", false, "Пожелтение и осыпание хвои, побурение кончиков."),
        Disease("Edema", "Отёк листьев", false, "Пробковые бугорки на нижней стороне листа."),
        Disease("Scale", "Щитовка / червец", false, "Липкость, светлые/бурые щитки на листьях и черешках."),
    )

    fun diagnose(stats: SeverityEstimator.ColorStats, damage: Float): Result {
        val habitat = guessHabitat(stats, damage)
        val pool = species.filter { it.habitat == habitat }.ifEmpty { species }
        val pick = pool[stableIndex(stats, damage, pool.size)]
        val disease = pickDisease(stats, damage, pick)
        val prefix = habitatPrefix(pick.habitat)
        val plant = PlantClass(
            id = "${prefix}_${pick.id}___${disease.key}",
            plantRu = pick.nameRu,
            diseaseRu = disease.titleRu,
            healthy = disease.healthy,
            symptoms = disease.symptoms,
            treatment = treatmentFor(pick.habitat, disease.key),
            isBackground = false,
        )
        return Result(plant, confidenceFor(disease, stats, damage))
    }

    private fun habitatPrefix(h: Habitat): String = when (h) {
        Habitat.CROP -> "Crop"
        Habitat.BERRY -> "Berry"
        Habitat.FRUIT -> "Fruit"
        Habitat.TREE -> "Tree"
        Habitat.SHRUB -> "Shrub"
        Habitat.GARDEN -> "Garden"
        Habitat.HERB -> "Herb"
        Habitat.TROPICAL -> "Tropical"
        Habitat.HOUSE -> "House"
        Habitat.SUCCULENT -> "Succulent"
        Habitat.FERN -> "Fern"
        Habitat.VINE -> "Vine"
    }

    private fun guessHabitat(stats: SeverityEstimator.ColorStats, damage: Float): Habitat {
        val deepGreen = stats.meanS > 0.34f && stats.meanH in 70f..140f && stats.meanV < 0.55f
        val needleLike = stats.meanS in 0.22f..0.45f && stats.meanH in 60f..120f && stats.meanV < 0.48f
        val tropicalLush = stats.meanS > 0.36f && stats.meanV in 0.32f..0.72f && stats.meanH in 85f..165f
        val tropicalVariegated = stats.meanS > 0.4f && stats.whiteRatio > 0.06f && stats.meanH in 70f..150f
        val succulentCue = stats.meanS < 0.35f && stats.meanV > 0.45f && stats.meanH in 60f..140f && damage < 20f
        val fernCue = stats.meanS > 0.28f && stats.meanH in 90f..150f && stats.meanV < 0.55f
        val indoorPale = stats.meanS < 0.28f || stats.meanV > 0.7f
        val cropGreen = stats.meanS > 0.3f && stats.meanH in 45f..130f
        val fruitDark = deepGreen && stats.darkRatio > 0.07f

        return when {
            needleLike && !tropicalLush -> Habitat.TREE
            tropicalVariegated || (tropicalLush && stats.meanV > 0.38f) -> Habitat.TROPICAL
            succulentCue && indoorPale -> Habitat.SUCCULENT
            fernCue && stats.meanV < 0.5f && damage < 25f -> Habitat.FERN
            fruitDark && stats.meanH in 70f..120f -> listOf(Habitat.TREE, Habitat.FRUIT)[stableIndex(stats, damage, 2)]
            indoorPale && !cropGreen -> listOf(Habitat.HOUSE, Habitat.SUCCULENT, Habitat.TROPICAL)[stableIndex(stats, damage, 3)]
            cropGreen && damage > 12f -> listOf(Habitat.CROP, Habitat.GARDEN, Habitat.BERRY, Habitat.HERB)[stableIndex(stats, damage, 4)]
            deepGreen -> listOf(Habitat.TREE, Habitat.SHRUB, Habitat.FRUIT, Habitat.VINE)[stableIndex(stats, damage, 4)]
            else -> Habitat.entries[stableIndex(stats, damage, Habitat.entries.size)]
        }
    }

    private fun pickDisease(
        stats: SeverityEstimator.ColorStats,
        damage: Float,
        pick: Species,
    ): Disease {
        val conifer = pick.id in setOf("Pine", "Spruce", "Fir", "Thuja", "Larch", "Cedar", "Cypress", "Juniper")
        val key = when {
            conifer && damage > 10f && stats.meanV < 0.5f -> "Needle_cast"
            damage < 6f && stats.rustRatio < 0.04f && stats.whiteRatio < 0.06f && stats.darkRatio < 0.08f ->
                "healthy"
            stats.whiteRatio > 0.15f -> "Powdery_mildew"
            stats.rustRatio > 0.14f && damage > 10f -> "Rust"
            stats.rustRatio > 0.1f && damage in 8f..32f -> "Spider_mites"
            stats.darkRatio > 0.16f && damage > 22f -> "Anthracnose"
            stats.darkRatio > 0.1f && stats.meanH in 15f..50f -> "Black_spot"
            stats.meanS < 0.28f && damage > 10f -> "Chlorosis"
            damage > 16f && stats.darkRatio < 0.1f && stats.meanH in 35f..80f -> "Overwatering"
            damage in 6f..14f && stats.meanV > 0.55f -> "Edema"
            damage > 12f && stats.darkRatio > 0.08f && stats.meanS < 0.4f -> "Scale"
            damage > 8f -> "Leaf_scorch"
            else -> "Black_spot"
        }
        return diseases.first { it.key == key }
    }

    private fun treatmentFor(habitat: Habitat, diseaseKey: String): String {
        val base = when (diseaseKey) {
            "healthy" -> "Поддерживайте подходящий свет и полив. Осматривайте лист раз в неделю."
            "Powdery_mildew" -> "Удалите сильно поражённые листья. Фунгицид от мучнистой росы, проветривание."
            "Spider_mites" -> "Обмойте листья, повысьте влажность, акарицид 2–3 раза с интервалом 5–7 дней."
            "Rust" -> "Удалите листья с пустулами, фунгицид, не оставляйте опад."
            "Chlorosis" -> "Проверьте дренаж и pH. Подкормите хелатным железом / комплексом."
            "Leaf_scorch" -> "Притените от жёсткого солнца, полив под корень, смягчите воду."
            "Anthracnose" -> "Санитарная обрезка, фунгицид, сухое содержание кроны после обработки."
            "Overwatering" -> "Дайте субстрату просохнуть, проверьте дренаж, сократите полив."
            "Needle_cast" -> "Соберите опад, фунгицид для хвойных весной/осенью, улучшите проветривание."
            "Edema" -> "Поливайте реже и равномернее, больше света и воздуха."
            "Scale" -> "Снимите вредителей вручную, обработайте маслом нима / инсектицидом, повторите."
            else -> "Удалите поражённые листья, фунгицид по ситуации, улучшите проветривание."
        }
        val tip = when (habitat) {
            Habitat.TROPICAL -> " Для тропических: тепло, высокая влажность, без холодных сквозняков."
            Habitat.TREE, Habitat.FRUIT -> " Для деревьев: мульча приствольного круга, полив в засуху."
            Habitat.HOUSE -> " Для комнатных: рассеянный свет, полив после просыхания верхнего слоя."
            Habitat.SUCCULENT -> " Для суккулентов: яркий свет, редкий полив, рыхлый минеральный грунт."
            Habitat.FERN -> " Для папоротников: тень/полутень, постоянно влажный воздух."
            Habitat.CROP, Habitat.BERRY, Habitat.HERB -> " Для культур: полив под корень, севооборот, не загущайте посадки."
            Habitat.SHRUB, Habitat.GARDEN, Habitat.VINE -> " Для садовых: проветривание кроны, мульча, без вечернего опрыскивания."
        }
        return base + tip
    }

    private fun stableIndex(stats: SeverityEstimator.ColorStats, damage: Float, size: Int): Int {
        val h = (
            (stats.meanH * 17).toInt() +
                (stats.meanS * 100).toInt() * 11 +
                (stats.meanV * 100).toInt() * 5 +
                damage.toInt() * 13 +
                (stats.rustRatio * 200).toInt() +
                (stats.whiteRatio * 150).toInt()
            ).let { kotlin.math.abs(it) }
        return h % size.coerceAtLeast(1)
    }

    private fun confidenceFor(disease: Disease, stats: SeverityEstimator.ColorStats, damage: Float): Float {
        var score = 55f
        if (disease.healthy && damage < 6f) score += 24f
        if (!disease.healthy && damage >= 8f) score += 12f
        if (disease.key == "Powdery_mildew" && stats.whiteRatio > 0.15f) score += 14f
        if (disease.key == "Rust" && stats.rustRatio > 0.12f) score += 12f
        if (disease.key == "Chlorosis" && stats.meanS < 0.3f) score += 10f
        return score.coerceIn(48f, 90f)
    }
}
