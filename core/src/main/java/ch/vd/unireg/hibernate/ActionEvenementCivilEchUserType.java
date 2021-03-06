package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.ActionEvenementCivilEch;

/**
 * Classe de transtypage pour Hibernate : ActionEvenementCivilEch <--> varchar
 */
public class ActionEvenementCivilEchUserType extends EnumUserType<ActionEvenementCivilEch> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public ActionEvenementCivilEchUserType() {
		super(ActionEvenementCivilEch.class);
	}
}
