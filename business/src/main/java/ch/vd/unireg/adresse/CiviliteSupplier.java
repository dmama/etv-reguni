package ch.vd.unireg.adresse;

import ch.vd.unireg.type.TypeFormulePolitesse;

/**
 * Interface implémentée par les tiers pour lesquels on veut expliciter les salutations
 * @see TypeFormulePolitesse pour la signification de "salutations" et "formule d'appel"
 */
public interface CiviliteSupplier {

	/**
	 * @return si non-vide, la formule utilisée dans les salutations
	 */
	String getSalutations();

	/**
	 * @return la formule utilisée dans les formules d'appel
	 */
	String getFormuleAppel();
}
