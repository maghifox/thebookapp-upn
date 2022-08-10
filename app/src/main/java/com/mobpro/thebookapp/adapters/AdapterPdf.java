package com.mobpro.thebookapp.adapters;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.listener.OnPageScrollListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.krishna.fileloader.FileLoader;
import com.krishna.fileloader.listener.FileRequestListener;
import com.krishna.fileloader.pojo.FileResponse;
import com.krishna.fileloader.request.FileLoadRequest;
import com.mobpro.thebookapp.BookViewActivity;
import com.mobpro.thebookapp.LoginActivity;
import com.mobpro.thebookapp.MyApplication;
import com.mobpro.thebookapp.PdfDetailActivity;
import com.mobpro.thebookapp.PdfEditActivity;
import com.mobpro.thebookapp.databinding.RowPdfBinding;
import com.mobpro.thebookapp.filters.FilterPdf;
import com.mobpro.thebookapp.models.ModelPdf;

import java.io.File;
import java.util.ArrayList;

public class AdapterPdf extends RecyclerView.Adapter<AdapterPdf.HolderPdf> implements Filterable {

    private Context context;
    //arraylist untuk menyimpan data ModelPdf
    public ArrayList<ModelPdf> pdfArrayList, filterList;

    private RowPdfBinding binding;

    private FilterPdf filter;

    File pdfUrl1;

    private static final String TAG = "PDF_ADAPTER_TAG";

    private ProgressDialog progressDialog;

    public AdapterPdf(Context context, ArrayList<ModelPdf> pdfArrayList) {
        this.context = context;
        this.pdfArrayList = pdfArrayList;
        this.filterList = pdfArrayList;
    }

    @NonNull
    @Override
    public HolderPdf onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        binding = RowPdfBinding.inflate(LayoutInflater.from(context), parent, false);

        progressDialog = new ProgressDialog(context);

        return new HolderPdf(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderPdf holder, int position) {

        //mendapatkan data
        ModelPdf model = pdfArrayList.get(position);
        String bookId = model.getId();
        String title = model.getTitle();
        String description = model.getDescription();
        String pdfUrl = model.getUrl();
        String categoryId = model.getCategoryId();
        String favorite = model.getFavorite();
        String readers = model.getReaders();
        String shared = model.getShared();
        String downloads = model.getDownloads();
        String pageBookmark = model.getPage();
        String identityBookmark = model.getIdentity();

        long timestamp = model.getTimestamp();


        //timestampe menggunakan dd/MM/yyyy format
        String formattedDate = MyApplication.formatTimestamp(Long.parseLong(bookId));

        //set data
        holder.titleTv.setText(title);
        holder.descriptionTv.setText(description);
        holder.dateTv.setText(formattedDate);

        //meload info detail seperti category, url, ukuran pdf di fungsi yang terpisah
        loadCategory(model, holder);
        loadPdfFromUrl(model, holder);
        loadPdfSize(model, holder);

        //menset visibility delete btn tergantung dari tipe user
        if (LoginActivity.user == 1){
            binding.moreBtn.setVisibility(View.GONE);
        }
        else if (LoginActivity.user == 2){
            binding.moreBtn.setVisibility(View.VISIBLE);
        }

        holder.moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moreOptionsDialog(model, holder);
            }
        });

        //item klik ke pdfDetailActivity juga memberikan pdf title, description dan pdfUrl
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if ("bookmarks".equals(identityBookmark)){
                    Intent intent = new Intent(context, BookViewActivity.class);
                    intent.putExtra("bookId", bookId);
                    intent.putExtra("title", title);
                    intent.putExtra("pdfUrl", pdfUrl);
                    intent.putExtra("favorite", favorite);
                    intent.putExtra("page", pageBookmark);
                    intent.putExtra("identity", identityBookmark);
                    context.startActivity(intent);
                }
                else {
                    Intent intent = new Intent(context, PdfDetailActivity.class);
                    intent.putExtra("bookId", bookId);
                    intent.putExtra("title", title);
                    intent.putExtra("description", description);
                    intent.putExtra("pdfUrl", pdfUrl);
                    intent.putExtra("categoryId", categoryId);
                    intent.putExtra("favorite", favorite);
                    intent.putExtra("readers", readers);
                    intent.putExtra("shared", shared);
                    intent.putExtra("downloads", downloads);
                    context.startActivity(intent);
                }

            }
        });
    }

    private void moreOptionsDialog(ModelPdf model, HolderPdf holder) {
        String bookId = model.getId();
        String bookUrl = model.getUrl();
        String bookTittle = model.getTitle();

        //options muncul di dialog
        String[] options = {"Edit", "Delete"};

        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose Options")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //klik dialog option
                        if(i == 0){
                            //Klik edit
                            Intent intent = new Intent(context, PdfEditActivity.class);
                            intent.putExtra("bookId", bookId);
                            context.startActivity(intent);
                        }
                        else if (i == 1){
                            //klik delete
                            deleteBook(model, holder);
                        }
                    }
                })
                .show();
    }

    private void deleteBook(ModelPdf model, HolderPdf holder) {
        String bookId = model.getId();
        String bookUrl = model.getUrl();
        String bookTitle = model.getTitle();

        Log.d(TAG, "deleteBook: Menghapus...");
        progressDialog.setMessage("Menghapus Buku");
        progressDialog.show();

        Log.d(TAG, "deleteBook: Menghapus dari penyimpanan...");
        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl);
        storageReference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Menghapus dari penyimpanan");

                        Log.d(TAG, "onSuccess: Menghapus info dari db");
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Books");
                        DatabaseReference refBookmark = FirebaseDatabase.getInstance().getReference("Bookmarks");
                        DatabaseReference refFav = FirebaseDatabase.getInstance().getReference("Favorites");
                        DatabaseReference refHistory= FirebaseDatabase.getInstance().getReference("Histories");
                        DatabaseReference refReads = FirebaseDatabase.getInstance().getReference("Reads");

                        reference.child(bookId)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "onSuccess: Buku Terhapus dari db");

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "onFailure: Gagal terhapus dari db karena "+e.getMessage());
                                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                    }
                                });

                        refBookmark.child(bookId)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "onSuccess: Bookmark buku terhapus dari db");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "onFailure: Gagal terhapus dari db karena "+e.getMessage());
                                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                    }
                                });

                        refFav.child(bookId)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "onSuccess: Favorite buku terhapus dari db");

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "onFailure: Gagal terhapus dari db karena "+e.getMessage());
                                        progressDialog.dismiss();
                                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                    }
                                });

                        refHistory.child(bookId)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "onSuccess: History buku terhapus dari db");

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "onFailure: Gagal terhapus dari db karena "+e.getMessage());
                                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                    }
                                });

                        refReads.child(bookId)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "onSuccess: Reads buku Terhapus dari db");
                                        progressDialog.dismiss();
                                        Toast.makeText(context, "Buku berhasil dihapus", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "onFailure: Gagal terhapus dari db karena "+e.getMessage());
                                        progressDialog.dismiss();
                                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: Gagal menghapus dari penyimpanan karena "+e.getMessage());
                        progressDialog.dismiss();
                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadPdfSize(ModelPdf model, HolderPdf holder) {
        //menggunakan url kita dapat mendapatkan file dan metadatanya dari firebase storage

        String pdfUrl = model.getUrl();

        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        ref.getMetadata()
                .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        //mendapatkan size dalam bytes
                        double bytes = storageMetadata.getSizeBytes();
                        Log.d(TAG, "onSuccess: "+model.getTitle() +" "+bytes);

                        //mengubah bytes ke KB, MB
                        double kb = bytes/1024;
                        double mb = kb/1024;

                        if (mb >= 1 ){
                            holder.sizeTv.setText(String.format("%.2f", mb)+" MB");
                        }
                        else if(kb >= 1){
                            holder.sizeTv.setText(String.format("%.2f", kb)+" KB");
                        }
                        else {
                            holder.sizeTv.setText(String.format("%.2f", bytes)+" bytes");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: "+e.getMessage());
                    }
                });
    }

    private void loadPdfFromUrl(ModelPdf model, HolderPdf holder) {
        //menggunakan url kita dapat mendapatkan file dan metadatanya dari firebase storage
        String url = model.getUrl();

        FileLoader.with(context)
            .load(url, false)
            .fromDirectory("test4", FileLoader.DIR_INTERNAL)
            .asFile(new FileRequestListener<File>() {
                @Override
                public void onLoad(FileLoadRequest request, FileResponse<File> response) {
                    holder.progressBar.setVisibility(View.INVISIBLE);
                    pdfUrl1 = response.getBody();

                    try {
                        holder.pdfView.fromFile(pdfUrl1)
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
                    holder.progressBar.setVisibility(View.INVISIBLE);
                    Log.d(TAG, "onPageError: "+t.getMessage());
                }
            });
    }

    private void loadCategory(ModelPdf model, HolderPdf holder) {
        //mendapatkan kategorri menggunakan categoryId

        String categoryId = model.getCategoryId();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Categories");
        ref.child(categoryId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //mendapatkan category
                        String category = ""+snapshot.child("category").getValue();

                        //menset ke category text view
                        holder.categoryTv.setText(category);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    @Override
    public int getItemCount() {
        return pdfArrayList.size(); //mereturn jumlah dari data
    }

    @Override
    public Filter getFilter() {
        if (filter == null){
            filter = new FilterPdf(filterList, this);
        }
        return filter;
    }

    //view holder class untuk row_pdf.xml
    class HolderPdf extends RecyclerView.ViewHolder{

        PDFView pdfView;
        ProgressBar progressBar;
        TextView titleTv, descriptionTv, categoryTv, sizeTv, dateTv;
        ImageButton moreBtn;

        public HolderPdf(@NonNull View itemView) {
            super(itemView);

            pdfView = binding.pdfView;
            progressBar = binding.progressBar;
            titleTv = binding.titleTv;
            descriptionTv = binding.descriptionTv;
            categoryTv = binding.categoryTv;
            sizeTv = binding.sizeTv;
            dateTv = binding.dateTv;
            moreBtn = binding.moreBtn;

        }
    }
}
