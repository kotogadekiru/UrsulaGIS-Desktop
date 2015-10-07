package tasks;

import java.util.ArrayList;
import java.util.Comparator;

import javafx.scene.paint.Color;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.Classifier;
import org.geotools.styling.FeatureTypeStyle;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.PropertyName;

public class NaturalBreaks {
	
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
