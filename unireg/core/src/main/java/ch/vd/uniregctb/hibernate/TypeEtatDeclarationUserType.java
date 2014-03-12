package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.TypeEtatDeclaration;

/**
 * Classe de transtypage pour Hibernate : TypeEtatDocument <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class TypeEtatDeclarationUserType extends EnumUserType<TypeEtatDeclaration> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TypeEtatDeclarationUserType() {
		super(TypeEtatDeclaration.class);
	}

}
