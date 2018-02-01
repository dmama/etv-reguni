package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.ModeImposition;

/**
 * Classe de transtypage pour Hibernate : ModeImposition <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class ModeImpositionUserType extends EnumUserType<ModeImposition> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public ModeImpositionUserType() {
		super(ModeImposition.class);
	}

}
