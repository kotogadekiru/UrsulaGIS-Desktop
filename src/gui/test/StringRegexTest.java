package gui.test;

public class StringRegexTest {

	public static void main(String[] args) {
	String tiffFileName = "jag 21 sin bajo 23-12-20181023831360154302365.tif";
	String base = tiffFileName.split("\\(")[0];
	System.out.println("base: "+base);

	}

}
