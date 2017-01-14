package ru.flightlabs.masks.camera;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.CameraGLSurfaceView;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import ru.flightlabs.masks.CompModel;
import ru.flightlabs.masks.DetectionBasedTracker;
import ru.flightlabs.masks.R;
import ru.flightlabs.masks.Static;
import ru.flightlabs.masks.activity.FdActivity2;
import ru.flightlabs.masks.activity.Settings;
import ru.flightlabs.masks.renderer.Model;
import ru.flightlabs.masks.utils.Decompress;
import ru.flightlabs.masks.utils.OpenGlHelper;
import ru.flightlabs.masks.utils.OpencvUtils;
import ru.flightlabs.masks.utils.PhotoMaker;
import ru.flightlabs.masks.utils.PoseHelper;
import ru.flightlabs.masks.utils.ShaderUtils;

/**
 * Created by sov on 05.01.2017.
 */

public class CameraTextureListenerImpl implements CameraGLSurfaceView.CameraTextureListener {

    private float mRelativeFaceSize = 0.3f;
    private int mAbsoluteFaceSize = 0;
    DetectionBasedTracker mNativeDetector;
    CompModel compModel;
    Activity act;

    // 2d
    private int vPos;
    private int vTex;

    // 3d
    private int vPos3d;
    private int vTexFor3d;

    private int maskTextureid;
    /** This will be used to pass in the texture. */
    private int mTextureUniformHandle;

    ByteBuffer m_bbPixels;
    Mat mRgba;
    Mat mGray;

    int frameCount;
    long timeStart;
    double lastCount = 0.5f;

    private static final String TAG = "CameraTextureL_class";
    int program2dEffectId;// shader program
    int program3dId;// shader program
    private int[] iFBO = null;//{0};

    // 3d
    private Model model;

    private Model modelGlasses;
    private int glassesTextureid;

    private Model modelHat;
    private int hatTextureid;

    Mat initialParams = null;

    String modelPath;

    public CameraTextureListenerImpl(Activity act, CompModel compModel) {
        this.act = act;
        this.compModel = compModel;
    }

    public void onCameraViewStarted(int width, int height) {

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat();
        //m_bbPixels = ByteBuffer.allocateDirect(width * height * 4);
        // workaround due to https://code.google.com/p/android/issues/detail?id=80064
        m_bbPixels = ByteBuffer.wrap(new byte[width * height * 4]);
        m_bbPixels.order(ByteOrder.LITTLE_ENDIAN);

//        int vertexShaderId = ShaderUtils.createShader(this, GLES20.GL_VERTEX_SHADER, R.raw.vertex_shader);
//        int fragmentShaderId = ShaderUtils.createShader(this, GLES20.GL_FRAGMENT_SHADER, R.raw.fragment_shader);
        int vertexShaderId = ShaderUtils.createShader(act, GLES20.GL_VERTEX_SHADER, R.raw.vss);
        int fragmentShaderId = ShaderUtils.createShader(act, GLES20.GL_FRAGMENT_SHADER, R.raw.fss4);
        program2dEffectId = ShaderUtils.createProgram(vertexShaderId, fragmentShaderId);

        int vertexShader3dId = ShaderUtils.createShader(act, GLES20.GL_VERTEX_SHADER, R.raw.vss3d);
        int fragmentShader3dId = ShaderUtils.createShader(act, GLES20.GL_FRAGMENT_SHADER, R.raw.fss3d);
        program3dId = ShaderUtils.createProgram(vertexShader3dId, fragmentShader3dId);
        load3dModel();
        bindData(width, height);

        Log.i(TAG, "onCameraViewStarted");
        //mGray = new Mat();
        //mRgba = new Mat();
    }

    private void load3dModel() {
        model = new Model(R.raw.for_android_test,
                act);
        maskTextureid = OpenGlHelper.loadTexture(act, R.raw.m1_2);
        Log.i(TAG, "load3dModel2");
        modelGlasses = new Model(R.raw.glasses_3d,
                act);
        Log.i(TAG, "load3dModel3");
        glassesTextureid = OpenGlHelper.loadTexture(act, R.raw.glasses);

        modelHat = new Model(R.raw.hat,
                act);
        hatTextureid = OpenGlHelper.loadTexture(act, R.raw.hat_tex);
    }

    private void bindData(int width, int height) {
        vPos = GLES20.glGetAttribLocation(program2dEffectId, "vPosition");
        vTex  = GLES20.glGetAttribLocation(program2dEffectId, "vTexCoord");
        GLES20.glEnableVertexAttribArray(vPos);
        GLES20.glEnableVertexAttribArray(vTex);

        vPos3d = GLES20.glGetAttribLocation(program3dId, "vPosition");
        GLES20.glEnableVertexAttribArray(vPos3d);
        vTexFor3d = GLES20.glGetAttribLocation(program3dId, "a_TexCoordinate");
        GLES20.glEnableVertexAttribArray(vTexFor3d);
    }


    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped");
        mGray.release();
    }

    public boolean onCameraTexture(int texIn, int texOut, int width, int height) {
        Log.i(TAG, "onCameraTexture");
        if (frameCount == 0) {
            timeStart = System.currentTimeMillis();
            frameCount = 1;
        } else {
            frameCount++;
            if (frameCount == 10) {
                lastCount = (System.currentTimeMillis() - timeStart) / 1000f;
                frameCount = 0;
            }
        }
        // TODO do in background
        if (FdActivity2.newIndexEye != FdActivity2.currentIndexEye) {
            FdActivity2.currentIndexEye = FdActivity2.newIndexEye;
            OpenGlHelper.changeTexture(act, FdActivity2.eyesResources.getResourceId(FdActivity2.newIndexEye, 0),maskTextureid);
        }

        if (compModel.mNativeDetector != null) {
            mNativeDetector = compModel.mNativeDetector;
        }
        Log.i(TAG, "onCameraTexture " + width + " " + height);
        Log.i(TAG, "onCameraTexture2");
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, m_bbPixels);
        m_bbPixels.rewind();
        Log.i(TAG, "onCameraTexture3" + m_bbPixels.array().length);

        //mRgba.get(width, height, pixelValues);
        mRgba.put(0, 0, m_bbPixels.array());
        Core.flip(mRgba, mRgba, 0);
        Log.i(TAG, "onCameraTexture5");
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);

        if (mAbsoluteFaceSize == 0) {
            mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            //mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = compModel.findFaces(mGray, mAbsoluteFaceSize);
        Rect[] facesArray = faces.toArray();
        final boolean haveFace = facesArray.length > 0;
        Log.i(TAG, "onCameraTexture5 " + haveFace);
        Point center = new Point(0.5, 0.5);
        Point center2 = new Point(0.5, 0.5);
        Point[] foundEyes = null;
        if (haveFace) {
            if (Settings.debugMode) {
                Imgproc.rectangle(mRgba, facesArray[0].tl(), facesArray[0].br(), new Scalar(255, 10 ,10), 3);
            }
            center = OpencvUtils.convertToGl(new Point((2 * facesArray[0].x + facesArray[0].width) / 2.0, (2 * facesArray[0].y + facesArray[0].height) / 2.0), width, height);
            if (mNativeDetector != null) {
                foundEyes = mNativeDetector.findEyes(mGray, facesArray[0]);
                if (Settings.debugMode) {
                    for (Point p : foundEyes) {
                        Imgproc.circle(mRgba, p, 2, new Scalar(255, 10, 10));
                    }
                }
                center = OpencvUtils.convertToGl(new Point((foundEyes[36].x + foundEyes[39].x) / 2.0, (foundEyes[36].y + foundEyes[39].y) / 2.0), width, height);
                center2 = OpencvUtils.convertToGl(new Point((foundEyes[42].x + foundEyes[45].x) / 2.0, (foundEyes[42].y + foundEyes[45].y) / 2.0), width, height);
            }
        }

        if (Settings.debugMode) {
            Imgproc.putText(mRgba, "frames " + String.format("%.3f", (1f / lastCount) * 10) + " in 1 second.", new Point(50, 50), Core.FONT_HERSHEY_SIMPLEX, 1,
                    new Scalar(255, 255, 255), 2);
        }
        Mat glMatrix = null;
        PoseHelper.bindMatrix(100, 100);
        if (foundEyes != null) {


            if (true) {
                Mat inputLandMarks = new Mat(68, 2, CvType.CV_64FC1);
                for (int i = 0; i < foundEyes.length; i++) {
                    inputLandMarks.put(i, 0, foundEyes[i].x);
                    inputLandMarks.put(i, 1, foundEyes[i].y);
                }
                Mat output3dShape = new Mat(113, 3, CvType.CV_64FC1);
                if (initialParams == null) {
                    initialParams = new Mat(14, 1, CvType.CV_64FC1, new Scalar(0));
                }
                if (modelPath == null) {
                    if (new File("/storage/extSdCard/models").exists()) {
                        modelPath = "/storage/extSdCard/models";
                    } else {
                        File cascadeDir = act.getApplicationContext().getDir("models", Context.MODE_PRIVATE);
                        Decompress.unzipFromAssets(act, "models.zip", cascadeDir.getPath());
                        modelPath = cascadeDir.getPath();
                    }
                    Log.i(TAG, "onCameraTexture1 " + modelPath);
                }

                mNativeDetector.morhpFace(inputLandMarks, output3dShape, initialParams, modelPath, true);
                for (int i = 0; i < output3dShape.rows(); i++) {
                    model.tempV[i * 3] = (float) output3dShape.get(i, 0)[0];
                    model.tempV[i * 3 + 1] = (float) output3dShape.get(i, 1)[0];
                    model.tempV[i * 3 + 2] = (float) output3dShape.get(i, 2)[0];
                }
                model.recalcV();
            }
            Log.i(TAG, "onCameraTexture1 " + model.tempV[0] + " " + model.tempV[1] + " " + model.tempV[2]);

            glMatrix = PoseHelper.findPose(model, width, act, foundEyes, mRgba);
            //PoseHelper.drawDebug(mRgba, model, glMatrix);
            if (Settings.debugMode) {
                for (Point e : foundEyes) {
                    Imgproc.circle(mRgba, e, 3, new Scalar(255, 255, 255), -1);
                }
            }

        }



        boolean makeAfterPhoto = false;
        // this photo for debug purpose
        if (Static.makePhoto) {
            Static.makePhoto = false;
            makeAfterPhoto= true;
            PhotoMaker.makePhoto(mRgba, act);
        }

        // temporary for debug purposes or maby for simple effects
        Core.flip(mRgba, mRgba, 0);
        mRgba.get(0, 0, m_bbPixels.array());

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIn);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,         // Type of texture
                0,                   // Pyramid level (for mip-mapping) - 0 is the top level
                GLES20.GL_RGBA,              // Internal colour format to convert to
                width,          // Image width  i.e. 640 for Kinect in standard mode
                height,          // Image height i.e. 480 for Kinect in standard mode
                0,                   // Border width in pixels (can either be 1 or 0)
                GLES20.GL_RGBA,              // Input image format (i.e. GL_RGB, GL_RGBA, GL_BGR etc.)
                GLES20.GL_UNSIGNED_BYTE,    // Image data type
                m_bbPixels);        // The actual image data itself

        GLES20.glFlush();
        GLES20.glFinish();
        if (iFBO == null) {
            //int hFBO;
            iFBO = new int[]{0};
            GLES20.glGenFramebuffers(1, iFBO, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, iFBO[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texOut, 0);
        } else {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, iFBO[0]);
        }
        GLES20.glViewport(0, 0, width, height);

        // shader effect
        shaderEfffect2d(center, center2, texIn);
        // TODO change buffer to draw
        if (FdActivity2.currentIndexEye != 0) {
            if (foundEyes != null) {
                if (FdActivity2.currentIndexEye == 2) {
                    shaderEfffect3d(glMatrix, texIn, width, height, modelGlasses, glassesTextureid, 0.9f);
                } else if (FdActivity2.currentIndexEye == 3) {
                    // FIXME depth doesn't work
                    GLES20.glClearDepthf(1.0f);
                    GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
                    shaderEfffect3d(glMatrix, texIn, width, height, modelHat, hatTextureid, 1.0f);
                    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
                    GLES20.glDepthMask(true);
                    GLES20.glDepthFunc(GLES20.GL_LEQUAL);
                    GLES20.glDepthRangef(0.0f, 1.0f);
                    shaderEfffect3d(glMatrix, texIn, width, height, model, maskTextureid, 0.0f);
                    GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                } else {
                    shaderEfffect3d(glMatrix, texIn, width, height, model, maskTextureid, 0.7f);
                }
            } else {
                shaderEfffect3dStub();
            }
        }
        // shader effect

        if (makeAfterPhoto) {
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, m_bbPixels);
            m_bbPixels.rewind();
            //mRgba.get(width, height, pixelValues);
            mRgba.put(0, 0, m_bbPixels.array());
            Core.flip(mRgba, mRgba, 0);
            PhotoMaker.makePhoto(mRgba, act);
            // save 3d model
            File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File newFile = new File(file, Settings.DIRECTORY_SELFIE);
            final File fileJpg = new File(newFile, "outModel.obj");
            model.saveModel(fileJpg.getPath());
        }

        Log.i(TAG, "onCameraTexture " + mRgba.height() + " " + facesArray.length);

        return !Static.drawOrigTexture;
    }

    private void shaderEfffect3d(Mat glMatrix, int texIn, int width, int height, final Model modelToDraw, int modelTextureId, float alpha) {
        GLES20.glUseProgram(program3dId);
        int matrixMvp = GLES20.glGetUniformLocation(program3dId, "u_MVPMatrix");

        float[] matrixView = PoseHelper.convertToArray(glMatrix);
        float[] mMatrix = new float[16];
        Matrix.multiplyMM(mMatrix, 0, PoseHelper.createProjectionMatrixThroughPerspective(width, height), 0, matrixView, 0);
        GLES20.glUniformMatrix4fv(matrixMvp, 1, false, mMatrix, 0);

        int fAlpha = GLES20.glGetUniformLocation(program3dId, "f_alpha");
        GLES20.glUniform1f(fAlpha, alpha);

        FloatBuffer mVertexBuffer = modelToDraw.getVertices();
        mVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(vPos3d, 3, GLES20.GL_FLOAT, false, 0, mVertexBuffer);

        FloatBuffer mTextureBuffer = modelToDraw.getTexCoords();
        mTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(vTexFor3d,  2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, modelTextureId);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program3dId, "u_Texture"), 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIn);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program3dId, "u_TextureOrig"), 1);

        ShortBuffer mIndices = modelToDraw.getIndices();
        mIndices.position(0);
        // FIXME with glDrawElements use can't use other texture coordinates
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, modelToDraw.getIndicesCount(), GLES20.GL_UNSIGNED_SHORT, mIndices);
        GLES20.glFlush();
    }

    private void shaderEfffect3dStub() {
        GLES20.glUseProgram(program3dId);
        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        int matrixMvp = GLES20.glGetUniformLocation(program3dId, "u_MVPMatrix");
        float[] matrix = new float[16];

        matrix[0] = 1;
        matrix[5] = 1;
        matrix[10] = 1;
        matrix[15] = 1;
        GLES20.glUniformMatrix4fv(matrixMvp, 1, false, matrix, 0);

        FloatBuffer vertexData;
        float[] vertices = {
                -1, -1, 0,
                -1,  1, 0,
                0, 0, 0
        };
        vertexData = ByteBuffer
                .allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexData.put(vertices);
        vertexData.position(0);
        GLES20.glVertexAttribPointer(vPos3d, 3, GLES20.GL_FLOAT, false, 0, vertexData);


        ShortBuffer indexArray;
        short[] indexes = {
                0, 1, 2
        };
        indexArray = ByteBuffer
                .allocateDirect(indexes.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        indexArray.put(indexes);
        indexArray.position(0);


//        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 3, GLES20.GL_UNSIGNED_SHORT, indexArray);
        GLES20.glFlush();

    }
    private void shaderEfffect2d(Point center, Point center2, int texIn) {
        GLES20.glUseProgram(program2dEffectId);
        int uColorLocation = GLES20.glGetUniformLocation(program2dEffectId, "u_Color");
        GLES20.glUniform4f(uColorLocation, 0.0f, 0.0f, 1.0f, 1.0f);

        int uCenter = GLES20.glGetUniformLocation(program2dEffectId, "uCenter");
        GLES20.glUniform2f(uCenter, (float)center.x, (float)center.y);

        int uCenter2 = GLES20.glGetUniformLocation(program2dEffectId, "uCenter2");
        GLES20.glUniform2f(uCenter2, (float)center2.x, (float)center2.y);

        FloatBuffer vertexData;
        float[] vertices = {
                -1, -1,
                -1,  1,
                1, -1,
                1,  1
        };
//        vertices = new float[]{
//                0, 0,
//                width,  0,
//                width, height,
//                0,  height
//        };
        vertexData = ByteBuffer
                .allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexData.put(vertices);

        FloatBuffer texData;
        float[] tex = {
                0,  0,
                0,  1,
                1,  0,
                1,  1
        };
        texData = ByteBuffer
                .allocateDirect(tex.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        texData.put(tex);

        vertexData.position(0);
        GLES20.glVertexAttribPointer(vPos, 2, GLES20.GL_FLOAT, false, 0, vertexData);
        texData.position(0);
        GLES20.glVertexAttribPointer(vTex,  2, GLES20.GL_FLOAT, false, 0, texData);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIn);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program2dEffectId, "sTexture"), 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush(); //?
    }
}
