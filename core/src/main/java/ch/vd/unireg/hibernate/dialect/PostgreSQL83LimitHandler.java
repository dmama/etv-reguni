package ch.vd.unireg.hibernate.dialect;

import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;

/**
 * Limit handler spécialisé pour PostegreSQL (adapté de {@link org.hibernate.dialect.pagination.LegacyLimitHandler}).
 */
public class PostgreSQL83LimitHandler extends AbstractLimitHandler {
	public PostgreSQL83LimitHandler(String sql, RowSelection selection) {
		super(sql, selection);
	}

	public boolean supportsLimit() {
		return true;
	}

	public boolean supportsLimitOffset() {
		return true;
	}

	public boolean supportsVariableLimit() {
		return true;
	}

	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	public boolean bindLimitParametersFirst() {
		return false;
	}

	public boolean useMaxForLimit() {
		return false;
	}

	public boolean forceLimitUsage() {
		return false;
	}

	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		return zeroBasedFirstResult;
	}

	public String getProcessedSql() {
		boolean useLimitOffset = LimitHelper.hasFirstRow(selection) && LimitHelper.hasMaxRows(selection);
		final int offset = useLimitOffset ? LimitHelper.getFirstRow(selection) : 0;
		final boolean hasOffset = offset > 0 || forceLimitUsage();
		return sql + (hasOffset ? " limit ? offset ?" : " limit ?");
	}
}
