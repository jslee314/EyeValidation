package com.tzutalin.dlibtest;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.os.TraceCompat;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TensorFlowClassifier {

    private static final String TAG = "TFClassifier";

    // Tensorflow parameter
    // v1(ResNet)
    //    private static final String[] RIGHT_INPUT_NAMES = {"right/input_scope/low_res_X", "right/input_scope/mid_res_X", "right/input_scope/high_res_X"};
    //    private static final String[] LEFT_INPUT_NAMES = {"left/input_scope/low_res_X", "left/input_scope/mid_res_X", "left/input_scope/high_res_X"};
    //    private static final String[] OUTPUT_NAMES = {"right_1/softmax", "left_1/softmax"};
    // v2(mobilenet v2)
    private static final String[] RIGHT_INPUT_NAMES = {"right/input_module/low_res_X","right/input_module/mid_res_X","right/input_module/high_res_X"};
    private static final String[] LEFT_INPUT_NAMES = {"left/input_module/low_res_X","left/input_module/mid_res_X","left/input_module/high_res_X"};
    private static final String[] OUTPUT_NAMES = {"right/output_module/softmax","left/output_module/softmax"};
    private static final String MODEL_FILE = "file:///android_asset/model_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/label_strings.txt";

    private static final int MAX_RESULTS = 3;  // result 개수 제한
    private static final float THRESHOLD = 0.1f;  // outputs 값의 threshold 설정
    public static final int MULTISCALE_CNT = 3;

    public static final int[] WIDTHS = {160, 200, 240};
    public static final int[] HEIGHTS = {60, 80, 100};

    float[] lowRightData = new float[WIDTHS[0] * HEIGHTS[0]];
    float[] midRightData = new float[WIDTHS[1] * HEIGHTS[1]];
    float[] highRightData = new float[WIDTHS[2] * HEIGHTS[2]];
    float[] lowLeftData = new float[WIDTHS[0] * HEIGHTS[0]];
    float[] midLeftData = new float[WIDTHS[1] * HEIGHTS[1]];
    float[] highLeftData = new float[WIDTHS[2] * HEIGHTS[2]];

    Bitmap bitmap_left[] = new Bitmap[5];
    Bitmap bitmap_right[] = new Bitmap[5];

    // neural network 관련 parameters
    private String[] rightInputNames;  // neural network right 입력 노드 이름
    private String[] leftInputNames;  // neural network left 입력 노드 이름
    private String[] outputNames;  // neural network 출력 노드 이름
    private int[] widths;  // 입력 이미지 가로 길이
    private int[] heights;  // 입력 이미지 세로 길이
    private Vector<String> labels = new Vector<>();  // label 정보
    private int numClasses = 7;
    private float[][] logits = new float[2][numClasses];  // logit 정보
    private boolean runStats = false;

    private TensorFlowInferenceInterface tii;

    private static AssetManager mAssetManager;

    private TensorFlowClassifier() {}

    private static class SingleToneHolder {
        static final TensorFlowClassifier instance = new TensorFlowClassifier();
    }

    public static TensorFlowClassifier getInstance() {
        return SingleToneHolder.instance;
    }


    /**
     * 텐서플로우 classifier 초기화 함수
     */
    public void initTensorFlowAndLoadModel(AssetManager assetManager) {

        mAssetManager = assetManager;

        Executor executor = Executors.newSingleThreadExecutor();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    createClassifier(
                            mAssetManager,
                            MODEL_FILE,
                            LABEL_FILE,
                            WIDTHS,
                            HEIGHTS,
                            RIGHT_INPUT_NAMES,
                            LEFT_INPUT_NAMES,
                            OUTPUT_NAMES);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }


    /**
     * 텐서플로우 classifier 생성 관련 초기화 함수
     * @param assetManager
     * @param modelFilename
     * @param labelFilename
     * @param widhts
     * @param heights
     * @param rightInputNames
     * @param leftInputNames
     * @param outputNames
     */
    public void createClassifier(
            AssetManager assetManager,
            String modelFilename,
            String labelFilename,
            int[] widhts,
            int[] heights,
            String[] rightInputNames,
            String[] leftInputNames,
            String[] outputNames) {
        this.rightInputNames = rightInputNames;
        this.leftInputNames = leftInputNames;
        this.outputNames = outputNames;
        this.widths = widhts;
        this.heights = heights;

        // label names 설정
        BufferedReader br = null;
        try {
            String actualFilename = labelFilename.split("file:///android_asset/")[1];
            br = new BufferedReader(new InputStreamReader(assetManager.open(actualFilename)));
            String line = "";
            while((line = br.readLine()) != null) {
                this.labels.add(line);
            }
        } catch (IOException e) {
            Log.d(TensorFlowClassifier.TAG, e.toString());
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                Log.d(TensorFlowClassifier.TAG, e.toString());
            }
        }

        this.tii = new TensorFlowInferenceInterface(assetManager, modelFilename);
    }


    /**
     * 오른쪽/왼쪽 눈에 대한 verification 을 수행하는 함수
     * @param lowRightData
     * @param midRightData
     * @param highRightData
     * @param lowLeftData
     * @param midLeftData
     * @param highLeftData
     * @return
     */
    public float[] verificationEye(float[] lowRightData, float[] midRightData, float[] highRightData,
                               float[] lowLeftData, float[] midLeftData, float[] highLeftData) {
        long startTime = System.currentTimeMillis();

        TraceCompat.beginSection("verificationEye");

        TraceCompat.beginSection("feed");
        for (int i = 0; i < this.widths.length; i++) {
            if (i == 0) {
                tii.feed(this.rightInputNames[i], lowRightData, 1, this.heights[i], this.widths[i], 1);
                tii.feed(this.leftInputNames[i], lowLeftData, 1, this.heights[i], this.widths[i], 1);
            } else if (i == 1) {
                tii.feed(this.rightInputNames[i], midRightData, 1, this.heights[i], this.widths[i], 1);
                tii.feed(this.leftInputNames[i], midLeftData, 1, this.heights[i], this.widths[i], 1);
            } else {
                tii.feed(this.rightInputNames[i], highRightData, 1, this.heights[i], this.widths[i], 1);
                tii.feed(this.leftInputNames[i], highLeftData, 1, this.heights[i], this.widths[i], 1);
            }
        }
//        tii.feed("right/input_scope/is_training", new boolean[]{false}, 1);
//        tii.feed("left/input_scope/is_training", new boolean[]{false}, 1);
        TraceCompat.endSection();

        TraceCompat.beginSection("run");
        tii.run(this.outputNames, this.runStats);
        TraceCompat.endSection();

        TraceCompat.beginSection("fetch");
        for (int i = 0; i < this.outputNames.length; i++) {
            float[] outputs = new float[this.numClasses];
            tii.fetch(this.outputNames[i], outputs);
            for (int j = 0; j < this.numClasses; j++) {
                this.logits[i][j] = outputs[j];
            }
        }
        TraceCompat.endSection();

        float[] result = {0f, 0f, 0f, 0f, 0f, 0f, 0f};
        for (int j = 0; j < this.logits[0].length; j++) {
            for (int i = 0; i < this.logits.length; i++) {
                result[j] += this.logits[i][j];
                Log.d(TAG, this.logits[i][j]+"");
            }
        }

        String temp = "";
        for (int i = 0; i < result.length; i++) {
            temp += result[i] + ",";
        }
        Log.d(TAG, temp);
        /*
        float maxValue = -1;
        int maxIndex = -1;
        for (int i = 0; i < this.numClasses; i++) {
            if (maxValue < result[i]) {
                maxValue = result[i];
                maxIndex = i;
            }
        }
        long endTime = System.currentTimeMillis();


        return maxIndex;
        */
        return result;
    }


    public ResultProbList Verification(Bundle bundle) {
        long startTime = System.currentTimeMillis();

        ParcelBitmapList leftList = new ParcelBitmapList();
        ParcelBitmapList rightList = new ParcelBitmapList();

        ArrayList<ParcelBitmap> left = bundle.getParcelableArrayList("LeftEyeList");
        ArrayList<ParcelBitmap> right = bundle.getParcelableArrayList("RightEyeList");

        // 리스트에서 이미지 꺼내오기
        for (int num = 0; num < 5; num++) {
            ParcelBitmap addData = left.get(num);
            bitmap_left[num] = addData.getBitmap();

            ParcelBitmap addDataa = right.get(num);
            bitmap_right[num] = addDataa.getBitmap();
        }

        ResultProbList resultList = new ResultProbList();

        for (int num = 0; num < 5; num++) {
            ResultProb resultPro = new ResultProb();

            Bitmap oriLeftBitmap = bitmap_left[num];
            Bitmap oriRightBitmap = bitmap_right[num];

            float[] tempResult = verificationEye(lowRightData, midRightData, highRightData, lowLeftData, midLeftData, highLeftData);
            resultPro.setBitmap(oriLeftBitmap, oriRightBitmap);
            resultPro.setProbResult(tempResult);
            resultList.add(resultPro);
        }

        long endTime = System.currentTimeMillis();
        long verificationtime = (endTime-startTime)/100;
        resultList.setVerificationtime(verificationtime);

        return resultList;
    }


    private static float[] grayScaleAndNorm(Bitmap bitmap) {
        int mWidth = bitmap.getWidth();
        int mHeight = bitmap.getHeight();

        int[] ori_pixels = new int[mWidth * mHeight];
        float[] norm_pixels = new float[mWidth * mHeight];

        bitmap.getPixels(ori_pixels, 0, mWidth, 0, 0, mWidth, mHeight);
        for (int i = 0; i < ori_pixels.length; i++) {
            int alpha = Color.alpha(ori_pixels[i]);
//            int grayPixel = (int) ((Color.red(ori_pixels[i]) * 0.2126) + (Color.green(ori_pixels[i]) * 0.7152) + (Color.blue(ori_pixels[i]) * 0.0722));
            int grayPixel = (int) ((Color.red(ori_pixels[i]) * 0.299) + (Color.green(ori_pixels[i]) * 0.587) + (Color.blue(ori_pixels[i]) * 0.114));  // decode_png -> grayscale 변환과 일치
            if (grayPixel < 0) grayPixel = 0;
            if (grayPixel > 255) grayPixel = 255;
            norm_pixels[i] = grayPixel / 255.0f;
        }
        return norm_pixels;
    }
}