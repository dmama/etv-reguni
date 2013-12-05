package ch.vd.uniregctb.dbutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Cette classe permet d'encapsuler un fragment (ou la totalité) d'une requête sql avec les paramètres dynamiques à utiliser.
 */
public class QueryFragment {

	private final StringBuilder query = new StringBuilder();
	private final List<Object> params = new ArrayList<>();

	public QueryFragment() {
	}

	public QueryFragment(String query) {
		this.query.append(query);
	}

	public QueryFragment(String query, List<Object> params) {
		this.query.append(query);
		this.params.addAll(params);
	}

	public QueryFragment(QueryFragment right) {
		this.query.append(right.query);
		this.params.addAll(right.params);
	}

	public QueryFragment add(String query) {
		return add(new QueryFragment(query));
	}

	public QueryFragment add(String query, Object param) {
		return add(new QueryFragment(query, Arrays.asList(param)));
	}

	public QueryFragment add(String query, List<Object> params) {
		return add(new QueryFragment(query, params));
	}

	public QueryFragment add(QueryFragment fragment) {
		if (this.query.length() > 0 && fragment.query.length() > 0) {
			this.query.append(" ");     // separator
		}
		this.query.append(fragment.query);
		this.params.addAll(fragment.params);
		return this;
	}

	public String getQuery() {
		return query.toString();
	}

	public List<Object> getParams() {
		return Collections.unmodifiableList(params);
	}

	/**
	 * Crée un objet <i>query</i> à partir du fragment de requête SQL courant et rempli avec les paramètres courants.
	 *
	 * @param session la session à utiliser pour créer la requête.
	 * @return la requête créée
	 */
	public Query createQuery(Session session) {
		final Query queryObject = session.createQuery(query.toString());
		for (int i = 0; i < params.size(); i++) {
			final Object val = params.get(i);
			if (val instanceof Date) {
				queryObject.setTimestamp(i, (Date) val);
			}
			else {
				queryObject.setParameter(i, val);
			}
		}
		return queryObject;
	}
}
