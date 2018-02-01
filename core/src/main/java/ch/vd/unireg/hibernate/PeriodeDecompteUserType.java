package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.PeriodeDecompte;

/**
 * Classe de transtypage pour Hibernate : Mois <--> varchar
 */
public class PeriodeDecompteUserType extends EnumUserType<PeriodeDecompte> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public PeriodeDecompteUserType() {
		super(PeriodeDecompte.class);
	}
}
