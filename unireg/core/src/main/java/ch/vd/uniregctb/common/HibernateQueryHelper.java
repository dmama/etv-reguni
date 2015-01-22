package ch.vd.uniregctb.common;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.hibernate.Query;
import org.jetbrains.annotations.Nullable;

/**
 * Quelques méthodes utiles autour des queries Hibernate
 */
public abstract class HibernateQueryHelper {

	/**
	 * Assigns the given named parameters to the query
	 * @param query query
	 * @param namedParameters map of named parameters
	 */
	public static void assignNamedParameterValues(Query query, @Nullable Map<String, ?> namedParameters) {
		if (namedParameters != null && !namedParameters.isEmpty()) {
			for (Map.Entry<String, ?> paramEntry : namedParameters.entrySet()) {
				final Object value = paramEntry.getValue();
				if (value instanceof Collection) {
					query.setParameterList(paramEntry.getKey(), (Collection) value);
				}
				else if (value instanceof Date) {
					query.setTimestamp(paramEntry.getKey(), (Date) value);
				}
				else {
					query.setParameter(paramEntry.getKey(), value);
				}
			}
		}
	}

}
