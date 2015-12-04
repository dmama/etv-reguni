package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.TypeEvenementFiscal;

/**
 * Classe de transtypage pour Hibernate : TypeEvenementFiscal <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class TypeEvenementFiscalUserType extends EnumUserType<TypeEvenementFiscal> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TypeEvenementFiscalUserType() {
		super(TypeEvenementFiscal.class);
	}

}
