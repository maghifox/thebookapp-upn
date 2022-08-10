package com.mobpro.thebookapp.filters;

import android.widget.Filter;

import com.mobpro.thebookapp.adapters.AdapterPdf;
import com.mobpro.thebookapp.models.ModelPdf;

import java.util.ArrayList;

public class FilterPdf extends Filter {

    //arraylist untuk meng-search
    ArrayList<ModelPdf> filterList;
    //adapter dimana untuk filter diaplikasikan
    AdapterPdf adapterPdf;

    //constructor


    public FilterPdf(ArrayList<ModelPdf> filterList, AdapterPdf adapterPdf) {
        this.filterList = filterList;
        this.adapterPdf = adapterPdf;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        //nilai tidak boleh kosong
        if(constraint !=null && constraint.length() > 0){

            //mengubah ke uppercase untuk menghindari case sensitivity
            constraint = constraint.toString().toUpperCase();
            ArrayList<ModelPdf> filteredModels = new ArrayList<>();

            for (int i=0; i<filterList.size(); i++){
                //validasi
                if(filterList.get(i).getTitle().toUpperCase().contains(constraint)){
                    //menambahkan ke filtered list
                    filteredModels.add(filterList.get(i));
                }
            }

            results.count = filteredModels.size();
            results.values = filteredModels;
        }
        else {
            results.count = filterList.size();
            results.values = filterList;
        }

        return results;
    }

    @Override
    protected void publishResults(CharSequence charSequence, FilterResults results) {
        //mengaplikasikan filter
        adapterPdf.pdfArrayList = (ArrayList<ModelPdf>)results.values;

        //menotifikasi perubahan
        adapterPdf.notifyDataSetChanged();
    }


}
