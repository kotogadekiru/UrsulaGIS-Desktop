package com.ursulagis.desktop.utils;

import java.util.List;
import java.util.stream.Collectors;

public class Lists {
	public static String toString(List<?> l) {
		String separator = ", ";
		String toPrint = l.stream().map(o -> o.toString()).collect(Collectors.joining(separator));
		return toPrint;
	}
	
	public static void println(List<?> l) {
		System.out.println(toString(l));
	}
}
