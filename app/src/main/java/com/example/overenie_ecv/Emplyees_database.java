package com.example.overenie_ecv;

import static com.example.overenie_ecv.R.id.camera;
import static com.example.overenie_ecv.R.id.database;
import static com.example.overenie_ecv.R.id.drawer_layout;
import static com.example.overenie_ecv.R.id.logout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class Emplyees_database extends AppCompatActivity {
    DatabaseReference reference;
    RecyclerView recyclerView;
    ArrayList<Employees> list = new ArrayList<>();
    EmployeeAdapter employeeAdapter;
    private TextView Name, Surname, Plate;
    private EditText name, surname, plate;
    private FloatingActionButton floatingActionButton;
    private DrawerLayout mDrawerLayout;
    Toolbar toolbar;
    private NavigationView navigationView;
    ActionBarDrawerToggle toggle;
    FirebaseAuth auth;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emplyees_database);
        recyclerView = findViewById(R.id.recyclerView);
        floatingActionButton = findViewById(R.id.FAB);
        auth = FirebaseAuth.getInstance();
        mDrawerLayout = findViewById(R.id.drawer_layout);
        navigationView =findViewById(R.id.nav);
        //toolbar = findViewById(R.id.toolbar);
        employeeAdapter = new EmployeeAdapter(list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(employeeAdapter);
        reference = FirebaseDatabase.getInstance().getReference().child("Employees");
        toggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        //mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.bringToFront();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @SuppressLint("UnsafeOptInUsageError")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == camera){
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    startActivity(new Intent(Emplyees_database.this, MainActivity.class));
                }
                if (id == logout){
                    auth.signOut();
                    startActivity(new Intent(Emplyees_database.this, LoginActivity.class));
                    finish();
                }
                return true;
            }
        });

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog dialog = new Dialog(Emplyees_database.this);
                dialog.setContentView(R.layout.add_employee_dialog);
                name = dialog.findViewById(R.id.edt_name);
                surname = dialog.findViewById(R.id.edt_surname);
                plate = dialog.findViewById(R.id.edt_ECV);
                Name = dialog.findViewById(R.id.edt_name);
                Surname = dialog.findViewById(R.id.edt_surname);
                Plate = dialog.findViewById(R.id.edt_ECV);
                Button buttonAction = dialog.findViewById(R.id.Dialog_btnAdd);
                buttonAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String plate_num = Plate.getText().toString();
                        String name = Name.getText().toString();
                        String surname = Surname.getText().toString();
                        if (plate_num.isEmpty() || name.isEmpty() || surname.isEmpty()){
                            Toast.makeText(Emplyees_database.this, "Vyplňte všwtky položky", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            HashMap<String , String> Map = new HashMap<>();

                            Map.put("plate" ,plate_num);
                            Map.put("name", name);
                            Map.put("surname", surname);

                            reference.push().setValue(Map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(Emplyees_database.this, "Dáta boli uspešne uložené", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    }
                });
                dialog.show();
            }
        });
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    Employees employees = ds.getValue(Employees.class);
                    employees.setKey(snapshot.getKey());
                    list.add(employees);
                }
                if (list.isEmpty()){
                    AlertDialog.Builder builder = new AlertDialog.Builder(Emplyees_database.this);
                    builder.setTitle("Upozornenie");
                    builder.setMessage("V zozname EČv sa nenachádza žiadna položka");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    builder.show();
                }
                employeeAdapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
}