package mw.expensecontrol;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by mstowska on 9/18/2016.
 */
public class RecogniseCharacters {

    protected static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/TesseractSample/";
    private static final String TESSDATA = "tessdata";

    private String recogniseText ="";
    private TessBaseAPI tessBaseApi;
    private String choosenLanguage;

    private Context context;

    private Bitmap recogniseImage;

    public RecogniseCharacters(Context context) {
        this.context = context;
    }

    public void initialize(Uri outputFileUri, String choosenLanguage) {
        this.choosenLanguage = choosenLanguage;

        prepareTesseract();
        startOCR(outputFileUri);

    }


    public void prepareDirectory(String path) {

        File dir = new File(DATA_PATH +""+ path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e("ss", "ERROR: Creation of directory " + path + " failed, check does Android Manifest have permission to write to external storage.");
            }
        } else {
            Log.i("xx", "Created directory " + path);
        }
    }

    public String getRecogniseText() {
        return recogniseText;
    }

    public Bitmap getRecogniseImage() {
        return recogniseImage;
    }


    private void prepareTesseract() {
        try {
            prepareDirectory(TESSDATA);
        } catch (Exception e) {
            e.printStackTrace();
        }

        copyTessDataFiles(TESSDATA);
    }

    /**
     * Copy tessdata files (located on assets/tessdata) to destination directory
     *
     * @param path - name of directory with .traineddata files
     */
    private void copyTessDataFiles(String path) {
        try {
            String fileList[] = context.getAssets().list(path);

            for (String fileName : fileList) {
                if(fileName.contains(choosenLanguage)) {

                    // open file within the assets folder
                    // if it is not already there copy it to the sdcard
                    String pathToDataFile = DATA_PATH + TESSDATA + "/" + fileName;
                    if (!(new File(pathToDataFile)).exists()) {

                        InputStream in = context.getAssets().open(path + "/" + fileName);

                        OutputStream out = new FileOutputStream(pathToDataFile);

                        // Transfer bytes from in to out
                        byte[] buf = new byte[1024];
                        int len;

                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        in.close();
                        out.close();

                        Log.d("", "Copied " + fileName + "to tessdata");
                    }
                }
            }
        } catch (IOException e) {
            Log.e("", "Unable to copy files to tessdata " + e.toString());
        }
    }


    /**
     * don't run this code in main thread - it stops UI thread. Create AsyncTask instead.
     * http://developer.android.com/intl/ru/reference/android/os/AsyncTask.html
     *
     * @param imgUri
     */
    private void startOCR(Uri imgUri) {
        try {


            // _path = path to the image to be OCRed
            ExifInterface exif = new ExifInterface(imgUri.getPath());
            int exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            int rotate = 0;

            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; // 1 - means max size. 4 - means maxsize/4 size. Don't use value <4, because you need more memory in the heap to store your data.
            Bitmap bitmap = BitmapFactory.decodeFile(imgUri.getPath(), options);

            if (rotate != 0) {
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();

                // Setting pre rotate
                Matrix mtx = new Matrix();
                mtx.preRotate(rotate);

                // Rotating Bitmap & convert to ARGB_8888, required by tess
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
            }
            recogniseImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);


            recogniseText = extractText(recogniseImage);

          //  receiptText.setText(result);

        } catch (Exception e) {
            Log.e("", e.getMessage());
        }
    }


    private String extractText(Bitmap bitmap) {
        try {
            Log.d("", "extractText");
            tessBaseApi = new TessBaseAPI();
        } catch (Exception e) {
            Log.e("", e.getMessage());
            if (tessBaseApi == null) {
                Log.e("", "TessBaseAPI is null. TessFactory not returning tess object.");
            }
        }

        tessBaseApi.init(DATA_PATH, choosenLanguage);

//       //EXTRA SETTINGS
//        //For example if we only want to detect numbers
        //tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "1234567890");
//
//        //blackList Example
        //tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-qwertyuiop[]}{POIU" +
      //          "YTRWQasdASDfghFGHjklJKLl;L:'\"\\|~`xcvXCVbnmBNM,./<>?");

        Log.d("", "Training file loaded");
        tessBaseApi.setImage(bitmap);
        String extractedText = "empty result";
        try {
            extractedText = tessBaseApi.getUTF8Text();
        } catch (Exception e) {
            Log.e("", "Error in recognizing text.");
        }
        tessBaseApi.end();
        return extractedText;
    }




}
