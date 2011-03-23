package ch.vd.uniregctb.common;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.util.Comparator;

import org.apache.commons.beanutils.PropertyUtils;

public class BaseComparator<T> implements Comparator<T> {

    /**
     *
     */
    private static final Integer FACTOR_DEFAULT = new Integer(1);

    /**
     *
     */
    private static final Integer FACTOR_INVERSE = new Integer(-1);

    /**
     * Use this collator.
     */
    private Collator collator;

    /**
     * les noms des propriétés
     */
    private final String[] properties;

    /**
     * facteur multiplicateur pour l'invertion de l'ordre.
     */
    private Integer[] factor;

    /**
     * Default constructor.
     *
     * @param properties paths des propriétés
     * @param ascending ascending
     */
    public BaseComparator(String[] properties, Boolean[] ascending) {
        if (properties.length != ascending.length) {
            throw new IllegalArgumentException();
        }
        this.collator = Collator.getInstance();
        collator.setStrength(Collator.PRIMARY); // ignore case and accents
        this.properties = properties;
        factor = new Integer[ascending.length];
        for (int i = 0; i < ascending.length; i++) {
            factor[i] = FACTOR_DEFAULT;
            if (!ascending[i].booleanValue()) {
                factor[i] = FACTOR_INVERSE;
            }
        }

    }

    /**
     * Default constructor.
     *
     * @param property path de la propriété
     * @param ascending ascending
     */
    public BaseComparator(String property, boolean ascending) {
        this.collator = Collator.getInstance();
        collator.setStrength(Collator.PRIMARY); // ignore case and accents
        this.properties = new String[] { property };
        if (!ascending) {
            factor = new Integer[] { FACTOR_INVERSE };
        }
    }

    /**
     * Compares two given objects. Not comparable objects are compared using their string representation. String
     * comparisons are done using a Collator.
     *
     * @param object1 first parameter
     * @param object2 second parameter
     * @return the value
     */
    public final int compare(T object1, T object2) {
        int returnValue = 0;

        Object val1 = null;
        Object val2 = null;
        for (int i = 0; i < properties.length; i++) {
            String property = properties[i];
            val1 = getProperty(object1, property);
            val2 = getProperty(object2, property);
            returnValue = checkNullsAndCompare(val1, val2);
            if (returnValue != 0) {
                return returnValue * factor[i].intValue();
            }
        }
        return returnValue;
    }

    /**
     * <p>
     * Return the value of the specified property of the specified bean, no matter which property reference format is
     * used, with no type conversions.
     * </p>
     *
     * @param bean bean.
     * @param prop perty name.
     *            <p>
     *            For more details see <code>PropertyUtilsBean</code>.
     *            </p>
     * @return Return the value of the specified property of the specified bean.
     * @see PropertyUtils#getProperty(Object, String)
     */
    protected final Object getProperty(Object bean, String prop) {
        try {
            return PropertyUtils.getProperty(bean, prop);
        } catch (IllegalAccessException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Compares two given objects, and handles the case where nulls are present.
     *
     * @param object1 first object to compare
     * @param object2 second object to compare
     * @return int result
     */
    @SuppressWarnings("unchecked")
    protected final int checkNullsAndCompare(Object object1, Object object2) {
        int returnValue;
        if (object1 == null && object2 != null) {
            returnValue = -1;
        } else if (object1 != null && object2 == null) {
            returnValue = 1;
        } else if (object1 == null && object2 == null) {
            returnValue = 0;
        } else {
            if (object1 instanceof String && object2 instanceof String) {
                returnValue = collator.compare(object1, object2);
            } else if (object1 instanceof Comparable && object2 instanceof Comparable) {
                returnValue = ((Comparable) object1).compareTo(object2);
            } else {
                returnValue = collator.compare(object1.toString(), object2.toString());
            }
        }
        return returnValue;
    }

}