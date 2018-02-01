package ch.vd.unireg.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.metamodel.spi.TypeContributions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.StringNVarcharType;

/**
 * Dialecte pour Oracle10g qui mappe les String sur des NVARCHAR2 pour l'encodage unicode
 */
public class Oracle10gDialectWithNVarChar extends Oracle10gDialect {

	/**
	 * Classe de descriptiuon du mapping entre NVARCHAR et String qui supplante
	 * complètement le mapping par défaut (vers un VARCHAR)
	 */
	private static final class UniregStringNVarcharType extends StringNVarcharType {
		@Override
		protected boolean registerUnderJavaType() {
			return true;
		}
	}

	private static final UniregStringNVarcharType NVARCHAR_TYPE_INSTANCE = new UniregStringNVarcharType();

	public Oracle10gDialectWithNVarChar() {
		registerHibernateType(Types.NVARCHAR, StandardBasicTypes.STRING.getName());
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);
		typeContributions.contributeType(NVARCHAR_TYPE_INSTANCE);
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
		registerColumnType( Types.NVARCHAR, 4000, "nvarchar2($l)" );
		registerColumnType( Types.NVARCHAR, "long" );
	}
}
