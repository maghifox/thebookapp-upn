package com.mobpro.thebookapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.etebarian.meowbottomnavigation.MeowBottomNavigation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mobpro.thebookapp.databinding.ActivityDashboardUserBinding;
import com.mobpro.thebookapp.fragments.FragmentBookmark;
import com.mobpro.thebookapp.fragments.FragmentProfile;
import com.mobpro.thebookapp.fragments.FragmentFavorite;
import com.mobpro.thebookapp.fragments.FragmentHistory;
import com.mobpro.thebookapp.fragments.FragmentHome;

public class DashboardUserActivity extends AppCompatActivity {

    private ActivityDashboardUserBinding binding;

    private FirebaseAuth firebaseAuth;

    MeowBottomNavigation bottomNavigation;

    Fragment selectedFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        checkUser();

        bottomNavigation = binding.bottomnavMbn;

        MeowBottomNavigation.ReselectListener reselectListener = new MeowBottomNavigation.ReselectListener() {
            @Override
            public void onReselectItem(MeowBottomNavigation.Model item) {

            }
        };

        bottomNavigation.setOnReselectListener(reselectListener);

        bottomNavigation.add(new MeowBottomNavigation.Model(1, R.drawable.ic_home));
        bottomNavigation.add(new MeowBottomNavigation.Model(2, R.drawable.ic_bookmark));
        bottomNavigation.add(new MeowBottomNavigation.Model(3, R.drawable.ic_history));
        bottomNavigation.add(new MeowBottomNavigation.Model(4, R.drawable.ic_favorite));
        bottomNavigation.add(new MeowBottomNavigation.Model(5, R.drawable.ic_person_gray));

        if (selectedFragment == null) {
            selectedFragment = new FragmentHome();
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, selectedFragment).commit();

        bottomNavigation.setOnClickMenuListener(new MeowBottomNavigation.ClickListener() {
            @Override
            public void onClickItem(MeowBottomNavigation.Model item) {
                String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                binding.subTitleTv.setText(email);
                if (item.getId() == 1) {
                    selectedFragment = new FragmentHome();
                    binding.titleTv.setText("Dashboard Mahasiswa");
                } else if (item.getId() == 2) {
                    selectedFragment = new FragmentBookmark();
                    binding.titleTv.setText("Bookmarks");
                } else if (item.getId() == 3) {
                    selectedFragment = new FragmentHistory();
                    binding.titleTv.setText("Recent List");
                } else if (item.getId() == 4) {
                    selectedFragment = new FragmentFavorite();
                    binding.titleTv.setText("Favorites");
                } else if (item.getId() == 5) {
                    selectedFragment = new FragmentProfile();
                    binding.titleTv.setText("Profile");
                } else {
                    selectedFragment = new FragmentHome();
                    binding.titleTv.setText("Dashboard Mahasiswa");
                }

                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, selectedFragment).commit();
            }
        });

        bottomNavigation.setOnShowListener(new MeowBottomNavigation.ShowListener() {
            @Override
            public void onShowItem(MeowBottomNavigation.Model item) {

            }
        });

        //klik logout btn
        binding.logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseAuth.signOut();
                checkUser();
            }
        });
    }

    private void checkUser() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if(firebaseUser==null){
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        else{
            //telah login, mengambil data user
            String email = firebaseUser.getEmail();
            //menset email di toolbar
            binding.subTitleTv.setText(email);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

}