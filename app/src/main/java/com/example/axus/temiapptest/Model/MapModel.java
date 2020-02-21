package com.example.axus.temiapptest.Model;

import java.util.HashMap;

public class MapModel {
    private int id;
    private String map_name;
    private String map_original;
    private String map_modify;
    private String map_data_url;
    private String resolution;
    private String bmp_x;
    private String bmp_y;
    private String bmp_w;
    private String bmp_h;
    private String display_w;
    private String display_h;
    private String rail_mode;
    private String rail_mode_in_use;
    private boolean cruiser_random = true;
    private HashMap<String, MapModel.Status> status;
    public static final String KEY_TABLE = "mapmodel";
    public static final String KEY_ID = "id";
    public static final String KEY_MAP_NAME = "map_name";
    public static final String KEY_MAP_ORIGINAL = "map_original";
    public static final String KEY_MAP_MODIFY = "map_modify";
    public static final String KEY_MAP_DATA_URL = "map_data_url";
    public static final String KEY_RESOLUTION = "resolution";
    public static final String KEY_BMP_X = "bmp_x";
    public static final String KEY_BMP_Y = "bmp_y";
    public static final String KEY_BMP_W = "bmp_w";
    public static final String KEY_BMP_H = "bmp_h";
    public static final String KEY_DISPLAY_W = "display_w";
    public static final String KEY_DISPLAY_H = "display_h";
    public static final String KEY_RAIL_MODE = "rail_mode";
    public static final String KEY_RAIL_MODE_IN_USE = "rail_mode_in_use";
    public static final String KEY_CRUISER_RANDOM = "cruiser_random";

    public MapModel() {
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMap_name() {
        return this.map_name;
    }

    public void setMap_name(String map_name) {
        this.map_name = map_name;
    }

    public String getMap_original() {
        return this.map_original;
    }

    public void setMap_original(String map_original) {
        this.map_original = map_original;
    }

    public String getMap_modify() {
        return this.map_modify;
    }

    public void setMap_modify(String map_modify) {
        this.map_modify = map_modify;
    }

    public String getMap_data_url() {
        return this.map_data_url;
    }

    public void setMap_data_url(String map_data_url) {
        this.map_data_url = map_data_url;
    }

    public String getResolution() {
        return this.resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getBmp_x() {
        return this.bmp_x;
    }

    public void setBmp_x(String bmp_x) {
        this.bmp_x = bmp_x;
    }

    public String getBmp_y() {
        return this.bmp_y;
    }

    public void setBmp_y(String bmp_y) {
        this.bmp_y = bmp_y;
    }

    public String getBmp_w() {
        return this.bmp_w;
    }

    public void setBmp_w(String bmp_w) {
        this.bmp_w = bmp_w;
    }

    public String getBmp_h() {
        return this.bmp_h;
    }

    public void setBmp_h(String bmp_h) {
        this.bmp_h = bmp_h;
    }

    public String getDisplay_w() {
        return this.display_w;
    }

    public void setDisplay_w(String display_w) {
        this.display_w = display_w;
    }

    public String getDisplay_h() {
        return this.display_h;
    }

    public void setDisplay_h(String display_h) {
        this.display_h = display_h;
    }

    public String getRail_mode() {
        return this.rail_mode;
    }

    public void setRail_mode(String rail_mode) {
        this.rail_mode = rail_mode;
    }

    public String getRail_mode_in_use() {
        return this.rail_mode_in_use;
    }

    public void setRail_mode_in_use(String rail_mode_in_use) {
        this.rail_mode_in_use = rail_mode_in_use;
    }

    public HashMap<String, MapModel.Status> getStatus() {
        return this.status;
    }

    public void setStatus(HashMap<String, MapModel.Status> status) {
        this.status = status;
    }

    public boolean isCruiser_random() {
        return this.cruiser_random;
    }

    public void setCruiser_random(boolean cruiser_random) {
        this.cruiser_random = cruiser_random;
    }

    public static enum Status {
        idle,
        done,
        error;

        private Status() {
        }
    }
}
