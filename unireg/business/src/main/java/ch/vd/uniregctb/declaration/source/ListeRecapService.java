package ch.vd.uniregctb.declaration.source;

import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.CategorieImpotSource;

public interface ListeRecapService {

	/**
	 * Recupere à l'éditique un document pour afficher
	 * une copie conforme (duplicata)
	 *
	 * @param lr
	 * @return le document pdf
	 */
	public byte[] getCopieConformeLR(DeclarationImpotSource lr) throws EditiqueException;

	/**
	 * Impression d'une sommation LR
	 * 		- Alimentation de l'objet EditiqueListeRecap
	 * 		- Envoi des informations nécessaires à l'éditique
	 * @param dpi
	 * @#param date de traitement de la sommation
	 * @throws Exception
	 */
	public void imprimerSommationLR (DeclarationImpotSource lr, RegDate dateTraitement) throws Exception ;

	/**
	 * Imprime toutes des LR a une date de fin de periode donnee
	 *
	 * @param dateFinPeriode
	 * @throws Exception
	 */
	public EnvoiLRsResults imprimerAllLR(RegDate dateFinPeriode, StatusManager status) throws Exception ;

	/**
	 * Somme toutes LR à sommer
	 *
	 * @param categorie
	 *@param dateTraitement  @throws Exception
	 */
	public EnvoiSommationLRsResults sommerAllLR(CategorieImpotSource categorie, RegDate dateTraitement, StatusManager status) throws Exception ;

	/**
	 * création et impression batch d'une LR
	 * @param dpi
	 * @param dateDebutPeriode
	 * @throws Exception
	 */
	public void imprimerLR(DebiteurPrestationImposable dpi, RegDate dateDebutPeriode) throws Exception ;


	/**
	 * Trouve toutes les LR manquantes d'un débiteur
	 *
	 * @param dpi            un débiteur de prestations imposables
	 * @param dateFinPeriode date de fin de la période considérée (la date de début étant la date de début d'activité du débiteur)
	 * @param lrTrouveesOut  liste des périodes pour lesquelles il existe déjà des LRs. Attention, ces périodes sont fusionnées si elles se touchent.
	 * @return une liste de range de LRs manquantes
	 */
	public List<DateRange> findLRsManquantes(DebiteurPrestationImposable dpi, RegDate dateFinPeriode, List<DateRange> lrTrouveesOut);
}
