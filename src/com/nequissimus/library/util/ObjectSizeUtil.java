package com.nequissimus.library.util;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * This utility can check an object's size.<br />
 * This class needs to be references as a javaagent in your JVM parameters<br />
 * e.g.<br />
 * <i>-javaagent:/PATH/TO/JAR/ObjectSize.jar</i>
 * @author Tim Steinbach
 */
public final class ObjectSizeUtil {

    /**
     * Instrumentation set by javaagent flag.
     */
    private static Instrumentation instrument = null;

    /**
     * Set of already checked objects.
     */
    private static final Set<Object> CHECKED_OBJECTS =
        new HashSet<Object>();

    /**
     * Hide constructor.
     */
    private ObjectSizeUtil() {

    }

    /**
     * Check the given object and all objects referenced.<br />
     * Only unique objects will be checked, so that there will be no infinite
     * loop.
     * @param obj Object to be checked
     * @return Object size in bytes
     */
    public static long getDeepObjectSize(final Object obj) {

        ObjectSizeUtil.CHECKED_OBJECTS.clear();

        final Stack<Object> stack = new Stack<Object>();
        stack.push(obj);

        long size = 0L;

        while (!stack.isEmpty()) {

            final Object tmp = stack.pop();

            if ((null != tmp)
                && (!ObjectSizeUtil.CHECKED_OBJECTS.contains(tmp))) {

                size += ObjectSizeUtil.getObjectSize(tmp);
                ObjectSizeUtil.CHECKED_OBJECTS.add(tmp);

                Class<? extends Object> c = tmp.getClass();
                final Class<?> comp = c.getComponentType();

                // Check this object (which may be an array)
                if ((null != comp) && (!comp.isPrimitive())) {

                    final Object[] array = (Object[]) tmp;

                    for (final Object object : array) {

                        stack.push(object);

                    }

                }

                // Check all objects/variables referenced in super classes
                while (null != c) {

                    final Field[] fields = c.getDeclaredFields();

                    for (final Field field : fields) {

                        if (!field.isAccessible()) {
                            field.setAccessible(true);
                        }

                        try {

                            final Object f = field.get(tmp);

                            if (null != f) {
                                stack.push(f);
                            }

                        } catch (final Exception e) {
                            // Keep going
                        }

                    }

                    c = c.getSuperclass();

                }

            }

        }

        return size;

    }

    /**
     * Check the object itself.
     * @param obj Object to be checked
     * @return Object size
     */
    public static long getObjectSize(final Object obj) {

        if (null == ObjectSizeUtil.instrument) {
            return 0;
        }

        return ObjectSizeUtil.instrument.getObjectSize(obj);

    }

    /**
     * Method called by javaagent parameter.
     * @param args Arguments
     * @param inst Instrumentation
     */
    public static void
        premain(final String args, final Instrumentation inst) {

        ObjectSizeUtil.instrument = inst;

    }

}
