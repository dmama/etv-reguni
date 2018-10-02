package ch.vd.unireg.declaration;

import java.util.List;

public interface DeclarationImpotOrdinaireDAO extends DeclarationDAO<DeclarationImpotOrdinaire> {

	/**
	 * Recherche des declarations d'impot ordinaire selon des criteres
	 */
	List<DeclarationImpotOrdinaire> find(DeclarationImpotCriteria criterion);

	List<DeclarationImpotOrdinaire> find(DeclarationImpotCriteria criterion, boolean doNotAutoFlush);

	/**
	 * Recherche toutes les DI en fonction du numero de contribuable
	 */
	List<DeclarationImpotOrdinaire> findByNumero(Long numero);

	/**
	 * Retourne le dernier EtatPeriodeDeclaration retournee
	 */
	EtatDeclaration findDerniereDiEnvoyee(Long numeroCtb);

	/**
	 * @param periodeDebut la première période fiscale considérée
	 * @return les ids des déclarations d'impôts <i>ordinaires</i> émises (non-annulées) depuis la période fiscale spécifiée.
	 */
	List<Long> findIdsDeclarationsOrdinairesEmisesFrom(int periodeDebut);
}
