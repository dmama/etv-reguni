package ch.vd.uniregctb.dao.jdbc;

import java.util.Collection;

import ch.vd.registre.base.utils.Assert;

public abstract class JdbcDaoUtils {

	/**
	 * Taille maximale des clauses 'in' supportée par le driver Jdbc Oracle.
	 */
	public static final int MAX_IN_SIZE = 500;

	public static String buildInClause(Collection<Long> tiersId) {
		Assert.notNull(tiersId);
		Assert.isTrue(!tiersId.isEmpty());

		StringBuilder buf = new StringBuilder();
		buf.append('(');

		final int size = tiersId.size();
		int i = 0;
		for (Long id : tiersId) {
			buf.append(id);
			if (i++ < size - 1) {
				buf.append(", ");
			}
		}
		buf.append(')');

		return buf.toString();
	}

}
