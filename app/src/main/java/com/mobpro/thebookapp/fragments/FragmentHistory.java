package com.mobpro.thebookapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mobpro.thebookapp.R;
import com.mobpro.thebookapp.adapters.AdapterPdf;
import com.mobpro.thebookapp.databinding.FragmentHistoryBinding;
import com.mobpro.thebookapp.models.ModelPdf;

import java.util.ArrayList;

public class FragmentHistory extends Fragment {

    FragmentHistoryBinding binding;

    //arraylist untuk menyimpan list dari tipe data ModelPdf
    private ArrayList<ModelPdf> pdfArrayList;

    LinearLayoutManager layoutManager;

    //adapter
    private AdapterPdf adapterPdf;

    private String uid;

    private static final String TAG = "PDF_HISTORY_TAG";

    public FragmentHistory() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        userHistory();

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

        return binding.getRoot();
    }

    private void userHistory() {
        pdfArrayList = new ArrayList<>();

        DatabaseReference refFav = FirebaseDatabase.getInstance().getReference("Histories");
        DatabaseReference refBook = FirebaseDatabase.getInstance().getReference("Books");
        refFav.orderByChild(uid).equalTo("true")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        pdfArrayList.clear();
                        for (DataSnapshot ds: snapshot.getChildren()){
                            //mendapatkan key yaitu bookId
                            String key = ds.getKey();

                            refBook.orderByChild("id").equalTo(key)
                                    .addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            for (DataSnapshot ds: snapshot.getChildren()){
                                                //mendapatkan data
                                                ModelPdf model = ds.getValue(ModelPdf.class);
                                                //menambahkan ke list
                                                pdfArrayList.add(model);

                                                Log.d(TAG, "onDataChange: "+model.getId()+" "+model.getTitle());
                                            }
                                            adapterPdf = new AdapterPdf(getContext(), pdfArrayList);
                                            binding.bookRv.setAdapter(adapterPdf);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }
}