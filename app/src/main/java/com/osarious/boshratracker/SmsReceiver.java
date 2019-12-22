package com.osarious.boshratracker;

import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.mindrot.jbcrypt.BCrypt;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationBasedOnActivityProvider;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider;
import io.nlopez.smartlocation.location.providers.LocationManagerProvider;

import static android.content.Context.MODE_PRIVATE;

public  class SmsReceiver extends BroadcastReceiver {


    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    private static final String RECEIVE_BOOT_COMPLETED = "android.intent.action.RECEIVE_BOOT_COMPLETED";
     static String recivedMessage = "";
     static Boolean enteredbesteffort = false;

    @Override
    public void onReceive(final Context context, Intent intent) {

        if (intent.getAction().equals(SMS_RECEIVED)|| intent.getAction().equals(RECEIVE_BOOT_COMPLETED)) {
            Bundle bundle = intent.getExtras();


            if (bundle != null) {
                // get sms objects
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus.length == 0) {
                    return;
                }
                // large message might be broken into many
                SmsMessage[] messages = new SmsMessage[pdus.length];
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    sb.append(messages[i].getMessageBody());
                }
                String sender = messages[0].getOriginatingAddress();
                recivedMessage = sb.toString();
                // prevent any other broadcast receivers from receiving broadcast
                // abortBroadcast();



                        checkAndSendSMS(context);


            }
        }
    }
    //todo remove later

       static void checkAndSendSMS(final Context context) {

            SharedPreferences pref = context.getSharedPreferences("MyPref", MODE_PRIVATE);
            String saved = pref.getString("secretmessage", "");


           if (!saved.trim().isEmpty()&&!recivedMessage.trim().isEmpty()) {

                try {
                    if (BCrypt.checkpw(saved, recivedMessage.trim())) {
                    final    Handler handler = new Handler();

                        Runnable r2 = new Runnable() {
                            public void run() {

                                SmartLocation.with(context).location().stop();
                                SmartLocation.with(context).geocoding().stop();

                            SmartLocation.with(context).location().oneFix().
                                   start(new OnLocationUpdatedListener() {
                                @Override
                                public void onLocationUpdated(Location location) {
                                    if (enteredbesteffort==false) {

                                        enteredbesteffort = true;

                                        handler.removeCallbacksAndMessages(null);


                                        SharedPreferences pref = context.getSharedPreferences("MyPref", 0);
                                        String recivedPhone = pref.getString("recivedPhone", "");// number to get the sms
                                        int countryPhoneCode = pref.getInt("recivedPhoneCode", 91);
                                        String countryPhoneCodeWithPlus = pref.getString("recivedPhoneCodeWithPlus", "");

                                        String geoUri = "https://www.google.com/maps/search/?api=1&query=" + location.getLatitude() + "," + location.getLongitude();


                                        SmsManager smsManager = SmsManager.getDefault();

                                        smsManager.sendTextMessage(String.valueOf(countryPhoneCodeWithPlus + recivedPhone), null, geoUri, null, null);

                                        SmartLocation.with(context).location().stop();
                                        SmartLocation.with(context).geocoding().stop();

                                        enteredbesteffort = null;


                                    }
                                }
                            });



                                if (enteredbesteffort!=null&&enteredbesteffort==false)
                            handler.postDelayed(this, 15000);

                                if (enteredbesteffort==null)
                                    enteredbesteffort = false;


                                }

                            };
                        handler.postDelayed(r2, 0);

                    }
                }catch(IllegalArgumentException e){

                }


            }
        }

    }



