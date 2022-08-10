package com.mobpro.thebookapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mobpro.thebookapp.adapters.AdapterPdf;
import com.mobpro.thebookapp.databinding.ActivityPdfListBinding;
import com.mobpro.thebookapp.models.ModelPdf;

import java.util.ArrayList;

public class PdfListActivity extends AppCompatActivity {

    private ActivityPdfListBinding binding;

    //arraylist untuk menyimpan list dari tipe data ModelPdf
    private ArrayList<ModelPdf> pdfArrayList;
    //adapter
    private AdapterPdf adapterPdf;
    
    private String categoryId, categoryTitle;

    private static final String TAG = "PDF_LIST_TAG";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        //mendapatkan data dari intent
        Intent intent = getIntent();
        categoryId = intent.getStringExtra("categoryId");
        categoryTitle = intent.getStringExtra("categoryTitle");

        //menset pdf kategori
        binding.subTitleTv.setText(categoryTitle);
        
        loadPdfList();

        //search
        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //mencari ketika user user mengetikan setiap huruf
                try {
                    adapterPdf.getFilter().filter(charSequence);
                }
                catch (Exception e){
                    Log.d(TAG, "onTextChanged: "+e.getMessage());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        
        //klik back btn
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void loadPdfList() {
        pdfArrayList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.orderByChild("categoryId").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        pdfArrayList.clear();
                        for (DataSnapshot ds: snapshot.getChildren()){
                            //mendapatkan data
                            ModelPdf model = ds.getValue(ModelPdf.class);
                            //menambahkan ke list
                            pdfArrayList.add(model);

                            Log.d(TAG, "onDataChange: "+model.getId()+" "+model.getTitle());
                        }
                        adapterPdf = new AdapterPdf(PdfListActivity.this, pdfArrayList);
                        binding.bookRv.setAdapter(adapterPdf);
                    }


                    @Override
                    public void onCancelled(@NonNull  DatabaseError error) {

                    }
                });
    }
}