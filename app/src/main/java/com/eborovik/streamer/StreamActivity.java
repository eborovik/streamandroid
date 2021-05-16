package com.eborovik.streamer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class StreamActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView mTextWelcome;
    private TextView mTextQuote;
    private Button mButtonRandom;
    private Button mButtonLogout;

    private AuthHelper mAuthHelper;

    private ProgressDialog mProgressDialog;

    public static Intent getCallingIntent(Context context) {
        return new Intent(context, StreamActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);

        mTextWelcome = (TextView) findViewById(R.id.text_username);
        mButtonRandom = (Button) findViewById(R.id.button_add);
        mButtonLogout = (Button) findViewById(R.id.button_logout);

        mProgressDialog = new ProgressDialog(this);
        mAuthHelper = AuthHelper.getInstance(this);

        if (mAuthHelper.isLoggedIn()) {
            setupView();
        } else {
            finish();
        }
    }

    private void setupView() {
        setWelcomeText(mAuthHelper.getUsername());
        mButtonRandom.setOnClickListener(this);
        mButtonLogout.setOnClickListener(this);
    }

    private void setWelcomeText(String username) {
        mTextWelcome.setText(String.format(getString(R.string.text_welcome), username));
    }

    @Override
    public void onClick(View view) {
        if (view == mButtonRandom) {
            //doAddStream(mAuthHelper.getIdToken());
        } else if (view == mButtonLogout) {
            mAuthHelper.clear();
            finish();
        }
    }



    /**
     * Dismiss the dialog if it's showing
     */
    private void dismissDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
