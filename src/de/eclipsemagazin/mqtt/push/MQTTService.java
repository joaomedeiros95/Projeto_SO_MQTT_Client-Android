package de.eclipsemagazin.mqtt.push;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;

/**
 * @author João Eduardo Medeiros
 * @author Danilo Damasceno
 * @author Ciro Martins
 * @author Gustavo Henrique
 * 
 * @see Baseado em uma versão feita por @author Dominik Obermaier
 */
public class MQTTService extends Service {

	public static String BROKER_URL;
	public static final String clientId = "android-client";

	public static final String TOPIC = "information";
	private MqttClient mqttClient;


	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		
		BROKER_URL = BlackIceActivity.BROKER_URL;
		
		try {

		mqttClient = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());

		mqttClient.setCallback(new PushCallback(this));
		} catch (MqttException e) {
			Toast.makeText(getApplicationContext(), "Something went wrong!" + e.getMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
		new Thread(new Runnable(){
			public void run() {
				try {
					mqttClient.connect();

					//Subscribe to all subtopics of homeautomation
					//			mqttClient.subscribe(TOPIC);

				} catch (MqttException e) {
					Toast.makeText(getApplicationContext(), "Something went wrong!" + e.getMessage(), Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
				// TODO Auto-generated method stub
				while(true)
				{
					try {
						Thread.sleep(50) ;
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					enviarInfo();
				}

			}
		}).start();

		super.onStart(intent, startId);
	}

	@Override
	public void onDestroy() {
		try {
			mqttClient.disconnect(0);
		} catch (MqttException e) {
			Toast.makeText(getApplicationContext(), "Something went wrong!" + e.getMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}

	public void enviarInfo() {
		while (true) {
			try {
				mqttClient.getTopic(TOPIC).publish(new MqttMessage((BlackIceActivity.values[0] + " " + BlackIceActivity.values[1] + " " + 
						BlackIceActivity.values[2] + " " + BlackIceActivity.isCharging + " " 
						+ BlackIceActivity.batteryPct + " " + BlackIceActivity.system + " " + 
						BlackIceActivity.info + " " + BlackIceActivity.internet).getBytes()));
			} catch (MqttPersistenceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}
	}
}
