package com.example.mapcreator.helpers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import androidx.fragment.app.DialogFragment;

public class AddAnchorDialogFragment extends DialogFragment {
    // The maximum number of characters that can be entered in the EditText.
    public interface OkListener {
        /**
         * This method is called by the dialog box when its OK button is pressed.
         *
         * @param dialogValue the long value from the dialog box
         */
        void onOkPressedString(String dialogValue);
    }

    public static com.example.mapcreator.helpers.AddAnchorDialogFragment createWithOkListener(OkListener listener) {
        com.example.mapcreator.helpers.AddAnchorDialogFragment frag = new com.example.mapcreator.helpers.AddAnchorDialogFragment();
        frag.okListener = listener;
        return frag;
    }


    private EditText descriptionField;
    private OkListener okListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setView(createDialogLayout())
                .setTitle("Anchor decsription")
                .setPositiveButton("Add", (dialog, which) -> onAddPressed())
                .setNegativeButton("Cancel", (dialog, which) -> {});
        return builder.create();
    }

    private LinearLayout createDialogLayout() {
        Context context = getContext();
        LinearLayout layout = new LinearLayout(context);
        descriptionField = new EditText(context);
        descriptionField.setInputType(InputType.TYPE_CLASS_TEXT);
        descriptionField.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        layout.addView(descriptionField);
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return layout;
    }

    private void onAddPressed() {
        Editable roomCodeText = descriptionField.getText();
        if (okListener != null && roomCodeText != null && roomCodeText.length() > 0) {

            okListener.onOkPressedString(roomCodeText.toString());
        }
    }
}
