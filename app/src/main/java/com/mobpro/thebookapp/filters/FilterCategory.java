package com.mobpro.thebookapp.filters;

import android.widget.Filter;

import com.mobpro.thebookapp.adapters.AdapterCategory;
import com.mobpro.thebookapp.models.ModelCategory;

import java.util.ArrayList;

public class FilterCategory extends Filter {

    //arraylist untuk meng-search
    ArrayList<ModelCategory> filterList;
    //adapter dimana untuk filter diaplikasikan
    AdapterCategory adapterCategory;

    //constructor


    public FilterCategory(ArrayList<ModelCategory> filterList, AdapterCategory adapterCategory) {
        this.filterList = filterList;
        this.adapterCategory = adapterCategory;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        //nilai tidak boleh kosong
        if(constraint !=null && constraint.length() > 0){

            //mengubah ke uppercase untuk menghindari case sensitivity
            constraint = constraint.toString().toUpperCase();
            ArrayList<ModelCategory> filteredModels = new ArrayList<>();

            for (int i=0; i<filterList.size(); i++){
                //validasi
                if(filterList.get(i).getCategory().toUpperCase().contains(constraint)){
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
        adapterCategory.categoryArrayList = (ArrayList<ModelCategory>)results.values;

        //menotifikasi perubahan
        adapterCategory.notifyDataSetChanged();
    }


}
