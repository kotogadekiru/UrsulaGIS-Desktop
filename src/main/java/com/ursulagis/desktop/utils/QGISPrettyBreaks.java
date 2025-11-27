package com.ursulagis.desktop.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class QGISPrettyBreaks {
	static int mMaximumSize = 3000;
	public static Double[] calculateBreaks( double minimum, double maximum,    List<Double> values, int nclasses )	{
		mMaximumSize=values.size();
		  // Jenks Optimal (Natural Breaks) algorithm
		  // Based on the Jenks algorithm from the 'classInt' package available for
		  // the R statistical prgramming language, and from Python code from here:
		  // http://danieljlewis.org/2010/06/07/jenks-natural-breaks-algorithm-in-python/
		  // and is based on a JAVA and Fortran code available here:
		  // https://stat.ethz.ch/pipermail/r-sig-geo/2006-March/000811.html

		  // Returns class breaks such that classes are internally homogeneous while
		  // assuring heterogeneity among classes.

		  if ( values.size()==0 ){
			  Double[]  ret = new Double[1];
			  ret[0]=maximum;
		    return ret;
		  }

		  if ( nclasses <= 1 )
		  {
			  Double[]  ret = new Double[1];
			  ret[0]=maximum;
		    return ret;
		  }

		  if ( nclasses >= values.size() )
		  {
		    return values.toArray(new Double[values.size()]);
		  }

		  Double[] sample=new Double[7];
		  Double[] sorted=new Double[7];

		  // if we have lots of values, we need to take a random sample
		  if ( values.size() > mMaximumSize )  {
		    // for now, sample at least maximumSize values or a 10% sample, whichever
		    // is larger. This will produce a more representative sample for very large
		    // layers, but could end up being computationally intensive...
			  sample=new Double[Math.max( mMaximumSize, ( values.size() ) / 10 )];
		   // sample.resize( Math.max( mMaximumSize, ( values.size() ) / 10 ) );

		   // QgsDebugMsgLevel( QStringLiteral( "natural breaks (jenks) sample size: %1" ).arg( sample.size() ), 2 );
		   // QgsDebugMsgLevel( QStringLiteral( "values:%1" ).arg( values.size() ), 2 );

		    sample[ 0 ]= minimum;
		    sample[1]= maximum;
		   
		    values.sort(Comparator.naturalOrder());
		    sorted = values.toArray(sorted);
		   // std::sort( sorted.begin(), sorted.end() );

		    int j = -1;

		    // loop through all values in initial array
		    // skip the first value as it is a minimum one
		    // skip the last value as that one is a maximum one
		    // and those are already in the sample as items 0 and 1
		    for ( int i = 1; i < sorted.length - 2; i++ )
		    {
		      if ( ( i * ( mMaximumSize - 2 ) / ( sorted.length - 2 ) ) > j )
		      {
		        j++;
		        sample[ j + 2 ] = sorted[ i ];
		      }
		    }
		  }
		  else
		  {
			sample=new Double[values.size()];
		    sample = values.toArray(sample);
		  }

		   int n = sample.length;
		   Arrays.sort(sample);
		  // sort the sample values
		  //std::sort( sample.begin(), sample.end() );

		  int[][] matrixOne=new int[ n + 1 ][ nclasses + 1 ];
		  double[][] matrixTwo=new double[ n + 1 ][ nclasses + 1];
		 



		  for ( int i = 1; i <= nclasses; i++ )
		  {
		    matrixOne[0][i] = 1;
		    matrixOne[1][i] = 1;
		    matrixTwo[0][i] = 0.0;
		    for ( int j = 2; j <= n; j++ )
		    {
		      matrixTwo[j][i] = Double.MAX_VALUE;
		    }
		  }

		  for ( int l = 2; l <= n; l++ )
		  {
		    double s1 = 0.0;
		    double s2 = 0.0;
		    int w = 0;

		    double v = 0.0;

		    for ( int m = 1; m <= l; m++ )
		    {
		       int i3 = l - m + 1;

		       double val = sample[ i3 - 1 ];

		      s2 += val * val;
		      s1 += val;
		      w++;

		      v = s2 - ( s1 * s1 ) / ( w );
		       int i4 = i3 - 1;
		      if ( i4 != 0 )
		      {
		        for ( int j = 2; j <= nclasses; j++ )
		        {
		          if ( matrixTwo[l][j] >= v + matrixTwo[i4][j - 1] )
		          {
		            matrixOne[l][j] = i4;
		            matrixTwo[l][j] = v + matrixTwo[i4][j - 1];
		          }
		        }
		      }
		    }
		    matrixOne[l][1] = 1;
		    matrixTwo[l][1] = v;
		  }

		  Double[] breaks=new Double [ nclasses] ;
		  breaks[nclasses - 1] = sample[n - 1];

		  for ( int j = nclasses, k = n; j >= 2; j-- )
		  {
		     int id = matrixOne[k][j] - 1;
		    breaks[j - 2] = sample[id];
		    k = matrixOne[k][j] - 1;
		  }
//		 ArrayList<Double> ret = new ArrayList<Double>();//.toList();
//		 for(Double b :breaks) {
//			 ret.add(b);
//		 }
//		
		 
		  return breaks;
		}


}
