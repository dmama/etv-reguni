package ch.vd.uniregctb.declaration.ordinaire;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.InformationsDocumentAdapter;
import ch.vd.uniregctb.declaration.ListeNoteResults;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDI;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeDocument;

public interface DeclarationImpotService {

	/**
	 * [SIFISC-2100] Valeur par défaut à attribuer au code segment d'une DI si celui-ci n'est pas renseigné
	 */
	public static final int VALEUR_DEFAUT_CODE_SEGMENT = 0;

	/**
	 * Détermine les déclaration d'impôts ordinaires à émettre et crée des tâches en instances pour chacunes d'elles. En cas de succès, de nouvelles tâches sont insérées dans la base de données, mais
	 * aucune déclaration d'impôt n'est créée ou modifiée.
	 *
	 * @param anneePeriode   l'année de la période fiscale considérée.
	 * @param dateTraitement la date de traitement officielle du job (= aujourd'hui, sauf pour les tests)
	 * @return le nombre de tâches en instance créées.
	 */
	DeterminationDIsResults determineDIsAEmettre(int anneePeriode, RegDate dateTraitement, int nbThreads, StatusManager status) throws DeclarationException;

	/**
	 * Envoie (en masse) à l'impression les déclarations d'impôts ordinaires à partir des tâches en instances pré-existantes. En cas de succès, les tâches concernées sont considérées comme "traitées" et
	 * les déclarations d'impôts correspondantes sont insérées dans la base de données.
	 *
	 * @param anneePeriode   l'année de la période fiscale considérée.
	 * @param categorie      la population de contribuables visée.
	 * @param noCtbMin       si renseigné, la limite inférieure (incluse) de la plage de contribuables à traiter
	 * @param noCtbMax       si renseigné, la limite supérieure (incluse) de la plage de contribuables à traiter
	 * @param nbMax          le nombre maximum de déclaration d'impôts retourner, ou <b>0</b> pour ne pas limiter le processus
	 * @param dateTraitement la date de traitement officielle du job (= aujourd'hui, sauf pour les tests)
	 * @param exclureDecedes Si vrai, exclure les décédés (date d'événement) se trouvant entre le 15.11 et le 31.12
	 * @return le nombre de déclarations envoyées.
	 */
	EnvoiDIsResults envoyerDIsEnMasse(int anneePeriode, CategorieEnvoiDI categorie, Long noCtbMin, Long noCtbMax, int nbMax, RegDate dateTraitement, boolean exclureDecedes, StatusManager status)
			throws DeclarationException;


	EnvoiAnnexeImmeubleResults envoyerAnnexeImmeubleEnMasse(int anneePeriode, RegDate dateTraitement, List<ContribuableAvecImmeuble> listeCtb,
	                                                        int nbAnnexesMax, StatusManager status) throws DeclarationException;

	/**
	 * Produit des statistiques sur les déclarations d'impôts ordinaires existantes.
	 *
	 * @param anneePeriode   l'année de la période fiscale considérée.
	 * @param dateTraitement la date de traitement officielle du job (= aujourd'hui, sauf pour les tests)
	 * @return les statistiques demandées
	 */
	StatistiquesDIs produireStatsDIs(int anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException;

	/**
	 * Produit des statistiques sur les contribuables assujettis pour la période fiscale spécifiée.
	 *
	 * @param anneePeriode   l'année de la période fiscale considérée.
	 * @param dateTraitement la date de traitement officielle du job (= aujourd'hui, sauf pour les tests)
	 * @return les statistiques demandées
	 */
	StatistiquesCtbs produireStatsCtbs(int anneePeriode, RegDate dateTraitement, StatusManager status) throws DeclarationException;

	/**
	 * Produit la liste des DIs non émises pour la période fiscale spécifiée.
	 *
	 * @param annee          l'année de la période fiscale considérée.
	 * @param dateTraitement la date de traitement officielle du job (= aujourd'hui, sauf pour les tests)
	 * @return les statistiques demandées
	 */
	ListeDIsNonEmises produireListeDIsNonEmises(Integer annee, RegDate dateTraitement, StatusManager statusManager) throws DeclarationException;

	/**
	 * Fait passer à l'état <i>ECHUE</i> toutes les déclarations d'imposition ordinaires sommées et dont le délai de retour est dépassé.
	 *
	 * @param dateTraitement la date de traitement pour vérifier le dépassement du délai de retour, et - le cas échéant - pour définir la date d'obtention de l'état échu.
	 * @param statusManager
	 * @return les résultats détaillés des DIs qui ont été traitées.
	 */
	EchoirDIsResults echoirDIsHorsDelai(RegDate dateTraitement, StatusManager statusManager) throws DeclarationException;

	/**
	 * Envoie à l'impression la déclaration spécifiée pour une visualisation on-line, et envoie un événement fiscal correspondant. Cette méthode retourne directement le document d'impression
	 *
	 * @param declaration   la déclaration d'impôt ordinaire à imprimer
	 * @param dateEvenement la date d'impression
	 * @return l'ID du document d'impression
	 */
	EditiqueResultat envoiDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws DeclarationException;

	/**
	 * Envoie à l'impression le duplicata de la déclaration spécifiée pour une visualisation on-line. Cette méthode retourne  directement le document d'impression ou, s'il est trop long à venir, re-route
	 * sur l'inbox
	 *
	 * @param declaration   la déclaration d'impôt ordinaire à imprimer
	 * @param dateEvenement la date d'impression
	 * @param typeDocument  le type de document
	 * @param annexes       la liste des annexes
	 * @return l'ID du document d'impression
	 */
	EditiqueResultat envoiDuplicataDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement, TypeDocument typeDocument,
	                                        List<ModeleFeuilleDocumentEditique> annexes) throws DeclarationException;

	/**
	 * Envoie à l'impression la déclaration spécifiée pour un envoi en masse, et envoie un événement fiscal correspondant. Cette méthode retourne immédiatement et du moment que la transaction est
	 * committée, il est de la responsabilité d'éditique d'imprimer la déclaration.
	 *
	 * @param declaration   la déclaration d'impôt ordinaire à imprimer
	 * @param dateEvenement la date d'impression
	 */
	void envoiDIForBatch(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws DeclarationException;

	/**
	 * Envoie à l'impression la sommation pour la déclaration spécifiée, et envoie un événement fiscal correspondant. Cette méthode retourne immédiatement et du moment que la transaction est committée,
	 * il est de la responsabilité d'éditique d'imprimer la déclaration.
	 *
	 * @param declaration           la déclaration d'impôt ordinaire à imprimer
	 * @param miseSousPliImpossible true si la mise sous pli automatique est impossible pour des raisons techniques
	 * @param dateEvenement         la date d'impression
	 */
	void envoiSommationDIForBatch(DeclarationImpotOrdinaire declaration, boolean miseSousPliImpossible, RegDate dateEvenement) throws DeclarationException;

	/**
	 * Fait passer la déclaration à l'état <i>échu</i>, et envoie un événement fiscal correspondant.
	 *
	 * @param declaration    la déclaration d'impôt ordinaire à échoir
	 * @param dateTraitement la date de passage à l'état échu de la déclaration.
	 */
	void echoirDI(DeclarationImpotOrdinaire declaration, RegDate dateTraitement);

	/**
	 * Quittancement d'une DI
	 *
	 * @param contribuable  un contribuable
	 * @param di            la déclaration qui doit être quittancée
	 * @param dateEvenement la date de quittancement de la déclaration d'impôt
	 * @param source        la source (= le nom de l'application) de quittancement
	 * @return la déclaration nouvellement quittancée
	 */
	DeclarationImpotOrdinaire quittancementDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement, String source);

	/**
	 * Annulation d'une DI
	 *
	 * @param contribuable
	 * @param di
	 * @param dateEvenement
	 * @return
	 */
	DeclarationImpotOrdinaire annulationDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement);

	/**
	 * Envoye des sommations à la date donnée
	 *
	 * @param dateTraitement        date de traitement pour l'envoi des sommations
	 * @param miseSousPliImpossible boolean a true si la mise sous pli est impossible (les sommations sont envoyées aux offices emetteurs et non au contribuable)
	 * @param nombreMax             Le nombre maximal de sommation que le batch peut emettre. 0 = pas de limite.
	 */
	EnvoiSommationsDIsResults envoyerSommations(RegDate dateTraitement, boolean miseSousPliImpossible, int nombreMax, StatusManager status);


	/**
	 * Récupère la copie conforme de la sommation éditée pour la DI donnée
	 *
	 * @param di
	 * @return
	 * @throws EditiqueException
	 */
	InputStream getCopieConformeSommationDI(DeclarationImpotOrdinaire di) throws EditiqueException;

	/**
	 * Récupère la copie conforme de la confirmation de delai
	 *
	 * @param delai
	 * @return
	 * @throws EditiqueException
	 */
	InputStream getCopieConformeConfirmationDelai(DelaiDeclaration delai) throws EditiqueException;

	/**
	 * Imprime les chemises TO pour les DIs échues pour lesquelle ces chemises n'ont pas encore été imprimées
	 *
	 * @param noColOid si donné, limite l'envoi des chemises à cet OID
	 */
	ImpressionChemisesTOResults envoiChemisesTaxationOffice(int nombreMax, Integer noColOid, StatusManager status);

	/**
	 * Ajoute un délai aux déclarations des contribuables spécifiés.
	 *
	 * @param ids            les ids des contribuables
	 * @param annee          la période fiscale considérée
	 * @param dateDelai      la date de délai à appliquer aux déclarations
	 * @param dateTraitement la date de traitement
	 * @param s              un status manager
	 */
	DemandeDelaiCollectiveResults traiterDemandeDelaiCollective(final List<Long> ids, int annee, final RegDate dateDelai, final RegDate dateTraitement, final StatusManager s);

	/**
	 * Permet de produire la liste des contribuables ayant une Di transformée en note
	 *
	 * @param dateTraitement la date de traitement
	 * @param nbThreads      nombre de thread
	 * @param annee          l apériode fiscale
	 * @param statusManager  status manager
	 * @return la liste des contribuables trouvés
	 */

	ListeNoteResults produireListeNote(RegDate dateTraitement, int nbThreads, Integer annee, StatusManager statusManager);

	int envoiAnnexeImmeubleForBatch(InformationsDocumentAdapter infoDocuments, Set<ModeleFeuilleDocument> listeModele, RegDate dateTraitement, int nombreAnnexesImmeuble) throws DeclarationException;

	/**
	 * Ajoute un delai à une declaration et renvoi le delai enregistré avec son id renseigné
	 *
	 * @param declaration
	 * @param delai
	 * @return
	 */
	DelaiDeclaration addAndSave(DeclarationImpotOrdinaire declaration, DelaiDeclaration delai);

	/**
	 * Modifie les données en base pour tenir compte des codes 'segment' fournis pas TAO
	 * @param input données fournies par TAO
	 * @param s status manager
	 * @return les données pour construire un rapport d'exécution
	 */
	ImportCodesSegmentResults importerCodesSegment(List<ContribuableAvecCodeSegment> input, StatusManager s);
}
