package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.TypeActivite;

/**
 * Classe de transtypage pour Hibernate : TypeActivite <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class TypeActiviteUserType extends EnumUserType<TypeActivite> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TypeActiviteUserType() {
		super(TypeActivite.class);
	}

}
