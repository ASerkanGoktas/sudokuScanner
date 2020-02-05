package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Mode;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    CameraView camera;
    Button photo_button;
    private List<MatOfPoint> aliveli = new ArrayList<>();

    private BaseLoaderCallback openCVloaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {


            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, openCVloaderCallback);

        } else {


            openCVloaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camera = findViewById(R.id.camera);
        photo_button = findViewById(R.id.photo_button);
        camera.setLifecycleOwner(this);
        camera.setMode(Mode.PICTURE);



        camera.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                Log.d("merhabalaar", "foto çekildi");



                byte[] image_data = result.getData();
                Bitmap bmp = BitmapFactory.decodeByteArray(image_data, 0, image_data.length);
                Mat frame = new Mat();


                Utils.bitmapToMat(bmp, frame);


                Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
                Core.rotate(frame, frame, Core.ROTATE_90_CLOCKWISE);

                Imgproc.GaussianBlur(frame, frame, new Size(15, 15), 6);
                Imgproc.GaussianBlur(frame, frame, new Size(15, 15), 6);
                Imgproc.GaussianBlur(frame, frame, new Size(15, 15), 6);
                Imgproc.resize(frame, frame, new Size(600, 800));

                int desired_height = 400;
                int desired_width = 300;

                int height = frame.rows();
                int width = frame.cols();

                int scale = height / desired_height;
                int x = 25 * scale;
                int y = 75 * scale;
                int rect_len = 250 * scale;

                Rect aoi = new Rect(x, y, rect_len, rect_len);
                Mat cropped = frame.submat(aoi);

                //TEMP
                Bitmap.Config CNF = Bitmap.Config.ARGB_8888;
                Bitmap temp_bmp = Bitmap.createBitmap(rect_len, rect_len, CNF);
                //TEMP
                Utils.matToBitmap(cropped, temp_bmp);


                double[][] kernel = {{-1, -1, -1}, {-1, 9.4, -1}, {-1, -1, -1}};

                Mat KERNEL = new Mat(3, 3, CvType.CV_8S);

                for(int row = 0; row < 3; row++){
                    for(int col = 0; col < 3; col++){
                        KERNEL.put(row, col, kernel[row][col]);
                    }
                }


                Imgproc.filter2D(cropped, cropped, -1, KERNEL);
                Utils.matToBitmap(cropped, temp_bmp);

                Imgproc.GaussianBlur(cropped, cropped, new Size(5, 5), 3);
                Utils.matToBitmap(cropped, temp_bmp);

                Imgproc.filter2D(cropped, cropped, -1, KERNEL);
                Utils.matToBitmap(cropped, temp_bmp);

                remove_shadows(cropped, cropped);
                Utils.matToBitmap(cropped, temp_bmp);

                Imgproc.medianBlur(cropped, cropped, 5);
                Utils.matToBitmap(cropped, temp_bmp);

                Imgproc.adaptiveThreshold(cropped, cropped, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 5);
                Utils.matToBitmap(cropped, temp_bmp);

                Imgproc.filter2D(cropped, cropped, -1, KERNEL);
                Utils.matToBitmap(cropped, temp_bmp);

                Imgproc.filter2D(cropped, cropped, -1, KERNEL);
                Utils.matToBitmap(cropped, temp_bmp);

                find_corners(cropped, cropped, temp_bmp);
                int a = 3;
            }
        });


        photo_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.takePicture();
            }
        });
    }

    private void remove_shadows(Mat src, Mat dst){
        Mat acc = src.clone();
        Mat dilated = new Mat();

        Mat all_ones = new Mat(7, 7, CvType.CV_8S);
        all_ones.setTo(new Scalar(1, 1));
        Imgproc.dilate(acc, dilated, all_ones);

        Mat bg_image = new Mat();

        Imgproc.medianBlur(dilated, bg_image, 21);
        Mat diff_img = new Mat();

        Core.absdiff(acc, bg_image, diff_img);
        Core.normalize(diff_img, dst, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
    }

    private void find_corners(Mat src, Mat dst, Bitmap bmp){

        Mat src_cpy = src.clone();

        Rect rect = null;

        Mat all_ones = new Mat(5, 5, CvType.CV_8S);
        all_ones.setTo(new Scalar(1, 1));

        Imgproc.dilate(src, src, all_ones);
        Utils.matToBitmap(src, bmp);

        double[][] kernel = {{-1, -1, -1}, {-1, 10, -1}, {-1, -1, -1}};

        Mat KERNEL = new Mat(3, 3, CvType.CV_8S);

        for(int row = 0; row < 3; row++){
            for(int col = 0; col < 3; col++){
                KERNEL.put(row, col, kernel[row][col]);
            }
        }

        Imgproc.filter2D(src, src, -1, KERNEL);
        Utils.matToBitmap(src, bmp);

        /*Imgproc.medianBlur(src, src, 13);
        Utils.matToBitmap(src, bmp);

        Imgproc.filter2D(src, src, -1, KERNEL);
        Utils.matToBitmap(src, bmp);*/
        Mat squa = new Mat(500, 500, CvType.CV_8UC1);
        squa.setTo(new Scalar(0));
        List<MatOfPoint> squares = new ArrayList<>();
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hier = new Mat();

        Imgproc.findContours(src, contours, hier, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        squares.add(find_most_area(contours));


        Imgproc.drawContours(squa, squares, 0, new Scalar(255, 255, 255), 1);
        Utils.matToBitmap(squa, bmp);


        MatOfPoint2f thiscont = new MatOfPoint2f();
        MatOfPoint2f approx = new MatOfPoint2f();

        squares.get(0).convertTo(thiscont, CvType.CV_32FC2);
        Imgproc.approxPolyDP(thiscont, approx, 0.5, true);

        squa.setTo(new Scalar(0));

        Mat a = new Mat(500, 500, CvType.CV_8UC1);
        approx.convertTo(a, CvType.CV_32S);


        int ch = a.channels();
        int asf = a.depth();

        Rect re = Imgproc.boundingRect(a);

        Utils.matToBitmap(src_cpy, bmp);

        Mat veleeey = src_cpy.clone();
        Imgproc.rectangle(veleeey, re, new Scalar(255, 255, 255), 3);
        Utils.matToBitmap(veleeey, bmp);

        Mat viola = new Mat();
        List<Point> source_list = new ArrayList<>();
        source_list.add(new Point(re.x, re.y));
        source_list.add(new Point(re.x + re.width, re.y));
        source_list.add(new Point(re.x, re.y + re.height));
        source_list.add(new Point(re.x + re.width, re.y + re.height));

        Mat source_M = Converters.vector_Point2f_to_Mat(source_list);

        List<Point> dest_list = new ArrayList<>();
        dest_list.add(new Point(0, 0));
        dest_list.add(new Point(500, 0));
        dest_list.add(new Point(0, 500));
        dest_list.add(new Point(500, 500));

        Mat dest_M = Converters.vector_Point2f_to_Mat(dest_list);

        Mat pers_tr = Imgproc.getPerspectiveTransform(source_M, dest_M);
        Imgproc.warpPerspective(src_cpy, viola, pers_tr, new Size(500, 500));

        Utils.matToBitmap(viola, bmp);

        Point bottom_left = closest(squares.get(0), new Point(re.x, re.y + re.height));
        Point bottom_right = closest(squares.get(0), new Point(re.x + re.width, re.y + re.height));

        source_list.clear();
        source_list.add(new Point(re.x, re.y + 5));
        source_list.add(new Point(re.x + re.width, re.y + 5));
        source_list.add(bottom_left);
        source_list.add(bottom_right);

        source_M = Converters.vector_Point2f_to_Mat(source_list);

        pers_tr = Imgproc.getPerspectiveTransform(source_M, dest_M);
        Imgproc.warpPerspective(src_cpy, viola, pers_tr, new Size(500, 500));

        Utils.matToBitmap(viola, bmp);

        int e = 3;
    }

    private MatOfPoint find_most_area(List<MatOfPoint> contours){
        MatOfPoint result = null;
        double max_area = -1;
        double temp = -1;
        for(MatOfPoint contour : contours){
            temp = Imgproc.contourArea(contour);

            if(temp > max_area){
                max_area = temp;
                result = contour;
            }

        }

        return result;
    }

    private boolean isContourSquare(MatOfPoint contour){

        Rect rect = null;

        MatOfPoint2f thisContour2f = new MatOfPoint2f();
        MatOfPoint approxContour = new MatOfPoint();
        MatOfPoint2f approxContour2f = new MatOfPoint2f();

        contour.convertTo(thisContour2f, CvType.CV_32FC2);
        Imgproc.approxPolyDP(thisContour2f, approxContour2f, 2, true);
        approxContour2f.convertTo(approxContour, CvType.CV_32S);


        if (approxContour.size().height == 4) {
            rect = Imgproc.boundingRect(approxContour);
        }

        return (rect != null);
    }

    private Point closest(MatOfPoint contour, Point reference){
        Point[] points = contour.toArray();

        Point result = null;
        double min = 1000000000;
        double temp_distance;
        for(Point point: points){
            temp_distance = euclid_distance(point, reference);

            if(temp_distance < min){
                min = temp_distance;
                result = point.clone();
            }
        }

        return result;
    }

    private double euclid_distance(Point a, Point b){
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }





}
