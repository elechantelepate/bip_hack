package com.example.puntobip;

import java.io.IOException;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private final int DIALOG_SALDO = 1;
	private final int DIALOG_CARGAR = 2;
	private final int DIALOG_NUMERO = 3;
	private final byte[] KEY_B_SECTOR_0 = new byte[]{(byte)0x1F,(byte)0xC2,(byte)0x35,(byte)0xAC,(byte)0x13,(byte)0x09};
	private final byte[] KEY_B_SECTOR_8 = new byte[]{(byte)0x64,(byte)0xE3,(byte)0xC1,(byte)0x03,(byte)0x94,(byte)0xC2};
	private final byte[] DATA_CARGA_B21 = new byte[]{(byte)0x10,(byte)0x27,(byte)0x00,(byte)0x00,(byte)0xef,(byte)0xd8,(byte)0xff,(byte)0xff,(byte)0x10,(byte)0x27,(byte)0x00,(byte)0x00,(byte)0x21,(byte)0xde,(byte)0x21,(byte)0xde};
	private final byte[] DATA_CARGA_B22 = new byte[]{(byte)0x10,(byte)0x27,(byte)0x00,(byte)0x00,(byte)0xef,(byte)0xd8,(byte)0xff,(byte)0xff,(byte)0x10,(byte)0x27,(byte)0x00,(byte)0x00,(byte)0x22,(byte)0xdd,(byte)0x22,(byte)0xdd};
	
	private TapDialog tapDialog;
	
	private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechList;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		showTapDialog(getResources().getString(R.string.consultar_saldo), DIALOG_SALDO);
		inicializarNFC();
	}
	
	public void onCargarSaldo(View view) {
		showTapDialog(getResources().getString(R.string.cargar_saldo), DIALOG_CARGAR);
	}
	
	public void onCambiarNumero(View view) {
		showTapDialog(getResources().getString(R.string.cambiar_numero), DIALOG_NUMERO);
	}
	
	private void showTapDialog(String message, int tag) {
		if(tapDialog == null) {
			tapDialog = new TapDialog(this);
			tapDialog.setCanceledOnTouchOutside(false);
		}
		tapDialog.setTitle(message);
		tapDialog.setTag(tag);
		tapDialog.show();
	}
	
	private void inicializarNFC() {
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        if (!mNfcAdapter.isEnabled()) {
        	AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        	alertDialog.setTitle("NFC deshabilitado");
        	alertDialog.setMessage("Su NFC está deshabilitado.\nIngrese a las opciones de configuración y activelo.");
        	alertDialog.show();
        }
        
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("error", e);
        }
        
        mFilters = new IntentFilter[] {ndef};
        mTechList = new String[][] { new String[] { MifareClassic.class.getName() } };
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechList);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        mNfcAdapter.disableForegroundDispatch(this);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
    	String action = intent.getAction();
    	
		if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			MifareClassic mfc = MifareClassic.get(tagFromIntent);
			
    		try {
    			mfc.connect();
    			
    			if(tapDialog.isShowing()) {
			    	switch (tapDialog.getTag()) {
					case DIALOG_CARGAR:
						if(mfc.authenticateSectorWithKeyB(0x08, KEY_B_SECTOR_8)) {
	        				mfc.writeBlock(0x21, DATA_CARGA_B21);
	        				mfc.writeBlock(0x22, DATA_CARGA_B22);
				    	} else {
				    		Toast.makeText(this, "error autenticando sector 0x21", Toast.LENGTH_LONG).show();
				    	}
						break;
			
					case DIALOG_NUMERO:
						if(mfc.authenticateSectorWithKeyB(0x00, KEY_B_SECTOR_0)) {
				    		byte[] data = mfc.readBlock(0x01);
	        				Random rand = new Random();
	        				data[4] = (byte) (rand.nextInt((0xff - 0x00) + 1) + 0x00);
	        				data[5] = (byte) (rand.nextInt((0xff - 0x00) + 1) + 0x00);
	        				data[6] = (byte) (rand.nextInt((0xff - 0x00) + 1) + 0x00);
	        				data[7] = (byte) (rand.nextInt((0xff - 0x00) + 1) + 0x00);
	        				mfc.writeBlock(0x01, data);
				    	} else {
				    		Toast.makeText(this, "error autenticando sector 0x00", Toast.LENGTH_LONG).show();
				    	}
						break;
					}
    			}
		    	
		    	//
		    	// datos de la tarjeta
		    	// ----------------------------------------------------------------
		    	//
		    	TextView tvNumeroUUID = (TextView)findViewById(R.id.tvNumeroUUID);
		    	TextView tvNumeroBIP = (TextView)findViewById(R.id.tvNumeroBIP);
		    	TextView tvSaldoBIP = (TextView)findViewById(R.id.tvSaldoBIP);
		    	
		    	// uuid
		    	tvNumeroUUID.setText(leToNumericString(mfc.getTag().getId(), 4));
		    	
		    	// numero bip
		    	if(mfc.authenticateSectorWithKeyB(0x00, KEY_B_SECTOR_0)) {
		    		byte[] data = mfc.readBlock(0x01);
		    		tvNumeroBIP.setText(leToNumericString(new byte[]{data[4],data[5],data[6],data[7]}, 4));
		    	} else {
		    		Toast.makeText(this, "error autenticando sector 0x00", Toast.LENGTH_LONG).show();
		    	}
		    	
		    	// saldo bip
		    	if(mfc.authenticateSectorWithKeyB(0x08, KEY_B_SECTOR_8)) {
		    		byte[] data = mfc.readBlock(0x21);
		    		tvSaldoBIP.setText(formatMoneda(leToNumeric(data, 2)));
		    	} else {
		    		Toast.makeText(this, "error autenticando sector 0x21", Toast.LENGTH_LONG).show();
		    	}
		    	
    		} catch (Exception e) {
				Log.e("error", e.getLocalizedMessage());
				Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
    		} finally {
    			try {mfc.close();} catch (IOException e) {}
    		}
		}
		
		tapDialog.dismiss();
    }
    
    private long leToNumeric(byte[] buffer, int size) {
    	long value = 0;
    	for (int i=0; i<size; i++) { value += ((long) buffer[i] & 0xffL) << (8 * i); }
    	return value;
    }
    
    private String leToNumericString(byte[] buffer, int size) {
    	return String.valueOf(leToNumeric(buffer, size));
    }
    
    private String formatMoneda(long valor) {
    	return "$"+String.format("%,d", valor);
    }
}
