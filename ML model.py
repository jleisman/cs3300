
# IMPORTS

import tensorflow as tf
from tensorflow.keras import layers, models
import os


# MOUNT GOOGLE DRIVE

from google.colab import drive
drive.mount('/content/drive')


# PATHS 

BASE_PATH = "/content/drive/MyDrive"


print("MyDrive contents:", os.listdir(BASE_PATH))
print("Colab Notebooks contents:", os.listdir(os.path.join(BASE_PATH, "Colab Notebooks")))
#check path
DATASET_PATH = os.path.join(BASE_PATH, "Colab Notebooks", "Training images")

TRAIN_PATH = os.path.join(DATASET_PATH, "train")
TEST_PATH = os.path.join(DATASET_PATH, "test")

#  DEBUG CHECK
print("Dataset path exists:", os.path.exists(DATASET_PATH))
print("Train path exists:", os.path.exists(TRAIN_PATH))
print("Test path exists:", os.path.exists(TEST_PATH))


# SETTINGS

IMG_SIZE = (128, 128)
BATCH_SIZE = 32
EPOCHS = 100

# LOAD DATA

train_dataset = tf.keras.utils.image_dataset_from_directory(
    TRAIN_PATH,
    image_size=IMG_SIZE,
    batch_size=BATCH_SIZE,
    color_mode="grayscale",
    shuffle=True
)

test_dataset = tf.keras.utils.image_dataset_from_directory(
    TEST_PATH,
    image_size=IMG_SIZE,
    batch_size=BATCH_SIZE,
    color_mode="grayscale",
    shuffle=False
)

class_names = train_dataset.class_names
NUM_CLASSES = len(class_names)


# NORMALIZATION

normalization_layer = layers.Rescaling(1./255)

train_dataset = train_dataset.map(lambda x, y: (normalization_layer(x), y))
test_dataset = test_dataset.map(lambda x, y: (normalization_layer(x), y))

AUTOTUNE = tf.data.AUTOTUNE
train_dataset = train_dataset.prefetch(AUTOTUNE)
test_dataset = test_dataset.prefetch(AUTOTUNE)


# MODEL

model = models.Sequential([
    layers.Input(shape=(128, 128, 1)),

    layers.Conv2D(32, (3,3), activation='relu'),
    layers.MaxPooling2D(),

    layers.Conv2D(64, (3,3), activation='relu'),
    layers.MaxPooling2D(),

    layers.Conv2D(128, (3,3), activation='relu'),
    layers.MaxPooling2D(),

    layers.Flatten(),
    layers.Dense(128, activation='relu'),
    layers.Dropout(0.3),

    layers.Dense(NUM_CLASSES, activation='softmax')
])


# COMPILE

model.compile(
    optimizer="adam",
    loss="sparse_categorical_crossentropy",
    metrics=["accuracy"])

model.summary()


# TRAIN

model.fit(
    train_dataset,
    epochs=EPOCHS,
    validation_data=test_dataset)


# SAVE MODEL TO LOCAL DEVICE

MODEL_PATH = "/content/model"
model.export(MODEL_PATH)

converter = tf.lite.TFLiteConverter.from_saved_model(MODEL_PATH)

converter.target_spec.supported_ops = [ tf.lite.OpsSet.TFLITE_BUILTINS]

tflite_model = converter.convert()

TFLITE_PATH = "/content/model.tflite"

with open(TFLITE_PATH, "wb") as f:
    f.write(tflite_model)

print("TFLite model saved at:", TFLITE_PATH)


# SAVE MODEL TO GOOGLE DRIVE

MODEL_PATH = "/content/drive/MyDrive/Colab Notebooks/cnn_model"

model.export(MODEL_PATH)

print("Model exported successfully.")