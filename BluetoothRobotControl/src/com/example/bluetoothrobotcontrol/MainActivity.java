package com.example.bluetoothrobotcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final int REQUEST_ENABLE_BT = 1;
	private static final String SERIAL_PORT_SERVICE = "00001101-0000-1000-8000-00805f9b34fb";
	
	TextView arduinoData;
	Spinner devicesSpinner;
	
	BluetoothAdapter mBluetoothAdapter;
	BluetoothDevice mmDevice;
	BluetoothSocket mmSocket;
	OutputStream mmOutputStream;
	InputStream mmInputStream;
	Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//If we have no Bluetooth adapter we are done.
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter == null){
			Toast.makeText(this, R.string.bluetooth_not_available, Toast.LENGTH_LONG).show();
	        finish();
	        return;
		}
		
		setContentView(R.layout.activity_main);		
		
		devicesSpinner = (Spinner) findViewById(R.id.bluetooth_devices);		
		arduinoData = (TextView)findViewById(R.id.arduinoData);
		
		ImageButton btnUp = (ImageButton)findViewById(R.id.btnUp);
		ImageButton btnDown = (ImageButton)findViewById(R.id.btnDown);
		ImageButton btnRight = (ImageButton)findViewById(R.id.btnRight);
		ImageButton btnLeft = (ImageButton)findViewById(R.id.btnLeft);
		ImageButton btnStop = (ImageButton)findViewById(R.id.btnStop);
		Button btnConnect = (Button)findViewById(R.id.connect);
		
		btnConnect.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v)
			{
				Button btnConnect = (Button)findViewById(R.id.connect);
				if(btnConnect.getText() == getResources().getString(R.string.connect)){
					String[] deviceSplit = devicesSpinner.getSelectedItem().toString().split("\n");
					mmDevice = mBluetoothAdapter.getRemoteDevice(deviceSplit[1].toString());
					openBT();
				}
				else{
					if(mmSocket.isConnected()){
						btnConnect.setText(R.string.connect);
						try {
							closeBT();
			            } catch (IOException closeException) { }
						
					}
				}
			}
		});
		
		btnUp.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try{
					mmOutputStream.write('8');
				}
				catch(IOException ex){}
			}
		});
		
		btnDown.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try{
					mmOutputStream.write('2');
				}
				catch(IOException ex){}
			}
		});
		
		btnLeft.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try{
					mmOutputStream.write('4');
				}
				catch(IOException ex){}				
			}
		});
		
		btnRight.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try{
					mmOutputStream.write('6');
				}
				catch(IOException ex){}
			}
		});
		
		btnStop.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try{
					mmOutputStream.write('5');
				}
				catch(IOException ex){}
			}
		});
	}

	protected void onStart() {
        super.onStart();
        // If Bluetooth is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            initialize();
        }
    }
	
	/**
     * Ensures user has turned on Bluetooth on the Android device.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK) {
                initialize();
            } else {
                finish();
                return;
            }
        }
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	void initialize(){
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		if(pairedDevices.size() > 0){
			List<String> s = new ArrayList<String>();
			for(BluetoothDevice device : pairedDevices){
				s.add(device.getName() + "\n" + device.getAddress());
			}
			ArrayAdapter<String> devicesAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, s);
			devicesSpinner.setAdapter(devicesAdapter);
		}
	}
	
	void openBT(){
		Button btnConnect = (Button)findViewById(R.id.connect);
		
		try{
			UUID uuid = UUID.fromString(SERIAL_PORT_SERVICE); //Standard SerialPortService ID
			mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(uuid);
			mmSocket.connect();
			mmOutputStream = mmSocket.getOutputStream();
			mmInputStream = mmSocket.getInputStream();
			btnConnect.setText(R.string.disconnect);
		}
		catch(IOException ex)
		{
			Toast.makeText(this, R.string.bluetooth_connection_failed, Toast.LENGTH_LONG).show();
			btnConnect.setText(R.string.connect);
			try {
                mmSocket.close();
            } catch (IOException closeException) { }
            return;			
		}
		
		beginListenForData();
	}
	
	void closeBT() throws IOException
	{
		stopWorker = true;
		mmOutputStream.close();
		mmInputStream.close();
		mmSocket.close();
	}
	
	void beginListenForData()
    {
        final Handler handler = new Handler(); 
        final byte delimiter = 10; //This is the ASCII code for a newline character
        
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {                
               while(!Thread.currentThread().isInterrupted() && !stopWorker)
               {
                    try 
                    {
                        int bytesAvailable = mmInputStream.available();                        
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            arduinoData.setText(data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } 
                    catch (IOException ex) 
                    {
                        stopWorker = true;
                    }
               }
            }
        });

        workerThread.start();
    }
}
