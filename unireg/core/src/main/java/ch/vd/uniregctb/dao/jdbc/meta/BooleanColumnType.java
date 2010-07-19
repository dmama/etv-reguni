package ch.vd.uniregctb.dao.jdbc.meta;

import java.sql.Types;

public class BooleanColumnType extends ColumnType {
public BooleanColumnType() {
	super(Boolean.class, Types.BOOLEAN);
}
}