package com.maxavd.nfc_ex;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;


public class MainActivity extends ActionBarActivity {

    NfcAdapter mNfcAdapter;
    private static final String TAG = "NFC";
    public static final String MIME_TEXT_PLAIN = "text/plain";

    TextView textView;

    int counter;

    @Override protected void onPause() {
        super.onPause();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("counter", counter);
        editor.apply();
        stopForegroundDispatch(this, mNfcAdapter);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);
        counter = 0;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        counter = prefs.getInt("counter", 0);
        textView.setText("Counter: " + String.valueOf(counter));

        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                counter = 0;
                textView.setText("Counter: " + String.valueOf(counter));
            }
        });

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter.isEnabled()){
            Toast.makeText(this, "NFC is Enabled", Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(this, "NFC is Disabled", Toast.LENGTH_LONG).show();
            Intent i;
            if (Build.VERSION.SDK_INT >= 10) {
                i = new Intent("android.settings.NFC_SETTINGS");
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.startActivity(i);
            } else {
                i = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.startActivity(i);
            }
        }

        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent().getAction())) {

            NdefMessage[] messages = getNdefMessages(getIntent());
            byte[] payload = messages[0].getRecords()[0].getPayload();
            try {
            		/*
	            	 * Text Record Type Definition
        	    	 * Technical Specification
            		 * NFC ForumTM
	            	 * RTD-Text 1.0
	            	 * NFCForum-TS-RTD_Text_1.0
        	    	 * Bit number (0 is LSB)
            			7  ---- > 0: The text is encoded in UTF-8
            					  1: The text is encoded in UTF16
            			6  -----> RFU (MUST be set to zero)
            			5..0 ---> The length of the IANA language code.
            		*/
                //Get the Text Encoding

                String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";
                Log.d(TAG, "*********** NFC textEncoding = " + textEncoding);
                //Get the Language Code
                int languageCodeLength = payload[0] & 0x3F;
                Log.d(TAG, "*********** NFC languageCodeLength = " + languageCodeLength);
                String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
                Log.d(TAG, "*********** NFC languageCode = " + languageCode);
                //Get the Text
                String text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
                counter ++;
                textView.setText("Counter: " + String.valueOf(counter));
                Log.d(TAG, "*********** NFC TAG = " + text);
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Toast.makeText(this, "NFC is available", Toast.LENGTH_LONG).show();
            Log.d(TAG, "NFC is available");
            //setIntent(new Intent()); // Consume this intent.

            handleIntent(getIntent());
        }

    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Log.d(TAG, "onResume");
        String X1 = NfcAdapter.ACTION_NDEF_DISCOVERED;
        String X2 = NfcAdapter.ACTION_TAG_DISCOVERED;
        String X3 = NfcAdapter.ACTION_TECH_DISCOVERED;
        String X4 = getIntent().getAction();
        String ALL = X1 + " - " + X2 + " - " + X3 + " - " + X4;
        Log.d(TAG, "onResume: " + ALL);
        setupForegroundDispatch(this, mNfcAdapter);
    }

    NdefMessage[] getNdefMessages(Intent intent) {
        // Parse the intent
        NdefMessage[] msgs = null;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {

                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[] {};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[] {
                        record
                });
                msgs = new NdefMessage[] {
                        msg
                };
            }
        } else {
            Log.d(TAG, "Unknown intent.");
            finish();
        }
        return msgs;
    }

    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

            counter ++;
            textView.setText("Counter: " + String.valueOf(counter));


    }


}
