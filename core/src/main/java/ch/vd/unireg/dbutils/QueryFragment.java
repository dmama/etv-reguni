package ch.vd.unireg.dbutils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.query.Query;

import ch.vd.unireg.common.HibernateQueryHelper;

/**
 * Cette classe permet d'encapsuler un fragment (ou la totalité) d'une requête sql avec les paramètres dynamiques à utiliser.
 */
public class QueryFragment {

	private final StringBuilder query = new StringBuilder();
	private final Map<String, Object> params = new HashMap<>();

	public QueryFragment() {
	}

	public QueryFragment(String query) {
		this.query.append(query);
	}

	public QueryFragment(String query, Map<String, ?> params) {
		this.query.append(query);
		this.params.putAll(params);
	}

	public QueryFragment(String query, String paramName, Object paramValue) {
		this.query.append(query);
		this.params.put(paramName, paramValue);
	}

	public QueryFragment(QueryFragment right) {
		this.query.append(right.query);
		this.params.putAll(right.params);
	}

	public QueryFragment add(String query) {
		return add(new QueryFragment(query));
	}

	public QueryFragment add(String query, String paramName, Object paramValue) {
		return add(new QueryFragment(query, paramName, paramValue));
	}

	public QueryFragment add(String query, Map<String, ?> params) {
		return add(new QueryFragment(query, params));
	}

	public QueryFragment add(QueryFragment fragment) {
		if (this.query.length() > 0 && fragment.query.length() > 0) {
			this.query.append(" ");     // separator
		}
		this.query.append(fragment.query);
		mergeParameters(fragment.params);
		return this;
	}

	public String getQuery() {
		return query.toString();
	}

	public Map<String, Object> getParams() {
		return Collections.unmodifiableMap(params);
	}

	/**
	 * Crée un objet <i>query</i> à partir du fragment de requête SQL courant et rempli avec les paramètres courants.
	 *
	 * @param session la session à utiliser pour créer la requête.
	 * @return la requête créée
	 */
	public Query createQuery(Session session) {
		final Query queryObject = session.createQuery(query.toString());
		HibernateQueryHelper.assignNamedParameterValues(queryObject, params);
		return queryObject;
	}

	private void mergeParameters(Map<String, ?> newParameters) {
		for (Map.Entry<String, ?> newEntry : newParameters.entrySet()) {
			final String parameterName = newEntry.getKey();
			if (this.params.containsKey(parameterName)) {
				final Object oldValue = this.params.get(parameterName);
				final Object newValue = newEntry.getValue();
				if (oldValue != newValue && (oldValue == null || !oldValue.equals(newValue))) {
					throw new IllegalArgumentException("Paramètre '" + parameterName + "' utilisé avec au moins deux valeurs différentes : '" + oldValue + "' et '" + newValue + "'");
				}
			}
			else {
				this.params.put(newEntry.getKey(), newEntry.getValue());
			}
		}
	}
}
