package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.PeriodiciteDecompte;

/**
 * Classe de transtypage pour Hibernate : PeriodiciteDecompte <--> varchar
 *
 * @author Ludovic BERTIN
 */
public class PeriodiciteDecompteUserType extends EnumUserType<PeriodiciteDecompte> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public PeriodiciteDecompteUserType() {
		super(PeriodiciteDecompte.class);
	}

}
