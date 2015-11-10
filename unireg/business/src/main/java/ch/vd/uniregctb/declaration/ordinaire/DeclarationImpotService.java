package ch.vd.uniregctb.declaration.ordinaire;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.ordinaire.pm.DeterminationDIsPMResults;
import ch.vd.uniregctb.declaration.ordinaire.pm.EchoirDIsPMResults;
import ch.vd.uniregctb.declaration.ordinaire.pm.EnvoiDIsPMResults;
import ch.vd.uniregctb.declaration.ordinaire.pm.EnvoiSommationsDIsPMResults;
import ch.vd.uniregctb.declaration.ordinaire.pm.TypeDeclarationImpotPM;
import ch.vd.uniregctb.declaration.ordinaire.pp.ContribuableAvecCodeSegment;
import ch.vd.uniregctb.declaration.ordinaire.pp.ContribuableAvecImmeuble;
import ch.vd.uniregctb.declaration.ordinaire.pp.DemandeDelaiCollectiveResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.DeterminationDIsPPResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EchoirDIsPPResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiAnnexeImmeubleResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiDIsPPResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiSommationsDIsPPResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImportCodesSegmentResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.InformationsDocumentAdapter;
import ch.vd.uniregctb.declaration.ordinaire.pp.ListeDIsPPNonEmises;
import ch.vd.uniregctb.declaration.ordinaire.pp.ListeNoteResults;
import ch.vd.uniregctb.declaration.ordinaire.pp.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.declaration.ordinaire.pp.StatistiquesCtbs;
import ch.vd.uniregctb.declaration.ordinaire.pp.StatistiquesDIs;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDI;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.type.TypeDocument;

public interface DeclarationImpotService {

	/**
	 * [SIFISC-2100] Valeur par défaut à attribuer au code segment d'une DI si celui-ci n'est pas renseigné
	 */
	int VALEUR_DEFAUT_CODE_SEGMENT = 0;

	/**
	 * Détermine les déclaration d'impôts PP ordinaires à émettre et crée des tâches en instances pour chacunes d'elles. En cas de succès, de nouvelles tâches sont insérées dans la base de données, mais
	 * aucune déclaration d'impôt n'est créée ou modifiée.
	 *
	 * @param anneePeriode   l'année de la période fiscale considérée.
	 * @param dateTraitement la date de traitement officielle du job (= aujourd'hui, sauf pour les tests)
	 * @param nbThreads      le degré de parallélisme demandé pour le calcul
	 * @return le rapport d'exécution du traitement
	 */
	DeterminationDIsPPResults determineDIsPPAEmettre(int anneePeriode, RegDate dateTraitement, int nbThreads, StatusManager status) throws DeclarationException;

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
	 * @param nbThreads      le nombre de threads sur lesquels doit s'effectuer le traitement
	 * @return le nombre de déclarations envoyées.
	 */
	EnvoiDIsPPResults envoyerDIsPPEnMasse(int anneePeriode, CategorieEnvoiDI categorie,
	                                      Long noCtbMin, Long noCtbMax,
	                                      int nbMax, RegDate dateTraitement, boolean exclureDecedes, int nbThreads, StatusManager status) throws DeclarationException;


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
	ListeDIsPPNonEmises produireListeDIsNonEmises(Integer annee, RegDate dateTraitement, StatusManager statusManager) throws DeclarationException;

	/**
	 * Fait passer à l'état <i>ECHUE</i> toutes les déclarations d'imposition ordinaires PP sommées et dont le délai de retour est dépassé.
	 *
	 * @param dateTraitement la date de traitement pour vérifier le dépassement du délai de retour, et - le cas échéant - pour définir la date d'obtention de l'état échu.
	 * @param statusManager
	 * @return les résultats détaillés des DIs qui ont été traitées.
	 */
	EchoirDIsPPResults echoirDIsPPHorsDelai(RegDate dateTraitement, StatusManager statusManager) throws DeclarationException;

	/**
	 * Envoie à l'impression la déclaration spécifiée pour une visualisation on-line, et envoie un événement fiscal correspondant. Cette méthode retourne directement le document d'impression
	 *
	 * @param declaration   la déclaration d'impôt ordinaire à imprimer
	 * @param dateEvenement la date d'impression
	 * @return l'ID du document d'impression
	 */
	EditiqueResultat envoiDIOnline(DeclarationImpotOrdinairePP declaration, RegDate dateEvenement) throws DeclarationException;

	/**
	 * Envoie à l'impression le duplicata de la déclaration spécifiée pour une visualisation on-line. Cette méthode retourne  directement le document d'impression ou, s'il est trop long à venir, re-route
	 * sur l'inbox
	 *
	 * @param declaration   la déclaration d'impôt ordinaire à imprimer
	 * @param typeDocument  le type de document
	 * @param annexes       la liste des annexes
	 * @return l'ID du document d'impression
	 */
	EditiqueResultat envoiDuplicataDIOnline(DeclarationImpotOrdinairePP declaration, TypeDocument typeDocument,
	                                        List<ModeleFeuilleDocumentEditique> annexes) throws DeclarationException;

	/**
	 * Envoie à l'impression la déclaration spécifiée pour un envoi en masse, et envoie un événement fiscal correspondant. Cette méthode retourne immédiatement et du moment que la transaction est
	 * committée, il est de la responsabilité d'éditique d'imprimer la déclaration.
	 *
	 * @param declaration   la déclaration d'impôt ordinaire PP à imprimer
	 * @param dateEvenement la date d'impression
	 */
	void envoiDIForBatch(DeclarationImpotOrdinairePP declaration, RegDate dateEvenement) throws DeclarationException;

	/**
	 * Envoie à l'impression la déclaration spécifiée pour un envoi en masse, et envoie un événement fiscal correspondant. Cette méthode retourne immédiatement et du moment que la transaction est
	 * committée, il est de la responsabilité d'éditique d'imprimer la déclaration.
	 *
	 * @param declaration   la déclaration d'impôt ordinaire PM à imprimer
	 * @param dateEvenement la date d'impression
	 */
	void envoiDIForBatch(DeclarationImpotOrdinairePM declaration, RegDate dateEvenement) throws DeclarationException;

	/**
	 * Envoie à l'impression la sommation pour la déclaration spécifiée, et envoie un événement fiscal correspondant. Cette méthode retourne immédiatement et du moment que la transaction est committée,
	 * il est de la responsabilité d'éditique d'imprimer la déclaration.
	 *
	 * @param declaration           la déclaration d'impôt ordinaire à imprimer
	 * @param miseSousPliImpossible true si la mise sous pli automatique est impossible pour des raisons techniques
	 * @param dateEvenement         la date d'impression
	 */
	void envoiSommationDIPPForBatch(DeclarationImpotOrdinairePP declaration, boolean miseSousPliImpossible, RegDate dateEvenement) throws DeclarationException;

	/**
	 * Envoie à l'impression la sommation pour la déclaration spécifiée, et envoie un événement fiscal correspondant. Cette méthode retourne immédiatement et du moment que la transaction est committée,
	 * il est de la responsabilité d'éditique d'imprimer la déclaration.
	 *
	 * @param declaration           la déclaration d'impôt ordinaire à imprimer
	 * @param dateEvenement         la date d'impression
	 */
	void envoiSommationDIPMForBatch(DeclarationImpotOrdinairePM declaration, RegDate dateEvenement) throws DeclarationException;

	/**
	 * Fait passer la déclaration à l'état <i>échu</i>, et envoie un événement fiscal correspondant.
	 *
	 * @param declaration    la déclaration d'impôt ordinaire à échoir
	 * @param dateTraitement la date de passage à l'état échu de la déclaration.
	 */
	void echoirDI(DeclarationImpotOrdinaire declaration, RegDate dateTraitement);

	/**
	 * Quittance une déclaration d'impôt ordinaire. C'est-à-dire : ajoute un état 'retourné' sur la déclaration et envoi un événement fiscal.
	 * <p/>
	 * <b>Note:</b> plusieurs états 'retournés' non-annulés peuvent coexister en parallèle depuis la version 12R4 (voir SIFISC-5208). Lorsque plusieurs états 'retourné' existent, le dernier état
	 * (= le plus récent) est utilisé. Finalement, la déclaration est considérée quittancée dès qu'au moins un état 'retourné' existe.
	 *
	 *
	 * @param contribuable  un contribuable
	 * @param di            la déclaration qui doit être quittancée
	 * @param dateEvenement la date de quittancement de la déclaration d'impôt
	 * @param source        la source (= le nom de l'application) de quittancement
	 * @param evtFiscal     <code>true</code> s'il faut envoyer un événement fiscal de quittancement de DI
	 * @return la déclaration nouvellement quittancée
	 */
	DeclarationImpotOrdinaire quittancementDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement, String source, boolean evtFiscal);

	/**
	 * Annulation d'une DI
	 *
	 * @param contribuable
	 * @param di
	 * @param tacheId Non-<code>null</code> si l'annulation de la DI est l'objet du traitement d'une tâche, auquel cas c'est l'ID de cette tâche
	 * @param dateEvenement
	 * @return
	 */
	DeclarationImpotOrdinairePP annulationDI(ContribuableImpositionPersonnesPhysiques contribuable, DeclarationImpotOrdinairePP di, @Nullable Long tacheId, RegDate dateEvenement);

	/**
	 * Désannule une déclaration d'impôt qui est annulée. Cette opération, outre de désannuler la déclaration, émet les événements fiscaux et DI qui vont bien.
	 *
	 * @param ctb           un contribuable
	 * @param di            la déclaration du contribuable à désannuler
	 * @param dateEvenement la date de désannulation
	 */
	void desannulationDI(ContribuableImpositionPersonnesPhysiques ctb, DeclarationImpotOrdinairePP di, RegDate dateEvenement);

	/**
	 * Envoi des sommations de DI PP à la date donnée
	 *
	 * @param dateTraitement        date de traitement pour l'envoi des sommations
	 * @param miseSousPliImpossible boolean a true si la mise sous pli est impossible (les sommations sont envoyées aux offices emetteurs et non au contribuable)
	 * @param nombreMax             Le nombre maximal de sommation que le batch peut emettre. 0 = pas de limite.
	 */
	EnvoiSommationsDIsPPResults envoyerSommationsPP(RegDate dateTraitement, boolean miseSousPliImpossible, int nombreMax, StatusManager status);

	/**
	 * Récupère la copie conforme de la sommation éditée pour la DI donnée
	 *
	 * @param di
	 * @return
	 * @throws EditiqueException
	 */
	EditiqueResultat getCopieConformeSommationDI(DeclarationImpotOrdinaire di) throws EditiqueException;

	/**
	 * Récupère la copie conforme de la confirmation de delai
	 *
	 * @param delai
	 * @return
	 * @throws EditiqueException
	 */
	EditiqueResultat getCopieConformeConfirmationDelai(DelaiDeclaration delai) throws EditiqueException;

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

	int envoiAnnexeImmeubleForBatch(InformationsDocumentAdapter infoDocuments, Set<ModeleFeuilleDocument> listeModele, int nombreAnnexesImmeuble) throws DeclarationException;

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

	/**
	 * Détermine les déclaration d'impôts PM à émettre et crée des tâches en instances pour chacunes d'elles. En cas de succès, de nouvelles tâches sont insérées dans la base de données, mais
	 * aucune déclaration d'impôt n'est créée ou modifiée.
	 *
	 * @param anneePeriode   l'année de la période fiscale considérée.
	 * @param dateTraitement la date de traitement officielle du job (= aujourd'hui, sauf pour les tests)
	 * @param nbThreads      le degré de parallélisme demandé pour le calcul
	 * @return le rapport d'exécution du traitement
	 */
	DeterminationDIsPMResults determineDIsPMAEmettre(int anneePeriode, RegDate dateTraitement, int nbThreads, StatusManager status) throws DeclarationException;

	/**
	 * Lancement du job multi-threadé d'envoi des DI des personnes morales
	 * @param periodeFiscale la période fiscale cible
	 * @param typeDeclaration le type de déclaration à envoyer
	 * @param dateLimiteBouclements la date limite (incluse) des bouclements à prendre en compte
	 * @param nbMaxEnvois (optionnel) le nombre maximal de documents à envoyer
	 * @param dateTraitement la date du traitement
	 * @param nbThreads le degré de parallélisme du job
	 * @param statusManager status manager
	 * @return les données du rapport d'exécution du job
	 */
	EnvoiDIsPMResults envoyerDIsPMEnMasse(int periodeFiscale, TypeDeclarationImpotPM typeDeclaration, RegDate dateLimiteBouclements, @Nullable Integer nbMaxEnvois, RegDate dateTraitement, int nbThreads, StatusManager statusManager) throws DeclarationException;

	/**
	 * Envoi des sommations de DI PM à la date donnée
	 *
	 * @param dateTraitement        date de traitement pour l'envoi des sommations
	 * @param nombreMax             Le nombre maximal de sommation que le batch peut emettre. 0 = pas de limite.
	 * @param statusManager status manager
	 * @return les données du rapport d'exécution du job
	 */
	EnvoiSommationsDIsPMResults envoyerSommationsPM(RegDate dateTraitement, Integer nombreMax, StatusManager statusManager);

	/**
	 * Fait passer à l'état <i>ECHUE</i> toutes les déclarations d'imposition ordinaires PP sommées et dont le délai de retour est dépassé.
	 *
	 * @param dateTraitement la date de traitement pour vérifier le dépassement du délai de retour, et - le cas échéant - pour définir la date d'obtention de l'état échu.
	 * @param statusManager
	 * @return les résultats détaillés des DIs qui ont été traitées.
	 */
	EchoirDIsPMResults echoirDIsPMHorsDelai(RegDate dateTraitement, StatusManager statusManager) throws DeclarationException;

}
