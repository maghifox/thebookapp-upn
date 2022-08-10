package com.mobpro.thebookapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.listener.OnPageScrollListener;
import com.github.barteksc.pdfviewer.listener.OnTapListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.krishna.fileloader.listener.FileRequestListener;
import com.krishna.fileloader.pojo.FileResponse;
import com.krishna.fileloader.request.FileLoadRequest;
import com.mobpro.thebookapp.databinding.ActivityBookViewBinding;
import com.krishna.fileloader.FileLoader;
import com.mobpro.thebookapp.models.ModelPdf;


import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class BookViewActivity extends AppCompatActivity implements OnLoadCompleteListener, OnPageChangeListener, OnPageErrorListener {

    private ActivityBookViewBinding binding;

    PDFView pdfView;

    private static final int WRITE_EXT_STORAGECODE = 1;

    private ProgressDialog progressDialog;

    private String url, bookId, title, uid, favorite;

    OnTapListener onTapListener;

    int currentPage = 0;
    String pageNo = "";
    int pageNoRecreate = 0;

    //inisialisi dari file path di penyimpanan
    File pdfUrl;

    private boolean tapped = false;

    boolean nightMode;

    String identity = "";
    String identityBookmark ="";

    Menu menu;
    BottomNavigationView bottomNav;

    MediaPlayer mp;

    boolean favoriteBool=false;
    boolean bookmarkBool=false;
    int favorites;

    private static final String TAG = "PDF_VIEW_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Mohon tunggu...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        Intent i = getIntent();
        url = i.getStringExtra("pdfUrl");
        bookId = i.getStringExtra("bookId");
        title = i.getStringExtra("title");
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        favorite = i.getStringExtra("favorite");
        pageNo = i.getStringExtra("page");
        identityBookmark = i.getStringExtra("identity");

        favorites = Integer.parseInt(favorite);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("bookmarks", Context.MODE_PRIVATE);
        nightMode = sharedPreferences.getBoolean("nightMode", false);
        identity = sharedPreferences.getString("identity", "");
        pageNoRecreate = sharedPreferences.getInt("page", -1) - 1;


        if (pageNoRecreate >= 0) {
            currentPage = pageNoRecreate;
        }

        if (pageNo != null) {
            currentPage = Integer.parseInt(pageNo) - 1;
        }

        if (identityBookmark != null){
            identity = "bookmarks";
        }

        bottomNav = binding.bottomNav;
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        onTapListener = new OnTapListener() {
            @Override
            public boolean onTap(MotionEvent e) {
                if (tapped) {
                    binding.bottomNav.setVisibility(View.INVISIBLE);
                    tapped = false;
                } else {
                    binding.bottomNav.setVisibility(View.VISIBLE);
                    tapped = true;
                }
                return true;
            }
        };

        loadPdf();

        menu = binding.bottomNav.getMenu();

        if (!nightMode) {
            menu.findItem(R.id.night_mode).setIcon(R.drawable.ic_night);
        } else {
            menu.findItem(R.id.night_mode).setIcon(R.drawable.ic_night_filled);

        }

        setFavorite();
        setBookmark();
    }

    private void setBookmark() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookmarks");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(bookId).hasChild(uid)) {
                    if (snapshot.child(bookId).child(uid).getValue(String.class).equals(String.valueOf(currentPage))) {
                        //sudah bookmark
                        menu.findItem(R.id.bookmark).setIcon(R.drawable.ic_bookmark_filled);
                    }
                    else{
                        //belum bookmark
                        menu.findItem(R.id.bookmark).setIcon(R.drawable.ic_bookmark_outline);
                    }
                } else {
                    //belum bookmark
                    menu.findItem(R.id.bookmark).setIcon(R.drawable.ic_bookmark_outline);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadPdf() {
        FileLoader.with(this)
            .load(url, false)
            .fromDirectory("test4", FileLoader.DIR_INTERNAL)
            .asFile(new FileRequestListener<File>() {
                @Override
                public void onLoad(FileLoadRequest request, FileResponse<File> response) {
                    pdfUrl = response.getBody();

                    if ("bookmarks".equals(identity) || "itself".equals(identity)) {
                        try {
                            binding.pdfView.fromFile(pdfUrl)
                                .onTap(onTapListener)
                                .onPageScroll(new OnPageScrollListener() {
                                    @Override
                                    public void onPageScrolled(int page, float positionOffset) {
                                        bottomNav.setVisibility(View.VISIBLE);

                                        final Handler handler = new Handler();
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                bottomNav.setVisibility(View.INVISIBLE);
                                            }
                                        }, 1500);
                                    }
                                })
                                .defaultPage(currentPage)
                                .enableSwipe(true)
                                .enableAnnotationRendering(true)
                                .onLoad(BookViewActivity.this)
                                .onPageChange(BookViewActivity.this)
                                .scrollHandle(new DefaultScrollHandle(BookViewActivity.this, true))
                                .enableDoubletap(true)
                                .onPageError(BookViewActivity.this)
                                .swipeHorizontal(true)
                                .pageFitPolicy(FitPolicy.WIDTH) // mode to fit pages in the view
                                .pageSnap(true)
                                .pageFling(true)
                                .autoSpacing(true)
                                .spacing(0)
                                .nightMode(nightMode)
                                .load();

                            SharedPreferences sharedPreferences = getSharedPreferences("bookmarks", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("identity", "");
                            editor.putInt("page", 0);
                            editor.commit();
                            Log.d(TAG, "FilePath: " + pdfUrl);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        try {
                            binding.pdfView.fromFile(pdfUrl)
                                .onTap(onTapListener)
                                .onPageScroll(new OnPageScrollListener() {
                                    @Override
                                    public void onPageScrolled(int page, float positionOffset) {
                                        binding.bottomNav.setVisibility(View.VISIBLE);

                                        final Handler handler = new Handler();
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                binding.bottomNav.setVisibility(View.INVISIBLE);
                                            }
                                        }, 1500);
                                    }
                                })
                                .defaultPage(0)
                                .enableSwipe(true)
                                .enableAnnotationRendering(true)
                                .onLoad(BookViewActivity.this)
                                .onPageChange(BookViewActivity.this)
                                .scrollHandle(new DefaultScrollHandle(BookViewActivity.this, true))
                                .enableDoubletap(true)
                                .onPageError(BookViewActivity.this)
                                .swipeHorizontal(true)
                                .pageFitPolicy(FitPolicy.BOTH) // mode to fit pages in the view
                                .pageSnap(true)
                                .pageFling(true)
                                .autoSpacing(true)
                                .spacing(0)
                                .nightMode(nightMode)
                                .load();

                            Log.d(TAG, "FilePath: " + pdfUrl);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onError(FileLoadRequest request, Throwable t) {
                    progressDialog.dismiss();
                    Toast.makeText(BookViewActivity.this, "" + t.getMessage() + ", File Error", Toast.LENGTH_SHORT).show();

                }
            });
    }

    private void setFavorite() {

        DatabaseReference refFavorite = FirebaseDatabase.getInstance().getReference("Favorites");
        refFavorite.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(bookId).hasChild(uid)){
                    //user telah memfavoritkan buku
                    menu.findItem(R.id.favourite).setIcon(R.drawable.ic_favorite);
                }
                else {
                    //user blum memfavoritkan buku
                    menu.findItem(R.id.favourite).setIcon(R.drawable.ic_favourite_border);
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @Override
    public void loadComplete(int nbPages) {
        progressDialog.dismiss();
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        currentPage = page + 1;
        setBookmark();
    }

    @Override
    public void onPageError(int page, Throwable t) {

    }

    public void screenShot() {
        Bitmap b = Bitmap.createBitmap(binding.pdfView.getWidth(), binding.pdfView.getHeight(), Bitmap.Config.ARGB_8888);

        int weight, height;
        weight = binding.pdfView.getWidth();
        height = binding.pdfView.getHeight();
        Bitmap cs = Bitmap.createBitmap(weight, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(cs);
        c.drawBitmap(b, 0, 0, null);
        binding.pdfView.draw(c);
        c.setBitmap(cs);

        long timestamp = System.currentTimeMillis();

        String time = String.valueOf(timestamp);
        File path = Environment.getExternalStorageDirectory();
        File dir = new File(path + "/DCIM/ThEBOOK");
        dir.mkdir();
        String imagename = time + ".JPEG";
        File file = new File(dir, imagename);
        OutputStream out;
        try {
            out = new FileOutputStream(file);
            cs.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            mp = MediaPlayer.create(this, R.raw.camerashutter);
            mp.start();
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.stop();
                }
            });
            //for gallery to be notified of the Image
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()},
                    null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {

                        }
                    });
            Toast.makeText(BookViewActivity.this, "Screenshot Captured", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_DENIED) {
                    String[] permission = {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    };
                    requestPermissions(permission, WRITE_EXT_STORAGECODE);
                }
            }
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            int id = item.getItemId();

            switch (id) {

                case R.id.night_mode:
                    SharedPreferences sharedPreferences = getSharedPreferences("bookmarks", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    if (nightMode != true) {
                        editor.putBoolean("nightMode", true);
                        editor.putString("identity", "itself");
                        editor.putInt("page", currentPage);
                        editor.commit();
                        item.setIcon(R.drawable.ic_night);
                        recreate();

                        Toast.makeText(BookViewActivity.this, "Night Mode telah aktif", Toast.LENGTH_SHORT).show();
                    } else {
                        editor.putBoolean("nightMode", false);
                        editor.putString("identity", "itself");
                        editor.putInt("page", currentPage);
                        editor.commit();
                        item.setIcon(R.drawable.ic_night_filled);
                        recreate();

                        Toast.makeText(BookViewActivity.this, "Night Mode telah dimatikan", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case R.id.screenshot:
                    screenShot();
                    break;

                case R.id.bookmark:
                    bookmarkBool = true;
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookmarks");

                    ref.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (bookmarkBool) {
                                if (snapshot.child(bookId).hasChild(uid)) {
                                    if (snapshot.child(bookId).child(uid).getValue(String.class).equals(String.valueOf(currentPage))) {
                                        //sudah bookmark dan menghapus bookmark
                                        ref.child(bookId).child(uid).removeValue();
                                        menu.findItem(R.id.bookmark).setIcon(R.drawable.ic_bookmark_outline);
                                        bookmarkBool = false;
                                        Toast.makeText(BookViewActivity.this, "Menghapus bookmark", Toast.LENGTH_SHORT).show();
                                    }
                                    else{
                                        //belum bookmark dan menambahkan bookmark
                                        ref.child(bookId).child(uid).setValue(String.valueOf(currentPage));
                                        menu.findItem(R.id.bookmark).setIcon(R.drawable.ic_bookmark_filled);
                                        bookmarkBool = false;
                                        Toast.makeText(BookViewActivity.this, "Sudah dibookmark", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    //belum bookmark dan menambahkan bookmark
                                    ref.child(bookId).child(uid).setValue(String.valueOf(currentPage));
                                    menu.findItem(R.id.bookmark).setIcon(R.drawable.ic_bookmark_filled);
                                    bookmarkBool = false;
                                    Toast.makeText(BookViewActivity.this, "Sudah dibookmark", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                    break;

                case R.id.favourite:
                    favoriteBool = true;
                    DatabaseReference refFavorite = FirebaseDatabase.getInstance().getReference("Favorites");
                    DatabaseReference refBooks = FirebaseDatabase.getInstance().getReference("Books");
                    refFavorite.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (favoriteBool){
                                if (snapshot.child(bookId).hasChild(uid)){
                                    //sudah difavoritkan jadi menghapus favorite jika diklik
                                    favorites = favorites - 1;
                                    refBooks.child(bookId).child("favorite").setValue(""+favorites);
                                    refFavorite.child(bookId).child(uid).removeValue();
                                    favoriteBool = false;
                                }
                                else{
                                    //belum difavoritkan jadi favorite jika diklik
                                    favorites = favorites + 1;
                                    refBooks.child(bookId).child("favorite").setValue(""+favorites);
                                    refFavorite.child(bookId).child(uid).setValue("true");
                                    favoriteBool = false;
                                }
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                    break;

                case R.id.share:
                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    String shareBody = "Hi, aku sedang membaca buku " + title + ". Jika kamu ingin membacanya silahkan download aplikasi ThEBOOK.";
                    String shareSubject = "Shareable link";

                    sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareSubject);


                    final DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Books");
                    final Query query = reference.orderByChild("id").equalTo(bookId);
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                                ModelPdf model = snapshot1.getValue(ModelPdf.class);
                                String share = model.getShared();
                                int integerReader = (Integer.parseInt(share)) + 1;
                                model.setShared(String.valueOf(integerReader));
                                reference.child(bookId).setValue(model);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                    startActivity(Intent.createChooser(sharingIntent, "Membagikan menggunakan"));
                    break;

                default:
                    Toast.makeText(BookViewActivity.this, "Nav_Home", Toast.LENGTH_SHORT).show();
                    break;
            }

            return true;
        }
    };
}