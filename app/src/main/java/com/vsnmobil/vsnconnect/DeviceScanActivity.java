/**
 * @version    1.0
 * @author    Rajiv M.
 * @copyright  Copyright (C) 2014 VSNMobil. All rights reserved.
 * @license    http://www.apache.org/licenses/LICENSE-2.0
 */
package com.vsnmobil.vsnconnect;

import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.vsnmobil.vsnconnect.adapter.ScannedDevice;
import com.vsnmobil.vsnconnect.adapter.ScannedDeviceAdapter;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends Activity {
	private ScannedDeviceAdapter scannedDeviceAdapter;
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;
	private Handler mHandler;

	private static final int REQUEST_ENABLE_BT = 1;
	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 10000;
	private ListView scanningDeviceListView;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_devicescan);
		getActionBar().setTitle(R.string.app_name);
		mHandler = new Handler();
		scanningDeviceListView = (ListView) findViewById(R.id.managedevices_scanning_device_listview);
		// Use this check to determine whether BLE is supported on the device.
		// Then you can
		// selectively disable BLE-related features.
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT)
			.show();
			finish();
		}

		// Initializes a Bluetooth adapter. For API level 18 and above, get a
		// reference to
		// BluetoothAdapter through BluetoothManager.
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported,
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		// Initializes list view adapter.
		scannedDeviceAdapter = new ScannedDeviceAdapter(this,R.layout.listitem_device, new ArrayList<ScannedDevice>());
		scanningDeviceListView.setAdapter(scannedDeviceAdapter);

		// Scanned Device item click listener
		scanningDeviceListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterview,View view, int position, long id) {
				ScannedDevice item = scannedDeviceAdapter.getItem(position);
				if (item != null) {
					final Intent intent = new Intent(DeviceScanActivity.this, DeviceControlActivity.class);
					intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME,item.getDisplayName() );
					intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, item.getDeviceMac());
					if (mScanning) {
						mBluetoothAdapter.stopLeScan(mLeScanCallback);
						mScanning = false;
					}
					startActivity(intent);
					
				}
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		if (!mScanning) {
			menu.findItem(R.id.menu_stop).setVisible(false);
			menu.findItem(R.id.menu_scan).setVisible(true);
			menu.findItem(R.id.menu_refresh).setActionView(null);
		} else {
			menu.findItem(R.id.menu_stop).setVisible(true);
			menu.findItem(R.id.menu_scan).setVisible(false);
			menu.findItem(R.id.menu_refresh).setActionView(
					R.layout.actionbar_indeterminate_progress);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_scan:
			scannedDeviceAdapter.clear();
			scanLeDevice(true);
			break;
		case R.id.menu_stop:
			scanLeDevice(false);
			break;
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Ensures Bluetooth is enabled on the device. If Bluetooth is not
		// currently enabled,
		// fire an intent to display a dialog asking the user to grant
		// permission to enable it.
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
		scanLeDevice(true);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT
				&& resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		super.onPause();
		scanLeDevice(false);
		scannedDeviceAdapter.clear();
	}

	private void scanLeDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					invalidateOptionsMenu();
				}
			}, SCAN_PERIOD);

			mScanning = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
		invalidateOptionsMenu();
	}

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,
				final byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					scannedDeviceAdapter.update(device, rssi, scanRecord);
					scannedDeviceAdapter.notifyDataSetChanged();
				}
			});
		}
	};
}