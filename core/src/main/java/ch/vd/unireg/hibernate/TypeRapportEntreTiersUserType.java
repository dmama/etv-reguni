package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Classe de transtypage pour Hibernate : TypeRapportEntreTiers <--> varchar
 *
 * @author Ludovic BERTIN
 */
public class TypeRapportEntreTiersUserType extends EnumUserType<TypeRapportEntreTiers> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TypeRapportEntreTiersUserType() {
		super(TypeRapportEntreTiers.class);
	}

}
