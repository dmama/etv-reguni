package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.EtatEvenementCivil;

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
