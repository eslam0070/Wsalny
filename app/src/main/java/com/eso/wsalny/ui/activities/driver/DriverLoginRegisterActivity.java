package com.eso.wsalny.ui.activities.driver;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.eso.wsalny.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

@SuppressLint("SetTextI18n")
public class DriverLoginRegisterActivity extends AppCompatActivity {


    TextView mDriverStatus,mDriverRegisterLink;
    EditText mDriverLoginEmail,mDriverLoginPassword;
    Button mDriverLoginBtn,mDriverRegisterBtn;
    ProgressDialog loadingBar;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    DatabaseReference DriverDatabaseRef;
    String onlineDriverID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);
        initView();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        onlineDriverID = mAuth.getCurrentUser().getUid();
        loadingBar = new ProgressDialog(this);
    }

    private void initView(){

        mDriverStatus = findViewById(R.id.driver_status);
        mDriverLoginEmail = findViewById(R.id.driver_login_email);
        mDriverLoginPassword = findViewById(R.id.driver_login_password);
        mDriverLoginBtn = findViewById(R.id.driver_login_btn);
        mDriverRegisterLink = findViewById(R.id.driver_register_link);
        mDriverRegisterBtn = findViewById(R.id.driver_register_btn);
        mDriverRegisterBtn.setVisibility(View.INVISIBLE);
        mDriverRegisterBtn.setEnabled(false);
        mDriverRegisterLink.setOnClickListener(v -> {
            mDriverLoginBtn.setVisibility(View.INVISIBLE);
            mDriverRegisterLink.setVisibility(View.INVISIBLE);
            mDriverStatus.setText("Driver Register");
            mDriverRegisterBtn.setVisibility(View.VISIBLE);
            mDriverRegisterBtn.setEnabled(true);

        });
        mDriverRegisterBtn.setOnClickListener(v -> {
            String email = mDriverLoginEmail.getText().toString().trim();
            String password = mDriverLoginPassword.getText().toString().trim();
            RegisterDriver(email,password);
        });
        mDriverLoginBtn.setOnClickListener(v -> {
            String email = mDriverLoginEmail.getText().toString().trim();
            String password = mDriverLoginPassword.getText().toString().trim();
            LoginDriver(email,password);
        });
    }


    private void RegisterDriver(String email, String password) {
        if (TextUtils.isEmpty(email))
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
        else if (TextUtils.isEmpty(password) || password.length() < 6)
            Toast.makeText(this, "You must have 6 characters in your password", Toast.LENGTH_SHORT).show();
        loadingBar.setTitle("Driver Registering");
        loadingBar.setMessage("Please wait while we create your account !");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();
        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                DriverDatabaseRef = FirebaseDatabase.getInstance().getReference()
                        .child("Users").child("Drivers").child(onlineDriverID);

                DriverDatabaseRef.setValue(true);
                loadingBar.dismiss();
                startActivity(new Intent(DriverLoginRegisterActivity.this, DriverMapActivity.class));
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            loadingBar.dismiss();
        });
    }
    private void LoginDriver(String email, String password) {
        if (TextUtils.isEmpty(email))
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
        else if (TextUtils.isEmpty(password) || password.length() < 6)
            Toast.makeText(this, "You must have 6 characters in your password", Toast.LENGTH_SHORT).show();
        loadingBar.setTitle("Driver Login");
        loadingBar.setMessage("Please wait, while we are checking your account");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();
        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                loadingBar.dismiss();
                startActivity(new Intent(DriverLoginRegisterActivity.this, DriverMapActivity.class));
                finish();
            }
        }).addOnFailureListener(e -> {
            loadingBar.dismiss();
            Toast.makeText(DriverLoginRegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }


}
