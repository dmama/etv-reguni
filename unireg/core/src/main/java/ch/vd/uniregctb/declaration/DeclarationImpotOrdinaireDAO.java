package ch.vd.uniregctb.declaration;

import java.util.List;

public interface DeclarationImpotOrdinaireDAO extends DeclarationDAO<DeclarationImpotOrdinaire> {

	/**
	 * Recherche des declarations d'impot ordinaire selon des criteres
	 *
	 * @param criterion
	 * @return
	 */
	List<DeclarationImpotOrdinaire> find(DeclarationImpotCriteria criterion);

	List<DeclarationImpotOrdinaire> find(DeclarationImpotCriteria criterion, boolean doNotAutoFlush);

	/**
	 * Recherche toutes les DI en fonction du numero de contribuable
	 *
	 * @param numero
	 * @return
	 */
	List<DeclarationImpotOrdinaire> findByNumero(Long numero);

	/**
	 * Retourne le dernier EtatPeriodeDeclaration retournee
	 *
	 * @param numeroCtb
	 * @return
	 */
	EtatDeclaration findDerniereDiEnvoyee(Long numeroCtb) ;
}
