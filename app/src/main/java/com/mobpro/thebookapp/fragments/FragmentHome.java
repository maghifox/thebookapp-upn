package com.mobpro.thebookapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mobpro.thebookapp.DashboardAdminActivity;
import com.mobpro.thebookapp.LoginActivity;
import com.mobpro.thebookapp.R;
import com.mobpro.thebookapp.adapters.AdapterCategory;
import com.mobpro.thebookapp.databinding.FragmentHomeBinding;
import com.mobpro.thebookapp.models.ModelCategory;

import java.util.ArrayList;

public class FragmentHome extends Fragment {

    FragmentHomeBinding binding;

    //arraylist untuk menyimpan kategori
    private ArrayList<ModelCategory> categoryArrayList;

    //adapter
    private AdapterCategory adapterCategory;


    public FragmentHome() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
       binding = FragmentHomeBinding.inflate(inflater, container, false);

       loadCategories();

        //edit text search
        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                //dipanggil saat user mengketikan huruf
                try {
                    adapterCategory.getFilter().filter(s);
                }
                catch (Exception e){

                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
       
       return binding.getRoot();
    }

    private void loadCategories() {
        categoryArrayList = new ArrayList<>();

        //mendapatkan semua kategori dari firebase > Categories
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Categories");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //mengkosongkan arraylist sebelum menambahkan data
                categoryArrayList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    //mendapatkan data
                    ModelCategory model = ds.getValue(ModelCategory.class);

                    //menambahkan ke arraylist
                    categoryArrayList.add(model);
                }
                if(LoginActivity.user == 1) {
                    adapterCategory = new AdapterCategory(getContext(), categoryArrayList);
                    //set adapter ke recyclerview
                    binding.categoriesRv.setAdapter(adapterCategory);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}