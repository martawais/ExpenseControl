package mw.expensecontrol;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/TesseractSample/";
    private static final String TESSDATA = "tessdata";


    private int permissionRequestCode = 200;
    private String[] permission = {"android.permission.CAMERA","android.permission.WRITE_EXTERNAL_STORAGE","android.permission.READ_EXTERNAL_STORAGE"};

    SharedPreferences settings;
    String choosenLanguage;
    Bitmap receiptBitmap;
    ImageView receiptImage;
    Uri outputFileUri;

    private boolean cameraAccepted;
    private boolean writeExternalStorageAccepted;
    private boolean readExternalStorageAccepted;


    private TessBaseAPI tessBaseApi;
    TextView receiptText;

    private static final String lang = "eng";
    String result = "empty";



    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setIcon(R.drawable.cash);

        requestPermissions(permission, permissionRequestCode);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        //receiptImage = (ImageView) findViewById(R.id.receiptImage);
        receiptText = (TextView) findViewById(R.id.receiptText);
        receiptText.setText(result);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
        switch(permsRequestCode){
            case 200:
                cameraAccepted = grantResults[0]== PackageManager.PERMISSION_GRANTED;
                writeExternalStorageAccepted = grantResults[1]== PackageManager.PERMISSION_GRANTED;
                readExternalStorageAccepted = grantResults[2]== PackageManager.PERMISSION_GRANTED;
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        choosenLanguage = settings.getString("language_list", "0");
        Log.d("choosenLanguage", choosenLanguage);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent openPreference = new Intent(MainActivity.this, PreferenceActivityView.class);
            startActivity(openPreference);
            return true;
        }

        else if (id == R.id.action_camera) {

            try {
                String IMGS_PATH = Environment.getExternalStorageDirectory().toString() + "/TesseractSample/imgs";
                prepareDirectory(IMGS_PATH);

                String img_path = IMGS_PATH + "/ocr.jpg";

                outputFileUri = Uri.fromFile(new File(img_path));

                final Intent openCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                openCamera.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

                if (openCamera.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(openCamera, REQUEST_IMAGE_CAPTURE);
                }
            }
            catch (Exception e) {
                Log.e("", e.getMessage());
            }



        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            prepareTesseract();
            startOCR(outputFileUri);
        } else {
            Toast.makeText(this, "ERROR: Image was not obtained.", Toast.LENGTH_SHORT).show();
        }

    }

    private void prepareDirectory(String path) {

        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e("ss", "ERROR: Creation of directory " + path + " failed, check does Android Manifest have permission to write to external storage.");
            }
        } else {
            Log.i("xx", "Created directory " + path);
        }
    }


    private void prepareTesseract() {
        try {
            prepareDirectory(DATA_PATH + TESSDATA);
        } catch (Exception e) {
            e.printStackTrace();
        }

        copyTessDataFiles("tessdata");
    }

    /**
     * Copy tessdata files (located on assets/tessdata) to destination directory
     *
     * @param path - name of directory with .traineddata files
     */
    private void copyTessDataFiles(String path) {
        try {
            Log.d("", "copyTessDataFiles");
            String fileList[] = getAssets().list(path);

            Log.d("list assets", fileList[0]);
            for (String fileName : fileList) {

                // open file within the assets folder
                // if it is not already there copy it to the sdcard
                String pathToDataFile = DATA_PATH + TESSDATA + "/"+ fileName;
                if (!(new File(pathToDataFile)).exists()) {

                    InputStream in = getAssets().open(path + "/" +fileName);

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
            Log.d("", "startOCR");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; // 1 - means max size. 4 - means maxsize/4 size. Don't use value <4, because you need more memory in the heap to store your data.
            Bitmap bitmap = BitmapFactory.decodeFile(imgUri.getPath(), options);
            Log.d("", "bitmap");
            result = extractText(bitmap);

            receiptText.setText(result);

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

        tessBaseApi.init(DATA_PATH, lang);

//       //EXTRA SETTINGS
//        //For example if we only want to detect numbers
       //tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "1234567890");
//
//        //blackList Example
//        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-qwertyuiop[]}{POIU" +
//                "YTRWQasdASDfghFGHjklJKLl;L:'\"\\|~`xcvXCVbnmBNM,./<>?");

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
