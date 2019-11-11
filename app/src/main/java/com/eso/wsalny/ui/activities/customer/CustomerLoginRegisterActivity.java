package com.eso.wsalny.ui.activities.customer;

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
import com.eso.wsalny.ui.activities.WelcomeActivity;
import com.eso.wsalny.ui.activities.driver.DriverLoginRegisterActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

@SuppressLint("SetTextI18n")
public class CustomerLoginRegisterActivity extends AppCompatActivity {


    TextView mCustomerStatus,mCustomerRegisterLink;
    EditText mCustomerLoginEmail,mCustomerLoginPassword;
    Button mCustomerLoginBtn,mCustomerRegisterBtn;
    FirebaseAuth mAuth;
    ProgressDialog loadingBar;
    DatabaseReference CustomerDatabaseRef;
    String onlineCustomerID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);
        initView();
        mAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);
    }

    private void initView(){
        mCustomerStatus = findViewById(R.id.customer_status);
        mCustomerLoginEmail = findViewById(R.id.customer_login_email);
        mCustomerLoginPassword = findViewById(R.id.customer_login_password);
        mCustomerLoginBtn = findViewById(R.id.customer_login_btn);
        mCustomerRegisterLink = findViewById(R.id.customer_register_link);
        mCustomerRegisterBtn = findViewById(R.id.customer_register_btn);
        mCustomerRegisterBtn.setVisibility(View.INVISIBLE);
        mCustomerRegisterBtn.setEnabled(false);
        mCustomerRegisterLink.setOnClickListener(v -> {
            mCustomerLoginBtn.setVisibility(View.INVISIBLE);
            mCustomerRegisterLink.setVisibility(View.INVISIBLE);
            mCustomerStatus.setText("Customer Register");
            mCustomerRegisterBtn.setVisibility(View.VISIBLE);
            mCustomerRegisterBtn.setEnabled(true);
        });
        mCustomerRegisterBtn.setOnClickListener(v -> {
            String email = mCustomerLoginEmail.getText().toString().trim();
            String password = mCustomerLoginPassword.getText().toString().trim();
            RegisterCustomer(email,password);
        });
        mCustomerLoginBtn.setOnClickListener(v -> {
            String email = mCustomerLoginEmail.getText().toString().trim();
            String password = mCustomerLoginPassword.getText().toString().trim();
            LoginCustomer(email,password);

        });
    }

    private void RegisterCustomer(String email, String password) {
        if (TextUtils.isEmpty(email))
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
        else if (TextUtils.isEmpty(password) || password.length() < 6)
            Toast.makeText(this, "You must have 6 characters in your password", Toast.LENGTH_SHORT).show();
        loadingBar.setTitle("Customer Registering");
        loadingBar.setMessage("Please wait while we create your account !");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();
        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                onlineCustomerID = mAuth.getCurrentUser().getUid();
                CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference()
                        .child("Users").child("Customers").child(onlineCustomerID);
                CustomerDatabaseRef.setValue(true);
                loadingBar.dismiss();
                startActivity(new Intent(this, CustomersMapActivity.class));
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            loadingBar.dismiss();
        });
    }

    private void LoginCustomer(String email, String password) {
        if (TextUtils.isEmpty(email))
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
        else if (TextUtils.isEmpty(password) || password.length() < 6)
            Toast.makeText(this, "You must have 6 characters in your password", Toast.LENGTH_SHORT).show();
        loadingBar.setTitle("Customer Login");
        loadingBar.setMessage("Please wait, while we are checking your account");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();
        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                loadingBar.dismiss();
                startActivity(new Intent(this, CustomersMapActivity.class));
                finish();
            }
        }).addOnFailureListener(e -> {
            loadingBar.dismiss();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

}
