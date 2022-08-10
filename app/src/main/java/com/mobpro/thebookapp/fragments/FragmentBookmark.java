package com.mobpro.thebookapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

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
import com.mobpro.thebookapp.LoginActivity;
import com.mobpro.thebookapp.PdfListActivity;
import com.mobpro.thebookapp.R;
import com.mobpro.thebookapp.adapters.AdapterPdf;
import com.mobpro.thebookapp.databinding.FragmentBookmarkBinding;
import com.mobpro.thebookapp.models.ModelCategory;
import com.mobpro.thebookapp.models.ModelPdf;

import java.util.ArrayList;

public class FragmentBookmark extends Fragment {

    FragmentBookmarkBinding binding;

    //arraylist untuk menyimpan list dari tipe data ModelPdf
    private ArrayList<ModelPdf> pdfArrayList, bookmarkArrayList;

    //adapter
    private AdapterPdf adapterPdf;

    private String uid, bookId, pages;

    private static final String TAG = "PDF_BOOKMARK_TAG";

    public FragmentBookmark() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentBookmarkBinding.inflate(inflater, container, false);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        bookmarkBookId();
        bookmarkArrayList = new ArrayList<>();

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

    private void bookmarkBookId() {
        pdfArrayList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pdfArrayList.clear();
                bookmarkArrayList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    //mendapatkan data
                    ModelPdf model = ds.getValue(ModelPdf.class);

                    bookId = model.getId();
                    bookmarkBook(bookId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void bookmarkBook(String id) {
        DatabaseReference refBookmark= FirebaseDatabase.getInstance().getReference("Bookmarks");
        refBookmark.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren()){
                    String key = ds.getKey();
                    pages = ds.child(uid).getValue(String.class);
                    try {
                        if (id.equals(key) && pages!=null){
                            viewBookmarkBook(key, pages);
                        }
                    }
                    catch (NullPointerException e){
                        Log.e(TAG, "onComplete: NullPointerException"+ e.getMessage());
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void viewBookmarkBook(String match, String pageNo) {

        DatabaseReference refBook= FirebaseDatabase.getInstance().getReference("Books");
        refBook.orderByChild("id").equalTo(match)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds: snapshot.getChildren()){
                            //mendapatkan data
                            ModelPdf model = ds.getValue(ModelPdf.class);
                            //menambahkan ke list
                            bookmarkArrayList.add(model);
                            model.setIdentity("bookmarks");
                            model.setPage(pageNo);
                            Log.d(TAG, "onDataChange: "+model.getPage());
                        }
                        adapterPdf = new AdapterPdf(getContext(), bookmarkArrayList);
                        binding.bookRv.setAdapter(adapterPdf);
                    }

                    @Override
                    public void onCancelled(@NonNull  DatabaseError error) {

                    }
                });
    }
}