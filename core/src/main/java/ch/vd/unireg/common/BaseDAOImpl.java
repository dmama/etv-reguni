package ch.vd.unireg.common;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.FlushMode;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.unireg.hibernate.HibernateTemplate;

public abstract class BaseDAOImpl<T, PK extends Serializable> extends GenericDAOImpl<T, PK> {

	private HibernateTemplate hibernateTemplate;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	protected BaseDAOImpl(Class<T> persistentClass) {
		super(persistentClass);
	}

	public <U> List<U> find(String hql, @Nullable Map<String, ?> namedParams, @Nullable FlushMode flushModeOverride) {
		return hibernateTemplate.find(hql, namedParams, flushModeOverride);
	}

	public <U> U findUnique(String hql, @Nullable Map<String, ?> namedParams, @Nullable FlushMode flushModeOverride) {
		return hibernateTemplate.findUnique(hql, namedParams, flushModeOverride);
	}

	public <U> Iterator<U> iterate(String hql, @Nullable Map<String, ?> namedParams, @Nullable FlushMode flushModeOverride) {
		return hibernateTemplate.iterate(hql, namedParams, flushModeOverride);
	}

	public <U> List<U> find(String hql, @Nullable FlushMode flushModeOverride) {
		return hibernateTemplate.find(hql, flushModeOverride);
	}

	public <U> Iterator<U> iterate(String hql, @Nullable FlushMode flushModeOverride) {
		return hibernateTemplate.iterate(hql, flushModeOverride);
	}

	@SafeVarargs
	protected static <T> Map<String, T> buildNamedParameters(Pair<String, T>... params) {
		if (params == null || params.length == 0) {
			return null;
		}
		return buildNamedParameters(Arrays.asList(params));
	}

	protected static <T> Map<String, T> buildNamedParameters(Collection<Pair<String, T>> params) {
		if (params == null || params.isEmpty()) {
			return null;
		}

		final Map<String, T> map = new HashMap<>(params.size());
		for (Pair<String, T> param : params) {
			if (map.containsKey(param.getKey())) {
				throw new IllegalArgumentException("Parameter '" + param.getKey() + "' given more than once!");
			}
			map.put(param.getKey(), param.getValue());
		}
		return map;
	}
}
