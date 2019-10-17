package com.example.cs3235;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

// Own implementation of EditText for onKey to work properly
public class CustomEditText extends EditText {

    private static final String TAG = "MainActivity";

    public CustomEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomEditText(Context context) {
        super(context);
    }

    // Override getTextBeforeCursorMethod so the IME thinks that there is always 1 character in the EditText Field
    /*
    Special thanks to: https://stackoverflow.com/questions/20614896/android-intercept-soft-keystrokes-from-my-own-application-backspace-issue
    which helped me solve the issue.
     */
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.actionLabel = null;
        outAttrs.inputType = InputType.TYPE_NULL;

        BaseInputConnection connection = new BaseInputConnection(this, false) {
            @Override
            public String getTextBeforeCursor(int ignore, int ignore2) {
                return " ";
            }
        };
        return connection;
    }
}