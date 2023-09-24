package com.example.overenie_ecv;

import static com.example.overenie_ecv.R.id.*;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Rational;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.overenie_ecv.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ExperimentalCamera2Interop public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private OrientationEventListener orientationEventListener;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private int screenRotation, orientation;
    public String fileName;
    private PreviewView viewFinder;
    private DrawerLayout mDrawerLayout;
    FirebaseAuth auth;
    FirebaseUser user;
    Toolbar toolbar;
   private NavigationView navigationView;
    ActionBarDrawerToggle toggle;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        mDrawerLayout = findViewById(R.id.drawer_layout);
        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener(v -> takePhoto());
        viewFinder = findViewById(R.id.viewFinder);
        navigationView =findViewById(R.id.nav);
        auth = FirebaseAuth.getInstance();
        toggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toggle.syncState();
        navigationView.bringToFront();
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == database){
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                }
                return true;
            }
        });

        cameraExecutor = Executors.newSingleThreadExecutor();
        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                Log.e("orientation", Integer.toString(orientation));
                screenRotation = orientation;
            }
        };

        Log.d("MainActivity", "orientationEventListener initialized");
        //gyro start measuring
//        orientationEventListener.enable();


    }

    /*public void onMenuItemClicked(View view) {
        // Handle the menu item click here
        // For example, you can close the drawer after an item is clicked
        mDrawerLayout.closeDrawers();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        return super.onOptionsItemSelected(item);
    }*/

    protected void onResume() {
        super.onResume();

        orientationEventListener.enable();

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissions();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        orientationEventListener.disable();

        Log.d("MainActivity", "onPause() called");

        // Disable the orientationEventListener if it's not null
        if (orientationEventListener != null) {
            orientationEventListener.disable();
            Log.d("MainActivity", "orientationEventListener disabled");
        } else {
            Log.e("MainActivity", "orientationEventListener is null");
        }

        // Release the imageCapture instance if it's not null
        if (imageCapture != null) {
            imageCapture = null;
            Log.d("MainActivity", "imageCapture released");
        } else {
            Log.e("MainActivity", "imageCapture is null");
        }

        cameraExecutor.shutdown();
    }

    public void startSecondActivity(Uri image){
        Intent intent = new Intent(MainActivity.this, AnalyserActivity.class);
        intent.putExtra("image", image);
        if (image != null){
            startActivity(intent);
        }
    }

    private void takePhoto() {
        orientation = screenRotation;
        // Get a stable reference of the modifiable image capture use case
        ImageCapture imageCapture = this.imageCapture;
        imageCapture.setCropAspectRatio(new Rational(8, 16));
        if (imageCapture == null) {
            return;
        }

        // Create time stamped name and MediaStore entry
        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());
        fileName = name;
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
        }

        // Create output options object which contains file + metadata
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                .build();

        // Set up image capture listener, which is triggered after the photo has been taken
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onError(@NonNull ImageCaptureException exc) {
                    Log.e(TAG, "Photo capture failed: " + exc.getMessage(), exc);
                }

                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                    String msg = "Photo capture succeeded: " + output.getSavedUri();
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, msg);

                    Uri savedUri = output.getSavedUri();
                    if (savedUri != null) {
                        rotatePicture(savedUri, orientation);
                    } else {
                        Log.e(TAG, "Failed to save the image.");
                    }
                }
            });
    }
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            ProcessCameraProvider cameraProvider;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                return;
            }

            //Preview
            Preview.Builder previewBuilder = new Preview.Builder();
            Preview preview = previewBuilder.setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build();


//            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

            // ImageCapture
            ImageCapture.Builder imageCaptureBuilder = new ImageCapture.Builder();
//            imageCapture.setTargetRotation(Surface.ROTATION_90);

            // Select back camera as a default
            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);


            } catch (Exception exc) {
                Log.e(TAG, "Use case binding failed", exc);
            }

        }, ContextCompat.getMainExecutor(this));

        imageCapture = new ImageCapture.Builder().build();
    }

    private void requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private static final String TAG = "CameraXApp";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static String[] REQUIRED_PERMISSIONS;

    static {
        REQUIRED_PERMISSIONS = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    private void rotatePicture(Uri imageUri, int orientation) {
        // Load the image from the URI
        Bitmap originalBitmap;
        try {
            originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load the image: " + e.getMessage(), e);
            return;
        }

        // Rotate the bitmap
        Matrix matrix = new Matrix();
        matrix.postRotate(orientation+90);
        Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

        // Save the rotated bitmap back to the URI
        OutputStream outputStream;
        try {
            outputStream = getContentResolver().openOutputStream(imageUri);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();
            startSecondActivity(imageUri);
        } catch (IOException e) {
            Log.e(TAG, "Failed to save the rotated image: " + e.getMessage(), e);
            return;
        }

        // Optionally, display a success message
        Toast.makeText(MainActivity.this, "Image rotated and saved successfully.", Toast.LENGTH_SHORT).show();
    }

    private final ActivityResultLauncher<String[]> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    new ActivityResultCallback<Map<String, Boolean>>() {
                        @Override
                        public void onActivityResult(Map<String, Boolean> permissions) {
                            // Handle Permission granted/rejected
                            boolean permissionGranted = true;
                            for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                                if (Arrays.asList(REQUIRED_PERMISSIONS).contains(entry.getKey())
                                        && !entry.getValue()) {
                                    permissionGranted = false;
                                }
                            }
                            if (!permissionGranted) {
                                Toast.makeText(getApplicationContext(),
                                        "Permission request denied",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                startCamera();
                            }
                        }
                    });

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == database){
            startActivity(new Intent(MainActivity.this, Emplyees_database.class));
        }
        return true;
    }
}
