package ch.vd.uniregctb.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.type.StandardBasicTypes;

/**
 * Dialecte pour Oracle10g qui mappe les String sur des NVARCHAR2 pour l'encodate unicode
 */
public class Oracle10gDialectWithNVarChar extends Oracle10gDialect {
	public Oracle10gDialectWithNVarChar() {
		registerHibernateType(Types.NVARCHAR, StandardBasicTypes.STRING.getName());
	}

	/**
	 * @see org.hibernate.dialect.Oracle9iDialect#registerCharacterTypeMappings()
	 */
	@Override
	protected void registerCharacterTypeMappings() {
		registerColumnType( Types.CHAR, "char(1 char)" );
//		registerColumnType( Types.VARCHAR, 4000, "varchar2($l char)" );
		registerColumnType( Types.VARCHAR, 4000, "nvarchar2($l)" );
		registerColumnType( Types.VARCHAR, "long" );
	}
}
