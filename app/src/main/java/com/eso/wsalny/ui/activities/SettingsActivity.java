package com.eso.wsalny.ui.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.eso.wsalny.R;
import com.eso.wsalny.ui.activities.customer.CustomersMapActivity;
import com.eso.wsalny.ui.activities.driver.DriverMapActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    ImageView closeButton, saveButton;
    CircleImageView profileImageView;
    EditText edtName, phoneNumber, driverCarName;
    String getType, checker = "", myUrl;
    TextView profileChangeBtn;
    Uri imageUri;
    StorageTask uploadTask;
    StorageReference storageProfilePicsRef;
    DatabaseReference databaseReference;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mAuth = FirebaseAuth.getInstance();
        getType = getIntent().getStringExtra("type");
        Toast.makeText(this, getType, Toast.LENGTH_SHORT).show();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(getType);
        storageProfilePicsRef = FirebaseStorage.getInstance().getReference().child("Profile Pictures");
        initView();
    }

    private void initView() {
        profileImageView = findViewById(R.id.profile_image);
        edtName = findViewById(R.id.name);
        phoneNumber = findViewById(R.id.phone_number);
        driverCarName = findViewById(R.id.driver_car_name);
        if (getType.equals("Drivers"))
            driverCarName.setVisibility(View.VISIBLE);
        closeButton = findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> {
            if (getType.equals("Drivers"))
                startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));
            else
                startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));

        });
        saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            if (checker.equals("clicked")) {
                validateControllers();
            } else {
                validateAndSaveOnlyInformation();
            }
        });
        profileChangeBtn = findViewById(R.id.change_picture_btn);
        profileChangeBtn.setOnClickListener(v -> {
            checker = "clicked";
            CropImage.activity().setAspectRatio(1, 1)
                    .start(SettingsActivity.this);
        });
        getUserInformation();
    }

    private void validateAndSaveOnlyInformation() {
        if (TextUtils.isEmpty(edtName.getText().toString()))
            Toast.makeText(this, "Please provide your edtName", Toast.LENGTH_SHORT).show();
        else if (TextUtils.isEmpty(phoneNumber.getText().toString()))
            Toast.makeText(this, "Please provide your phone number", Toast.LENGTH_SHORT).show();
        else if ("Drivers".equals(getType) && TextUtils.isEmpty(driverCarName.getText().toString()))
            Toast.makeText(this, "Please provide your car edtName", Toast.LENGTH_SHORT).show();

        else {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("uid", mAuth.getCurrentUser().getUid());
            hashMap.put("edtName", edtName.getText().toString());
            hashMap.put("phone", phoneNumber.getText().toString());
            if (getType.equals("Drivers"))
                hashMap.put("car", driverCarName.getText().toString());

            databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(hashMap);
            if (getType.equals("Drivers"))
                startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));
            else
                startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            imageUri = result.getUri();
            profileImageView.setImageURI(imageUri);
        } else {
            if (getType.equals("Drivers"))
                startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));
            else
                startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));

            Toast.makeText(this, "Error, Try Again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateControllers() {
        if (TextUtils.isEmpty(edtName.getText().toString()))
            Toast.makeText(this, "Please provide your edtName", Toast.LENGTH_SHORT).show();
        else if (TextUtils.isEmpty(phoneNumber.getText().toString()))
            Toast.makeText(this, "Please provide your phone number", Toast.LENGTH_SHORT).show();
        else if ("Drivers".equals(getType) && TextUtils.isEmpty(driverCarName.getText().toString()))
            Toast.makeText(this, "Please provide your car edtName", Toast.LENGTH_SHORT).show();
        else if ("clicked".equals(checker))
            updateProfilePicture();
    }

    private void updateProfilePicture() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Settings Account Information");
        progressDialog.setMessage("Please wait, while we are settings your account information");
        progressDialog.show();
        if (imageUri != null) {
            final StorageReference fileRef = storageProfilePicsRef.child(mAuth.getCurrentUser().getUid() + ".jpg");
            uploadTask = fileRef.putFile(imageUri);
            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful())
                    throw task.getException();

                return fileRef.getDownloadUrl();
            }).addOnCompleteListener((OnCompleteListener<Uri>) task -> {
                    if (task.isSuccessful()) {
                        Uri downloadUrl = task.getResult();
                        myUrl = downloadUrl.toString();
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("uid", mAuth.getCurrentUser().getUid());
                        hashMap.put("edtName", edtName.getText().toString());
                        hashMap.put("phone", phoneNumber.getText().toString());
                        hashMap.put("image", myUrl);
                        if (getType.equals("Drivers"))
                            hashMap.put("car", driverCarName.getText().toString());

                        databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(hashMap);
                        progressDialog.dismiss();
                        if (getType.equals("Drivers"))
                            startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));
                        else
                            startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));

                    }else {
                        Toast.makeText(SettingsActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        } else {
            progressDialog.dismiss();
            Toast.makeText(this, "Image is not selected..", Toast.LENGTH_SHORT).show();
        }

    }

    private void getUserInformation(){
        databaseReference.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                    String name = "" + dataSnapshot.child("edtName").getValue();
                    String phone = "" + dataSnapshot.child("phone").getValue();

                    edtName.setText(name);
                    phoneNumber.setText(phone);

                    if (getType.equals("Drivers")) {
                        String car = "" + dataSnapshot.child("car").getValue();
                        driverCarName.setText(car);
                    }
                    else if (dataSnapshot.hasChild("image")) {
                        String image = "" + dataSnapshot.child("image").getValue();
                        Glide.with(SettingsActivity.this).load(image).into(profileImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
