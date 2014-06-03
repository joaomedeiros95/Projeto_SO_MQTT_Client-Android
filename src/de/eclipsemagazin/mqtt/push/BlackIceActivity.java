package de.eclipsemagazin.mqtt.push;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.NotificationManager;
import android.view.GestureDetector;

/**
 * @author João Eduardo Medeiros
 * @author Danilo Damasceno
 * @author Ciro Martins
 * @author Gustavo Henrique
 * 
 * @see Baseado em uma versão feita por @author Dominik Obermaier
 */
public class BlackIceActivity extends Activity implements SensorEventListener {
	
	public static String BROKER_URL = "tcp://";
	public static final String clientId = "android-client1";
	public static final String TOPIC = "mouse";
	private MqttClient mqttClient;

	public static final String SERVICE_CLASSNAME = "de.eclipsemagazin.mqtt.push.MQTTService";
	private Button button;
	private TextView text;
	private EditText IP1;
	private EditText IP2;
	private EditText IP3;
	private EditText IP4;

	private SensorManager mSensorManager;
	private Sensor mLight;
	private Sensor mMagn;

	private float[] mGravity;
	private float[] mMagnetic;
	private float[] temp;
	private float[] R1;
	private Double degrees;

	//Passados para o service
	public static float[] values;
	public static boolean isCharging;
	public static float batteryPct;
	public static String system;
	public static String info;
	public static String internet;
	public static String notifications;
	public static boolean zero = false;
	public static boolean um = false;
	public static boolean dois = false;
	public static boolean tres = false;	
	
	private ImageView cima;
	private ImageView baixo;
	private ImageView esquerda;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		IP1 = (EditText) findViewById(R.id.editText1);
		IP2 = (EditText) findViewById(R.id.editText2);
		IP3 = (EditText) findViewById(R.id.editText3);
		IP4 = (EditText) findViewById(R.id.editText4);
		
		button = (Button) findViewById(R.id.button1);
		updateButton();

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMagn = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		String serviceName = Context.TELEPHONY_SERVICE;
		TelephonyManager m_telephonyManager = (TelephonyManager) getSystemService(serviceName);
		String IMEI,IMSI;
		IMEI = m_telephonyManager.getDeviceId();
		IMSI = m_telephonyManager.getSubscriberId();

		ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null &&
				activeNetwork.isConnectedOrConnecting();
		boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;

		Display display = getWindowManager().getDefaultDisplay(); 
		int width = display.getWidth();
		int height = display.getHeight();

		system = System.getProperty("os.version");

		info = width + " " + height + " " + m_telephonyManager.getNetworkCountryIso() + " " + IMEI + " " + IMSI;

		internet = String.valueOf(isConnected) + " " +  String.valueOf(isWiFi);
		
		cima = (ImageView) findViewById(R.id.ImageView01);
		esquerda = (ImageView) findViewById(R.id.ImageView02);
		baixo = (ImageView) findViewById(R.id.ImageView03);
		
		cima.setRotation(270);
		esquerda.setRotation(180);
		baixo.setRotation(90);

	}

	@Override
	protected void onResume() {
		super.onResume();
		updateButton();
		mSensorManager.registerListener(this, mLight, 500000);
		mSensorManager.registerListener(this, mMagn, 500000);
		registerReceiver(this.batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			mqttClient.disconnect();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		unregisterReceiver(this.batteryInfoReceiver);
	}

	private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
					status == BatteryManager.BATTERY_STATUS_FULL;
			int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

			batteryPct = level / (float)scale;
		}
	};

	private void updateButton() {
		if (serviceIsRunning()) {
			button.setText("Stop Service");
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					button.setText("Start Service");
					stopBlackIceService();
					updateButton();
				}
			});

		} else {
			button.setText("Start Service");
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					button.setText("Stop Service");
					BROKER_URL += IP1.getText().toString() + "." + IP2.getText().toString() +
							"." + IP3.getText().toString() + "." + IP4.getText().toString() +
							":1883";
					mouseConnect();
					startBlackIceService();
					updateButton();
				}
			});
		}
	}
	
	private void mouseConnect() {
		try {
			mqttClient = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		new Thread(new Runnable(){
			public void run() {
					try {
						mqttClient.connect();
					} catch (MqttSecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (MqttException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		}).start();
	}

	private void startBlackIceService() {

		final Intent intent = new Intent(this, MQTTService.class);
		startService(intent);
	}

	private void stopBlackIceService() {

		final Intent intent = new Intent(this, MQTTService.class);
		stopService(intent);
	}

	private boolean serviceIsRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (SERVICE_CLASSNAME.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch(event.sensor.getType()) {

		case Sensor.TYPE_ACCELEROMETER:
			mGravity = event.values.clone();
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			mMagnetic = event.values.clone();
			break;
		default:
			return;
		}
		if(mGravity != null && mMagnetic != null) {
			getDirection();
		}
	}

	private void getDirection() 
	{

		temp = new float[9];
		R1 = new float[9];
		//Load rotation matrix into R
		SensorManager.getRotationMatrix(temp, null,
				mGravity, mMagnetic);

		//Remap to camera's point-of-view
		SensorManager.remapCoordinateSystem(temp,
				SensorManager.AXIS_X,
				SensorManager.AXIS_Z, R1);

		//Return the orientation values
		values = new float[3];
		SensorManager.getOrientation(R1, values);

		//Convert to degrees
		for (int i=0; i < values.length; i++) {
			degrees = (values[i] * 180) / Math.PI;
			values[i] = degrees.floatValue();
		}

	}
	
	public void mudarmouse(View v) {
		switch(v.getId()) {
		case(R.id.ImageView01):
			zero = true;
			break;
		case(R.id.ImageView02):
			tres = true;
			break;
		case(R.id.ImageView03):
			dois = true;
			break;
		case(R.id.imageView1):
			um = true;
			break;
		}
		try {
			mqttClient.getTopic(TOPIC).publish(new MqttMessage((zero + " " + um + " " + dois + " " + tres).getBytes()));
		} catch (MqttPersistenceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mudarParaFalso();
	}
	
	public static void mudarParaFalso() {
		zero = false;
		um = false;
		dois = false;
		tres = false;
	}
}
