package ch.vd.uniregctb.dao.jdbc.meta;

import java.sql.Types;

import ch.vd.uniregctb.dao.jdbc.meta.ColumnType;

public class StringColumnType extends ColumnType {
	public StringColumnType() {
		super(String.class, Types.VARCHAR);
	}
}
