package com.example.overenie_ecv;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.MyViewHolder> {


    List<Employees> employees;

    public EmployeeAdapter(ArrayList<Employees> list) {
        this.employees = list;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.employee_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Employees employee = employees.get(position);
        holder.plate.setText(employee.getPlate());
        holder.name.setText(employee.getName());
        holder.surname.setText(employee.getSurname());

        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alert = new AlertDialog.Builder(holder.plate.getContext());
                alert.setTitle("Odstrániť");
                alert.setMessage("Naozaj chcete odstrániť túto položku?");
                alert.setIcon(R.drawable.ic_baseline_delete_24);
                alert.setPositiveButton("Ano", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        FirebaseDatabase.getInstance().getReference("Employees").child(employee.getKey()).removeValue();
                        employees.remove(position);
                        notifyItemRemoved(position);
                    }
                });
                alert.setNegativeButton("Nie", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(holder.plate.getContext(), "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                });
                alert.show();
            }
        });
    }
    @Override
    public int getItemCount() {
        return employees.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView plate, name, surname;
        Button button;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            plate = itemView.findViewById(R.id.list_text_plate);
            name = itemView.findViewById(R.id.text_name);
            surname = itemView.findViewById(R.id.text_surname);
            button = itemView.findViewById(R.id.delete_btn);
        }
    }
}
