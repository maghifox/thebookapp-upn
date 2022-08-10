package com.mobpro.thebookapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.krishna.fileloader.FileLoader;
import com.krishna.fileloader.listener.FileRequestListener;
import com.krishna.fileloader.pojo.FileResponse;
import com.krishna.fileloader.request.FileLoadRequest;
import com.mobpro.thebookapp.databinding.ActivityPdfDetailBinding;
import com.mobpro.thebookapp.models.ModelPdf;

import java.io.File;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class PdfDetailActivity extends AppCompatActivity {

    private ActivityPdfDetailBinding binding;

    private String bookId, title,description,pdfUrl, categoryId, uid, favorite, readers, downloads, shared;
    int favoritesInt, readersInt, downloadsInt;

    boolean favoriteBool=false;

    File pdfUrl1;

    private static final String TAG = "PDF_DETAIL_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //mendapatkan data dari intent
        Intent intent = getIntent();
        bookId =intent.getStringExtra("bookId");
        title = intent.getStringExtra("title");
        description = intent.getStringExtra("description");
        pdfUrl = intent.getStringExtra("pdfUrl");
        categoryId = intent.getStringExtra("categoryId");
        favorite = intent.getStringExtra("favorite");
        readers = intent.getStringExtra("readers");
        shared = intent.getStringExtra("shared");
        downloads = intent.getStringExtra("downloads");
        readersInt = Integer.parseInt(readers);
        favoritesInt = Integer.parseInt(favorite);
        downloadsInt = Integer.parseInt(downloads);

        //menset book stats
        binding.bookName.setText(title);
        binding.description.setText(description);
        binding.favoritesText.setText(favorite);
        binding.readerText.setText(readers);
        binding.shareText.setText(shared);
        binding.downloadText.setText(downloads);

        loadCategory(categoryId);
        loadPdfFromUrl(pdfUrl);
        setFavorite(bookId);

        binding.favoriteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userFavoriteBooks();
            }
        });

        binding.downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userDownloadedBooks(PdfDetailActivity.this, title, ".pdf", DIRECTORY_DOWNLOADS);
            }
        });

        binding.readBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readBook();
                userReads();
                userReadHistory();
            }
        });

    }

    private void userReadHistory() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Histories");
        DatabaseReference refBook = FirebaseDatabase.getInstance().getReference("Books");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(bookId).hasChild(uid)){
                    //sudah membaca
                    refBook.child(bookId).child("timestamp").setValue(System.currentTimeMillis());
                }
                else{
                    //belum membaca
                    ref.child(bookId).child(uid).setValue("true");
                    refBook.child(bookId).child("timestamp").setValue(System.currentTimeMillis());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void readBook() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.orderByChild("id").equalTo(bookId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    ModelPdf model = snapshot1.getValue(ModelPdf.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        Intent i = new Intent(PdfDetailActivity.this, BookViewActivity.class);
        i.putExtra("pdfUrl", pdfUrl);
        i.putExtra("bookId", bookId);
        i.putExtra("title", title);
        i.putExtra("favorite", String.valueOf(favoritesInt));
        startActivity(i);
    }

    private void userReads() {
        DatabaseReference refReads = FirebaseDatabase.getInstance().getReference("Reads");
        DatabaseReference refBooks = FirebaseDatabase.getInstance().getReference("Books");
        refReads.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(bookId).hasChild(uid)){
                    //buku sudah dibaca jadi tidak perlu mengubah apapun
                }
                else{
                    //belum dibaca jadi menambah jumlah readers
                    readersInt = readersInt + 1;
                    refBooks.child(bookId).child("readers").setValue(""+ readersInt);
                    binding.readerText.setText(String.valueOf(readersInt));
                    refReads.child(bookId).child(uid).setValue("true");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void userDownloadedBooks(Context context, String filename, String fileExtension, String destinationDirectory) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        Uri uri = Uri.parse(pdfUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(context, destinationDirectory, filename + fileExtension);

        downloadManager.enqueue(request);

        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Books");
        final Query query = reference.orderByChild("id").equalTo(bookId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    ModelPdf model = snapshot1.getValue(ModelPdf.class);
                    downloadsInt = downloadsInt + 1;
                    model.setDownloads(String.valueOf(downloadsInt));
                    binding.downloadText.setText(String.valueOf(downloadsInt));
                    reference.child(bookId).setValue(model);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void setFavorite(String bookId) {

        DatabaseReference refFavorite = FirebaseDatabase.getInstance().getReference("Favorites");
        refFavorite.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(bookId).hasChild(uid)){
                    //user telah memfavoritkan buku
                    binding.favoriteFab.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite_book, getApplicationContext().getTheme()));
                    binding.favoritesText.setText(String.valueOf(favoritesInt));
                }
                else {
                    //user blum memfavoritkan buku
                    binding.favoritesText.setText(String.valueOf(favoritesInt));
                    binding.favoriteFab.setImageDrawable(getResources().getDrawable(R.drawable.ic_favourite_border, getApplicationContext().getTheme()));
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }


    private void userFavoriteBooks() {
        favoriteBool = true;
        DatabaseReference refFavorite = FirebaseDatabase.getInstance().getReference("Favorites");
        DatabaseReference refBooks = FirebaseDatabase.getInstance().getReference("Books");
        refFavorite.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (favoriteBool){
                    if (snapshot.child(bookId).hasChild(uid)){
                        //sudah difavoritkan jadi menghapus favorite jika diklik
                        favoritesInt = favoritesInt - 1;
                        refBooks.child(bookId).child("favorite").setValue(""+ favoritesInt);
                        binding.favoritesText.setText(String.valueOf(favoritesInt));
                        refFavorite.child(bookId).child(uid).removeValue();
                        favoriteBool = false;
                    }
                    else{
                        //belum difavoritkan jadi favorite jika diklik
                        favoritesInt = favoritesInt + 1;
                        refBooks.child(bookId).child("favorite").setValue(""+ favoritesInt);
                        binding.favoritesText.setText(String.valueOf(favoritesInt));
                        refFavorite.child(bookId).child(uid).setValue("true");
                        favoriteBool = false;
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void loadCategory(String categoryId) {
        //mendapatkan kategorri menggunakan categoryId

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Categories");
        ref.child(categoryId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //mendapatkan category
                        String category = ""+snapshot.child("category").getValue();

                        //menset ke category text view
                        binding.bookCategory.setText(category);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadPdfFromUrl(String url) {
        //menggunakan url kita dapat mendapatkan file dan metadatanya dari firebase storage
        FileLoader.with(this)
            .load(url, false)
            .fromDirectory("test4", FileLoader.DIR_INTERNAL)
            .asFile(new FileRequestListener<File>() {
                @Override
                public void onLoad(FileLoadRequest request, FileResponse<File> response) {
                    binding.progressBar.setVisibility(View.INVISIBLE);
                    pdfUrl1 = response.getBody();

                    try {
                        binding.pdfView.fromFile(pdfUrl1)
                            .pages(0)
                            .spacing(0)
                            .swipeHorizontal(false)
                            .enableSwipe(false)
                            .load();

                        Log.d(TAG, "FilePath: "+ pdfUrl1);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(FileLoadRequest request, Throwable t) {
                    binding.progressBar.setVisibility(View.INVISIBLE);
                    Log.d(TAG, "onPageError: "+t.getMessage());
                }
            });

    }
}