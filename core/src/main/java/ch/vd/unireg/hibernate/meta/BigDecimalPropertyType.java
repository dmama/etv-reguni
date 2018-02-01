package ch.vd.unireg.hibernate.meta;

import java.math.BigDecimal;

public class BigDecimalPropertyType extends PropertyType {
	public BigDecimalPropertyType() {
		super(BigDecimal.class, BigDecimal.class);
	}
}
