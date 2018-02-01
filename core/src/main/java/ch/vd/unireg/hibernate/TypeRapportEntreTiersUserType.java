package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.TypeRapportEntreTiers;

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
