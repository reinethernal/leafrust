export type PlantClass = {
  id: string;
  plantEn: string;
  plantRu: string;
  diseaseEn: string;
  diseaseRu: string;
  healthy: boolean;
  symptoms: string;
};

/** PlantVillage-style 38 classes with Russian labels */
export const PLANT_CLASSES: PlantClass[] = [
  { id: 'Apple___Apple_scab', plantEn: 'Apple', plantRu: 'Яблоня', diseaseEn: 'Apple scab', diseaseRu: 'Парша яблони', healthy: false, symptoms: 'Оливково-бурые пятна на листьях, бархатистый налёт.' },
  { id: 'Apple___Black_rot', plantEn: 'Apple', plantRu: 'Яблоня', diseaseEn: 'Black rot', diseaseRu: 'Чёрная гниль', healthy: false, symptoms: 'Концентрические тёмные пятна, усыхание ткани.' },
  { id: 'Apple___Cedar_apple_rust', plantEn: 'Apple', plantRu: 'Яблоня', diseaseEn: 'Cedar apple rust', diseaseRu: 'Ржавчина яблони', healthy: false, symptoms: 'Ярко-оранжевые/ржавые пятна на верхней стороне листа.' },
  { id: 'Apple___healthy', plantEn: 'Apple', plantRu: 'Яблоня', diseaseEn: 'Healthy', diseaseRu: 'Здоровый лист', healthy: true, symptoms: 'Равномерный зелёный цвет без очагов поражения.' },
  { id: 'Blueberry___healthy', plantEn: 'Blueberry', plantRu: 'Голубика', diseaseEn: 'Healthy', diseaseRu: 'Здоровый лист', healthy: true, symptoms: 'Чистый зелёный лист без пятен.' },
  { id: 'Cherry___Powdery_mildew', plantEn: 'Cherry', plantRu: 'Вишня', diseaseEn: 'Powdery mildew', diseaseRu: 'Мучнистая роса', healthy: false, symptoms: 'Белёсый мучнистый налёт на поверхности листа.' },
  { id: 'Cherry___healthy', plantEn: 'Cherry', plantRu: 'Вишня', diseaseEn: 'Healthy', diseaseRu: 'Здоровый лист', healthy: true, symptoms: 'Здоровый лист без налёта и некрозов.' },
  { id: 'Corn___Cercospora_leaf_spot', plantEn: 'Corn', plantRu: 'Кукуруза', diseaseEn: 'Cercospora leaf spot', diseaseRu: 'Серая пятнистость', healthy: false, symptoms: 'Прямоугольные серо-бурые пятна вдоль жилок.' },
  { id: 'Corn___Common_rust', plantEn: 'Corn', plantRu: 'Кукуруза', diseaseEn: 'Common rust', diseaseRu: 'Обыкновенная ржавчина', healthy: false, symptoms: 'Мелкие ржаво-коричневые пустулы на листе.' },
  { id: 'Corn___Northern_Leaf_Blight', plantEn: 'Corn', plantRu: 'Кукуруза', diseaseEn: 'Northern leaf blight', diseaseRu: 'Северный гельминтоспориоз', healthy: false, symptoms: 'Удлинённые серо-зелёные/бурые поражения.' },
  { id: 'Corn___healthy', plantEn: 'Corn', plantRu: 'Кукуруза', diseaseEn: 'Healthy', diseaseRu: 'Здоровый лист', healthy: true, symptoms: 'Здоровый лист кукурузы.' },
  { id: 'Grape___Black_rot', plantEn: 'Grape', plantRu: 'Виноград', diseaseEn: 'Black rot', diseaseRu: 'Чёрная гниль', healthy: false, symptoms: 'Круглые бурые пятна с тёмной каймой.' },
  { id: 'Grape___Esca', plantEn: 'Grape', plantRu: 'Виноград', diseaseEn: 'Esca', diseaseRu: 'Эска (чёрная корь)', healthy: false, symptoms: 'Межжилковый хлороз и некроз «тигровый» узор.' },
  { id: 'Grape___Leaf_blight', plantEn: 'Grape', plantRu: 'Виноград', diseaseEn: 'Leaf blight', diseaseRu: 'Пятнистость листьев', healthy: false, symptoms: 'Тёмные угловатые пятна, краевой некроз.' },
  { id: 'Grape___healthy', plantEn: 'Grape', plantRu: 'Виноград', diseaseEn: 'Healthy', diseaseRu: 'Здоровый лист', healthy: true, symptoms: 'Здоровый виноградный лист.' },
  { id: 'Orange___Haunglongbing', plantEn: 'Orange', plantRu: 'Апельсин', diseaseEn: 'Huanglongbing', diseaseRu: 'Позеленение цитрусовых', healthy: false, symptoms: 'Асимметричный хлороз, желтоватые прожилки.' },
  { id: 'Peach___Bacterial_spot', plantEn: 'Peach', plantRu: 'Персик', diseaseEn: 'Bacterial spot', diseaseRu: 'Бактериальная пятнистость', healthy: false, symptoms: 'Мелкие тёмные водянистые пятна, дырчатость.' },
  { id: 'Peach___healthy', plantEn: 'Peach', plantRu: 'Персик', diseaseEn: 'Healthy', diseaseRu: 'Здоровый лист', healthy: true, symptoms: 'Здоровый лист персика.' },
  { id: 'Pepper___Bacterial_spot', plantEn: 'Pepper', plantRu: 'Перец', diseaseEn: 'Bacterial spot', diseaseRu: 'Бактериальная пятнистость', healthy: false, symptoms: 'Тёмные пятна с жёлтым ореолом.' },
  { id: 'Pepper___healthy', plantEn: 'Pepper', plantRu: 'Перец', diseaseEn: 'Healthy', diseaseRu: 'Здоровый лист', healthy: true, symptoms: 'Здоровый лист перца.' },
  { id: 'Potato___Early_blight', plantEn: 'Potato', plantRu: 'Картофель', diseaseEn: 'Early blight', diseaseRu: 'Ранняя пятнистость', healthy: false, symptoms: 'Концентрические «мишени» бурого цвета.' },
  { id: 'Potato___Late_blight', plantEn: 'Potato', plantRu: 'Картофель', diseaseEn: 'Late blight', diseaseRu: 'Фитофтороз', healthy: false, symptoms: 'Водянистые тёмные пятна, быстро разрастаются.' },
  { id: 'Potato___healthy', plantEn: 'Potato', plantRu: 'Картофель', diseaseEn: 'Healthy', diseaseRu: 'Здоровый лист', healthy: true, symptoms: 'Здоровый лист картофеля.' },
  { id: 'Raspberry___healthy', plantEn: 'Raspberry', plantRu: 'Малина', diseaseEn: 'Healthy', diseaseRu: 'Здоровый лист', healthy: true, symptoms: 'Здоровый лист малины.' },
  { id: 'Soybean___healthy', plantEn: 'Soybean', plantRu: 'Соя', diseaseEn: 'Healthy', diseaseRu: 'Здоровый лист', healthy: true, symptoms: 'Здоровый лист сои.' },
  { id: 'Squash___Powdery_mildew', plantEn: 'Squash', plantRu: 'Тыква', diseaseEn: 'Powdery mildew', diseaseRu: 'Мучнистая роса', healthy: false, symptoms: 'Белый мучнистый налёт, особенно сверху.' },
  { id: 'Strawberry___Leaf_scorch', plantEn: 'Strawberry', plantRu: 'Клубника', diseaseEn: 'Leaf scorch', diseaseRu: 'Ожог листьев', healthy: false, symptoms: 'Пурпурно-бурые пятна, ожог края листа.' },
  { id: 'Strawberry___healthy', plantEn: 'Strawberry', plantRu: 'Клубника', diseaseEn: 'Healthy', diseaseRu: 'Здоровый лист', healthy: true, symptoms: 'Здоровый лист клубники.' },
  { id: 'Tomato___Bacterial_spot', plantEn: 'Tomato', plantRu: 'Томат', diseaseEn: 'Bacterial spot', diseaseRu: 'Бактериальная пятнистость', healthy: false, symptoms: 'Мелкие тёмные пятна с жёлтым ореолом.' },
  { id: 'Tomato___Early_blight', plantEn: 'Tomato', plantRu: 'Томат', diseaseEn: 'Early blight', diseaseRu: 'Ранняя пятнистость', healthy: false, symptoms: 'Концентрические бурые пятна на старых листьях.' },
  { id: 'Tomato___Late_blight', plantEn: 'Tomato', plantRu: 'Томат', diseaseEn: 'Late blight', diseaseRu: 'Фитофтороз', healthy: false, symptoms: 'Крупные маслянистые тёмные зоны, быстрый некроз.' },
  { id: 'Tomato___Leaf_Mold', plantEn: 'Tomato', plantRu: 'Томат', diseaseEn: 'Leaf mold', diseaseRu: 'Бурая пятнистость', healthy: false, symptoms: 'Жёлтые пятна сверху, оливковый налёт снизу.' },
  { id: 'Tomato___Septoria_leaf_spot', plantEn: 'Tomato', plantRu: 'Томат', diseaseEn: 'Septoria leaf spot', diseaseRu: 'Септориоз', healthy: false, symptoms: 'Много мелких круглых пятен с тёмной каймой.' },
  { id: 'Tomato___Spider_mites', plantEn: 'Tomato', plantRu: 'Томат', diseaseEn: 'Spider mites', diseaseRu: 'Паутинный клещ', healthy: false, symptoms: 'Крапчатость, бронзовость, тонкая паутина.' },
  { id: 'Tomato___Target_Spot', plantEn: 'Tomato', plantRu: 'Томат', diseaseEn: 'Target spot', diseaseRu: 'Коричневая пятнистость', healthy: false, symptoms: 'Пятна с концентрическими кольцами.' },
  { id: 'Tomato___Yellow_Leaf_Curl_Virus', plantEn: 'Tomato', plantRu: 'Томат', diseaseEn: 'Yellow leaf curl virus', diseaseRu: 'Вирус жёлтой курчавости', healthy: false, symptoms: 'Пожелтение, скручивание и измельчение листьев.' },
  { id: 'Tomato___Tomato_mosaic_virus', plantEn: 'Tomato', plantRu: 'Томат', diseaseEn: 'Tomato mosaic virus', diseaseRu: 'Вирус мозаики томата', healthy: false, symptoms: 'Мозаичный светло/тёмно-зелёный узор.' },
  { id: 'Tomato___healthy', plantEn: 'Tomato', plantRu: 'Томат', diseaseEn: 'Healthy', diseaseRu: 'Здоровый лист', healthy: true, symptoms: 'Здоровый лист томата.' },
];

export function getClassById(id: string): PlantClass | undefined {
  return PLANT_CLASSES.find((c) => c.id === id);
}
