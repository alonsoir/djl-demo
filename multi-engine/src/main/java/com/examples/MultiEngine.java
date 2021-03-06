/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.examples;

import ai.djl.Application;
import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.basicmodelzoo.BasicModelZoo;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.mxnet.zoo.MxModelZoo;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.pytorch.zoo.PtModelZoo;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.tensorflow.zoo.TfModelZoo;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.util.Progress;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MultiEngine {

    private static final Logger logger = LoggerFactory.getLogger(MultiEngine.class);

    private MultiEngine() {}

    public static void main(String[] args)
            throws IOException, ModelNotFoundException, MalformedModelException,
                    TranslateException {
        engineInference();
    }

    /**
     * Method that uses Criteria to load a model from the Tensorflow, MXNet, PyTorch, Basic ModelZoo, does
     * inference on a sample image, and logs the results.
     */
    private static void engineInference()
            throws IOException, ModelNotFoundException, MalformedModelException,
                    TranslateException {

        String filePath = "src/test/resources/kitten.jpg";

        /** Using Model Zoo to load in the model. */
        // Using Criteria to load model from TfModelZoo and call it.
        Map<String, String> criteria = new ConcurrentHashMap<>();
        criteria.put("layers", "50");
        criteria.put("dataset", "imagenet");

        Progress device = new ProgressBar();

        Path imageFile = Paths.get(filePath);
        Image img = ImageFactory.getInstance().fromFile(imageFile);

        try (ZooModel<Image, Classifications> model =
                TfModelZoo.RESNET.loadModel(criteria, device)) {
            try (Predictor<Image, Classifications> predictor = model.newPredictor()) {
                logger.info("TensorFlow Resnet50: " + predictor.predict(img));
            }
        }

        // Using Criteria to load model from MXModelZoo and call it.
        criteria = new ConcurrentHashMap<>();
        criteria.put("layers", "50");
        criteria.put("flavor", "v1");
        criteria.put("dataset", "cifar10");

        device = new ProgressBar();

        img = ImageFactory.getInstance().fromFile(imageFile);

        try (ZooModel<Image, Classifications> model =
                MxModelZoo.RESNET.loadModel(criteria, device)) {
            try (Predictor<Image, Classifications> predictor = model.newPredictor()) {
                logger.info("MXNet Resnet50: " + predictor.predict(img));
            }
        }

        // Using Criteria to load model from PtModelZoo and call it.
        criteria = new ConcurrentHashMap<>();
        criteria.put("layers", "50");
        criteria.put("dataset", "imagenet");

        device = new ProgressBar();

        img = ImageFactory.getInstance().fromFile(imageFile);
        try (ZooModel<Image, Classifications> model =
                 PtModelZoo.RESNET.loadModel(criteria, device)) {
            try (Predictor<Image, Classifications> predictor = model.newPredictor()) {
                logger.info("Pytorch ResNet50: " + predictor.predict(img));
            }
        }

        // Using Criteria to load model from BasicModelZoo and call it.
        criteria = new ConcurrentHashMap<>();
        criteria.put("layers", "50");
        criteria.put("dataset", "cifar10");

        device = new ProgressBar();

        img = ImageFactory.getInstance().fromFile(imageFile);

        try (ZooModel<Image, Classifications> model =
                BasicModelZoo.RESNET.loadModel(criteria, device)) {
            try (Predictor<Image, Classifications> predictor = model.newPredictor()) {
                logger.info("Basic Model Zoo: " + predictor.predict(img));
            }
        }
    }

    private static void loadingModelManuallyInference()
            throws IOException, ModelNotFoundException, MalformedModelException,
                    TranslateException {
        String filePath = "src/test/resources/kitten.jpg";

        String path_to_model_dir = "<INSERT PATH TO MODEL DIR>";

        /** Loading the model in manually. */
        // Example of calling Tensorflow Engine
        try (Model tfModel = Model.newInstance(Device.defaultDevice(), "TensorFlow")) {
            Path modelPath = Paths.get(path_to_model_dir);
            tfModel.load(modelPath);

            Predictor<Image, Classifications> predictor =
                    tfModel.newPredictor(new MyTranslatorTF());

            Image img = ImageFactory.getInstance().fromFile(Paths.get(filePath));

            Classifications result = predictor.predict(img);
            logger.info("Tensorflow Manual Model Load Result: " + result.toString());
        }

        // Example of calling MXNet Engine
        try (Model mxModel = Model.newInstance(Device.defaultDevice(), "MXNet")) {

            Path modelPath = Paths.get(path_to_model_dir);
            mxModel.load(modelPath);

            Predictor<Image, Classifications> predictor =
                    mxModel.newPredictor(new MyTranslatorMX());

            Image img = ImageFactory.getInstance().fromFile(Paths.get(filePath));

            Classifications result = predictor.predict(img);
            logger.info("MXNet Manual Model Load Result: " + result.toString());
        }

        // Example of calling Pytorch Engine
        try (Model pyModel = Model.newInstance(Device.defaultDevice(), "PyTorch")) {
            Path modelPath = Paths.get(path_to_model_dir);
            pyModel.load(modelPath);

            Predictor<Image, Classifications> predictor =
                    pyModel.newPredictor(new MyTranslatorPT());
            Image img = ImageFactory.getInstance().fromFile(Paths.get(filePath));

            Classifications result = predictor.predict(img);
            logger.info("PyTorch Manual Model Load Result: " + result.toString());
        }
    }

    private static final class MyTranslatorTF
            implements Translator<Image, Classifications> {

        private List<String> classes;

        public MyTranslatorTF() {
            classes = IntStream.range(0, 10).mapToObj(String::valueOf).collect(Collectors.toList());
        }

        /** {@inheritDoc} */
        @Override
        public NDList processInput(TranslatorContext ctx, Image input) {
            NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
            return new NDList(NDImageUtils.resize(array, 224).div(255));
        }

        /** {@inheritDoc} */
        @Override
        public Classifications processOutput(TranslatorContext ctx, NDList list) {
            NDArray probabilities = list.singletonOrThrow().softmax(0);
            return new Classifications(classes, probabilities);
        }
    }

    private static final class MyTranslatorMX
            implements Translator<Image, Classifications> {

        private List<String> classes;

        public MyTranslatorMX() {
            classes = IntStream.range(0, 10).mapToObj(String::valueOf).collect(Collectors.toList());
        }

        /** {@inheritDoc} */
        @Override
        public NDList processInput(TranslatorContext ctx, Image input) {
            NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
            return new NDList(NDImageUtils.toTensor(array));
        }

        /** {@inheritDoc} */
        @Override
        public Classifications processOutput(TranslatorContext ctx, NDList list) {
            NDArray probabilities = list.singletonOrThrow().softmax(0);
            return new Classifications(classes, probabilities);
        }
    }

    private static final class MyTranslatorPT
            implements Translator<Image, Classifications> {

        private List<String> classes;

        public MyTranslatorPT() {
            classes = IntStream.range(0, 10).mapToObj(String::valueOf).collect(Collectors.toList());
        }

        /** {@inheritDoc} */
        @Override
        public NDList processInput(TranslatorContext ctx, Image input) {
            NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
            return new NDList(NDImageUtils.toTensor(array));
        }

        /** {@inheritDoc} */
        @Override
        public Classifications processOutput(TranslatorContext ctx, NDList list) {
            NDArray probabilities = list.singletonOrThrow().softmax(0);
            return new Classifications(classes, probabilities);
        }
    }
}
