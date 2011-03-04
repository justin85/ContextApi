/*
 * Copyright (c) 2009-2010 Chris Smith
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package activity.classifier.accel.async;

import java.util.Arrays;

import activity.classifier.accel.SampleBatch;
import activity.classifier.common.Constants;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * An accelerometer reader which reads real data from the device's
 * accelerometer.
 *
 * @author chris
 */
public class AsyncRealAccelReader implements AsyncAccelReader {
	
	//	actually all we need is a buffer size of 2,
	//	to make sure that values are being written into one
	//	array while being read from another array, but we
	//	can keep 3 just in case the listener is filling
	//	faster than the samples are being read...
	private static final int BUFFER_SIZE = 3;

    private final SensorEventListener accelListener = new SensorEventListener() {

        /** {@inheritDoc} */
//        @Override
        public void onSensorChanged(final SensorEvent event) {
        	int nextValues = (currentValues + 1) % BUFFER_SIZE;
        	synchronized (values[nextValues]) {
            	values[nextValues][0] = event.values[SensorManager.DATA_X]; 
            	values[nextValues][1] = event.values[SensorManager.DATA_Y]; 
            	values[nextValues][2] = event.values[SensorManager.DATA_Z];
			}
        	currentValues = nextValues;
        	valuesAssigned = true;
        }

        /** {@inheritDoc} */
//        @Override
        public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
            // Don't really care
        }

    };

    float[][] values = new float[BUFFER_SIZE][3];
    boolean valuesAssigned = false;
    int currentValues = 0;
    private SensorManager manager;

    public AsyncRealAccelReader(final Context context) {
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    public void startSampling() {
        manager.registerListener(accelListener,
                manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void stopSampling() {
        manager.unregisterListener(accelListener);
    }

    public void assignSample(SampleBatch batch) {
    	//	sometimes values are requested before the first
    	//		accelerometer sensor change event has occurred,
    	//		so wait for the sensor to change.
    	if (!valuesAssigned) {
    		Log.v(Constants.DEBUG_TAG, "No values assigned yet from accelerometer, going to wait for values.");
	    	while (!valuesAssigned) {
	    		Thread.yield();
	    	}
    	}
    	int j = this.currentValues;
    	synchronized (this.values[j]) {
    		batch.assignSample(this.values[j]);
		}
    }

    @Override
    protected void finalize() throws Throwable {
    }

}
