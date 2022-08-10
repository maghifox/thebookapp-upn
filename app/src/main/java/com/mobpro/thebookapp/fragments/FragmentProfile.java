package com.mobpro.thebookapp.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mobpro.thebookapp.databinding.DialogChangePasswordBinding;
import com.mobpro.thebookapp.databinding.FragmentProfileBinding;
import com.mobpro.thebookapp.dialog.ConfirmPasswordDialog;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import static android.app.Activity.RESULT_OK;

public class FragmentProfile extends Fragment implements ConfirmPasswordDialog.OnConfirmPasswordListener{

    FragmentProfileBinding binding;

    DialogChangePasswordBinding dialog;

    private String uid, photoUrl;

    private Uri photoUri = null;

    private ProgressDialog progressDialog;

    private static final String TAG = "PROFILE_TAG";



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Mohon Tunggu");
        progressDialog.setCanceledOnTouchOutside(false);

        setDataUser();

        binding.photoIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //memilih image
                ImagePicker.with(getActivity())
                        .cropSquare()
                        .compress(256)
                        .maxResultSize(256,256)
                        .galleryOnly()
                        .start();

            }
        });



        binding.updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //validasi data
                validateData();
            }
        });

        binding.changePasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changePass();
            }
        });

        return binding.getRoot();
    }

    private void changePass() {
        dialog = DialogChangePasswordBinding.inflate(getLayoutInflater());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialog.getRoot());

        AlertDialog dialog1 = builder.create();
        dialog1.show();

        dialog.updatePasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //validasi data
                //mendapatkan data
                String oldPassword = dialog.passwordEt.getText().toString().trim();
                String newPassword = dialog.passwordConfirmEt.getText().toString().trim();

                //validasi data
                if (TextUtils.isEmpty(oldPassword)){
                    Toast.makeText(getContext(), "Masukan Password Lama mu..", Toast.LENGTH_SHORT).show();
                }
                else if (newPassword.length()<6){
                    Toast.makeText(getContext(), "Password minimal 6 huruf...", Toast.LENGTH_SHORT).show();
                }
                else{
                    dialog1.dismiss();
                    updatePassword(oldPassword, newPassword);
                }

            }
        });
    }

    private void updatePassword(String oldPassword, String newPassword) {
        progressDialog.show();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        //sebelum mengubah password re-authentication terlebih dahulu
        AuthCredential authCredential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
        user.reauthenticate(authCredential)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        user.updatePassword(newPassword)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        progressDialog.dismiss();
                                        Toast.makeText(getContext(), "Password Berhasil Diubah...", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull @NotNull Exception e) {
                                        progressDialog.dismiss();
                                        Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadPhoto() {
        //langkah 2: uploud photo ke firebase storage
        Log.d(TAG, "uploadPhotoToStorage: mengupload ke storage...");

        progressDialog.setMessage("Mengupload Photo...");
        progressDialog.show();

        long timestamp = System.currentTimeMillis();

        //path dari pdf di firebase storage
        String filePathAndName = "Photo/" + uid;

        StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
        storageReference.putFile(photoUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG, "onSuccess: Photo telah terupload ke storage");
                        Log.d(TAG, "onSuccess: mendapatkan url photo");

                        //mendapatkan photo url
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        photoUrl = ""+uriTask.getResult();

                        //upload ke firebase db
                        uploadPhotoUrl(photoUrl);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onFailure: Photo upload gagal karena"+e.getMessage());
                        Toast.makeText(getContext(), "Photo upload gagal karena"+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

    }

    private void uploadPhotoUrl(String photoUrl) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("profileImage");
        ref.setValue(photoUrl)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        binding.photoIv.setImageURI(photoUri);
                        progressDialog.dismiss();
                        Log.d(TAG, "onSuccess: Berhasil diupload...");
                        Toast.makeText(getContext(), "Berhasil diupload...", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onFailure: Gagal mengupload ke db karena "+e.getMessage());
                        Toast.makeText(getContext(), "Gagal mengupload ke db karena "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private String name = "", email = "";

    private void validateData() {
        //langkah 1 :validasi data
        Log.d(TAG, "validateData: memvalidasi data...");

        //mendapatkan data
        name = binding.nameEt.getText().toString().trim();
        email = binding.emailEt.getText().toString().trim();

        //validasi data
        if (TextUtils.isEmpty(name)){
            Toast.makeText(getContext(), "Masukan Nama...", Toast.LENGTH_SHORT).show();
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(getContext(), "Masukan email dengan benar...!", Toast.LENGTH_SHORT).show();
        }
        else{
            //semua data sudah valid, bisa ubah sekarang

            //ubah email
            ConfirmPasswordDialog dialog = new ConfirmPasswordDialog();
            dialog.show(getFragmentManager(), "ConfirmPasswordDialog");
            dialog.setTargetFragment(FragmentProfile.this, 1);

        }
    }

    private void changeUserData() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                ref.child("name").setValue(binding.nameEt.getText().toString().trim());
                ref.child("email").setValue(FirebaseAuth.getInstance().getCurrentUser().getEmail());
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

    }

    private void setDataUser() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue().toString();
                String emailFb = snapshot.child("email").getValue().toString();
                String userType = snapshot.child("userType").getValue().toString();
                String photo = snapshot.child("profileImage").getValue().toString();

                binding.nameEt.setText(name);
                binding.emailEt.setText(emailFb);
                binding.statusEt.setText(userType);
                binding.emailTv.setText(emailFb);

                if(!photo.equals("")){
                    Picasso.get().load(photo).into(binding.photoIv);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            //Image Uri will not be null for RESULT_OK
            photoUri = data.getData();

            uploadPhoto();

        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(getContext(), ImagePicker.getError(data), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Pengambilan dibatalkan", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConfirmPassword(String password) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        progressDialog.show();

        //re-authenticate
        AuthCredential credential = EmailAuthProvider
                .getCredential(FirebaseAuth.getInstance().getCurrentUser().getEmail(), password);

        // memasukan kembali email dan password
        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "User re-authenticated.");

                            //cek untuk melihat apakah email baru ini belum digunakan di database
                            FirebaseAuth.getInstance().fetchSignInMethodsForEmail(binding.emailEt.getText().toString().trim())
                                    .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                                        @Override
                                        public void onComplete(@NonNull @NotNull Task<SignInMethodQueryResult> task) {
                                            try {
                                                if (task.isSuccessful()){
                                                    if(task.getResult().getSignInMethods().size() == 1){
                                                        Log.d(TAG, "onComplete: Emailnya sudah digunakan...");
                                                        changeUserData();
                                                        progressDialog.dismiss();
                                                        if (!FirebaseAuth.getInstance().getCurrentUser().getEmail().equals(binding.emailEt.getText().toString().trim())){
                                                            Toast.makeText(getContext(), "Email Sudah digunakan User lain ", Toast.LENGTH_SHORT).show();
                                                        }
                                                        Toast.makeText(getContext(), "Berhasil update nama ", Toast.LENGTH_SHORT).show();
                                                    }
                                                    else {
                                                        Log.d(TAG, "onComplete: Emailnya dapat digunakan");

                                                        //email dapat digunakan
                                                        user.updateEmail(binding.emailEt.getText().toString().trim())
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if (task.isSuccessful()) {
                                                                            Log.d(TAG, "User email address updated.");
                                                                            changeUserData();
                                                                            progressDialog.dismiss();
                                                                            Toast.makeText(getContext(), "Profil sudah di update", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    }
                                                                });
                                                    }
                                                }
                                            }
                                            catch (NullPointerException e){
                                                Log.e(TAG, "onComplete: NullPointerException"+ e.getMessage());
                                                progressDialog.dismiss();
                                            }

                                        }
                                    });
                        }
                        else {
                            Log.d(TAG, "onComplete: Gagal re-authenticated");
                            progressDialog.dismiss();
                        }

                    }
                });

    }
}