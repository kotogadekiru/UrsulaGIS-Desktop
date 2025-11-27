package com.ursulagis.desktop.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;

import javafx.scene.paint.Color;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.filter.expression.PropertyName;

import org.locationtech.jts.geom.Geometry;

public class NaturalBreaks {
    private String amountCol ="";
    private int numClases=9;
    private double areaRef = 0;
	
    public NaturalBreaks(String amountColumn, int numClases,double a) {
        this.amountCol=amountColumn;
        this.numClases=numClases;
        this.areaRef = a;//0.7/ProyectionConstants.A_HAS();
    }
    
	/**
	 * @return int[] devuelve el numero del elemento maximo que pertenece a la clase
	 * @param list   com.sun.java.util.collections.ArrayList
	 * @param numclass  int  cantidad de clases
	 */
	public static Double[] getJenksBreaks(ArrayList<Double> list, int numclass) {
		// int numclass;
		int numdata = list.size();

		double[][] mat1 = new double[numdata + 1][numclass + 1];
		double[][] mat2 = new double[numdata + 1][numclass + 1];
		double[] st = new double[numdata];

		for (int i = 1; i <= numclass; i++) {
			mat1[1][i] = 1;
			mat2[1][i] = 0;
			for (int j = 2; j <= numdata; j++)
				mat2[j][i] = Double.MAX_VALUE;
		}
		
		double v = 0;
		for (int l = 2; l <= numdata; l++) {
			double s1 = 0;
			double s2 = 0;
			double w = 0;
			for (int m = 1; m <= l; m++) {
				int i3 = l - m + 1;

				double val = ((Double) list.get(i3 - 1)).doubleValue();

				s2 += val * val;
				s1 += val;

				w++;
				v = s2 - (s1 * s1) / w;
				int i4 = i3 - 1;
				if (i4 != 0) {
					for (int j = 2; j <= numclass; j++) {
						if (mat2[l][j] >= (v + mat2[i4][j - 1])) {
							mat1[l][j] = i3;
							mat2[l][j] = v + mat2[i4][j - 1];
						};
					};//fin del for j
				};
			};//fin del for m
			mat1[l][1] = 1;
			mat2[l][1] = v;
		};//fin del for l
	
		int k = numdata;

		int[] kclass = new int[numclass];

		kclass[numclass - 1] = list.size() - 1;

		for (int j = numclass; j >= 2; j--) {
		//	System.out.println("rank = " + mat1[k][j]);
			int id = (int) (mat1[k][j]) - 2;
		//	System.out.println("val = " + list.get(id));
			// System.out.println(mat2[k][j]);

			kclass[j - 2] = id;

			k = (int) mat1[k][j] - 1;

		};
		
		Double [] rangos = new Double[numclass];
		
		for(int index=0; index<numclass;index++){
			rangos[index]=list.get(kclass[index]);
		}
		return rangos;
	}

	 /**
     * This is based on James' GeoTools1 code which seems to be based on
     * http://lib.stat.cmu.edu/cmlib/src/cluster/fish.f
     * 
     * @param feature
     * @return a RangedClassifier
     */
    public Object calculate(SimpleFeatureCollection featureCollection) {    	
        SimpleFeatureIterator features = featureCollection.features();
        ArrayList<Double> data = new ArrayList<Double>();
        try {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                final Object result = feature.getAttribute(this.amountCol);//getParameters().get(0).evaluate(feature);
                
              //  logger.finest("importing " + result);
                if (result != null) {
                    final Double e = Double.parseDouble(result.toString());
                    if (!e.isInfinite() && !e.isNaN()) {
                    	Geometry g= (Geometry) feature.getDefaultGeometry();
                    	double times = Math.max(1, g.getArea()/this.areaRef);
                    	//System.out.println("multiplicando el feature "+ e+ " por " + times);
                    	  
                    	for(int t=0; t<times;t++) {
                    		data.add(Double.valueOf(e));
                    	}
                    }
                }
            }
        } catch (NumberFormatException e) {
            return null; // if it isn't a number what should we do?
        }
        features.close();
      
        Double[] breaks = QGISPrettyBreaks.calculateBreaks(0, 1000000,data, numClases+1);//.getJenksBreaks(data, this.numClases);

        Comparable[] localMin = new Comparable[numClases];
        Comparable[] localMax = new Comparable[numClases];
        
     
        for(int i=0;i<numClases;i++) {
        	  localMax[i] = breaks[i+1];
              localMin[i] = breaks[i];
        }
//        localMax[numClases-1] = breaks[breaks.length-1];
//        localMin[numClases-1] = breaks[breaks.length-1];
        System.out.println("localMax: "+Arrays.toString(localMax));
        System.out.println("localMin: "+Arrays.toString(localMin));
  
        return new RangedClassifier(localMin, localMax);
    }
	
    public Object evaluate(Object feature) {
        if (!(feature instanceof FeatureCollection)) {
            return null;
        }
        return calculate((SimpleFeatureCollection) feature);
    }

    
	class doubleComp implements Comparator<Object> {
		public int compare(Object a, Object b) {
			if (((Double) a).doubleValue() < ((Double) b).doubleValue())
				return -1;
			if (((Double) a).doubleValue() > ((Double) b).doubleValue())
				return 1;
			return 0;
		}
	}

}
