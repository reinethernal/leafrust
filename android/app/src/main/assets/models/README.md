# Model assets for LeafRust
#
# Bundled: PlantAi ResNet-18 TFLite (PlantVillage, 39 classes)
# Source: https://github.com/Nishant1998/PlantAi
#   plantvillage_mobilenet.tflite  (~11 MB)
#   labels.txt                     (39 lines, index 4 = Background)
#
# Dataset: PlantVillage (Mohanty et al.). Weights are redistributed for
# on-device inference. F-Droid lists AntiFeature NonFreeAssets because the
# .tflite file is a prebuilt binary, not rebuilt from training scripts
# during the F-Droid recipe. See NOTICE and FDROID.md.
#
# On launch the app can also download/update from:
#   https://raw.githubusercontent.com/Nishant1998/PlantAi/master/model/model.tflite
