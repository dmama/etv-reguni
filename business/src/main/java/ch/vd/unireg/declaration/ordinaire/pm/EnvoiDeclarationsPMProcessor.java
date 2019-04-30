package ch.vd.unireg.declaration.ordinaire.pm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TicketService;
import ch.vd.unireg.common.TicketTimeoutException;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.DeclarationGenerationOperation;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationEmise;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.ordinaire.DatesDelaiInitialDI;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.CategorieEnvoiDIPM;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpotPM;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.GroupeTypesDocumentBatchLocal;
import ch.vd.unireg.type.TypeDelaiDeclaration;
import ch.vd.unireg.type.TypeEtatTache;

public class EnvoiDeclarationsPMProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnvoiDeclarationsPMProcessor.class);

	private final HibernateTemplate hibernateTemplate;
	private final PeriodeFiscaleDAO periodeDAO;
	private final DeclarationImpotService declarationImpotService;
	private final AssujettissementService assujettissementService;
	private final int tailleLot;
	private final PlatformTransactionManager transactionManager;
	private final ParametreAppService parametres;
	private final TicketService ticketService;
	private final AuditManager audit;

	public EnvoiDeclarationsPMProcessor(HibernateTemplate hibernateTemplate, PeriodeFiscaleDAO periodeDAO, DeclarationImpotService declarationImpotService,
	                                    AssujettissementService assujettissementService, int tailleLot,
	                                    PlatformTransactionManager transactionManager, ParametreAppService parametres,
	                                    TicketService ticketService, AuditManager audit) {
		this.hibernateTemplate = hibernateTemplate;
		this.periodeDAO = periodeDAO;
		this.declarationImpotService = declarationImpotService;
		this.assujettissementService = assujettissementService;
		this.tailleLot = tailleLot;
		this.transactionManager = transactionManager;
		this.parametres = parametres;
		this.ticketService = ticketService;
		this.audit = audit;
	}

	/**
	 * C'est ici que ça commence réellement...
	 */
	public EnvoiDIsPMResults run(final int periodeFiscale,
	                             final CategorieEnvoiDIPM categorieEnvoi,
	                             final RegDate dateLimiteBouclements,
	                             @Nullable final Integer nbMaxEnvois,
	                             final RegDate dateTraitement,
	                             final int nbThreads,
	                             StatusManager s) throws DeclarationException {

		final StatusManager status = s == null ? new LoggingStatusManager(LOGGER) : s;
		final EnvoiDIsPMResults rapportFinal = new EnvoiDIsPMResults(dateTraitement, nbThreads, categorieEnvoi, periodeFiscale, dateLimiteBouclements, nbMaxEnvois);

		status.setMessage("Récupération des contribuables à traiter...");
		final List<Long> idsContribuables = getIdsContribuables(categorieEnvoi, periodeFiscale);

		// Traitement des contribuables par lots
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, EnvoiDIsPMResults> template = new ParallelBatchTransactionTemplateWithResults<>(idsContribuables, tailleLot, nbThreads, Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                        transactionManager, status, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, EnvoiDIsPMResults>() {
			@Override
			public EnvoiDIsPMResults createSubRapport() {
				return new EnvoiDIsPMResults(dateTraitement, nbThreads, categorieEnvoi, periodeFiscale, dateLimiteBouclements, nbMaxEnvois);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, EnvoiDIsPMResults rapport) throws DeclarationException {

				// un petit log quand on démarre le lot, pour que l'extérieur sache où on en est...
				status.setMessage("Traitement du lot [" + batch.get(0) + " -> " + batch.get(batch.size() - 1) + "]", progressMonitor.getProgressInPercent());

				// limitation, si nécessaire, du nombre de documents envoyés
				final int dejaFaits = rapportFinal.getEnvoyees().size();
				if (nbMaxEnvois != null && dejaFaits + batch.size() > nbMaxEnvois) {
					// limite le nombre de contribuable pour ne pas dépasser le nombre max
					int reducedSize = nbMaxEnvois - dejaFaits;
					batch = batch.subList(0, Math.max(reducedSize, 0));
				}

				if (!batch.isEmpty()) {
					traiterBatch(batch, rapport, categorieEnvoi, dateLimiteBouclements, dateTraitement);
				}

				return !rapportFinal.interrompu && (nbMaxEnvois == null || rapportFinal.getEnvoyees().size() + batch.size() < nbMaxEnvois);

			}
		}, progressMonitor);

		if (status.isInterrupted()) {
			status.setMessage("L'envoi en masse des déclarations d'impôt PM a été interrompu."
					                  + " Nombre de déclarations envoyées au moment de l'interruption = " + rapportFinal.getEnvoyees().size());
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("L'envoi en masse des déclarations d'impôt PM est terminé. Nombre de déclarations envoyées = "
					                  + rapportFinal.getEnvoyees().size() + ". Nombre d'erreurs = " + rapportFinal.getErreurs().size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Traitement d'un lot de contribuable dans une transaction
	 * @param idsContribuables identifiants des contribuables à traiter
	 * @param rapport rapport à remplir
	 * @param categorieEnvoi type de document à générer
	 * @param dateLimiteBouclements date limite (incluse) des bouclements à prendre en compte
	 * @param dateTraitement date du traitement
	 */
	private void traiterBatch(Collection<Long> idsContribuables, EnvoiDIsPMResults rapport, CategorieEnvoiDIPM categorieEnvoi, RegDate dateLimiteBouclements, RegDate dateTraitement) throws DeclarationException {
		final PeriodeFiscale pf = periodeDAO.getPeriodeFiscaleByYear(rapport.getPeriodeFiscale());
		if (pf == null) {
			throw new DeclarationException("Période fiscale " + rapport.getPeriodeFiscale() + " inexistante!");
		}
		rapport.addLotContribuablesVus(idsContribuables.size());
		final InformationsFiscales informationsFiscales = new InformationsFiscales(pf);
		final Iterator<TacheEnvoiDeclarationImpotPM> tacheIterator = getTaches(categorieEnvoi, rapport.getPeriodeFiscale(), idsContribuables);
		while (tacheIterator.hasNext()) {
			final TacheEnvoiDeclarationImpotPM tache = tacheIterator.next();
			traiterTache(tache, rapport, dateLimiteBouclements, dateTraitement, informationsFiscales);
		}
	}

	/**
	 * Traitement d'une tâche d'envoi de DI PM (auparavant, on verrouille la création de DI sur ce contribuable)
	 * @param tache la tâche à traiter
	 * @param rapport rapport à remplir
	 * @param dateLimiteBouclements date limite (incluse) des bouclements à prendre en compte
	 * @param dateTraitement date de traitement
	 * @param informationsFiscales accesseurs vers les informations fiscales "lourdes" d'un contribuable
	 */
	private void traiterTache(TacheEnvoiDeclarationImpotPM tache, EnvoiDIsPMResults rapport, RegDate dateLimiteBouclements, RegDate dateTraitement, InformationsFiscales informationsFiscales) throws DeclarationException {
		final ContribuableImpositionPersonnesMorales pm = tache.getContribuable();
		final DeclarationGenerationOperation tickettingKey = new DeclarationGenerationOperation(pm.getNumero());
		try {
			final TicketService.Ticket ticket = ticketService.getTicket(tickettingKey, Duration.ofMillis(500));
			try {
				traiterTache(tache, pm, rapport, dateLimiteBouclements, dateTraitement, informationsFiscales);
			}
			finally {
				ticket.release();
			}
		}
		catch (TicketTimeoutException e) {
			throw new DeclarationException(String.format("Une DI est actuellement déjà en cours d'émission pour le contribuable %d.", pm.getNumero()), e);
		}
		catch (InterruptedException e) {
			throw new DeclarationException(e);
		}
	}

	/**
	 * Traitement d'une tâche d'envoi de DI PM
	 * @param tache la tâche à traiter
	 * @param pm le contribuable concerné
	 * @param rapport rapport à remplir
	 * @param dateLimiteBouclements date limite (incluse) des bouclements à prendre en compte
	 * @param dateTraitement date de traitement
	 * @param informationsFiscales accesseurs vers les informations fiscales "lourdes" d'un contribuable  @throws DeclarationException en cas de souci
	 */
	private void traiterTache(TacheEnvoiDeclarationImpotPM tache, ContribuableImpositionPersonnesMorales pm, EnvoiDIsPMResults rapport,
	                          RegDate dateLimiteBouclements, RegDate dateTraitement, InformationsFiscales informationsFiscales) throws DeclarationException {

		// TODO faut-il vérifier que la tâche est toujours d'actualité ?

		// quelles sont les déclarations existantes sur la PF pour ce contribuable ?
		final Collection<DeclarationImpotOrdinairePM> declarationsSurPeriode = informationsFiscales.getDeclarationsSurPeriodeFiscale(pm);
		if (!declarationsSurPeriode.isEmpty()) {
			final List<DeclarationImpotOrdinairePM> trieesNonAnnuleesSurPeriode = AnnulableHelper.sansElementsAnnules(declarationsSurPeriode);
			trieesNonAnnuleesSurPeriode.sort(DateRangeComparator::compareRanges);
			if (!trieesNonAnnuleesSurPeriode.isEmpty()) {
				if (DateRangeHelper.intersect(tache, trieesNonAnnuleesSurPeriode)) {
					// ahah... il y a ou bien conflit ou bien redondance...
					final DeclarationImpotOrdinairePM diDebutTache = DateRangeHelper.rangeAt(trieesNonAnnuleesSurPeriode, tache.getDateDebut());
					final DeclarationImpotOrdinairePM diFinTache = DateRangeHelper.rangeAt(trieesNonAnnuleesSurPeriode, tache.getDateFin());
					if (diDebutTache == null || diFinTache == null || diDebutTache != diFinTache || areIncompatibles(diDebutTache, tache)) {
						// cas de conflit
						final String msg = String.format("La tâche d'envoi de déclaration PM %d %s est en conflit avec les déclarations existantes du contribuable %s. Aucune nouvelle déclaration n'est créée et la tâche reste en instance.",
						                                 tache.getId(), DateRangeHelper.toDisplayString(tache), FormatNumeroHelper.numeroCTBToDisplay(pm.getNumero()));
						audit.error(msg);
						rapport.addCollisionAvecDi(pm.getNumero(), msg);
					}
					else {
						// en fait, la DI correspond tout pile à la tâche... on va juste marquer la tâche comme traitée
						audit.warn(String.format("Une déclaration correspond déjà à la tâche %d %s du contribuable %s. Aucune nouvelle déclaration n'est créée et la tâche est considérée comme traitée.",
						                         tache.getId(), DateRangeHelper.toDisplayString(tache), FormatNumeroHelper.numeroCTBToDisplay(pm.getNumero())));
						tache.setEtat(TypeEtatTache.TRAITE);
						rapport.addTacheIgnoreeDeclarationExistante(pm.getNumero(), tache.getDateDebut(), tache.getDateFin());
					}
					return;
				}
			}
		}

		// les éventuels conflit ou redondance sont maintenant traités, si on est ici, c'est que la tâche peut être traitée

		// tâche dans le futur de la date de traitement -> ignorée
		if (RegDateHelper.isAfterOrEqual(tache.getDateFin(), dateTraitement, NullDateBehavior.LATEST)) {
			rapport.addTacheIgnoreeBouclementFutur(pm.getNumero(), tache.getDateDebut(), tache.getDateFin());
			return;
		}
		// maintenant, la tâche est peut-être écartée si elle correspond à un bouclement (sans fin d'assujettissement) après la limite acceptée
		if (RegDateHelper.isAfter(tache.getDateFin(), dateLimiteBouclements, NullDateBehavior.LATEST) && !isFinAssujettissement(pm, tache.getDateFin(), informationsFiscales)) {
			rapport.addTacheIgnoreeBouclementTropRecent(pm.getNumero(), tache.getDateDebut(), tache.getDateFin());
			return;
		}

		// création d'une déclaration
		final DeclarationImpotOrdinairePM di = new DeclarationImpotOrdinairePM();
		di.setDateDebut(tache.getDateDebut());
		di.setDateFin(tache.getDateFin());
		di.setDateDebutExerciceCommercial(tache.getDateDebutExercice());
		di.setDateFinExerciceCommercial(tache.getDateFinExercice());
		di.setTypeContribuable(tache.getTypeContribuable());
		di.setTiers(pm);
		di.setNumero(getNewSequenceNumber(pm, informationsFiscales));

		final PeriodeFiscale periodeFiscale = informationsFiscales.getPeriodeFiscale();
		di.setPeriode(periodeFiscale);
		di.setModeleDocument(periodeFiscale.get(tache.getTypeDocument()));

		if (pm.shouldAssignCodeControle(di)) {
			di.setCodeControle(ContribuableImpositionPersonnesMorales.generateCodeControleForPM(informationsFiscales.getDeclarations(pm), periodeFiscale));
		}
		if (pm instanceof Entreprise && tache.getTypeDocument() != null) {
			di.setCodeSegment(declarationImpotService.computeCodeRoutage((Entreprise) pm, tache.getDateFin(), tache.getTypeDocument()).getCode());
		}

		// ajout de l'état initial de la DI
		di.addEtat(new EtatDeclarationEmise(dateTraitement));

		// ajout du délai initial de retour
		final DatesDelaiInitialDI datesDelaiInitial = declarationImpotService.getDelaiInitialRetourDIPM(tache.getTypeContribuable(), tache.getDateFin(), dateTraitement, periodeFiscale);
		di.setDelaiRetourImprime(datesDelaiInitial.getDateImprimee());

		final DelaiDeclaration delaiInitial = new DelaiDeclaration();
		delaiInitial.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		delaiInitial.setCleArchivageCourrier(null);
		delaiInitial.setDateDemande(dateTraitement);
		delaiInitial.setDateTraitement(dateTraitement);
		delaiInitial.setDelaiAccordeAu(datesDelaiInitial.getDateEffective());
		delaiInitial.setTypeDelai(TypeDelaiDeclaration.IMPLICITE); // [FISCPROJ-873] Par définition, les délais des envois en masse sont implicites
		di.addDelai(delaiInitial);

		// emvoyer le document à l'éditique
		final DeclarationImpotOrdinairePM savedDi = hibernateTemplate.merge(di);
		declarationImpotService.envoiDIForBatch(savedDi, dateTraitement);

		informationsFiscales.addNouvelleDeclaration(pm, savedDi);
		rapport.addDiEnvoyee(pm.getNumero(), tache.getDateDebut(), tache.getDateFin());
		tache.setEtat(TypeEtatTache.TRAITE);
	}

	/**
	 * Génère le prochain numéro de séquence pour une DI du contribuable
	 * @param pm le contribuable personne morale
	 * @param informationsFiscales les informations fiscales
	 * @return le numéro de séquence à utiliser pour une nouvelle DI
	 */
	private static int getNewSequenceNumber(ContribuableImpositionPersonnesMorales pm, InformationsFiscales informationsFiscales) {
		final Collection<DeclarationImpotOrdinairePM> surPeriode = informationsFiscales.getDeclarationsSurPeriodeFiscale(pm);
		int highWaterMark = 0;
		for (DeclarationImpotOrdinairePM di : surPeriode) {
			if (di.getNumero() > highWaterMark) {
				highWaterMark = di.getNumero();
			}
		}
		return highWaterMark + 1;
	}

	/**
	 * @param pm contribuable personne morale
	 * @param dateFinPeriodeImposition date de fin de la période d'imposition
	 * @param informationsFiscales données fiscales du tiers
	 * @return <code>true</code> s'il n'y a pas d'assujettissement de la PM au lendemain de la date de fin de période d'imposition
	 */
	private static boolean isFinAssujettissement(ContribuableImpositionPersonnesMorales pm, RegDate dateFinPeriodeImposition, InformationsFiscales informationsFiscales) throws DeclarationException {
		final List<Assujettissement> assujettissements = informationsFiscales.getAssujettissements(pm);
		return DateRangeHelper.rangeAt(assujettissements, dateFinPeriodeImposition.getOneDayAfter()) == null;
	}

	/**
	 * @param di une déclaration d'impôt existante
	 * @param tache une tâche que l'on aimerait bien traiter
	 * @return <code>true</code> si les deux ne sont pas compatibles
	 */
	private static boolean areIncompatibles(DeclarationImpotOrdinairePM di, TacheEnvoiDeclarationImpotPM tache) {
		final boolean typesCompatibles;
		if (di.getTypeDeclaration() == tache.getTypeDocument()) {
			typesCompatibles = true;
		}
		else {
			final GroupeTypesDocumentBatchLocal groupeTypeDI = GroupeTypesDocumentBatchLocal.of(di.getTypeDeclaration());
			final GroupeTypesDocumentBatchLocal groupeTypeTache = GroupeTypesDocumentBatchLocal.of(tache.getTypeDocument());
			typesCompatibles = groupeTypeDI != null && groupeTypeTache != null && groupeTypeDI == groupeTypeTache;
		}
		return !typesCompatibles || !DateRangeHelper.equals(di, tache);
	}

	/**
	 * Cache des informations fiscales des contribuables personnes morales d'un lot
	 */
	private final class InformationsFiscales {

		private final Map<Long, List<Assujettissement>> assujettissements = new HashMap<>();
		private final Map<Long, List<DeclarationImpotOrdinairePM>> declarations = new HashMap<>();
		private final PeriodeFiscale periodeFiscale;

		public InformationsFiscales(PeriodeFiscale periodeFiscale) {
			this.periodeFiscale = periodeFiscale;
		}

		/**
		 * @return La période fiscale pour ce run
		 */
		public PeriodeFiscale getPeriodeFiscale() {
			return periodeFiscale;
		}

		/**
		 * @param ctb contribuable personne morale
		 * @return l'assujettissement de la personne morale, calculé une fois et maintenu en cache pour les appels ultérieurs
		 * @throws AssujettissementException en cas de souci
		 */
		@NotNull
		private List<Assujettissement> _getAssujettissement(ContribuableImpositionPersonnesMorales ctb) throws AssujettissementException {
			if (assujettissements.containsKey(ctb.getNumero())) {
				return assujettissements.get(ctb.getNumero());
			}
			final List<Assujettissement> computed = assujettissementService.determine(ctb);
			final List<Assujettissement> saved = computed == null ? Collections.emptyList() : computed;
			assujettissements.put(ctb.getNumero(), saved);
			return saved;
		}

		/**
		 * @param ctb contribuable personne morale
		 * @return l'assujettissement de la personne morale, calculé une fois et maintenu en cache pour les appels ultérieurs (dans une collection non-modifiable pour être sûr de ne pas faire de bêtise)
		 * @throws DeclarationException en cas de souci
		 */
		@NotNull
		public List<Assujettissement> getAssujettissements(ContribuableImpositionPersonnesMorales ctb) throws DeclarationException {
			try {
				return Collections.unmodifiableList(_getAssujettissement(ctb));
			}
			catch (AssujettissementException e) {
				throw new DeclarationException(e);
			}
		}

		/**
		 * @param ctb contribuable personne morale
		 * @return l'ensemble des déclarations de la personne morale (y compris les annulées), sans ordre particulier, extraites une fois depuis la base de données
		 * et maintenues en cache pour les appels ultérieurs (les déclarations créées dans ce job peuvent être enregistrées ici à l'aide de la méthode
		 * {@link #addNouvelleDeclaration(ContribuableImpositionPersonnesMorales, DeclarationImpotOrdinairePM)})
		 */
		@NotNull
		private Collection<DeclarationImpotOrdinairePM> _getDeclarations(ContribuableImpositionPersonnesMorales ctb) {
			if (declarations.containsKey(ctb.getNumero())) {
				return declarations.get(ctb.getNumero());
			}

			// new ArrayList<>() pour être certain d'avoir une copie modifiable de la liste...
			final List<DeclarationImpotOrdinairePM> toutes = new ArrayList<>(ctb.getDeclarationsTriees(DeclarationImpotOrdinairePM.class, true));
			declarations.put(ctb.getNumero(), toutes);
			return toutes;
		}

		/**
		 * @param ctb contribuable personne morale
		 * @return l'ensemble de toutes les déclarations du contribuable (la collection retournée n'est pas modifiable ni impactée par les ajouts au travers de la méthode
		 * {@link #addNouvelleDeclaration(ContribuableImpositionPersonnesMorales, DeclarationImpotOrdinairePM)})
		 */
		@NotNull
		public Collection<DeclarationImpotOrdinairePM> getDeclarations(ContribuableImpositionPersonnesMorales ctb) {
			return Collections.unmodifiableCollection(new ArrayList<>(_getDeclarations(ctb)));
		}

		/**
		 * @param ctb contribuable personne morale
		 * @return l'ensemble de toutes les déclarations du contribuable de la période fiscale (la collection retournée n'est pas modifiable ni impactée par les ajouts au travers de la méthode
		 * {@link #addNouvelleDeclaration(ContribuableImpositionPersonnesMorales, DeclarationImpotOrdinairePM)})
		 */
		@NotNull
		public Collection<DeclarationImpotOrdinairePM> getDeclarationsSurPeriodeFiscale(ContribuableImpositionPersonnesMorales ctb) {
			final Collection<DeclarationImpotOrdinairePM> toutes = _getDeclarations(ctb);
			final List<DeclarationImpotOrdinairePM> surPeriode = new ArrayList<>(toutes.size());
			for (DeclarationImpotOrdinairePM di : toutes) {
				if (di.getPeriode().getAnnee().intValue() == periodeFiscale.getAnnee().intValue()) {
					surPeriode.add(di);
				}
			}
			return Collections.unmodifiableCollection(surPeriode);
		}

		/**
		 * Permet d'enregistrer dans le cache une déclaration nouvellement créée.
		 * @param ctb contribuable personne morale
		 * @param di nouvelle déclaration à enregistrer
		 */
		public void addNouvelleDeclaration(ContribuableImpositionPersonnesMorales ctb, DeclarationImpotOrdinairePM di) {
			final Collection<DeclarationImpotOrdinairePM> toutes = _getDeclarations(ctb);
			toutes.add(di);
		}
	}

	/**
	 * Requête d'extraction des identifiants de contribuable PM concernés par l'envoi en masse
	 */
	private static final String HQL_CTB =
			"SELECT DISTINCT tache.contribuable.id FROM TacheEnvoiDeclarationImpotPM AS tache WHERE tache.annulationDate IS NULL AND tache.etat = 'EN_INSTANCE' AND tache.dateFin BETWEEN :debut AND :fin AND tache.typeDocument in (:typesDoc) AND tache.typeContribuable in (:typesCtb) ORDER BY tache.contribuable.id ASC";

	private List<Long> getIdsContribuables(final CategorieEnvoiDIPM categorieEnvoi, final int periodeFiscale) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(status -> hibernateTemplate.execute(session -> {
			final Query query = session.createQuery(HQL_CTB);
			query.setParameter("debut", RegDate.get(periodeFiscale, 1, 1));
			query.setParameter("fin", RegDate.get(periodeFiscale, 12, 31));
			query.setParameterList("typesDoc", categorieEnvoi.getTypesDocument());
			query.setParameterList("typesCtb", categorieEnvoi.getTypesContribuables());
			//noinspection unchecked
			return (List<Long>) query.list();
		}));
	}

	/**
	 * Requête d'extraction des tâches correspondant à un ensembles de contribuables
	 */
	private static final String HQL_TACHE =
			"SELECT tache FROM TacheEnvoiDeclarationImpotPM AS tache WHERE tache.annulationDate IS NULL AND tache.etat = 'EN_INSTANCE' AND tache.dateFin BETWEEN :debut AND :fin AND tache.typeDocument in (:typesDoc) AND tache.contribuable.id in (:ids) AND tache.typeContribuable in (:typesCtb) ORDER BY tache.contribuable.id ASC, tache.id ASC";

	private Iterator<TacheEnvoiDeclarationImpotPM> getTaches(final CategorieEnvoiDIPM categorieEnvoi, final int periodeFiscale, final Collection<Long> idsContribuables) {
		return hibernateTemplate.execute(session -> {
			final Query query = session.createQuery(HQL_TACHE);
			query.setParameter("debut", RegDate.get(periodeFiscale, 1, 1));
			query.setParameter("fin", RegDate.get(periodeFiscale, 12, 31));
			query.setParameterList("typesDoc", categorieEnvoi.getTypesDocument());
			query.setParameterList("typesCtb", categorieEnvoi.getTypesContribuables());
			query.setParameterList("ids", idsContribuables);
			//noinspection unchecked
			return (Iterator<TacheEnvoiDeclarationImpotPM>) query.iterate();
		});
	}
}
