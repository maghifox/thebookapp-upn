package com.mobpro.thebookapp;

import android.app.Application;
import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;

//application class berjalan sebelum launcher activity
public class MyApplication extends Application {

    @Override
    public void onCreate(){
        super.onCreate();
    }

    //membuat static method untuk mengubah timestamp ke date format yg sesuai jadi kita dapat menggunakan ini dimana saja di project ini tanpa menulis lagi
    public static final String formatTimestamp(long timestamp){
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timestamp);
        //format timestamp ke dd/mm/yyyy
        String date = DateFormat.format("dd/MM/yyyy", cal).toString();

        return date;
    }

}
