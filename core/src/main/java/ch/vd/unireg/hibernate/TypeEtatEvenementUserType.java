package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.evenement.externe.EtatEvenementExterne;

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
