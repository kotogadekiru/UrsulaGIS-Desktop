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
package mmg.gui.candlestickchart;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.GregorianCalendar;

/**
 *
 * @author RobTerpilowski
 */
public class BarData implements Serializable {
        
    public static long serialVersionUID = 1L;

    public static final Number NULL = -9D;
    public static final int OPEN = 1;
    public static final int HIGH = 2;
    public static final int LOW = 3;
    public static final int CLOSE = 4;

    public enum LENGTH_UNIT {

        TICK, SECOND, MINUTE, HOUR, DAY, WEEK, MONTH, YEAR
    };

    protected Number open;
    protected BigDecimal formattedOpen;
    protected Number high;
    protected BigDecimal formattedHigh;
    protected Number low;
    protected BigDecimal formattedLow;
    protected Number close;
    protected BigDecimal formattedClose;
    protected Number volume = 0;
    protected Number openInterest = 0;
    protected int barLength = 1;
    protected Number rinde;
    //protected Logger logger = Logger.getLogger( Bar.class );

    public BarData() {
    }

    public BarData( Number rinde, Number open, Number high, Number low, Number close, long volume) {
        this.rinde = rinde;
        this.open = open;
        this.formattedOpen = format(open);
        this.close = close;
        this.formattedClose = format(close);
        this.low = low;
        this.formattedLow = format(low);
        this.high = high;
        this.formattedHigh = format(high);
        this.volume = volume;
    }

    
    /**
     * Creates a new instance of a Bar
     *
     * @param date The date of this bar.
     * @param open The open price.
     * @param high The high price.
     * @param low The low price.
     * @param close The closing price.
     * @param volume The volume for the bar.
     * @param openInterest The open interest for the bar.
     */
    public BarData(Number rinde, Number open, Number high, Number low, Number close, long volume, long openInterest) {
        this(rinde, open, high, low, close, volume);
        this.openInterest = openInterest;
    }//constructor()

    public Number getRinde() {
        return rinde;
    }

    public void setRinde(Number dateTime) {
        this.rinde = dateTime;
    }

    /**
     * @return the open price of this bar.
     */
    public Number getOpen() {
        return open;
    }

    /**
     * @return the High price of this bar.
     */
    public Number getHigh() {
        return high;
    }

    /*
     * @return the Low price of this Bar.
     */
    public Number getLow() {
        return low;
    }

    /**
     * @return the close price for this bar.
     */
    public Number getClose() {
        return close;
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
    public void setOpen(Number open) {
        this.open = open;
    }

    /**
     * Sets the high price for this bar.
     *
     * @param high The high price for this bar.
     */
    public void setHigh(Number high) {
        this.high = high;
    }

    /**
     * Sets the low price for this bar.
     *
     * @param low The low price for this bar.
     */
    public void setLow(Number low) {
        this.low = low;
    }

    /**
     * Sets the closing price for this bar.
     *
     * @param close The closing price for this bar.
     */
    public void setClose(Number close) {
        this.close = close;
    }

    /**
     * Sets the volume for this bar.
     *
     * @param volume Sets the volume for this bar.
     */
    public void setVolume(long volume) {
        this.volume = volume;
    }
    
    
    /**
     * Updates the last price, adjusting the high and low
     * @param close The last price
     */
    public void update( Number close ) {
        if( close.doubleValue() > high.doubleValue() ) {
            high = close;
        }
        
        if( close.doubleValue() < low.doubleValue() ) {
            low = close;
        }
        this.close = close;
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
        sb.append("Rinde: ").append(rinde);
        sb.append(" Open: ").append(open);
        sb.append(" High: ").append(high);
        sb.append(" Low: ").append(low);
        sb.append(" Close: ").append(close);
        sb.append(" Volume: ").append(volume);
        sb.append(" Open Int ").append(openInterest);

        return sb.toString();
    }//toString()

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        long temp;
        temp = close.longValue();
        result = PRIME * result + (int) (temp ^ (temp >>> 32));
        temp = high.longValue();
        result = PRIME * result + (int) (temp ^ (temp >>> 32));
        temp = low.longValue();
        result = PRIME * result + (int) (temp ^ (temp >>> 32));
        temp = open.longValue();
        result = PRIME * result + (int) (temp ^ (temp >>> 32));
        temp = openInterest.longValue();
        result = PRIME * result + (int) (temp ^ (temp >>> 32));
        result = PRIME * result + ((rinde == null) ? 0 : rinde.hashCode());
        result = PRIME * result + ((volume == null) ? 0 : volume.hashCode());
        return result;
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
        if (close != other.close) {
            return false;
        }
        if (high != other.high ) {
            return false;
        }
        if (low != other.low) {
            return false;
        }
        if (open != other.open) {
            return false;
        }
        if (openInterest != other.openInterest) {
            return false;
        }
        if (rinde == null) {
            if (other.rinde != null) {
                return false;
            }
        } else if (!rinde.equals(other.rinde)) {
            return false;
        }
        if (volume != other.volume) {
            return false;
        }
        return true;
    }
    
}
