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
	public List<DeclarationImpotOrdinaire> find(DeclarationImpotCriteria criterion);

	public List<DeclarationImpotOrdinaire> find(DeclarationImpotCriteria criterion, boolean doNotAutoFlush);

	/**
	 * Recherche toutes les DI en fonction du numero de contribuable
	 *
	 * @param numero
	 * @return
	 */
	public List<DeclarationImpotOrdinaire> findByNumero(Long numero);

	/**
	 * Retourne le dernier EtatPeriodeDeclaration retournee
	 *
	 * @param numeroCtb
	 * @return
	 */
	public EtatDeclaration findDerniereDiEnvoyee(Long numeroCtb) ;

	/**
	 * @return un ensemble de DIs avec les délais et les états préinitialisés.
	 */
	public Set<DeclarationImpotOrdinaire> getDIsForSommation(Collection<Long> idsDI);
}
