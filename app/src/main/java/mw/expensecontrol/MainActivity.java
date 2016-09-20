package mw.expensecontrol;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import java.io.File;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Uri outputFileUri;


    static final int REQUEST_PERMISSION_CODE = 200;
    private String[] permission = {"android.permission.CAMERA","android.permission.WRITE_EXTERNAL_STORAGE","android.permission.READ_EXTERNAL_STORAGE"};

    private SharedPreferences sharedPreferences;
    private String choosenLanguage;


    private boolean cameraAccepted;
    private boolean writeExternalStorageAccepted;
    private boolean readExternalStorageAccepted;


    private Context context;

    private RecogniseCharacters recogniseCharacters;
    private TextView recogniseText;
    private ImageView recogniseImage;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setIcon(R.drawable.cash);

        context = getApplicationContext();

        if(Build.VERSION.SDK_INT >= 7.0) {
            requestPermissions(permission, REQUEST_PERMISSION_CODE);
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        recogniseText = (TextView) findViewById(R.id.receiptText);
        recogniseImage = (ImageView) findViewById(R.id.receiptImage);

        recogniseCharacters = new RecogniseCharacters(context);

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
        choosenLanguage = sharedPreferences.getString("language_list", "0");
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
                recogniseCharacters.prepareDirectory("imgs");
                outputFileUri = Uri.fromFile(new File(recogniseCharacters.DATA_PATH + "imgs/ocr.jpg"));

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

            recogniseCharacters.initialize(outputFileUri, choosenLanguage);
            recogniseText.setText(recogniseCharacters.getRecogniseText());
            recogniseImage.setImageBitmap(recogniseCharacters.getRecogniseImage());
        }
        else {
            Toast.makeText(this, "ERROR: Image was not obtained.", Toast.LENGTH_SHORT).show();
        }
    }
}
