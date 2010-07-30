package ch.vd.uniregctb.declaration.ordinaire;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.metier.assujettissement.TypeContribuableDI;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeDocument;

public interface DeclarationImpotService {

	/**
	 * Détermine les déclaration d'impôts ordinaires à émettre et crée des tâches en instances pour chacunes d'elles. En cas de succès, de
	 * nouvelles tâches sont insérées dans la base de données, mais aucune déclaration d'impôt n'est créée ou modifiée.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param dateTraitement
	 *            la date de traitement officielle du job (= aujourd'hui, sauf pour les tests)
	 * @return le nombre de tâches en instance créées.
	 */
	public DeterminationDIsResults determineDIsAEmettre(int anneePeriode, RegDate dateTraitement, int nbThreads, StatusManager status)
			throws DeclarationException;

	/**
	 * Envoie (en masse) à l'impression les déclarations d'impôts ordinaires à partir des tâches en instances pré-existantes. En cas de
	 * succès, les tâches concernées sont considérées comme "traitées" et les déclarations d'impôts correspondantes sont insérées dans la
	 * base de données.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param type
	 *            la population de contribuables visée.
	 * @param noCtbMin
	 * 			  si renseigné, la limite inférieure (incluse) de la plage de contribuables à traiter
	 * @param noCtbMax
	 * 			  si renseigné, la limite supérieure (incluse) de la plage de contribuables à traiter
	 * @param nbMax
	 *            le nombre maximum de déclaration d'impôts retourner, ou <b>0</b> pour ne pas limiter le processus
	 * @param dateTraitement
	 *            la date de traitement officielle du job (= aujourd'hui, sauf pour les tests)
	 * @return le nombre de déclarations envoyées.
	 */
	public EnvoiDIsResults envoyerDIsEnMasse(int anneePeriode, TypeContribuableDI type, Long noCtbMin, Long noCtbMax, int nbMax, RegDate dateTraitement, StatusManager status)
			throws DeclarationException;

	/**
	 * Produit des statistiques sur les déclarations d'impôts ordinaires existantes.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param dateTraitement
	 *            la date de traitement officielle du job (= aujourd'hui, sauf pour les tests)
	 * @return les statistiques demandées
	 */
	public StatistiquesDIs produireStatsDIs(int anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException;

	/**
	 * Produit des statistiques sur les contribuables assujettis pour la période fiscale spécifiée.
	 *
	 * @param anneePeriode
	 *            l'année de la période fiscale considérée.
	 * @param dateTraitement
	 *            la date de traitement officielle du job (= aujourd'hui, sauf pour les tests)
	 * @return les statistiques demandées
	 */
	public StatistiquesCtbs produireStatsCtbs(int anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException;

	/**
	 * Produit la liste des DIs non émises pour la période fiscale spécifiée.
	 *
	 * @param annee
	 *            l'année de la période fiscale considérée.
	 * @param dateTraitement
	 *            la date de traitement officielle du job (= aujourd'hui, sauf pour les tests)
	 * @return les statistiques demandées
	 */
	public ListeDIsNonEmises produireListeDIsNonEmises(Integer annee, RegDate dateTraitement, StatusManager statusManager)
			throws DeclarationException;

	/**
	 * Fait passer à l'état <i>ECHUE</i> toutes les déclarations d'imposition ordinaires sommées et dont le délai de retour est dépassé.
	 *
	 * @param dateTraitement
	 *            la date de traitement pour vérifier le dépassement du délai de retour, et - le cas échéant - pour définir la date
	 *            d'obtention de l'état échu.
	 * @param statusManager
	 * @return les résultats détaillés des DIs qui ont été traitées.
	 */
	public EchoirDIsResults echoirDIsHorsDelai(RegDate dateTraitement, StatusManager statusManager) throws DeclarationException;

	/**
	 * Envoie à l'impression la déclaration spécifiée pour une visualisation on-line, et envoie un événement fiscal correspondant. Cette
	 * méthode retourne directement le document d'impression
	 *
	 * @param declaration
	 *            la déclaration d'impôt ordinaire à imprimer
	 * @param dateEvenement
	 *            la date d'impression
	 * @return l'ID du document d'impression
	 */
	public EditiqueResultat envoiDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws DeclarationException;

	/**
	 * Envoie à l'impression le duplicata de la déclaration spécifiée pour une visualisation on-line. Cette
	 * méthode retourne  directement le document d'impression
	 *
	 * @param declaration
	 *            la déclaration d'impôt ordinaire à imprimer
	 * @param dateEvenement
	 *            la date d'impression
	 * @param typeDocument
	 *            le type de document
	 * @param annexes
	 *            la liste des annexes
	 * @return l'ID du document d'impression
	 */
	public EditiqueResultat envoiDuplicataDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement, TypeDocument typeDocument,
			List<ModeleFeuilleDocumentEditique> annexes) throws DeclarationException;

	/**
	 * Envoie à l'impression la déclaration spécifiée pour un envoi en masse, et envoie un événement fiscal correspondant. Cette méthode
	 * retourne immédiatement et du moment que la transaction est committée, il est de la responsabilité d'éditique d'imprimer la
	 * déclaration.
	 *
	 * @param declaration
	 *            la déclaration d'impôt ordinaire à imprimer
	 * @param dateEvenement
	 *            la date d'impression
	 */
	public void envoiDIForBatch(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws DeclarationException;

	/**
	 * Envoie à l'impression la sommation pour la déclaration spécifiée, et envoie un événement fiscal correspondant. Cette méthode
	 * retourne immédiatement et du moment que la transaction est committée, il est de la responsabilité d'éditique d'imprimer la
	 * déclaration.
	 *
	 * @param declaration
	 *            la déclaration d'impôt ordinaire à imprimer
	 *
	 * @param miseSousPliImpossible
	 * 				true si la mise sous pli automatique est impossible pour des raisons techniques
	 * @param dateEvenement
	 *            la date d'impression
	 */
	public void envoiSommationDIForBatch(DeclarationImpotOrdinaire declaration, boolean miseSousPliImpossible, RegDate dateEvenement) throws DeclarationException;

	/**
	 * Fait passer la déclaration à l'état <i>échu</i>, et envoie un événement fiscal correspondant.
	 *
	 * @param declaration
	 *            la déclaration d'impôt ordinaire à échoir
	 * @param dateTraitement
	 *            la date de passage à l'état échu de la déclaration.
	 */
	void echoirDI(DeclarationImpotOrdinaire declaration, RegDate dateTraitement);

	/**
	 * Retour d'une DI
	 *
	 * @param contribuable
	 * @param di
	 * @param dateEvenement
	 * @return
	 */
	public DeclarationImpotOrdinaire retourDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement);

	/**
	 * Sommation d'une DI
	 *
	 * @param contribuable
	 * @param di
	 * @param dateEvenement
	 * @return
	 */
	public DeclarationImpotOrdinaire sommationDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement);

	/**
	 * Taxation d'office
	 *
	 * @param contribuable
	 * @param di
	 * @param dateEvenement
	 * @return
	 */
	public DeclarationImpotOrdinaire taxationOffice(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement);

	/**
	 * Annulation d'une DI
	 *
	 * @param contribuable
	 * @param di
	 * @param dateEvenement
	 * @return
	 */
	public DeclarationImpotOrdinaire annulationDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement);

	/**
	 * Envoye des sommations à la date donnée
	 *
	 * @param dateTraitement
	 * 			date de traitement pour l'envoi des sommations
	 * @param miseSousPliImpossible
	 * 			boolean a true si la mise sous pli est impossible (les sommations sont envoyées aux offices emetteurs et non au contribuable)
	 * @param nombreMax
	 * 			Le nombre maximal de sommation que le batch peut emettre. 0 = pas de limite.
	 */
	public EnvoiSommationsDIsResults envoyerSommations(RegDate dateTraitement, final boolean miseSousPliImpossible, final Integer nombreMax, StatusManager status);


	/**
	 * Récupère la copie conforme de la sommation éditée pour la DI donnée
	 * @param di
	 * @return
	 * @throws EditiqueException
	 */
	public byte[] getCopieConformeSommationDI(DeclarationImpotOrdinaire di) throws EditiqueException;

	/**
	 * Imprime les chemises TO pour les DIs échues pour lesquelle ces chemises
	 * n'ont pas encore été imprimées
	 * @param noColOid si donné, limite l'envoi des chemises à cet OID
	 */
	public ImpressionChemisesTOResults envoiChemisesTaxationOffice(int nombreMax, Integer noColOid, StatusManager status);

	/**
	 * Ajoute un délai aux déclarations des contribuables spécifiés.
	 *
	 * @param ids
	 *            les ids des contribuables
	 * @param annee
	 *            la période fiscale considérée
	 * @param dateDelai
	 *            la date de délai à appliquer aux déclarations
	 * @param dateTraitement
	 *            la date de traitement
	 * @param s
	 *            un status manager
	 */
	public DemandeDelaiCollectiveResults traiterDemandeDelaiCollective(final List<Long> ids, int annee, final RegDate dateDelai,
			final RegDate dateTraitement, final StatusManager s);
}
