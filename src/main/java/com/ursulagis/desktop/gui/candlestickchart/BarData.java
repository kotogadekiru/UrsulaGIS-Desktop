/*
Copyright 2014 Zoi Capital, LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.ursulagis.desktop.gui.candlestickchart;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.GregorianCalendar;

/**
 *
 * @author RobTerpilowski
 */
public class BarData implements Serializable, Comparable<BarData> {
        
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//public static long serialVersionUID = 1L;

    public static final Number NULL = -9D;
    public static final int OPEN = 1;
    public static final int HIGH = 2;
    public static final int LOW = 3;
    public static final int CLOSE = 4;

    public enum LENGTH_UNIT {

        TICK, SECOND, MINUTE, HOUR, DAY, WEEK, MONTH, YEAR
    };

//    protected Number open;
    protected BigDecimal formattedOpen;
    protected Number max;
    protected BigDecimal formattedHigh;
    protected Number min;
    protected BigDecimal formattedLow;
    protected Number average;
    protected BigDecimal formattedClose;
    protected Number volume = 0;
    protected Number openInterest = 0;
    protected int barLength = 1;
    protected Number elevacion;

	private int clase;
    //protected Logger logger = Logger.getLogger( Bar.class );

    public BarData() {
    }

    public BarData( Number elev, Number max, Number min, Number average, Number volume) {
        this.elevacion = elev;
      //  this.open = open;
       // this.formattedOpen = format(open);
        this.average = average;
        this.formattedClose = format(average);
        this.min = min;
        this.formattedLow = format(min);
        this.max = max;
        this.formattedHigh = format(max);
        this.volume = volume;
    }

    
    /**
     * Creates a new instance of a Bar
     *
     * @param date The date of this bar.
     * @param open The open price.
     * @param max The high price.
     * @param min The low price.
     * @param average The closing price.
     * @param volume The volume for the bar.
     * @param openInterest The open interest for the bar.
     */
    public BarData(Number elev, Number max, Number min, Number average, long volume, long openInterest) {
        this(elev, max, min, average, volume);
        this.openInterest = openInterest;
    }//constructor()

    public Number getElevacion() {
        return elevacion;
    }

    public void setElevacion(Number dateTime) {
        this.elevacion = dateTime;
    }

//    /**
//     * @return the open price of this bar.
//     */
//    public Number getOpen() {
//        return open;
//    }

    /**
     * @return the High price of this bar.
     */
    public Number getMax() {
        return max;
    }

    /*
     * @return the Low price of this Bar.
     */
    public Number getMin() {
        return min;
    }

    /**
     * @return the close price for this bar.
     */
    public Number getAverage() {
        return average;
    }

    /**
     * @return the Volume for this bar.
     */
    public Number getVolume() {
        return volume;
    }

    /**
     * @return the open interest for this bar.
     */
    public Number getOpenInterest() {
        return openInterest;
    }

    /**
     * Sets the open price for this bar.
     *
     * @param open The open price for this bar.
     */
//    public void setOpen(Number open) {
//        this.open = open;
//    }

    /**
     * Sets the high price for this bar.
     *
     * @param high The high price for this bar.
     */
    public void setMax(Number high) {
        this.max = high;
    }

    /**
     * Sets the low price for this bar.
     *
     * @param low The low price for this bar.
     */
    public void setMin(Number low) {
        this.min = low;
    }

    /**
     * Sets the closing price for this bar.
     *
     * @param average The closing price for this bar.
     */
    public void setAverage(Number average) {
        this.average = average;
    }

    /**
     * Sets the volume for this bar.
     *
     * @param volume Sets the volume for this bar.
     */
    public void setVolume(Number volume) {
        this.volume = volume;
    }
    
    
    /**
     * Updates the last price, adjusting the high and low
     * @param close The last price
     */
    public void update( Number close ) {
        if( close.doubleValue() > max.doubleValue() ) {
            max = close;
        }
        
        if( close.doubleValue() < min.doubleValue() ) {
            min = close;
        }
        this.average = close;
    }
    

    /**
     * Sets the open interest for this bar.
     *
     * @param openInterest The open interest for this bar.
     */
    public void setOpenInterest(long openInterest) {
        this.openInterest = openInterest;
    }
    
    protected BigDecimal format( Number price ) {
        return BigDecimal.ZERO;
    }
    

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Elevacion: ").append(elevacion);
     //   sb.append(" Open: ").append(open);
        sb.append(" Max: ").append(max);
        sb.append(" Min: ").append(min);
        sb.append(" Average: ").append(average);
        sb.append(" Volume: ").append(volume);
     //   sb.append(" Open Int ").append(openInterest);

        return sb.toString();
    }//toString()

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        long temp;
        temp = average.longValue();
        result = PRIME * result + (int) (temp ^ (temp >>> 32));
        temp = max.longValue();
        result = PRIME * result + (int) (temp ^ (temp >>> 32));
        temp = min.longValue();
        result = PRIME * result + (int) (temp ^ (temp >>> 32));
     //   temp = open.longValue();
    //    result = PRIME * result + (int) (temp ^ (temp >>> 32));
        temp = openInterest.longValue();
        result = PRIME * result + (int) (temp ^ (temp >>> 32));
        result = PRIME * result + ((elevacion == null) ? 0 : elevacion.hashCode());
        result = PRIME * result + ((volume == null) ? 0 : volume.hashCode());
        return result;
    }
    
    @Override
    public int compareTo(BarData o){
    
    	try{
    		BarData b = (BarData)o;
    		return Double.compare(this.getElevacion().doubleValue(), b.getElevacion().doubleValue());
    	}catch(ClassCastException e){
    	e.printStackTrace();
    		return -2;
    	}
    	
    	
    }
    

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BarData other = (BarData) obj;
        if (average != other.average) {
            return false;
        }
        if (max != other.max ) {
            return false;
        }
        if (min != other.min) {
            return false;
        }
//        if (open != other.open) {
//            return false;
//        }
        if (openInterest != other.openInterest) {
            return false;
        }
        if (elevacion == null) {
            if (other.elevacion != null) {
                return false;
            }
        } else if (!elevacion.equals(other.elevacion)) {
            return false;
        }
        if (volume != other.volume) {
            return false;
        }
        return true;
    }

	public void setClase(int index) {
		this.clase=index;
		// TODO Auto-generated method stub
		
	}
	public int getClase(){
		return this.clase;
	}
    
}
