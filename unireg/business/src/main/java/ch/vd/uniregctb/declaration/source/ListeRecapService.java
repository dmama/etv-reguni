package ch.vd.uniregctb.declaration.source;

import java.io.InputStream;
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
	InputStream getCopieConformeSommationLR(DeclarationImpotSource lr) throws EditiqueException;

	/**
	 * Impression d'une sommation LR
	 * 		- Alimentation de l'objet EditiqueListeRecap
	 * 		- Envoi des informations nécessaires à l'éditique
	 * @param dpi
	 * @#param date de traitement de la sommation
	 * @throws Exception
	 */
	void imprimerSommationLR (DeclarationImpotSource lr, RegDate dateTraitement) throws Exception ;

	/**
	 * Imprime toutes des LR a une date de fin de periode donnee
	 *
	 * @param dateFinPeriode
	 * @throws Exception
	 */
	EnvoiLRsResults imprimerAllLR(RegDate dateFinPeriode, StatusManager status) throws Exception ;

	/**
	 * Somme toutes LR à sommer
	 *
	 * @param categorie      la catégorie de débiteurs à traiter; ou <b>null</b> pour traiter tous les catégories de débiteurs
	 * @param dateFinPeriode paramètre optionnel qui - s'il est renseigné - est utilisé pour restreindre les sommations aux LRs dont la période finit à la date spécifiée.
	 * @param dateTraitement la date de traitement
	 * @param status         un status manager; ou <b>null</b> pour logger la progression dans log4j.
	 * @return les résultats détaillés du run.
	 */
	EnvoiSommationLRsResults sommerAllLR(CategorieImpotSource categorie, RegDate dateFinPeriode, RegDate dateTraitement, StatusManager status);

	/**
	 * création et impression batch d'une LR
	 *
	 * @param dpi
	 * @param dateDebutPeriode
	 * @param dateFinPeriode
	 * @throws Exception
	 */
	void imprimerLR(DebiteurPrestationImposable dpi, RegDate dateDebutPeriode, RegDate dateFinPeriode) throws Exception ;


	/**
	 * Trouve toutes les LR manquantes d'un débiteur
	 *
	 * @param dpi            un débiteur de prestations imposables
	 * @param dateFinPeriode date de fin de la période considérée (la date de début étant la date de début d'activité du débiteur)
	 * @param lrTrouveesOut  en sortie, liste des périodes pour lesquelles il existe déjà des LRs. Attention, ces périodes sont fusionnées si elles se touchent.
	 * @return une liste de range de LRs manquantes
	 */
	List<DateRange> findLRsManquantes(DebiteurPrestationImposable dpi, RegDate dateFinPeriode, List<DateRange> lrTrouveesOut);

	/**
	 * Pour chacun des débiteurs à qui on a envoyé toutes les LR de la période fiscale donnée, et pour lesquels il existe au
	 * moins une LR échue (dont le délai de retour de la sommation a été bien dépassé à la date de traitement),
	 * envoie un événement fiscal "liste récapitulative manquante"
	 * @param periodeFiscale période fiscale sur laquelle les LR sont inspectées
	 * @param dateTraitement date déterminante pour savoir si un délai a été dépassé
	 */
	DeterminerLRsEchuesResults determineLRsEchues(int periodeFiscale, RegDate dateTraitement, StatusManager status) throws Exception;
}
