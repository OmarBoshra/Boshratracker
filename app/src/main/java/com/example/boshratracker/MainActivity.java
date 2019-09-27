package com.example.boshratracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.nlopez.smartlocation.OnActivityUpdatedListener;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;

import android.app.ProgressDialog;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.graphics.Typeface;
import android.location.Location;
import android.nfc.Tag;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.hbb20.CountryCodePicker;

import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.List;

import static io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider.REQUEST_CHECK_SETTINGS;

public class MainActivity extends AppCompatActivity {

    AlertDialog.Builder builder;
    TextView currentMessage ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECEIVE_SMS);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.SEND_SMS);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 1);

            getRecivedOrtrackedPhoneNumber(true,false);

        }



        SharedPreferences pref = this.getSharedPreferences("MyPref", MODE_PRIVATE);


        SpannableString secretword=new SpannableString(pref.getString("secretmessage", ""));

        secretword.setSpan(new android.text.style.StyleSpan(Typeface.BOLD_ITALIC), 0, secretword.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        currentMessage = findViewById(R.id.currentmessage);

        currentMessage.setText(!pref.contains("secretmessage")?"":secretword);

final EditText newMessage = findViewById(R.id.newmessage);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.holo_blue_light)));

        Button submit=findViewById(R.id.submitsecretmessage);

        final Button sendSms=findViewById(R.id.sendsms);


        sendSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                getRecivedOrtrackedPhoneNumber(false,true);

            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
                SharedPreferences.Editor editor = pref.edit();

                //save hashed secret message


                editor.putString("secretmessage", newMessage.getText().toString().trim());
                editor.apply();

if(!newMessage.getText().toString().isEmpty()) {
    currentMessage.setText(newMessage.getText().toString());
    Toast.makeText(MainActivity.this, "submitted", Toast.LENGTH_SHORT).show();


}else
    Toast.makeText(MainActivity.this, "KeyWord is empty", Toast.LENGTH_SHORT).show();

            }
        });

//start checking of location is open or not
        createLocationRequest();
    }
    void createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());


        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                checkAndSendSMS();
            }

        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {


                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                1);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==REQUEST_CHECK_SETTINGS){

           if(resultCode==RESULT_CANCELED){

                Toast.makeText(this, "Please open GPS YASMIN", Toast.LENGTH_LONG).show();

               SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
               String number = pref.getString("recivedPhone", "");// number to get the sms


                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(number, null, "يا حج ياسمين مش فاتحة ال GPS", null, null);

                // in case user back press or refuses to open gps
            }else{



                   Toast.makeText(this, "Gps opened", Toast.LENGTH_SHORT).show();
                   //if user allows to open gps
               checkAndSendSMS();

               }
            }
        }

    public boolean onCreateOptionsMenu(Menu menu) {


        getMenuInflater().inflate(R.menu.menu,menu);

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){


        int id = item.getItemId();


        if (id == R.id.recived_Phone) {
getRecivedOrtrackedPhoneNumber(true,false);
        } else {

            getRecivedOrtrackedPhoneNumber(false,false);

        }
        return super.onOptionsItemSelected(item);

    }

    void checkAndSendSMS(){
        if(!SmsReceiver.recivedMessage.trim().isEmpty()) {
            SharedPreferences pref = this.getSharedPreferences("MyPref", MODE_PRIVATE);
            String saved = pref.getString("secretmessage", "");


            final ProgressDialog dialogProgress = new ProgressDialog(MainActivity.this);
            dialogProgress.setMessage("Decrypting message...");
            dialogProgress.setCancelable(false);
            dialogProgress.show();


            if (BCrypt.checkpw(saved, SmsReceiver.recivedMessage.trim())) {
                SmartLocation.with(this).location()
                        .oneFix().start(new OnLocationUpdatedListener() {
                            @Override
                            public void onLocationUpdated(Location location) {

                                dialogProgress.dismiss();

                                SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
                                String recivedPhone = pref.getString("recivedPhone", "");// number to get the sms
                               int countryPhoneCode= pref.getInt("recivedPhoneCode", 91);
                               String countryPhoneCodeWithPlus= pref.getString("recivedPhoneCodeWithPlus", "");

                                String geoUri = "https://www.google.com/maps/search/?api=1&query=" + location.getLatitude() + "," + location.getLongitude();


                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage(String.valueOf(countryPhoneCodeWithPlus+recivedPhone), null, geoUri, null, null);

                                SmartLocation.with(MainActivity.this).location().stop();
                                SmartLocation.with(MainActivity.this).geocoding().stop();

                            }
                        });

            }
        }
    }

    void getRecivedOrtrackedPhoneNumber(boolean isRecived, final boolean IsUserSendingmessage){

        builder = new AlertDialog.Builder(MainActivity.this);


        final EditText input = new EditText(MainActivity.this);

        final CountryCodePicker ccp = new CountryCodePicker(MainActivity.this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);

        final LinearLayout linearLayout = new LinearLayout(MainActivity.this);

        linearLayout.setOrientation(LinearLayout.VERTICAL);

        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.weight=1;

        input.setLayoutParams(lp);

        linearLayout.addView(ccp);
        linearLayout.addView(input);


        // for inputting message

            final EditText inputMessage = new EditText(MainActivity.this);

            if (IsUserSendingmessage) {
                inputMessage.setHint("Enter your message");
                inputMessage.setLayoutParams(lp);
                linearLayout.addView(inputMessage);

            }


        builder.setView(linearLayout);

        SharedPreferences pref = MainActivity.this.getSharedPreferences("MyPref", MODE_PRIVATE);


        if(isRecived){//RECIVED phone number

            builder.setTitle("Please add the reciving phone number");
            input.setLayoutParams(lp);
            String recivedPhone = pref.getString("recivedPhone", "");

            if (!recivedPhone.isEmpty()) {// just show phone to user if it exists
                input.setText(recivedPhone);
ccp.setCountryForPhoneCode(pref.getInt("recivedPhoneCode", 91));

            }else
                builder.setCancelable(false);

            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {

                    String countryCode= ccp.getSelectedCountryNameCode().substring(0,ccp.getSelectedCountryNameCode().length()-1);
                    int countryPhoneCode= ccp.getSelectedCountryCodeAsInt();
                    String countryPhoneCodeWithPlus= ccp.getSelectedCountryCodeWithPlus();

                    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                    try {
                        Phonenumber.PhoneNumber swissNumberProto = phoneUtil.parse(input.getText().toString(), countryCode);
                        boolean isValid = phoneUtil.isValidNumber(swissNumberProto); // returns true

                        if(isValid){
                            SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putString("recivedPhone", input.getText().toString());
                            editor.putInt("recivedPhoneCode", countryPhoneCode);
                            editor.putString("recivedPhoneCodeWithPlus", countryPhoneCodeWithPlus);
                            editor.apply();
                            dialog.dismiss();



                        }
                        else {
                            Toast.makeText(MainActivity.this, "Please enter valid phone number", Toast.LENGTH_SHORT).show();
                            getRecivedOrtrackedPhoneNumber(true,IsUserSendingmessage);
                        }
                    } catch (NumberParseException e) {
                        Toast.makeText(MainActivity.this, "Please enter valid phone number", Toast.LENGTH_SHORT).show();
                        getRecivedOrtrackedPhoneNumber(true,IsUserSendingmessage);

                    }


                }
            });


        }else{//tracked phone number

            builder.setTitle("Please add the phone number to track");

            String trackedPhone = pref.getString("trackedPhone", "");

            if (!trackedPhone.isEmpty()) {// // just show phone to user if it exists
                    input.setText(trackedPhone);
                ccp.setCountryForPhoneCode(pref.getInt("trackedPhoneCode", 91));// se the saved country


            }
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                       String countryCode= ccp.getSelectedCountryNameCode().substring(0,ccp.getSelectedCountryNameCode().length()-1);
                        int countryPhoneCode= ccp.getSelectedCountryCodeAsInt();
                        String countryPhoneCodeWithPlus= ccp.getSelectedCountryCodeWithPlus();

                        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                        try {
                            Phonenumber.PhoneNumber swissNumberProto = phoneUtil.parse(input.getText().toString(), countryCode);
                            boolean isValid = phoneUtil.isValidNumber(swissNumberProto); // returns true

                            if(isValid){
                                SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putString("trackedPhone", input.getText().toString());
                                editor.putInt("trackedPhoneCode", countryPhoneCode);
                                editor.apply();
                                dialog.dismiss();


                                if(IsUserSendingmessage) {

                                        SmsManager smsManager = SmsManager.getDefault();


                                    ProgressDialog dialogProgress = new ProgressDialog(MainActivity.this);
                                    dialogProgress.setMessage("Encrypting message...");
                                    dialogProgress.setCancelable(false);
                                    dialogProgress.show();

                                    smsManager.sendTextMessage(String.valueOf(countryPhoneCodeWithPlus+input.getText().toString()), null,BCrypt.hashpw( inputMessage.getText().toString().trim(), BCrypt.gensalt()), null, null);
                                    dialogProgress.dismiss();
                                }
                            }
                            else {
                                Toast.makeText(MainActivity.this, "Please enter valid phone number", Toast.LENGTH_SHORT).show();
                                getRecivedOrtrackedPhoneNumber(false,IsUserSendingmessage);
                            }
                        } catch (NumberParseException e) {
                            Toast.makeText(MainActivity.this, "Please enter valid phone number", Toast.LENGTH_SHORT).show();
                            getRecivedOrtrackedPhoneNumber(false,IsUserSendingmessage);

                        }



                    }
                });

        }


        builder.show();
    }

    public void test(View view) {

    }
}
