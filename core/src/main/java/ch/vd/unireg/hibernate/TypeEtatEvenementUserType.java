package ch.vd.unireg.hibernate;

import ch.vd.unireg.evenement.externe.EtatEvenementExterne;

/**
 * Classe de transtypage pour Hibernate : EtatEvenementExterne <--> varchar
 *
 * @author xcicfh
 */
public class TypeEtatEvenementUserType extends EnumUserType<EtatEvenementExterne> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TypeEtatEvenementUserType() {
		super(EtatEvenementExterne.class);
	}

}
