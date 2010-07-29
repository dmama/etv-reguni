package ch.vd.uniregctb.dao.jdbc.meta;

import java.sql.Types;

import ch.vd.uniregctb.dao.jdbc.meta.ColumnType;

public class LongColumnType extends ColumnType {
	public LongColumnType() {
		super(Long.class, Types.BIGINT);
	}
}
