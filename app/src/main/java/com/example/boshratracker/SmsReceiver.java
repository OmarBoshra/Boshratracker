package com.example.boshratracker;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.TextView;
import android.widget.Toast;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;

import static android.content.Context.MODE_PRIVATE;

public  class SmsReceiver extends BroadcastReceiver {


    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    private static final String RECEIVE_BOOT_COMPLETED = "android.intent.action.RECEIVE_BOOT_COMPLETED";
     static String recivedMessage = "";

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
                String message = sb.toString();
                // prevent any other broadcast receivers from receiving broadcast
                // abortBroadcast();

                incommingmesage(message);


                Intent toApp = new Intent(context, MainActivity.class);
                toApp.addFlags(intent.FLAG_ACTIVITY_CLEAR_TASK|intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(toApp);


            }
        }
    }
    static void incommingmesage(String message){
        recivedMessage=message;


    }


}
