package ch.vd.uniregctb.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersDAO;

/**
 * Classe qui permet de validation la cohérence des données d'un tiers dans un thread autonome.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ValidationJobThread extends Thread {

	private static final Logger LOGGER = Logger.getLogger(ValidationJobThread.class);

	private static final int BATCH_SIZE = 100;

	private final TiersDAO tiersDAO;
	private final PlatformTransactionManager transactionManager;
	private final AdresseService adresseService;
	private final int premiereAnneeFiscale;
	private final ValidationService validationService;
	private final PeriodeImpositionService periodeImpositionService;

	private final BlockingQueue<Long> queue;
	private final ValidationJobResults results; // accès concurrents sur cette variable !

	public ValidationJobThread(BlockingQueue<Long> queue, ValidationJobResults results, TiersDAO tiersDAO,
	                           PlatformTransactionManager transactionManager, AdresseService adresseService,
	                           ParametreAppService parametreService, ValidationService validationService, PeriodeImpositionService periodeImpositionService) {
		this.queue = queue;
		this.results = results;
		this.tiersDAO = tiersDAO;
		this.transactionManager = transactionManager;
		this.adresseService = adresseService;
		this.validationService = validationService;
		this.periodeImpositionService = periodeImpositionService;

		this.premiereAnneeFiscale = parametreService.getPremierePeriodeFiscale();
	}

	@Override
	public void run() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		boolean continueProcessing = true;

		// Valide les tiers dans la queue en procédant par batchs, ceci pour limiter le nombre d'objets en mémoire
		do {
			continueProcessing = template.execute(new TransactionCallback<Boolean>() {
				@Override
				public Boolean doInTransaction(TransactionStatus status) {
					try {
						processBatch();
					}
					catch (InterruptedException e) {
						return Boolean.FALSE;
					}
					return Boolean.TRUE;
				}
			});
		} while (continueProcessing);
	}

	private void processBatch() throws InterruptedException {

		for (int i = 0; i < BATCH_SIZE; ++i) {

			final Long id = queue.take();
			Assert.notNull(id);

			results.incCtbsTotal();

			final Contribuable contribuable = (Contribuable) tiersDAO.get(id);
			if (contribuable == null || contribuable.isAnnule()) {
				continue;
			}

			checkValidation(contribuable, results);

			if (results.calculatePeriodesImposition) {
				checkPeriodesImposition(contribuable, results);
			}

			if (results.calculateAdresses) {
				checkAdresses(contribuable, results);
			}
		}
	}

	private void checkValidation(final Contribuable contribuable, ValidationJobResults results) {
		final ValidationResults r = validationService.validate(contribuable);
		if (r.hasErrors()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Le contribuable n°" + contribuable.getNumero() + " est invalide");
			}
			results.addErrorCtbInvalide(contribuable, r);
		}
	}

	private void checkPeriodesImposition(final Contribuable contribuable, ValidationJobResults results) {

		final RegDate aujourdhui = RegDate.get();
		final RegDate dateDebut = contribuable.getDateDebutActivite();
		final RegDate dateFin = contribuable.getDateFinActivite();

		final int anneeDebut = Math.max(premiereAnneeFiscale, (dateDebut == null ? aujourdhui.year() : dateDebut.year()));
		final int anneeFin = Math.max(premiereAnneeFiscale, (dateFin == null ? aujourdhui.year() : dateFin.year()));

		for (int annee = anneeDebut; annee <= anneeFin; ++annee) {
			try {
				// on vérifie les périodes d'imposition
				final List<PeriodeImposition> imposition = periodeImpositionService.determine(contribuable, annee);
				if (results.coherencePeriodesImpositionWrtDIs) {
					checkCoherencePeriodeImpositionAvecDI(contribuable, results, annee, imposition);
				}
			}
			catch (Exception e) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Calcul impossible de la période d'imposition pour le contribuable n°" + contribuable.getNumero() + " et l'année" + annee);
				}
				results.addErrorPeriodeImposition(contribuable, annee, e);
			}
		}
	}

	private static void checkCoherencePeriodeImpositionAvecDI(Contribuable contribuable, ValidationJobResults results, int annee, List<PeriodeImposition> periodesImposition) {

		// filtrage des di annulées
		final List<Declaration> toutesDIs = contribuable.getDeclarationsForPeriode(annee, false);
		List<Declaration> dis = toutesDIs == null || toutesDIs.isEmpty() ? null : new ArrayList<Declaration>(toutesDIs.size());
		if (dis != null) {
			for (Declaration di : toutesDIs) {
				if (!di.isAnnule()) {
					dis.add(di);
				}
			}
			if (dis.isEmpty()) {
				// toutes ont été annulées !
				dis = null;
			}
		}

		checkCoherencePeriodesImpositionAvecDIs(contribuable, results, annee, periodesImposition, dis);
	}

	private static void checkCoherencePeriodesImpositionAvecDIs(Contribuable contribuable, ValidationJobResults results, int annee, List<PeriodeImposition> periodesImposition, List<Declaration> declarations) {

		if (periodesImposition == null && declarations == null) {
			// pas de période d'imposition ni di -> pas de problème
		}
		else if (periodesImposition == null) {
			// pas de période d'imposition et au moins une DI
			for (Declaration di : declarations) {
				addErrorCoherenceDiDiSansPeriodeImposition(contribuable, results, di);
			}
		}
		else if (declarations == null) {
			// au moins une période d'imposition, mais pas de DI (pas grave si c'est l'année en cours...)
			if (annee < RegDate.get().year()) {
				for (PeriodeImposition pi : periodesImposition) {
					if (!pi.isOptionnelle() && !pi.isRemplaceeParNote() && !pi.isDiplomateSuisseSansImmeuble()) {
						addErrorCoherenceDiPeriodeImpositionSansDi(contribuable, results, pi);
					}
				}
			}
		}
		else {
			// une (ou plusieurs) DI, une (ou plusieurs) périodes d'imposition

			// ce sont les DI déjà utilisées (bonnes dates, ou mauvaises dates à corriger) dans une periode d'imposition
			// d'une autre itération de la boucle des périodes d'imposition
			final Set<Declaration> diUtilisees = new HashSet<Declaration>(declarations.size());

			// on va d'abord écarter les DI (et les périodes d'imposition) qui matchent parfaitement
			for (Declaration di : declarations) {
				final Iterator<PeriodeImposition> iter = periodesImposition.iterator();
				while (iter.hasNext()) {
					final PeriodeImposition pi = iter.next();
					if (DateRangeHelper.equals(di, pi)) {
						diUtilisees.add(di);
						iter.remove();
					}
				}
			}

			// ensuite, on prend les DI et PI qui s'intersectent
			final Iterator<PeriodeImposition> iterPeriodes = periodesImposition.iterator();
			while (iterPeriodes.hasNext()) {
				final List<Declaration> diCandidates = new ArrayList<Declaration>(declarations.size() - diUtilisees.size());
				final PeriodeImposition pi = iterPeriodes.next();

				// au cas où plusieurs DI intersectent la période d'imposition sans la recouvrir tout à fait
				// on prend la première complètement incluse dans la période d'imposition pour changer ses
				// dates, et à défaut la première tout court...
				for (Declaration di : declarations) {
					if (!diUtilisees.contains(di) && DateRangeHelper.intersect(di, pi)) {
						diCandidates.add(di);
					}
				}

				if (!diCandidates.isEmpty()) {
					Declaration di = null;
					for (Declaration candidate : diCandidates) {
						if (di == null) {
							di = candidate;
						}
						else if (pi.isValidAt(candidate.getDateDebut()) && pi.isValidAt(candidate.getDateFin())) {
							// complètement incluse dans la période d'imposition
							di = candidate;
							break;
						}
					}
					diUtilisees.add(di);
					addErrorCoherenceDiMauvaisesDates(contribuable, results, pi, di);
					iterPeriodes.remove();
				}
			}

			// ensuite, on associe un à un les élements restant dans chacune des collections
			final int nbDeclarationNonAssociees = declarations.size() - diUtilisees.size();
			if (nbDeclarationNonAssociees > 0 && !periodesImposition.isEmpty()) {
				final List<Declaration> diNonUtilisees = new ArrayList<Declaration>(nbDeclarationNonAssociees);
				for (Declaration di : declarations) {
					if (!diUtilisees.contains(di)) {
						diNonUtilisees.add(di);
					}
				}
				final int nbElementsAssocies = Math.min(nbDeclarationNonAssociees, periodesImposition.size());
				for (int i = 0 ; i < nbElementsAssocies ; ++ i) {
					final Declaration di = diNonUtilisees.get(i);
					final PeriodeImposition pi = periodesImposition.get(i);
					addErrorCoherenceDiMauvaisesDates(contribuable, results, pi, di);
					diUtilisees.add(di);
				}
			}
			
			// et pour finir, il ne reste que des éléments seuls (uniquement des DI non-utilisées ou uniquement des PI non-couvertes)
			if (nbDeclarationNonAssociees < periodesImposition.size()) {
				// il ne reste que des périodes d'imposition sans DI
				for (PeriodeImposition pi : periodesImposition.subList(nbDeclarationNonAssociees, periodesImposition.size())) {
					if (!pi.isOptionnelle() && !pi.isRemplaceeParNote() && !pi.isDiplomateSuisseSansImmeuble()) {
						addErrorCoherenceDiPeriodeImpositionSansDi(contribuable, results, pi);
					}
				}
			}
			else if (nbDeclarationNonAssociees > periodesImposition.size()) {
				// il ne reste que des DI sans périodes d'imposition
				for (Declaration di : declarations) {
					if (!diUtilisees.contains(di)) {
						addErrorCoherenceDiDiSansPeriodeImposition(contribuable, results, di);
					}
				}
			}
		}
	}

	private static void addErrorCoherenceDiMauvaisesDates(Contribuable contribuable, ValidationJobResults results, PeriodeImposition pi, Declaration di) {
		final String message = String.format("Trouvé une déclaration (%d) du %s au %s pour la période d'imposition du %s au %s sur le contribuable n°%d",
											di.getId(), di.getDateDebut(), di.getDateFin(), pi.getDateDebut(), pi.getDateFin(), contribuable.getNumero());
		addErrorCoherenceDi(contribuable, results, message);
	}

	private static void addErrorCoherenceDiPeriodeImpositionSansDi(Contribuable contribuable, ValidationJobResults results, PeriodeImposition pi) {
		final String message = String.format("Trouvé période d'imposition du %s au %s sans déclaration sur le contribuable n°%d",
											pi.getDateDebut(), pi.getDateFin(), contribuable.getNumero());
		addErrorCoherenceDi(contribuable, results, message);
	}

	private static void addErrorCoherenceDiDiSansPeriodeImposition(Contribuable contribuable, ValidationJobResults results, Declaration di) {
		final String message = String.format("Trouvé déclaration (%d) du %s au %s sans période d'imposition sur le contribuable n°%d",
											di.getId(), di.getDateDebut(), di.getDateFin(), contribuable.getNumero());
		addErrorCoherenceDi(contribuable, results, message);
	}

	private static void addErrorCoherenceDi(Contribuable contribuable, ValidationJobResults results, String message) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(message);
		}
		results.addErrorCoherenceDi(contribuable, message);
	}

	private void checkAdresses(final Contribuable contribuable, ValidationJobResults results) {
		try {
			adresseService.getAdressesFiscalHisto(contribuable, true);
		}
		catch (Exception e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Calcul impossible des adresses historiques du contribuable n°" + contribuable.getNumero());
			}
			results.addErrorAdresses(contribuable, e);
		}
	}
}
