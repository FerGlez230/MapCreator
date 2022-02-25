package com.example.mapcreator.controllers;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.mapcreator.models.AnchorStorageObject;

import java.util.logging.Logger;

public class BDRequest {
    BDController admin;
    public BDRequest(Context context){
        admin=new BDController(context, "cetiColomosAR.db", null, 1);
    }


    public long addAnchorOnBD(AnchorStorageObject anchor, BDController admin) {
        long id = -1;
        SQLiteDatabase bd = admin.getWritableDatabase();
        ContentValues newAnchor = new ContentValues();
        newAnchor.put("shortCode", anchor.getShortCode());
        newAnchor.put("id", anchor.getIdAnchor());
        newAnchor.put("latitude", anchor.getLatitude());
        newAnchor.put("longitude", anchor.getLongitude());
        if (bd != null) {
            try {
                id = bd.insert("anchors", null, newAnchor);
            } catch (SQLException e) {
                Log.e("HELPME", "Error"+e.getMessage());
            }
            bd.close();
        }
        return id;
    }
}
