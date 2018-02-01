package ch.vd.unireg.load;

import java.util.Arrays;
import java.util.Iterator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Classe utilisée pour décrire chaque appel
 */
public final class MethodCallDescriptor {

	private final String methodName;
	private final String[] parameterNames;
	private final Object[] parameterValues;

	public MethodCallDescriptor(String methodName) {
		this.methodName = methodName;
		this.parameterNames = null;
		this.parameterValues = null;
	}

	public MethodCallDescriptor(String methodName, String parameterName, Object parameterValue) {
		this(methodName, buildArray(parameterName), buildArray(parameterValue));
	}

	public MethodCallDescriptor(String methodName, String parameterName1, Object parameterValue1, String parameterName2, Object parameterValue2) {
		this(methodName, buildArray(parameterName1, parameterName2), buildArray(parameterValue1, parameterValue2));
	}

	public MethodCallDescriptor(String methodName, String parameterName1, Object parameterValue1, String parameterName2, Object parameterValue2, String parameterName3, Object parameterValue3) {
		this(methodName, buildArray(parameterName1, parameterName2, parameterName3), buildArray(parameterValue1, parameterValue2, parameterValue3));
	}

	public MethodCallDescriptor(String methodName, String parameterName1, Object parameterValue1, String parameterName2, Object parameterValue2, String parameterName3, Object parameterValue3, String parameterName4, Object parameterValue4) {
		this(methodName, buildArray(parameterName1, parameterName2, parameterName3, parameterName4), buildArray(parameterValue1, parameterValue2, parameterValue3, parameterValue4));
	}

	public MethodCallDescriptor(String methodName, String[] parameterNames, Object[] parameterValues) {
		if (parameterNames != null || parameterValues != null) {
			final int nameSize = parameterNames != null ? parameterNames.length : 0;
			final int valueSize = parameterValues != null ? parameterValues.length : 0;
			if (nameSize != valueSize) {
				throw new IllegalArgumentException("Parameter names and values' sizes do not match!");
			}
		}

		this.methodName = methodName;
		this.parameterNames = parameterNames;
		this.parameterValues = parameterValues;
	}

	@SafeVarargs
	private static <T> T[] buildArray(T... values) {
		return values;
	}

	@Override
	public String toString() {
		if (parameterNames == null || parameterNames.length == 0) {
			return methodName;
		}
		else {
			final StringBuilder b = new StringBuilder();
			b.append(methodName).append("{");
			for (int i = 0 ; i < parameterNames.length ; ++ i) {
				b.append(parameterNames[i]).append("=");
				append(b, parameterValues[i]);
				if (i < parameterNames.length - 1) {
					b.append(", ");
				}
			}
			b.append("}");
			return b.toString();
		}
	}

	private static StringBuilder append(StringBuilder b, Object value) {
		if (value == null) {
			b.append("null");
		}
		else if (value instanceof Number || value instanceof Enum) {
			b.append(value);
		}
		else if (value instanceof RegDate) {
			b.append(RegDateHelper.dateToDisplayString((RegDate) value));
		}
		else if (value instanceof Object[]) {
			append(b, (Object[]) value);
		}
		else if (value instanceof Iterable) {
			append(b , (Iterable) value);
		}
		else {
			b.append("'").append(value).append("'");
		}
		return b;
	}

	private static StringBuilder append(StringBuilder b, Iterable<?> collection) {
		if (collection == null) {
			b.append("null");
		}
		else {
			b.append("[");
			final Iterator<?> iter = collection.iterator();
			while (iter.hasNext()) {
				append(b, iter.next());
				if (iter.hasNext()) {
					b.append(", ");
				}
			}
			b.append("]");
		}
		return b;
	}

	private static StringBuilder append(StringBuilder b, Object[] array) {
		return b.append(Arrays.toString(array));
	}
}
