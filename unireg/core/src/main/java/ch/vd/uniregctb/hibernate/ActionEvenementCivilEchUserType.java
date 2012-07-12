package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.ActionEvenementCivilEch;

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
