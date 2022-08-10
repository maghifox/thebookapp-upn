package com.mobpro.thebookapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mobpro.thebookapp.databinding.ActivityPdfAddBinding;

import java.util.ArrayList;
import java.util.HashMap;

public class PdfAddActivity extends AppCompatActivity {

    private ActivityPdfAddBinding binding;

    private FirebaseAuth firebaseAuth;

    private ProgressDialog progressDialog;

    //arraylist untuk menyimpan kategori
    private ArrayList<String> categoryTittleArrayList, categoryIdArrayList;

    //Uri dari pdf yang diambil
    private Uri pdfUri = null;

    private static final int PDF_PICK_CODE = 1000;

    //buat debugging
    private static final String TAG = "ADD_PDF_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        loadPdfCategories();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Mohon Tunggu");
        progressDialog.setCanceledOnTouchOutside(false);

        //klik back btn
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //klik attach pdf
        binding.attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pdfPickIntent();
            }
        });
        
        //klik memilih kategori
        binding.categoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                categoryPickDialog();
            }
        });

        //klik uploud pdf
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //validasi data
                validateData();
            }
        });
    }

    private String title = "", description = "";

    private void validateData() {
        //langkah 1 :validasi data
        Log.d(TAG, "validateData: memvalidasi data...");

        //mendapatkan data
        title = binding.titleEt.getText().toString().trim();
        description = binding.descriptionEt.getText().toString().trim();

        //validasi data
        if (TextUtils.isEmpty(title)){
            Toast.makeText(this, "Masukan Judul...", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(description)){
            Toast.makeText(this, "Masukan Deskripsi...", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(selectedCategoryTittle)){
            Toast.makeText(this, "Pilih Kategori...", Toast.LENGTH_SHORT).show();
        }
        else if (pdfUri==null){
            Toast.makeText(this, "Pilih PDF...", Toast.LENGTH_SHORT).show();
        }
        else{
            //semua data sudah valid, bisa uploud sekarang
            uploadPdfToStorage();
        }
    }

    private void uploadPdfToStorage() {
        //langkah 2: uploud pdf ke firebase storage
        Log.d(TAG, "uploadPdfToStorage: mengupload ke storage...");

        progressDialog.setMessage("Mengupload Pdf...");
        progressDialog.show();

        long timestamp = System.currentTimeMillis();

        //path dari pdf di firebase storage
        String filePathAndName = "Books/" + timestamp;

        StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
        storageReference.putFile(pdfUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG, "onSuccess: PDF telah terupload ke storage");
                        Log.d(TAG, "onSuccess: mendapatkan url pdf");

                        //mendapatkan pdf url
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String uploadPdfUrl = ""+uriTask.getResult();

                        //upload ke firebase db
                        uploadPdfInfoToDb(uploadPdfUrl, timestamp);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onFailure: PDF upload gagal karena"+e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "PDF upload gagal karena"+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

    }

    private void uploadPdfInfoToDb(String uploadPdfUrl, long timestamp) {
        //langkah 3: uploud pdf info ke firebase db
        Log.d(TAG, "uploadPdfToStorage: mengupload pdf info ke firebase db...");

        progressDialog.setMessage("Uploading pdf info...");

        String uid = firebaseAuth.getUid();
        String favorite = "0";
        String readers = "0";
        String shared = "0";
        String downloads = "0";

        //data untuk diupload
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", ""+uid);
        hashMap.put("id", ""+timestamp);
        hashMap.put("title", ""+title);
        hashMap.put("description", ""+description);
        hashMap.put("categoryId", ""+selectedCategoryId);
        hashMap.put("url", ""+uploadPdfUrl);
        hashMap.put("favorite", ""+favorite);
        hashMap.put("readers", ""+readers);
        hashMap.put("shared", ""+shared);
        hashMap.put("downloads", ""+downloads);
        hashMap.put("timestamp", timestamp);


        //db reference, db > Books
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(""+timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onSuccess: Berhasil diupload...");
                        Toast.makeText(PdfAddActivity.this, "Berhasil diupload...", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onFailure: Gagal mengupload ke db karena "+e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "Gagal mengupload ke db karena "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadPdfCategories(){
        Log.d(TAG, "LoadPdfCategories: Loading pdf categories...");
        categoryTittleArrayList = new ArrayList<>();
        categoryIdArrayList = new ArrayList<>();

        //db reference untuk meload kategori db > categories
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Categories");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryTittleArrayList.clear(); //menghapus array terlebih dahulu sebelum menambahkan data
                categoryIdArrayList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){

                    //mendapatkan id dan judul dari kategori
                    String categoryId = ""+ds.child("id").getValue();
                    String categoryTitle = ""+ds.child("category").getValue();

                    //menambahkan ke arraylists
                    categoryTittleArrayList.add(categoryTitle);
                    categoryIdArrayList.add(categoryId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //kategori id dan judul kategori yang telah dipilih
    private String selectedCategoryId, selectedCategoryTittle;

    private void categoryPickDialog() {
        Log.d(TAG, "categoryPickDialog: showing category pick dialog");

        //mendapatkan string array dari kategori arraylist
        String[] categoriesArray = new String[categoryTittleArrayList.size()];
        for (int i = 0; i< categoryTittleArrayList.size(); i++){
            categoriesArray[i] = categoryTittleArrayList.get(i);
        }

        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Category")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        //item klik
                        //mendapatkan item yang diklik dari list
                        selectedCategoryTittle = categoryTittleArrayList.get(i);
                        selectedCategoryId = categoryIdArrayList.get(i);
                        //set ke kategori textview
                        binding.categoryTv.setText(selectedCategoryTittle);

                        Log.d(TAG, "onClick: Selected Category: "+selectedCategoryId+" "+selectedCategoryTittle);
                    }
                })
                .show();
    }

    private void pdfPickIntent() {
        Log.d(TAG, "pdfPickIntent: Memulai pdf pick intent");

        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Pilih Pdf"), PDF_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){
            if (requestCode == PDF_PICK_CODE){
                Log.d(TAG, "OnActivityResult: Pdf telah diambil");

                pdfUri = data.getData();

                Log.d(TAG, "onActivityResult: URI: "+pdfUri);
            }
        }
        else {
            Log.d(TAG, "onActivityResult: pengambilan pdf dibatalkan");
            Toast.makeText(this, "Pengambilan pdf dibatalkan", Toast.LENGTH_SHORT).show();
        }

    }
}