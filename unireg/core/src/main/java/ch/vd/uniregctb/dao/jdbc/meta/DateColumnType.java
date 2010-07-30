package ch.vd.uniregctb.dao.jdbc.meta;

import java.sql.Types;
import java.util.Date;

public class DateColumnType extends ColumnType {
	public DateColumnType() {
		super(Date.class, Types.TIMESTAMP);
	}
}
