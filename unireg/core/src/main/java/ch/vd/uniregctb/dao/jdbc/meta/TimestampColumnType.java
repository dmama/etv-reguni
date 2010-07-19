package ch.vd.uniregctb.dao.jdbc.meta;

import java.sql.Timestamp;
import java.sql.Types;

import ch.vd.uniregctb.dao.jdbc.meta.ColumnType;

public class TimestampColumnType extends ColumnType {
	public TimestampColumnType() {
		super(Timestamp.class, Types.TIMESTAMP);
	}
}
