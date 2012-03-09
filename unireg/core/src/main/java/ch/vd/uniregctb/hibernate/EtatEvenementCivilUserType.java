package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.EtatEvenementCivil;

/**
 * Classe de transtypage pour Hibernate : EtatEvenementCivil <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class EtatEvenementCivilUserType extends EnumUserType<EtatEvenementCivil> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public EtatEvenementCivilUserType() {
		super(EtatEvenementCivil.class);
	}

}
