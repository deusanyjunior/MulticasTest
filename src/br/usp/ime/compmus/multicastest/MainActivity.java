package br.usp.ime.compmus.multicastest;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import br.usp.ime.compmus.dj.multicast.R;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

	// UDP Multicast
	private UDPMulticast udpMulticast = null;
	private NetworkInterface networkInterface = null;
	private String ip;  
	private int port;

	// Application status
	private boolean isConnected = false;
	private boolean isSending = false;
	private boolean isReceiving = false;
	
	// Interface
	private Spinner spinnerInterfaces = null;
	private EditText editTextIp = null;
	private EditText editTextPort = null;
	private ToggleButton toggleUDP = null;
	private ToggleButton toggleSender = null;
	private ToggleButton toggleReceiver = null;
	private TextView textViewConsole = null;
	
	// Interface helpers
	private ArrayList<NetworkInterface> arrayInterfaces;
	private ArrayAdapter<CharSequence> arrayAdapterInterfaces = null;
	private StringBuffer consoleText = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Load view items
		setupTexts();
		setupToggles();		
	}
	
	@Override
	public void onResume() {
		// Reload interface options 
		setupSpinnerNetworkInterfaces();
		super.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onStop() {
		// Stop everything and reset toggles
		isConnected = false;
		udpMulticast = null;
		isSending = false;
		isReceiving = false;
		toggleUDP.setChecked(false);
		toggleSender.setChecked(false);
		toggleReceiver.setChecked(false);
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		// Stop everything and destroy
		isConnected = false;
		udpMulticast = null;
		isSending = false;
		isReceiving = false;
		super.onDestroy();
	}
	
	/**
	 * reloadTextViewConsole()
	 * 
	 * This method is used in order to reload the console text,
	 * because you cannot modify interface objects outside the UI thread.
	 */
	private synchronized void reloadTextViewConsole() {
		MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
            	textViewConsole.setText(consoleText);
            }
        });
	}
	
	/**
	 * loadInterfaces()
	 * 
	 * This method loads Network Interfaces available on device 
	 * and set up the array adapter used on the spinner.
	 */
	private void loadInterfaces() {
		try {
			Enumeration<NetworkInterface> enumerationInterfaces = NetworkInterface.getNetworkInterfaces();
			arrayInterfaces = Collections.list(enumerationInterfaces);
			
			CharSequence[] array = new CharSequence[arrayInterfaces.size()];
			for(int i = 0; i < arrayInterfaces.size(); i++ ) {
				array[i] = arrayInterfaces.get(i).getName();
			}
			
			arrayAdapterInterfaces = new ArrayAdapter<CharSequence>(getApplicationContext(), android.R.layout.simple_spinner_item, array);
			arrayAdapterInterfaces.notifyDataSetChanged();
		} catch (SocketException e1) {
			Toast.makeText(getApplicationContext(), getResources().getText(R.string.socket_exception),  Toast.LENGTH_SHORT).show();
			e1.printStackTrace();
		}
	}
	
	/**
	 * setupSpinnerNetworkInterfaces()
	 * 
	 * Configure the spinner options based on interfaces available.
	 */
	private void setupSpinnerNetworkInterfaces() {
		
		loadInterfaces();
		
		spinnerInterfaces = (Spinner) findViewById(R.id.spinnerInterfaces);
		spinnerInterfaces.setAdapter(arrayAdapterInterfaces);
		spinnerInterfaces.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				if(position >= 0 && position < arrayInterfaces.size()) {
					networkInterface = arrayInterfaces.get(position);
				} else {
					// If the interface list is obsolete, this method will update.  
					setupSpinnerNetworkInterfaces();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// If nothing is selected, this method will update the list.
				setupSpinnerNetworkInterfaces();
			}
		});
	}
	
	/**
	 * startUDPMulticast()
	 * 
	 * The method will get the configurations defined (Interface, IP, Port)
	 * and try to connect on the network.
	 * @return true If the device is successfully connected.
	 */
	public boolean startUDPMulticast() {
		
		// Get IP from UI
		this.ip = editTextIp.getText().toString();
			
		// Get Port from UI
		try {
			this.port = Integer.parseInt(editTextPort.getText().toString());
		} catch (NumberFormatException e1) {
			Toast.makeText(getApplicationContext(), getResources().getText(R.string.invalid_port), Toast.LENGTH_SHORT).show();
			this.port = getResources().getInteger(R.integer.default_port);
			editTextPort.setText(String.valueOf(this.port));
		}
		// Join the UDP multicast group  
		try {
			udpMulticast = new UDPMulticast(this.ip, this.port, this.networkInterface);
			return true;
		} catch (IOException e) {
			udpMulticast = null;
			Toast.makeText(getApplicationContext(), getResources().getText(R.string.invalid_ip_port), Toast.LENGTH_LONG).show();
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * stopUDPMulticast
	 * 
	 * Close any connection and disable the toggles
	 */
	private void stopUDPMulticast() {
		udpMulticast = null;
		isConnected = isSending = isReceiving = false;
		toggleUDP.setChecked(false);
		toggleReceiver.setChecked(false);
		toggleSender.setChecked(false);
	}
	
	/**
	 * setupTexts
	 * 
	 * Define interface text input and output areas
	 */
	private void setupTexts() {
		this.ip = getResources().getText(R.string.default_ip).toString();
		editTextIp = (EditText) findViewById(R.id.editTextIP);
		editTextIp.setText(this.ip);
		
		this.port = getResources().getInteger(R.integer.default_port);
		editTextPort = (EditText) findViewById(R.id.editTextPort);
		editTextPort.setText(String.valueOf(this.port));
				
		textViewConsole = (TextView) findViewById(R.id.textViewConsole);
		consoleText = new StringBuffer();
		reloadTextViewConsole();
	}
	
	/**
	 * setupToggles
	 * 
	 * Define the configuration of each toggle
	 */
	private void setupToggles() {
		
		// Toggle UDP joins multicast group
		toggleUDP = (ToggleButton) findViewById(R.id.toggleButtonUDP);
		toggleUDP.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) {
					if(startUDPMulticast()) {
						isConnected = true;
					} else {
						stopUDPMulticast();
					}	
				} else {
					stopUDPMulticast();
				}
			}
		});
		
		// Toggle Sender starts the multicast sender 
		toggleSender = (ToggleButton) findViewById(R.id.toggleSENDER);
		toggleSender.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) {
					if(isConnected) {
						isSending = true;
						SenderMulticast sender = new SenderMulticast();
						sender.execute();
					} else {
						toggleSender.setChecked(false);
					}
				} else {
					isSending = false;
				}
				
			}
		});
		
		// Toggle Receiver starts a multicast receiver
		toggleReceiver = (ToggleButton) findViewById(R.id.toggleRECEIVER);
		toggleReceiver.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) {
					if(isConnected) {
						isReceiving = true;
						ReceiverMulticast receiver = new ReceiverMulticast();
						receiver.execute();
					} else {
						toggleReceiver.setChecked(false);
					}
				} else {
					isReceiving = false;
				}
			}
		});
	}
	
	/**
	 * SenderMulticast
	 *
	 * This task sends a multicast message every 1s
	 */
	private class SenderMulticast extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			int count = 0;
			while(isSending){
				try{
					String msg = "#" + count;
					udpMulticast.send(msg);
					consoleText.insert(0, getResources().getText(R.string.msg_sent).toString() + msg  + "\n");
					reloadTextViewConsole();
					Thread.sleep(1000);
					count++;
				} catch(Exception e){
					Toast.makeText(getApplicationContext(), getResources().getText(R.string.sender_exception), Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}
			return null;
		}
	}
	
	/**
	 * ReceiverMulticast
	 * 
	 * This task receives the messages from the multicast group
	 */
	private class ReceiverMulticast extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			byte[] buf = new byte[1000];
			while(isReceiving){
				try{
					udpMulticast.receive(buf);
					consoleText.insert(0, getResources().getText(R.string.msg_received).toString() + new String(buf) + "\n");
					reloadTextViewConsole();
				} catch(Exception e){
					Toast.makeText(getApplicationContext(), getResources().getText(R.string.receiver_exception), Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}
			return null;
		}

	}

}
