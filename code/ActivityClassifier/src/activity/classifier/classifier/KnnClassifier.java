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

package activity.classifier.classifier;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import activity.classifier.common.Constants;
import activity.classifier.utils.CalcStatistics;
import activity.classifier.utils.FeatureExtractor;
import android.util.Log;

/**
 * Extracts basic features and applies a K-Nearest Network algorithm to an
 * array of data in order to determine the classification. The data consists
 * of two interleaved data sets, and each set has two features extracted -
 * the range and the mean.
 * 
 * @author chris
 */
public class KnnClassifier implements Classifier {

    private final Set<Map.Entry<Float[], String>> model;
    
    /**
     * {@link FeatureExtractor} instance to extract features from samples.
     */
    private FeatureExtractor featureExtractor;
    
    /**
     * Set the clustered data set for classification.
     * @param model clustered data set
     */
    public KnnClassifier(final Set<Entry<Float[], String>> model) {
        this.model = model;
        this.featureExtractor = new FeatureExtractor(Constants.NUM_OF_SAMPLES_PER_BATCH);
    }

    /* (non-Javadoc)
	 * @see activity.classifier.classifier.Classifier#classifyRotated(float[][])
	 */
    @Override
	synchronized
    public String classifyRotated(final float[][] data) {
    	return internClassify(featureExtractor.extractRotated(data, 0));
    }
    
    private String internClassify(float[] features) {
    	//Log.v(Constants.DEBUG_TAG, "Classifier.classify: "+Arrays.toString(features));
    	
    	float temp;
        float bestDistance = Float.MAX_VALUE;
        String bestActivity = "UNCLASSIFIED/UNKNOWN";

        /*
         *  Compare between the points from the sample data and the points from the clustered data set.
         *  Get the closest points in the clustered data set, and classify the activity.
         */
        //	TODO: This doesn't have to be iterative (linear). It can be optimized using windows
        //			to cut out most of the checking.
        for (Map.Entry<Float[], String> entry : model) {
        	String activity = entry.getValue();
        	Float[] activityFeatures = entry.getKey();
        	
            float distance = 0;

            for (int i = 0; i < features.length; i++) {
            	temp = features[i] - activityFeatures[i];
                distance += temp*temp;
            }

            if (distance < bestDistance) {
                bestDistance = distance;
                bestActivity = entry.getValue();
            }
        }
        
        Log.v(Constants.DEBUG_TAG, "Best Activity: '"+bestActivity+"' by "+bestDistance);

        return bestActivity;
    }
}
