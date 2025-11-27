package com.ursulagis.desktop.utils;

import java.util.*;
public class CustomProperties extends Properties {
	private static final long serialVersionUID = 1L;
	private final LinkedHashSet<Object> keyOrder = new LinkedHashSet<>();

	@Override
	public synchronized Enumeration<Object> keys() {
		return Collections.enumeration(keyOrder);
	}

	@Override
	public synchronized Object put(Object key, Object value) {
		keyOrder.add(key);
		return super.put(key, value);
	}
}
