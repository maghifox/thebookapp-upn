package com.mobpro.thebookapp.dialog;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.mobpro.thebookapp.databinding.DialogConfirmPasswordBinding;

import org.jetbrains.annotations.NotNull;

public class ConfirmPasswordDialog extends DialogFragment {

    DialogConfirmPasswordBinding binding;

    private static final String TAG = "ConfirmPasswordDialog";

    public interface OnConfirmPasswordListener{
        public void onConfirmPassword(String password);
    }
    OnConfirmPasswordListener onConfirmPasswordListener;

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        binding = DialogConfirmPasswordBinding.inflate(inflater, container, false);
        Log.d(TAG, "onCreateView: Dimulai");
        
        binding.konfirmTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Password sudah diisi dan dikonfirm");

                String password = binding.passwordEt.getText().toString().trim();
                if (!password.equals("")){
                    onConfirmPasswordListener.onConfirmPassword(password);
                    getDialog().dismiss();
                }
                else {
                    Toast.makeText(getContext(), "Password tidak boleh kosong...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.batalTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().dismiss();
                Toast.makeText(getContext(), "Update Profil Dibatalkan...", Toast.LENGTH_SHORT).show();
            }
        });



        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        try {
            onConfirmPasswordListener = (OnConfirmPasswordListener) getTargetFragment();
        }
        catch (ClassCastException e){
            Log.d(TAG, "onAttach: ClassCastException: " + e.getMessage());
        }
    }
}
