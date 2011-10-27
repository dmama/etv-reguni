package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.IdentifiantDeclaration;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.DecompositionForsAnneeComplete;
import ch.vd.uniregctb.metier.assujettissement.Indigent;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class EnvoiSommationsDIsProcessor  {

	/**
	 * Un logger pour {@link EnvoiSommationsDIsProcessor}
	 */
	private static final Logger LOGGER = Logger.getLogger(EnvoiSommationsDIsProcessor.class);

	private final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;
	private final DeclarationImpotOrdinaireDAO declarationImpotOrdinaireDAO;
	private final DelaisService delaisService;
	private final DeclarationImpotService diService;
	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;

	public EnvoiSommationsDIsProcessor(
			HibernateTemplate hibernateTemplate,
			DeclarationImpotOrdinaireDAO declarationImpotOrdinaireDAO,
			DelaisService delaisService,
			DeclarationImpotService diService,
			TiersService tiersService,
			PlatformTransactionManager transactionManager
	) {
		this.hibernateTemplate = hibernateTemplate;
		this.declarationImpotOrdinaireDAO = declarationImpotOrdinaireDAO;
		this.delaisService = delaisService;
		this.diService = diService;
		this.tiersService = tiersService;
		this.transactionManager = transactionManager;
	}

	protected boolean isSourcierPur(DeclarationImpotOrdinaire di, List<Assujettissement> assujettissements) {
		//Verification que l'assujettissement ne soit pas sourcier Pur à la date de la fin de di
		boolean isSourcierPur = false;
		for (Assujettissement a : assujettissements) {
			if (a.isValidAt(di.getDateFin())) {
			   	if (a instanceof SourcierPur) {
					isSourcierPur = true;
				}
				else {
					isSourcierPur = false;
					break;

				}

			}
		}
		return isSourcierPur;
	}

	public EnvoiSommationsDIsResults run(final RegDate dateTraitement, final boolean miseSousPliImpossible, final int nombreMax, StatusManager statusManager) {

		if (statusManager == null) {
			statusManager = new LoggingStatusManager(LOGGER);
		}
		final StatusManager status = statusManager;

		final EnvoiSommationsDIsResults rapportFinal = new EnvoiSommationsDIsResults();
		rapportFinal.setDateTraitement(dateTraitement);
		rapportFinal.setMiseSousPliImpossible(miseSousPliImpossible);
		rapportFinal.setNombreMaxSommations(nombreMax);

		status.setMessage(
			String.format(
				"Envoi des sommations pour les DI au %s (Récupération de la liste de DI)",
				RegDateHelper.dateToDisplayString(dateTraitement))
		);

		final List<IdentifiantDeclaration> dis = retrieveListIdDIs(dateTraitement);

		final BatchTransactionTemplate<IdentifiantDeclaration, EnvoiSommationsDIsResults> t = new BatchTransactionTemplate<IdentifiantDeclaration, EnvoiSommationsDIsResults>(dis, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		t.execute(rapportFinal, new BatchCallback<IdentifiantDeclaration, EnvoiSommationsDIsResults>() {

			int currentBatch = 0;

			@Override
			public EnvoiSommationsDIsResults createSubRapport() {
				return new EnvoiSommationsDIsResults();
			}

			@Override
			public boolean doInTransaction(List<IdentifiantDeclaration> batch, EnvoiSommationsDIsResults r) {
				currentBatch++;
				List<Long> numerosDis = getListNumerosDis(batch);
				try {
					final Set<DeclarationImpotOrdinaire> declarations = declarationImpotOrdinaireDAO.getDIsForSommation(numerosDis);
					final Iterator<DeclarationImpotOrdinaire> iter = declarations.iterator();
					while (iter.hasNext() && ! status.interrupted() && (nombreMax == 0 || (rapportFinal.getTotalDisSommees()  + r.getTotalDisSommees()) < nombreMax)) {
						final DeclarationImpotOrdinaire di = iter.next();
						traiterDI(di, r, dateTraitement, miseSousPliImpossible);
					}
				} finally {
					LOGGER.debug("Batch no " + currentBatch + " terminé");
				}
				return  (nombreMax == 0 || (rapportFinal.getTotalDisSommees()  + r.getTotalDisSommees() ) < nombreMax) && !status.interrupted();
			}

			@Override
			public void afterTransactionCommit() {
				int nombreTotal = dis.size();
				int percent = (100 * rapportFinal.getTotalDisTraitees()) / nombreTotal;
				status.setMessage(String.format(
						"%d sur %d déclarations d'impôt analysées : %d sommée(s), %d en erreur",
						rapportFinal.getTotalDisTraitees(), nombreTotal, rapportFinal.getTotalDisSommees(), rapportFinal.getTotalSommationsEnErreur()), percent);
			}
		});

		final String msg = String.format(
				"Envoi des sommations pour les DI au %s (traitement terminé; %d sommées, %d en erreur))",
				RegDateHelper.dateToDisplayString(dateTraitement),
				rapportFinal.getTotalDisSommees(),
				rapportFinal.getTotalSommationsEnErreur());
		LOGGER.info(msg);
		statusManager.setMessage(msg);
		rapportFinal.setInterrompu(statusManager.interrupted());
		rapportFinal.end();
		return rapportFinal;
	}

	private List<Long> getListNumerosDis(List<IdentifiantDeclaration> batch) {
		List<Long> ids = new ArrayList<Long>();

		for (IdentifiantDeclaration identifiantDeclaration : batch) {
			ids.add(identifiantDeclaration.getIdDeclaration());
		}
		return ids;
	}

	protected void traiterDI(DeclarationImpotOrdinaire di, EnvoiSommationsDIsResults r, RegDate dateTraitement, boolean miseSousPliImpossible) {
		// Verification des pré-requis avant la sommation
		if (checkEtat(di, r) && checkDateDelai(di, r) && checkContribuable(di, r)) {

			final RegDate finDelai = delaisService.getDateFinDelaiEnvoiSommationDeclarationImpot(di.getDelaiAccordeAu());
			if (finDelai.isBefore(dateTraitement)) {
				try {
					final List<Assujettissement> assujettissements = Assujettissement.determine((Contribuable) di.getTiers(), di.getPeriode().getAnnee());
					if (assujettissements == null || assujettissements.isEmpty()) {
						final String msg = String.format("La di [id: %d] n'a pas été sommée car le contribuable [%s] n'est pas assujetti pour la période fiscale %s",
						                                 di.getId(), di.getTiers().getNumero(), di.getPeriode().getAnnee());
						LOGGER.info(msg);
						r.addNonAssujettissement(di);
					}
					else if (isSourcierPur(di, assujettissements)) {
						final String msg = String.format("La di [id: %d] n'a pas été sommée car le contribuable [%s] est sourcier Pur au %s",
						                                 di.getId(), di.getTiers().getNumero(), RegDateHelper.dateToDisplayString(di.getDateFin()));
						LOGGER.info(msg);
						r.addSourcierPur(di);
					}
					else if (isIndigent(di,	assujettissements)) {
						final String msg = String.format("La di [id: %d] n'a pas été sommée car le contribuable [%s] est indigent au %s",
						                                 di.getId(), di.getTiers().getNumero(), RegDateHelper.dateToDisplayString(di.getDateFin()));
						LOGGER.info(msg);
						r.addIndigent(di);
					}
					else if (isOptionnelle(di, assujettissements)) {
						final String msg = String.format("La di [id: %d] du contribuable [%s] n'a pas été sommée car elle était optionelle",
						                                 di.getId(), di.getTiers().getNumero());
						LOGGER.info(msg);
						r.addDiOptionelle(di);
					}
					else if (isSurMenageSansPersonnesPhysiques(di)) {
						final String msg = String.format("La di [id: %d] n'a pas été sommée car le contribuable [%s] est un ménage commun dont les membres sont inconnus",
						                                 di.getId(), di.getTiers().getNumero());
						LOGGER.warn(msg);
						r.addError(di, msg);
					}
					else {
						sommerDI(di, miseSousPliImpossible, dateTraitement);
						LOGGER.info(String.format(
										"La di [id: %d; ctb: %d; periode: %d; debut: %s; fin: %s] a été sommée",
										di.getId(),
										di.getTiers().getNumero(),
										di.getPeriode().getAnnee(),
										RegDateHelper.dateToDisplayString(di.getDateDebut()),
										RegDateHelper.dateToDisplayString(di.getDateFin())));
						r.addDiSommee(di.getPeriode().getAnnee(), di);
					}
				}
				catch (RuntimeException e) {
					r.addError(di,e.getMessage());
					LOGGER.error(e.getMessage(), e);
					throw e;
				} catch (Exception e) {
					r.addError(di,e.getMessage());
					LOGGER.error(e.getMessage(), e);
					throw new RuntimeException(e);
				}
			} else {
				LOGGER.info(String.format("le délai de la DI (au %s) + le délai de sommation effective (au %s) n'est pas dépassé",
				                          RegDateHelper.dateToDisplayString(di.getDelaiAccordeAu()), RegDateHelper.dateToDisplayString(finDelai)));
			}
		}
	}

	private boolean checkDateDelai(DeclarationImpotOrdinaire di, EnvoiSommationsDIsResults r) {
		if (di.getDelaiAccordeAu() == null) {
			// Ce cas ne devrait plus se produire, toute les di devraient avoir un délai
			final String msg = String.format("La di [id: %s] n'a pas de délai, cela ne devrait pas être possible !", di.getNumero());
			LOGGER.error(msg);
			r.addError(di, msg);
			return false;
		}
		return true;
	}

	private boolean checkContribuable(DeclarationImpotOrdinaire di, EnvoiSommationsDIsResults r) {
		if (!(di.getTiers() instanceof Contribuable)) {
			// Ce cas ne devrait pas se produire, une di est forcement rattaché à un tiers qui est un contribuable.
			final String msg = String.format("Le tiers [%s] n'est pas un contribuable, on ne peut pas calculer son assujettisement", di.getId().toString());
			LOGGER.error(msg);
			r.addError(di, msg);
			return false;
		}
		return true;
	}

	private boolean checkEtat(DeclarationImpotOrdinaire di, EnvoiSommationsDIsResults r) {
		if (TypeEtatDeclaration.EMISE != di.getDernierEtat().getEtat()) {
			// Ce cas pourrait eventuellement se produire dans le cas ou une di aurait 2 états à la même date,
			// il s'agirait alors de donnée corrompue ...
			final String msg = String.format("La di [id: %s] n'est pas à l'état 'EMISE' et ne peut donc être sommée",	di.getId().toString());
			LOGGER.error(msg);
			r.addError(di, msg);
			return false;
		}
		return true;
	}

	private void sommerDI(final DeclarationImpotOrdinaire di, boolean miseSousPliImpossible, final RegDate dateTraitement) throws DeclarationException {

		RegDate dateExpedition = delaisService.getDateFinDelaiCadevImpressionDeclarationImpot(dateTraitement);
		EtatDeclarationSommee etat = new EtatDeclarationSommee(dateTraitement,dateExpedition);
		etat.setDeclaration(di);
		etat.setAnnule(false);
		di.addEtat(etat);

		diService.envoiSommationDIForBatch(di, miseSousPliImpossible, dateTraitement);
	}

	/**
	 * Si la DI était optionelle (ou remplacée par une note), alors il ne faut pas la sommer
	 */
	private boolean isOptionnelle(DeclarationImpotOrdinaire di, List<Assujettissement> assujettissements) {
		final DecompositionForsAnneeComplete fors = new DecompositionForsAnneeComplete((Contribuable) di.getTiers(), di.getPeriode().getAnnee());
		boolean optionnel = true;
		for (Assujettissement a : assujettissements) {
			final PeriodeImposition periodeImposition = PeriodeImposition.determinePeriodeImposition(fors, a);
			//Ajout du test afin de detecter les periodes d'imposition null
			//exemple: contribuable avec 2 types d'assujettissements sur la même période, sourcier et ensuite à l'ordinaire
			//la méthode doit prendre en compte les assujettissements autre que le type sourcier
			//  qui est caractérisé par un rsultat null lor du calcul de la période d'imposition.
			if (periodeImposition != null) {
				if (DateRangeHelper.intersect(di, periodeImposition)) {
					optionnel = periodeImposition.isOptionnelle() || periodeImposition.isRemplaceeParNote();
					if (!optionnel) {
						break;
					}
				}
			}

		}
		return optionnel;
	}

	/**
	 * Si la sommation doit être faite pour une DI dont le contribuable est un couple dont
	 * on ne connait pas les membres (ce qui rend vite les choses compliquées pour les adresses...)
	 * @param di la déclaration d'impôt à tester
	 * @return <code>true</code> si le tiers de la DI est un ménage dont le principal est inconnu
	 */
	private boolean isSurMenageSansPersonnesPhysiques(DeclarationImpotOrdinaire di) {
		final Tiers tiers = di.getTiers();
		boolean isSurMenageIncomplet = false;
		if (tiers instanceof MenageCommun) {
			isSurMenageIncomplet = tiersService.getPrincipal((MenageCommun) tiers) == null;
		}
		return isSurMenageIncomplet;
	}

	/**
	 * [UNIREG-1472] Verification que l'assujettissement ne soit pas indigent à la date de la fin de di
	 */
	protected boolean isIndigent(DeclarationImpotOrdinaire di, List<Assujettissement> assujettissements) {
		// [UNIREG-1472] Verification que l'assujettissement ne soit pas indigent à la date de la fin de di
		boolean isIndigent = false;
		for (Assujettissement a : assujettissements) {
			if (a.isValidAt(di.getDateFin())) {
				if (a instanceof Indigent) {
					isIndigent = true;
				} else {
					isIndigent = false;
					break;
				}
			}
		}
		return isIndigent;
	}

	@SuppressWarnings("unchecked")
	private List<IdentifiantDeclaration> retrieveListIdDIs(final RegDate dateLimite) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(new TransactionCallback<List<IdentifiantDeclaration>>() {
			@Override
			public List<IdentifiantDeclaration> doInTransaction(TransactionStatus status) {
				final List<IdentifiantDeclaration> identifiantDi = new ArrayList<IdentifiantDeclaration>();
				final List<Object[]> declarationsASommer = hibernateTemplate.execute(new HibernateCallback<List<Object[]>>() {
					@Override
					public List<Object[]> doInHibernate(Session session) throws HibernateException {

						final StringBuilder b = new StringBuilder();
						b.append("SELECT di.id, di.tiers.id FROM DeclarationImpotOrdinaire AS di");
						b.append(" WHERE di.annulationDate IS NULL");
						b.append(" AND EXISTS (SELECT etat.declaration.id FROM EtatDeclaration AS etat WHERE di.id = etat.declaration.id AND etat.annulationDate IS NULL AND etat.class = EtatDeclarationEmise)");
						b.append(" AND NOT EXISTS (SELECT etat.declaration.id FROM EtatDeclaration AS etat WHERE di.id = etat.declaration.id AND etat.annulationDate IS NULL AND etat.class IN (EtatDeclarationRetournee, EtatDeclarationSommee))");
						b.append(" AND EXISTS (SELECT delai.declaration.id FROM DelaiDeclaration AS delai WHERE di.id = delai.declaration.id AND delai.annulationDate IS NULL AND delai.delaiAccordeAu IS NOT NULL");
						b.append(" GROUP BY delai.declaration.id HAVING MAX(delai.delaiAccordeAu) < :dateLimite)");
						final String sql = b.toString();
						final Query query = session.createQuery(sql);
						query.setParameter("dateLimite", dateLimite.index());
						return query.list();
					}
				});
				for (Object[] objects : declarationsASommer) {
					final Number numeroDi = (Number) objects[0];
					final Number numeroTiers = (Number) objects[1];
					final IdentifiantDeclaration identifiantDeclaration = new IdentifiantDeclaration(numeroDi.longValue(), numeroTiers.longValue());
					identifiantDi.add(identifiantDeclaration);
				}

				return identifiantDi;
			}
		});
	}

}