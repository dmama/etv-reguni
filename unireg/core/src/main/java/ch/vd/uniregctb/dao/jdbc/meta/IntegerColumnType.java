package ch.vd.uniregctb.dao.jdbc.meta;

import java.sql.Types;

public class IntegerColumnType extends ColumnType {
	public IntegerColumnType() {
		super(Integer.class, Types.INTEGER);
	}
}
