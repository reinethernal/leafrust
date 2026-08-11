package com.leafrust.data.ai

/** Human-readable plant group shown in UI. */
object PlantTaxonomy {
    fun categoryFor(id: String): String = when {
        id.equals("Background", ignoreCase = true) -> "—"
        id.startsWith("Crop_") -> "Овощная / полевая культура"
        id.startsWith("Berry_") -> "Ягодная культура"
        id.startsWith("Fruit_") -> "Плодовое дерево"
        id.startsWith("Tree_") -> "Дерево"
        id.startsWith("Shrub_") -> "Кустарник"
        id.startsWith("Garden_") -> "Садовое декоративное"
        id.startsWith("Herb_") -> "Пряная / лекарственная трава"
        id.startsWith("Tropical_") -> "Тропическое / экзотическое"
        id.startsWith("House_") -> "Комнатное растение"
        id.startsWith("Succulent_") -> "Суккулент / кактус"
        id.startsWith("Fern_") -> "Папоротник / влаголюбивое"
        id.startsWith("Vine_") -> "Лиана / вьющееся"
        id.startsWith("Apple") || id.startsWith("Cherry") || id.startsWith("Peach") ||
            id.startsWith("Orange") -> "Плодовое дерево"
        id.startsWith("Grape") -> "Садовая культура"
        id.startsWith("Blueberry") || id.startsWith("Raspberry") || id.startsWith("Strawberry") ->
            "Ягодная культура"
        id.startsWith("Tomato") || id.startsWith("Potato") || id.startsWith("Pepper") ||
            id.startsWith("Squash") -> "Овощная культура"
        id.startsWith("Corn") || id.startsWith("Soybean") -> "Полевая культура"
        else -> "Растение"
    }
}
