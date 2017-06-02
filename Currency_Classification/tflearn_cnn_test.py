from __future__ import division, print_function, absolute_import
#%matplotlib inline
import os
import numpy as np
import scipy as sp
import sklearn as sk
import cv2
import matplotlib.pyplot as plt
from skimage.io import imread
from random import shuffle
import scipy.io as io
from scipy.misc import imresize
import seaborn as sns
import tensorflow as tf

def normalize_image(im_vector):
    mean_ = np.mean(im_vector, axis = (0,1))
    std_ = np.std(im_vector, axis = (0,1))
    return (im_vector - mean_)/std_

def preprocess_image(im_vector):
    im_vector = imresize(im_vector, (400, 300, 3))
    im_vector = normalize_image(im_vector)
    return im_vector
"""
train_data = []
train_labels = []
val_data = []
val_labels = []
for i, denomination in enumerate(os.listdir("../images")):
    if denomination == ".DS_Store":
        continue
    all_images = os.listdir("../images/" + denomination)
    shuffle(all_images)
    val_images = all_images[:50]
    train_images = all_images[50:]
    for image in train_images:
        if image == ".DS_Store":
            continue
        im_vector = imread("../images/" + denomination + "/" + image)
        train_data.append(preprocess_image(im_vector))
        label_vector = np.zeros(6)
        label_vector[i] = 1
        train_labels.append(label_vector)
    for image in val_images:
        if image == ".DS_Store":
            continue
        im_vector = imread("../images/" + denomination + "/" + image)
        val_data.append(preprocess_image(im_vector))
        label_vector = np.zeros(6)
        label_vector[i] = 1
        val_labels.append(label_vector)      

train_data = np.array(train_data)
val_data = np.array(val_data)
val_labels = np.array(val_labels)
train_labels = np.array(train_labels)

denom_to_idx = {}
for i, denomination in enumerate(os.listdir("../images")):
    denom_to_idx[denomination] = i

idx_to_denom = {}
for i, denomination in enumerate(os.listdir("../images")):
    idx_to_denom[i] = denomination

# Randomize train data

idx = range(train_data.shape[0])
shuffle(idx)
train_data = train_data[idx]
train_labels = train_labels[idx]
"""
#from __future__ import division, print_function, absolute_import

# Import tflearn and some helpers
import tflearn
from tflearn.data_utils import shuffle
from tflearn.layers.core import input_data, dropout, fully_connected
from tflearn.layers.conv import conv_2d, max_pool_2d
from tflearn.layers.estimator import regression
from tflearn.data_preprocessing import ImagePreprocessing
from tflearn.data_augmentation import ImageAugmentation
import pickle

# # Load the data set
# X, Y, X_test, Y_test = pickle.load(open("full_dataset.pkl", "rb"))

# # Shuffle the data
# X, Y = shuffle(X, Y)

# Make sure the data is normalized
img_prep = ImagePreprocessing()
img_prep.add_featurewise_zero_center()
img_prep.add_featurewise_stdnorm()

# Create extra synthetic training data by flipping, rotating and blurring the
# images on our data set.
img_aug = ImageAugmentation()
img_aug.add_random_flip_leftright()
img_aug.add_random_rotation(max_angle=25.)
img_aug.add_random_blur(sigma_max=3.)

# Define our network architecture:

# Input is a 32x32 image with 3 color channels (red, green and blue)
network = input_data(shape=[None, 400, 300, 3],
                     data_preprocessing=img_prep,
                     data_augmentation=img_aug)

# Step 1: Convolution
network = conv_2d(network, 16, 11, activation='relu')

# Step 2: Max pooling
network = max_pool_2d(network, 2)

# Step 3: Convolution again
network = conv_2d(network, 16, 7, activation='relu')

network = max_pool_2d(network, 2)


# Step 4: Convolution yet again
network = conv_2d(network, 32, 5, activation='relu')

# Step 5: Max pooling again
network = max_pool_2d(network, 2)

network = conv_2d(network, 32, 5, activation='relu')

network = max_pool_2d(network, 2)

network = conv_2d(network, 64, 3, activation='relu')

network = max_pool_2d(network, 2)


# Step 6: Fully-connected 512 node neural network
network = fully_connected(network, 1024, activation='relu')

# Step 7: Dropout - throw away some data randomly during training to prevent over-fitting
network = dropout(network, 0.5)

# Step 8: Fully-connected neural network with two outputs (0=isn't a bird, 1=is a bird) to make the final prediction
network = fully_connected(network, 6, activation='softmax')

# Tell tflearn how we want to train the network
network = regression(network, optimizer='adam',
                     loss='categorical_crossentropy',
                     learning_rate=0.001)


# Wrap the network in a model object
model = tflearn.DNN(network,tensorboard_verbose=0, checkpoint_path="currency_classifier_model")
#tflearn.config.init_graph(seed=123, num_cores=8, gpu_memory_fraction=0.6)
model.load("currency_classifier_model.tflearn")
# Train it! We'll do 100 training passes and monitor it as it goes.
#model.fit(train_data, train_labels, n_epoch=100, shuffle=True, validation_set=(val_data, val_labels),
#          show_metric=True, batch_size=96,
#          snapshot_epoch=True,
#          run_id='currency-classifier')



model.save("currency_classifier_model.tflearn")
im_vector = imread("./images/headphone.jpg")
images = []
im_vector = images.append(preprocess_image(im_vector))
print(model.predict(images))
