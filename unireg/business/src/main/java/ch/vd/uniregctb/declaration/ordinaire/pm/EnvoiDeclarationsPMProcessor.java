package ch.vd.uniregctb.declaration.ordinaire.pm;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.TicketService;
import ch.vd.uniregctb.common.TicketTimeoutException;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationGenerationOperation;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscalePM;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPM;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeEtatTache;

public class EnvoiDeclarationsPMProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnvoiDeclarationsPMProcessor.class);

	private final TiersService tiersService;
	private final HibernateTemplate hibernateTemplate;
	private final ModeleDocumentDAO modeleDAO;
	private final PeriodeFiscaleDAO periodeDAO;
	private final DelaisService delaisService;
	private final DeclarationImpotService declarationImpotService;
	private final AssujettissementService assujettissementService;
	private final PeriodeImpositionService periodeImpositionService;
	private final int tailleLot;
	private final PlatformTransactionManager transactionManager;
	private final ParametreAppService parametres;
	private final AdresseService adresseService;
	private final TicketService ticketService;

	public EnvoiDeclarationsPMProcessor(TiersService tiersService, HibernateTemplate hibernateTemplate, ModeleDocumentDAO modeleDAO, PeriodeFiscaleDAO periodeDAO, DelaisService delaisService, DeclarationImpotService declarationImpotService,
	                                    AssujettissementService assujettissementService, PeriodeImpositionService periodeImpositionService, int tailleLot, PlatformTransactionManager transactionManager, ParametreAppService parametres,
	                                    AdresseService adresseService, TicketService ticketService) {
		this.tiersService = tiersService;
		this.hibernateTemplate = hibernateTemplate;
		this.modeleDAO = modeleDAO;
		this.periodeDAO = periodeDAO;
		this.delaisService = delaisService;
		this.declarationImpotService = declarationImpotService;
		this.assujettissementService = assujettissementService;
		this.periodeImpositionService = periodeImpositionService;
		this.tailleLot = tailleLot;
		this.transactionManager = transactionManager;
		this.parametres = parametres;
		this.adresseService = adresseService;
		this.ticketService = ticketService;
	}

	/**
	 * C'est ici que ça commence réellement...
	 */
	public EnvoiDIsPMResults run(final int periodeFiscale,
	                             final TypeDeclarationImpotPM typeDeclaration,
	                             final RegDate dateLimiteBouclements,
	                             @Nullable final Integer nbMaxEnvois,
	                             final RegDate dateTraitement,
	                             final int nbThreads,
	                             StatusManager s) throws DeclarationException {

		final StatusManager status = s == null ? new LoggingStatusManager(LOGGER) : s;
		final EnvoiDIsPMResults rapportFinal = new EnvoiDIsPMResults(dateTraitement, nbThreads, typeDeclaration, periodeFiscale, dateLimiteBouclements, nbMaxEnvois);

		status.setMessage("Récupération des contribuables à traiter...");
		final List<Long> idsContribuables = getIdsContribuables(typeDeclaration, periodeFiscale);

		// Traitement des contribuables par lots
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, EnvoiDIsPMResults> template = new ParallelBatchTransactionTemplateWithResults<>(idsContribuables, tailleLot, nbThreads, Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                        transactionManager, status, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, EnvoiDIsPMResults>() {
			@Override
			public EnvoiDIsPMResults createSubRapport() {
				return new EnvoiDIsPMResults(dateTraitement, nbThreads, typeDeclaration, periodeFiscale, dateLimiteBouclements, nbMaxEnvois);
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
					batch = batch.subList(0, reducedSize);
				}

				if (!batch.isEmpty()) {
					traiterBatch(batch, rapport, typeDeclaration, dateLimiteBouclements, dateTraitement);
				}

				return !rapportFinal.interrompu && (nbMaxEnvois == null || rapportFinal.getEnvoyees().size() + batch.size() < nbMaxEnvois);

			}
		}, progressMonitor);

		if (status.interrupted()) {
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
	 * @param typeDeclaration type de document à générer
	 * @param dateLimiteBouclements date limite (incluse) des bouclements à prendre en compte
	 * @param dateTraitement date du traitement
	 */
	private void traiterBatch(Collection<Long> idsContribuables, EnvoiDIsPMResults rapport, TypeDeclarationImpotPM typeDeclaration, RegDate dateLimiteBouclements, RegDate dateTraitement) throws DeclarationException {
		final PeriodeFiscale pf = periodeDAO.getPeriodeFiscaleByYear(rapport.getPeriodeFiscale());
		if (pf == null) {
			throw new DeclarationException("Période fiscale " + rapport.getPeriodeFiscale() + " inexistante!");
		}
		rapport.addLotContribuablesVus(idsContribuables.size());
		final InformationsFiscales informationsFiscales = new InformationsFiscales(pf);
		final Iterator<TacheEnvoiDeclarationImpotPM> tacheIterator = getTaches(typeDeclaration, rapport.getPeriodeFiscale(), idsContribuables);
		while (tacheIterator.hasNext()) {
			final TacheEnvoiDeclarationImpotPM tache = tacheIterator.next();
			traiterTache(tache, rapport, typeDeclaration, dateLimiteBouclements, dateTraitement, informationsFiscales);
		}
	}

	/**
	 * Traitement d'une tâche d'envoi de DI PM (auparavant, on verrouille la création de DI sur ce contribuable)
	 * @param tache la tâche à traiter
	 * @param rapport rapport à remplir
	 * @param typeDeclaration type de document à générer
	 * @param dateLimiteBouclements date limite (incluse) des bouclements à prendre en compte
	 * @param dateTraitement date de traitement
	 * @param informationsFiscales accesseurs vers les informations fiscales "lourdes" d'un contribuable
	 */
	private void traiterTache(TacheEnvoiDeclarationImpotPM tache, EnvoiDIsPMResults rapport, TypeDeclarationImpotPM typeDeclaration, RegDate dateLimiteBouclements, RegDate dateTraitement, InformationsFiscales informationsFiscales) throws DeclarationException {
		final ContribuableImpositionPersonnesMorales pm = tache.getContribuable();
		final DeclarationGenerationOperation tickettingKey = new DeclarationGenerationOperation(pm.getNumero());
		try {
			final TicketService.Ticket ticket = ticketService.getTicket(tickettingKey, 500);
			try {
				traiterTache(tache, pm, rapport, typeDeclaration, dateLimiteBouclements, dateTraitement, informationsFiscales);
			}
			finally {
				ticketService.releaseTicket(ticket);
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
	 * @param typeDeclaration type de document à générer
	 * @param dateLimiteBouclements date limite (incluse) des bouclements à prendre en compte
	 * @param dateTraitement date de traitement
	 * @param informationsFiscales accesseurs vers les informations fiscales "lourdes" d'un contribuable  @throws DeclarationException en cas de souci
	 */
	private void traiterTache(TacheEnvoiDeclarationImpotPM tache, ContribuableImpositionPersonnesMorales pm, EnvoiDIsPMResults rapport, TypeDeclarationImpotPM typeDeclaration,
	                          RegDate dateLimiteBouclements, RegDate dateTraitement, InformationsFiscales informationsFiscales) throws DeclarationException {

		// TODO faut-il vérifier que la tâche est toujours d'actualité ?

		// quelles sont les déclarations existantes sur la PF pour ce contribuable ?
		final Collection<DeclarationImpotOrdinairePM> declarationsSurPeriode = informationsFiscales.getDeclarationsSurPeriodeFiscale(pm);
		if (!declarationsSurPeriode.isEmpty()) {
			final List<DeclarationImpotOrdinairePM> trieesNonAnnuleesSurPeriode = AnnulableHelper.sansElementsAnnules(declarationsSurPeriode);
			Collections.sort(trieesNonAnnuleesSurPeriode, new DateRangeComparator<>());
			if (!trieesNonAnnuleesSurPeriode.isEmpty()) {
				if (DateRangeHelper.intersect(tache, trieesNonAnnuleesSurPeriode)) {
					// ahah... il y a ou bien conflit ou bien redondance...
					final DeclarationImpotOrdinairePM diDebutTache = DateRangeHelper.rangeAt(trieesNonAnnuleesSurPeriode, tache.getDateDebut());
					final DeclarationImpotOrdinairePM diFinTache = DateRangeHelper.rangeAt(trieesNonAnnuleesSurPeriode, tache.getDateFin());
					if (diDebutTache == null || diFinTache == null || diDebutTache != diFinTache || areIncompatibles(diDebutTache, tache)) {
						// cas de conflit
						final String msg = String.format("La tâche d'envoi de déclaration PM %d %s est en conflit avec les déclarations existantes du contribuable %s. Aucune nouvelle déclaration n'est créée et la tâche reste en instance.",
						                                 tache.getId(), DateRangeHelper.toDisplayString(tache), FormatNumeroHelper.numeroCTBToDisplay(pm.getNumero()));
						Audit.error(msg);
						rapport.addCollisionAvecDi(pm.getNumero(), msg);
					}
					else {
						// en fait, la DI correspond tout pile à la tâche... on va juste marquer la tâche comme traitée
						Audit.warn(String.format("Une déclaration correspond déjà à la tâche %d %s du contribuable %s. Aucune nouvelle déclaration n'est créée et la tâche est considérée comme traitée.",
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
		di.setTiers(pm);
		di.setNumero(getNewSequenceNumber(pm, informationsFiscales));
		di.setCodeControle(getNewCodeControle(pm, informationsFiscales));
		di.setPeriode(informationsFiscales.getPeriodeFiscale());

		// TODO le type de document doit passer dans le modèle de document choisi...

		final DeclarationImpotOrdinairePM savedDi = hibernateTemplate.merge(di);

		// ajout de l'état initial de la DI
		savedDi.addEtat(new EtatDeclarationEmise(dateTraitement));

		// ajout du délai initial de retour
		// TODO prendre en compte la présence de mandataire général...
		final boolean hasMandatGeneral = false;
		final DatesDelaiInitial datesDelaiInitial = getDelaiInitialRetour(tache.getTypeContribuable(), hasMandatGeneral,
		                                                                  tache.getDateFin(), dateTraitement, informationsFiscales.getPeriodeFiscale());
		final DelaiDeclaration delaiInitial = new DelaiDeclaration();
		delaiInitial.setConfirmationEcrite(Boolean.FALSE);
		delaiInitial.setDateDemande(dateTraitement);
		delaiInitial.setDateTraitement(dateTraitement);
		delaiInitial.setDelaiAccordeAu(datesDelaiInitial.getDateEffective());
		savedDi.addDelai(delaiInitial);

		// emvoyer le document à l'éditique
		// TODO composition du document et envoi...

		informationsFiscales.addNouvelleDeclaration(pm, savedDi);
		rapport.addDiEnvoyee(pm.getNumero(), tache.getDateDebut(), tache.getDateFin());
		tache.setEtat(TypeEtatTache.TRAITE);
	}

	/**
	 * Structure de données qui contient les deux dates importantes dans le délai initial :
	 * la date imprimée sur le document et la date effective, qui peuvent être (ou pas) différentes
	 */
	private static class DatesDelaiInitial {

		private final RegDate dateImprimee;
		private final RegDate dateEffective;

		public DatesDelaiInitial(@NotNull RegDate dateImprimee, @NotNull RegDate dateEffective) {
			this.dateImprimee = dateImprimee;
			this.dateEffective = dateEffective;
		}

		/**
		 * @param seuil date minimale acceptable
		 * @return une nouvelle instance (ou pas, selon nécessité) qui prend en compte le seuil
		 */
		public DatesDelaiInitial auPlusTot(@NotNull RegDate seuil) {
			if (seuil.isAfter(dateImprimee) || seuil.isAfter(dateEffective)) {
				return new DatesDelaiInitial(RegDateHelper.maximum(dateImprimee, seuil, NullDateBehavior.EARLIEST),
				                             RegDateHelper.maximum(dateEffective, seuil, NullDateBehavior.EARLIEST));
			}
			else {
				return this;
			}
		}

		public RegDate getDateImprimee() {
			return dateImprimee;
		}

		public RegDate getDateEffective() {
			return dateEffective;
		}
	}

	/**
	 * Calcul de la date initiale limite de retour (brutte, car uniquement basée sur la date de bouclement, sans tenir compte de la date d'émission,
	 * qui pourrait en théorie être très éloignée de la date de bouclement en cas de rattrapage)
	 * @param typeContribuable type de contribuable : VD, HC, HS
	 * @param hasMandatGeneral <code>true</code> si la personne morale possède un mandataire général pour la PF, <code>false</code> sinon
	 * @param dateBouclement date de bouclement (= date de fin de la période d'imposition)
	 * @param pf la période fiscale de la DI émise
	 * @return le couple de date (imprimée/effective) à utiliser pour le délai initial de retour
	 */
	@NotNull
	private DatesDelaiInitial getDelaiInitialBrutRetour(TypeContribuable typeContribuable, boolean hasMandatGeneral, RegDate dateBouclement, PeriodeFiscale pf) throws DeclarationException {
		final ParametrePeriodeFiscalePM params = pf.getParametrePeriodeFiscalePM(typeContribuable);
		if (params == null) {
			throw new DeclarationException("Pas de paramètrage trouvé pour le type de contribuable " + typeContribuable + " et la PF " + pf.getAnnee());
		}
		final RegDate delaiImprime = appliquerDelaiEnJour(dateBouclement, hasMandatGeneral ? params.getDelaiImprimeAvecMandataireDepuisBouclement() : params.getDelaiImprimeDepuisBouclement());
		final RegDate delaiEffectif = appliquerDelaiEnJour(dateBouclement, hasMandatGeneral ? params.getDelaiEffectifAvecMandataireDepuisBouclement() : params.getDelaiEffectifDepuisBouclement());
		return new DatesDelaiInitial(delaiImprime, delaiEffectif);
	}

	/**
	 * Applique le délai donné (en jours) et se place ensuite à la fin du mois
	 * @param dateSource date de départ du délai
	 * @param jours nombre de jours à utiliser
	 * @return date à utiliser comme date limite
	 */
	@NotNull
	private static RegDate appliquerDelaiEnJour(RegDate dateSource, int jours) {
		return dateSource.addDays(jours).getLastDayOfTheMonth();
	}

	/**
	 * Calcul de la date initiale limite de retour (brutte, car uniquement basée sur la date de bouclement, sans tenir compte de la date d'émission,
	 * qui pourrait en théorie être très éloignée de la date de bouclement en cas de rattrapage)
	 * @param typeContribuable type de contribuable : VD, HC, HS
	 * @param hasMandatGeneral <code>true</code> si la personne morale possède un mandataire général pour la PF, <code>false</code> sinon
	 * @param dateBouclement date de bouclement (= date de fin de la période d'imposition)
	 * @param dateEmission date de l'émission de la DI
	 * @param pf la période fiscale de la DI émise
	 * @return le couple de date (imprimée/effective) à utiliser pour le délai initial de retour
	 */
	@NotNull
	private DatesDelaiInitial getDelaiInitialRetour(TypeContribuable typeContribuable, boolean hasMandatGeneral, RegDate dateBouclement, RegDate dateEmission, PeriodeFiscale pf) throws DeclarationException {
		final RegDate dateMinimale = dateEmission.addMonths(parametres.getDelaiMinimalRetourDeclarationImpotPM());
		final DatesDelaiInitial brut = getDelaiInitialBrutRetour(typeContribuable, hasMandatGeneral, dateBouclement, pf);
		return brut.auPlusTot(dateMinimale);
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
	 * Génère un nouveau code de contrôle différent de tous les codes de contrôles jusqu'ici utilisés pour le contribuable
	 * @param pm le contribuable personne morale
	 * @param informationsFiscales les données fiscales
	 * @return un nouveau code de contrôle unique
	 */
	private static String getNewCodeControle(ContribuableImpositionPersonnesMorales pm, InformationsFiscales informationsFiscales) {
		// faisons le tour de tous les codes de contrôles existant
		final Collection<DeclarationImpotOrdinairePM> toutes = informationsFiscales.getDeclarations(pm);
		final Set<String> codesExistants = new HashSet<>(toutes.size());
		for (DeclarationImpotOrdinairePM di : toutes) {
			codesExistants.add(di.getCodeControle());
		}

		// on boucle la génération tant qu'on n'a pas quelque chose de neuf...
		String codeControle;
		do {
			codeControle = DeclarationImpotOrdinairePM.generateCodeControle();
		}
		while (codesExistants.contains(codeControle));
		return codeControle;
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
		return di.getTypeDeclaration() != tache.getTypeDocument() || !DateRangeHelper.equals(di, tache);
	}

	/**
	 * Cache des informations fiscales des contribuables personnes morales d'un lot
	 */
	private final class InformationsFiscales {

		private final Map<Long, List<PeriodeImpositionPersonnesMorales>> periodesImposition = new HashMap<>();
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
			final List<Assujettissement> saved = computed == null ? Collections.<Assujettissement>emptyList() : computed;
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
		 * @return les périodes d'imposition de la personne morale, calculées une fois et maintenues en cache pour les appels ultérieurs
		 * @throws AssujettissementException en cas de souci
		 */
		@NotNull
		private List<PeriodeImpositionPersonnesMorales> _getPeriodesImposition(ContribuableImpositionPersonnesMorales ctb) throws AssujettissementException {
			if (periodesImposition.containsKey(ctb.getNumero())) {
				return periodesImposition.get(ctb.getNumero());
			}
			final List<PeriodeImposition> computed = periodeImpositionService.determine(ctb);
			final List<PeriodeImpositionPersonnesMorales> saved;
			if (computed == null || computed.isEmpty()) {
				saved = Collections.emptyList();
			}
			else {
				saved = new ArrayList<>(computed.size());
				for (PeriodeImposition pi : computed) {
					saved.add((PeriodeImpositionPersonnesMorales) pi);
				}
			}
			periodesImposition.put(ctb.getNumero(), saved);
			return saved;
		}

		/**
		 * @param ctb contribuable personne morale
		 * @return les périodes d'imposition de la personne morale, calculées une fois et maintenues en cache pour les appels ultérieurs (dans une collection non-modifiable pour être sûr de ne pas faire de bêtise)
		 * @throws DeclarationException en cas de souci
		 */
		@NotNull
		public List<PeriodeImpositionPersonnesMorales> getPeriodesImposition(ContribuableImpositionPersonnesMorales ctb) throws DeclarationException {
			try {
				return Collections.unmodifiableList(_getPeriodesImposition(ctb));
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
			final Set<Declaration> toutes = ctb.getDeclarations();
			final List<DeclarationImpotOrdinairePM> computed = new ArrayList<>(toutes.size());
			for (Declaration d : toutes) {
				if (d instanceof DeclarationImpotOrdinairePM) {
					computed.add((DeclarationImpotOrdinairePM) d);
				}
			}
			declarations.put(ctb.getNumero(), computed);
			return computed;
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
			"SELECT DISTINCT tache.contribuable.id FROM TacheEnvoiDeclarationImpotPM AS tache WHERE tache.annulationDate IS NULL AND tache.etat = 'EN_INSTANCE' AND tache.dateFin BETWEEN :debut AND :fin AND tache.typeDocument = :typeDoc ORDER BY tache.contribuable.id ASC";

	private List<Long> getIdsContribuables(final TypeDeclarationImpotPM typeDeclaration, final int periodeFiscale) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
						final Query query = session.createQuery(HQL_CTB);
						query.setParameter("debut", RegDate.get(periodeFiscale, 1, 1));
						query.setParameter("fin", RegDate.get(periodeFiscale, 12, 31));
						query.setParameter("typeDoc", typeDeclaration.getTypeDocument());
						//noinspection unchecked
						return query.list();
					}
				});
			}
		});
	}

	/**
	 * Requête d'extraction des tâches correspondant à un ensembles de contribuables
	 */
	private static final String HQL_TACHE =
			"SELECT tache FROM TacheEnvoiDeclarationImpotPM AS tache WHERE tache.annulationDate IS NULL AND tache.etat = 'EN_INSTANCE' AND tache.dateFin BETWEEN :debut AND :fin AND tache.typeDocument =:typeDoc AND tache.contribuable.id in (:ids) ORDER BY tache.contribuable.id ASC, tache.id ASC";

	private Iterator<TacheEnvoiDeclarationImpotPM> getTaches(final TypeDeclarationImpotPM typeDeclaration, final int periodeFiscale, final Collection<Long> idsContribuables) {
		return hibernateTemplate.execute(new HibernateCallback<Iterator<TacheEnvoiDeclarationImpotPM>>() {
			@Override
			public Iterator<TacheEnvoiDeclarationImpotPM> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(HQL_TACHE);
				query.setParameter("debut", RegDate.get(periodeFiscale, 1, 1));
				query.setParameter("fin", RegDate.get(periodeFiscale, 12, 31));
				query.setParameter("typeDoc", typeDeclaration.getTypeDocument());
				query.setParameterList("ids", idsContribuables);
				//noinspection unchecked
				return query.iterate();
			}
		});
	}
}
