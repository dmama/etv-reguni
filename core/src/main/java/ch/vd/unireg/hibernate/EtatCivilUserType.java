package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.EtatCivil;

/**
 * Classe de transtypage pour Hibernate : EtatCivil <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class EtatCivilUserType extends EnumUserType<EtatCivil> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public EtatCivilUserType() {
		super(EtatCivil.class);
	}

}
