package ch.vd.uniregctb.declaration;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.dao.GenericDAO;

public interface DeclarationImpotOrdinaireDAO extends GenericDAO<DeclarationImpotOrdinaire, Long> {

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

	/**
	 * @return un ensemble de DIs PP avec les délais et les états préinitialisés.
	 */
	Set<DeclarationImpotOrdinairePP> getDeclarationsImpotPPForSommation(Collection<Long> idsDI);

	/**
	 * @return un ensemble de DIs PM avec les délais et les états préinitialisés.
	 */
	Set<DeclarationImpotOrdinairePM> getDeclarationsImpotPMForSommation(Collection<Long> idsDI);
}
