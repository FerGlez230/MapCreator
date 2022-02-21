package com.example.mapcreator.helpers;

public interface OkListener {
    /**
     * This method is called by the dialog box when its OK button is pressed.
     *
     * @param dialogValue the long value from the dialog box
     */
    void onOkPressedInt(int dialogValue);
    void onOkPressedString(String dialogValue);
}