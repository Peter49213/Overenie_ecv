package com.example.overenie_ecv;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.ArrayList;

//851351531
public class AnalyserActivity extends AppCompatActivity {
    private InputImage image;
    private String resultText;
    private TextView text, owner;
    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyser);
        text = findViewById(R.id.textView);
        owner = findViewById(R.id.owner_tv);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Employees");
        String textViewText = text.getText().toString();
        String ocrResult = String.valueOf(text);
        Uri uri = getIntent().getParcelableExtra("image");

        try {
            image = InputImage.fromFilePath(this, uri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        processImage(recognizer, image);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                    Employees employees = dataSnapshot.getValue(Employees.class);
                    if (employees != null){
                        String plate = employees.getPlate();
                        if (plate.equals(resultText)){
                            owner.setText(employees.getName());
                        }
                        else {
                            owner.setText("cudzí");
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void processImage(TextRecognizer recognizer, InputImage image) {
        Task<Text> result =
                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {
                                // Task completed successfully
                                Toast.makeText(AnalyserActivity.this, "Text recognized successfully", Toast.LENGTH_SHORT).show();
//                                getText(visionText);
                                Text.TextBlock tallestBlock = getTallestBlock(visionText);
                                if (tallestBlock != null) {
                                    extractText(tallestBlock);
                                }
                                String resultText = visionText.getText();
                                setTextOnMainThread(resultText);

                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        Toast.makeText(AnalyserActivity.this, "Text recognizer failed", Toast.LENGTH_SHORT).show();
                                    }
                                });
    }

    private Text.TextBlock getTallestBlock(Text visionText) {
        Text.TextBlock tallestBlock = null;
        float maxBlockHeight = 0f;

        for (Text.TextBlock block : visionText.getTextBlocks()) {
            Rect blockFrame = block.getBoundingBox();
            float blockHeight = blockFrame.height();

            if (blockHeight > maxBlockHeight) {
                tallestBlock = block;
                maxBlockHeight = blockHeight;
            }
        }

        return tallestBlock;
    }

    private void extractText(Text.TextBlock tallestBlock) {
        resultText = tallestBlock.getText();
        resultText = editPlate(resultText);
        text.setText(resultText);
        // You can also display a Toast if needed:
        // Toast.makeText(AnalyserActivity.this, resultText, Toast.LENGTH_LONG).show();
    }

    private String editPlate(String resultText){
        resultText = String.format(resultText.replaceAll("[^A-Z0-9]",""));

        return resultText;
    }


//    private void getText(Text result){
//        String resultText = "";
//        resultText = result.getText();
//
//        text.setText(resultText);
//
////        Toast.makeText(AnalyserActivity.this, resultText, Toast.LENGTH_LONG).show();
//    }

    private void setTextOnMainThread(String textValue) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(textValue);
            }
        });
    }
}