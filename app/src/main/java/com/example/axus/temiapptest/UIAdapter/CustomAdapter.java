package com.example.axus.temiapptest.UIAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.axus.temiapptest.R;

import java.util.List;

public class CustomAdapter extends ArrayAdapter<String> {

    private List<String> savedLocations;

    public List<String> getSavedLocations() {
        return savedLocations;
    }

    public void setSavedLocations(List<String> savedLocations) {
        this.savedLocations = savedLocations;
    }



    public CustomAdapter(Context context, int textViewResourceId, List<String> savedLocations) {
        super(context, textViewResourceId, savedLocations);
        this.savedLocations = savedLocations;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.location_row, null);
        }

        String loc = savedLocations.get(position);
        if (loc != null) {
            TextView tvName = (TextView) convertView.findViewById(R.id.name);
            if (tvName != null) {
                tvName.setText(loc);
            }
        }
        return convertView;
    }
}
