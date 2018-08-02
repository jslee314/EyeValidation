package com.tzutalin.dlibtest;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.tzutalin.dlib.VisionDetRet;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.Core.BORDER_DEFAULT;
import static org.opencv.core.CvType.CV_16S;
import static org.opencv.core.CvType.CV_32F;

/**
 * Created by jslee on 2018-07-19.
 */

public class CheckQuality {

    private static final String TAG = "CheckQuality";
    private Bitmap mBitmap;
    private VisionDetRet mRet;
    public double mBlur_Bitmap;
    public double mBlur_R;
    public double mBlur_L;
    public double mEar;
    public double mRotat;
    public boolean imageScope;
    private boolean mAccept;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("opencv_java3");
    }

    public CheckQuality(Bitmap bitmap, VisionDetRet ret) {
        super();
        this.mBitmap = bitmap;
        this.mRet = ret;
    }


    public boolean isImageScope(){

        Rect bounds = new Rect();

        bounds.left = mRet.getLeft();
        bounds.top = mRet.getTop();
        bounds.right = mRet.getRight();
        bounds.bottom = mRet.getBottom();
        int faceWidth = (bounds.right - bounds.left);
        Log.i(TAG,"Face width:" + String.valueOf(faceWidth));

        int width_L = mRet.mStartLeftX + mRet.mWidthLeft;
        int width_R = mRet.mStartRightX +mRet.mWidthRight;
        int height_L = mRet.mStartLeftY + mRet.mHightLeft;
        int height_R = mRet.mStartRightY +mRet.mHightRight;
        Log.i(TAG,"Left size("+String.valueOf(width_L)+", "+String.valueOf(height_L)+")");
        Log.i(TAG,"Right size("+String.valueOf(width_R)+", "+String.valueOf(height_R)+")");

        // 얼굴 크기가 너무 작거나 큰것 제외시킨다.
        if(faceWidth>200 && faceWidth<450) {
            // 눈의 좌표가 이미지 사이즈 넘어가지 않도록 한다.
            if(0<width_L && width_L<mBitmap.getWidth()){
                if(0<width_R && width_R<mBitmap.getWidth()) {
                    if (0 < height_L && height_L < mBitmap.getHeight()) {
                        if (0 < height_R && height_R < mBitmap.getHeight()) {
                            // 눈 크기가 너무 작은것 제외시킨다.
                            if(150<width_L &&150<width_R){
                                imageScope = true;
                            }
                        }

                    }
                }
            }
        }

        return imageScope;

    }

    public boolean isBlur(Bitmap bitCrop_L, Bitmap bitCrop_R){

        Mat matGray = new Mat();
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble std = new MatOfDouble();

        /** ------------------------------------------------------
         *  제외할 조건들: blur, ear, rotation
         *  --------------------------------------------------- **/

        // Bitmap -> Mat(matOri, matLeft, matRight)
        // 전체 Mat: matOri
        Mat matOri = new Mat (mBitmap.getWidth(), mBitmap.getHeight(), CV_32F);     // CV_8UC1
        Utils.bitmapToMat(mBitmap, matOri);
        // 왼쪽눈 Mat: matLeft
        Mat matLeft = new Mat (bitCrop_L.getWidth(), bitCrop_L.getHeight(), CV_32F);  // CV_8UC1
        Utils.bitmapToMat(bitCrop_L, matLeft);
        // 오른쪽눈 Mat: matRight
        Mat matRight = new Mat (bitCrop_R.getWidth(), bitCrop_R.getHeight(), CV_32F);
        Utils.bitmapToMat(bitCrop_R, matRight);

        // blur : cv2.Laplacian(image, cv2.CV_64F).var()--------------------
        // 전체
        Imgproc.cvtColor(matOri, matGray, Imgproc.COLOR_BGR2GRAY);
        // Imgproc.Laplacian(matLeftGray, matLeftGray, CV_16S); // -> Gray CV_16S
        Imgproc.Laplacian(matGray, matGray, CV_16S, 3, 1, 0, BORDER_DEFAULT );
        Core.meanStdDev(matGray, mean, std);
        this.mBlur_Bitmap = std.get(0,0)[0];
        Log.i(TAG, "Blur all:" + String.valueOf(this.mBlur_Bitmap));

        // 왼쪽눈
        Imgproc.cvtColor(matLeft, matGray, Imgproc.COLOR_BGR2GRAY);
        //Bitmap leftGray = .
        //Utils.matToBitmap(matGray,leftGray);
        // Imgproc.Laplacian(matLeftGray, matLeftGray, CV_16S); //-> Gray CV_16S
        Imgproc.Laplacian(matGray, matGray, CV_16S, 3, 1, 0, BORDER_DEFAULT );
        Core.meanStdDev(matGray, mean, std);
        // Imgproc.Laplacian(matLeft,matLeft,CV_64F);//-> Color
        // Core.meanStdDev(matLeft, mean, std);
        // this.mBlur_L = Math.pow(std.get(0,0)[0], 2);  //double stdDev = std.get(0,0)[0];
        this.mBlur_L = std.get(0,0)[0];  //double stdDev = std.get(0,0)[0];
        Log.i(TAG,"Blur Left:" + String.valueOf(this.mBlur_L));

        // 오른쪽눈
        Imgproc.cvtColor(matLeft, matGray, Imgproc.COLOR_BGR2GRAY);
        //Bitmap rightGray = null;
        //Utils.matToBitmap(matGray,rightGray);
        Imgproc.Laplacian(matGray, matGray, CV_16S); //-> Gray
        Core.meanStdDev(matGray, mean, std);
        // Imgproc.Laplacian(matLeft,matLeft,CV_64F);//-> Color
        // Core.meanStdDev(matLeft, mean, std);
        this.mBlur_R = std.get(0,0)[0];
        // Math.pow(std.get(0,0)[0], 2);
        // double stdDev = std.get(0,0)[0];
        Log.i(TAG,"Blur Right: "+String.valueOf( this.mBlur_R));

        boolean blur = false;
        if(mBlur_L > 0 && mBlur_R > 0){
            blur = true;
        }
        ///////////////
        /*
        String left = "left_g" + String.valueOf(mBlur_L);
        String right = "right_g" + String.valueOf(mBlur_R);
        ImageUtils.saveBitmap(rightGray, right);
        ImageUtils.saveBitmap(leftGray, left);
        */
        ///////////////////////

        return blur;
    }

    public void setImageScope(boolean imageScope) {
        this.imageScope = imageScope;
    }

    public boolean isAccept() {
        return mAccept;
    }

    public void setAccept(boolean accept) {
        this.mAccept = accept;
    }

}
