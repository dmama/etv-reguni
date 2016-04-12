package ch.vd.uniregctb.hibernate.meta;

import java.math.BigDecimal;
import java.sql.Types;

public class BigDecimalPropertyType extends PropertyType {
	public BigDecimalPropertyType() {
		super(BigDecimal.class, Types.NUMERIC);
	}
}
