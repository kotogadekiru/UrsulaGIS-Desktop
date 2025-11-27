package com.ursulagis.desktop.gui.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import javafx.scene.control.Tooltip;
import javafx.util.Duration;

public class TooltipUtil {
	/**
     * <p>
     * Tooltip behavior is controlled by a private class javafx.scene.control.Tooltip$TooltipBehavior.
     * All Tooltips share the same TooltipBehavior instance via a static private member BEHAVIOR, which
     * has default values of 1sec for opening, 5secs visible, and 200 ms close delay (if mouse exits from node before 5secs). 
     * 
     * The hack below constructs a custom instance of TooltipBehavior and replaces private member BEHAVIOR with
     * this custom instance.
     * </p>
     *
     */
public static void setupCustomTooltipBehavior(int openDelayInMillis, int visibleDurationInMillis, int closeDelayInMillis) {
        try {
             
            Class<?> TTBehaviourClass = null;
            Class<?>[] declaredClasses = Tooltip.class.getDeclaredClasses();
            for (Class<?> c:declaredClasses) {
                if (c.getCanonicalName().equals("javafx.scene.control.Tooltip.TooltipBehavior")) {
                    TTBehaviourClass = c;
                    break;
                }
            }
            if (TTBehaviourClass == null) {
                // abort
                return;
            }
            Constructor<?> constructor = TTBehaviourClass.getDeclaredConstructor(
                    Duration.class, Duration.class, Duration.class, boolean.class);
            if (constructor == null) {
                // abort
                return;
            }
            constructor.setAccessible(true);
            Object newTTBehaviour = constructor.newInstance(
                    new Duration(openDelayInMillis), new Duration(visibleDurationInMillis), 
                    new Duration(closeDelayInMillis), false);
            if (newTTBehaviour == null) {
                // abort
                return;
            }
            Field ttbehaviourField = Tooltip.class.getDeclaredField("BEHAVIOR");
            if (ttbehaviourField == null) {
                // abort
                return;
            }
            ttbehaviourField.setAccessible(true);
             
            // Cache the default behavior if needed.
            Object defaultTTBehavior = ttbehaviourField.get(Tooltip.class);
            ttbehaviourField.set(Tooltip.class, newTTBehaviour);
             
        } catch (Exception e) {
            System.out.println("Aborted setup due to error:" + e.getMessage());
        }
    }
}
