# -----------------------------------------------------------
# IMPORTS
# -----------------------------------------------------------
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import tensorflow as tf
from tensorflow.keras import layers, models
import os

# -----------------------------------------------------------
# 1. Function to LOAD a CSV file (goes here, near the top!)
# -----------------------------------------------------------
def load_csv(path, label_map):
    df = pd.read_csv(path)

    # Convert string labels to numeric values
    df["label"] = df["label"].map(label_map)

    # Convert pixel columns to numeric
    pixel_cols = df.columns[df.columns != "label"]
    df[pixel_cols] = df[pixel_cols].apply(pd.to_numeric, errors="coerce")

    labels = df["label"].values.astype("int32")
    pixels = df[pixel_cols].values.astype("float32")

    pixels = pixels / 255.0
    images = pixels.reshape((-1, 128, 128, 1))

    return images, labels


# -----------------------------------------------------------
# 2. LOAD your CSV dataset
# -----------------------------------------------------------
DATA_PATH = r"/content/drive/MyDrive/Colab Notebooks/Deep Learning Project CSVs"

emotion_files = {
    "angry":   (os.path.join(DATA_PATH, "train_angry.csv"),
                os.path.join(DATA_PATH, "test_angry.csv")),
    "happy":   (os.path.join(DATA_PATH, "train_happy.csv"),
                os.path.join(DATA_PATH, "test_happy.csv")),
    "sad":     (os.path.join(DATA_PATH, "train_sad.csv"),
                os.path.join(DATA_PATH, "test_sad.csv")),
    "neutral": (os.path.join(DATA_PATH, "train_neutral.csv"),
                os.path.join(DATA_PATH, "test_neutral.csv")),
}

label_map = {"angry": 0, "happy": 1, "sad": 2, "neutral": 3}

train_X, train_y = [], []
test_X, test_y = [], []

for emotion, (train_file, test_file) in emotion_files.items():
    X_train, y_train = load_csv(train_file, label_map)
    X_test, y_test = load_csv(test_file, label_map)

    train_X.append(X_train)
    train_y.append(y_train)
    test_X.append(X_test)
    test_y.append(y_test)

X_train = np.concatenate(train_X)
y_train = np.concatenate(train_y)
X_test = np.concatenate(test_X)
y_test = np.concatenate(test_y)


print("Train:", X_train.shape, y_train.shape)
print("Test:", X_test.shape, y_test.shape)

# -----------------------------------------------------------
# 3. BUILD the CNN model
# -----------------------------------------------------------
model = models.Sequential([
    layers.Conv2D(32, (3,3), activation='relu', input_shape=(128,128,1)),
    layers.BatchNormalization(),
    layers.MaxPooling2D(2,2),

    layers.Conv2D(64, (3,3), activation='relu'),
    layers.BatchNormalization(),
    layers.MaxPooling2D(2,2),

    layers.Conv2D(128, (3,3), activation='relu'),
    layers.BatchNormalization(),
    layers.MaxPooling2D(2,2),

    layers.Flatten(),
    layers.Dense(128, activation='relu'),
    layers.Dropout(0.3),

    layers.Dense(4, activation='softmax')
])

model.compile(
    optimizer="adam",
    loss="sparse_categorical_crossentropy",
    metrics=["accuracy"]
)

model.summary()

# -----------------------------------------------------------
# 4. TRAIN the CNN
# -----------------------------------------------------------
history = model.fit(
    X_train, y_train,
    epochs=60,
    batch_size=32,
    validation_split=0.15,
    shuffle=True,verbose=1
)

# -----------------------------------------------------------
# 5. EVALUATE
# -----------------------------------------------------------
loss, acc = model.evaluate(X_test, y_test)
print("Test accuracy:", acc)

# -----------------------------------------------------------
# 6. PREDICT function
# -----------------------------------------------------------
emotions = ["angry", "happy", "sad", "neutral"]

def predict_emotion(pixel_row_1024):
    img = np.array(pixel_row_1024, dtype="float32").reshape(1, 128, 128, 1) / 255.0 ##############
    prediction = model.predict(img)
    return emotions[np.argmax(prediction)]


# -----------------------------------------------------------
# 7. SAVE THE MODEL (Name: Jarvis)
# -----------------------------------------------------------

# Create a folder to store the model
save_path = "/content/drive/MyDrive/Colab Notebooks/Jarvis_Model"
os.makedirs(save_path, exist_ok=True)

model_file = os.path.join(save_path, "Jarvis.keras")

# Save the model
model.save(model_file)
# -----------------------------------------------------------
# 8. PLOT TRAINING & VALIDATION GRAPHS
# -----------------------------------------------------------


# Accuracy Graph
plt.figure(figsize=(10, 5))
plt.plot(history.history['accuracy'], label='Train Accuracy', linewidth=2)
plt.plot(history.history['val_accuracy'], label='Validation Accuracy', linewidth=2)
plt.title('Model Accuracy Over Epochs')
plt.xlabel('Epoch')
plt.ylabel('Accuracy')
plt.legend()
plt.grid(True)
plt.show()

# Loss Graph
plt.figure(figsize=(10, 5))
plt.plot(history.history['loss'], label='Train Loss', linewidth=2)
plt.plot(history.history['val_loss'], label='Validation Loss', linewidth=2)
plt.title('Model Loss Over Epochs')
plt.xlabel('Epoch')
plt.ylabel('Loss')
plt.legend()
plt.grid(True)
plt.show()

print("\nModel successfully saved!")
print("Saved model path:", model_file)
