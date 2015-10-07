package gisUI;

import javafx.scene.paint.Color;

public class SimpleColorGenerator {
        private static final int NR255 = 255;

        private int r;
        private int g;
        private int b;

        public SimpleColorGenerator() {
                this(0, NR255, 0);
        }

        /**
         * Generates color using r,g,b values
         * 
         * @param red
         * @param green
         * @param blue
         */
        public SimpleColorGenerator(int red, int green, int blue) {
                this.r = red;
                this.g = green;
                this.b = blue;
        }

        /**
         * Gets the next Color.
         * 
         * @return a Color
         */
        public Color nextColor() {
                nextRGB();
                return makeColor();
        }

        /**
         * Gets the next color after this increment. Use 256 for prime colors only.
         */
        public Color nextColor(int jump) {
                for (int i = 0; i < jump; i++) {
                        nextRGB();
                }
                return makeColor();
        }

        private void nextRGB() {
                if (r == NR255 && g < NR255 && b == 0) {
                        g++;
                }
                if (g == NR255 && r > 0 && b == 0) {
                        r--;
                }
                if (g == NR255 && b < NR255 && r == 0) {
                        b++;
                }
                if (b == NR255 && g > 0 && r == 0) {
                        g--;
                }
                if (b == NR255 && r < NR255 && g == 0) {
                        r++;
                }
                if (r == NR255 && b > 0 && g == 0) {
                        b--;
                }
        }

        private Color makeColor() {        	
                return Color.color(r,g,b);
        }

}