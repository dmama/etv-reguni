package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.AddAndSaveHelper;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.EtatDeclarationSommee;
import ch.vd.unireg.declaration.IdentifiantDeclaration;
import ch.vd.unireg.declaration.ParametrePeriodeFiscaleEmolument;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscalAddAndSaveAccessor;
import ch.vd.unireg.hibernate.HibernateCallback;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.DecompositionForsAnneeComplete;
import ch.vd.unireg.metier.assujettissement.Indigent;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesPhysiques;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.metier.assujettissement.SourcierPur;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeDocumentEmolument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class EnvoiSommationsDIsPPProcessor {

	/**
	 * Un logger pour {@link EnvoiSommationsDIsPPProcessor}
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(EnvoiSommationsDIsPPProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;
	private final DeclarationImpotOrdinaireDAO declarationImpotOrdinaireDAO;
	private final DelaisService delaisService;
	private final DeclarationImpotService diService;
	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;
	private final AssujettissementService assujettissementService;
	private final PeriodeImpositionService periodeImpositionService;
	private final AdresseService adresseService;

	public EnvoiSommationsDIsPPProcessor(
			HibernateTemplate hibernateTemplate,
			DeclarationImpotOrdinaireDAO declarationImpotOrdinaireDAO,
			DelaisService delaisService,
			DeclarationImpotService diService,
			TiersService tiersService,
			PlatformTransactionManager transactionManager,
			AssujettissementService assujettissementService, PeriodeImpositionService periodeImpositionService, AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.declarationImpotOrdinaireDAO = declarationImpotOrdinaireDAO;
		this.delaisService = delaisService;
		this.diService = diService;
		this.tiersService = tiersService;
		this.transactionManager = transactionManager;
		this.assujettissementService = assujettissementService;
		this.periodeImpositionService = periodeImpositionService;
		this.adresseService = adresseService;
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

	public EnvoiSommationsDIsPPResults run(final RegDate dateTraitement, final boolean miseSousPliImpossible, final int nombreMax, StatusManager statusManager) {

		if (statusManager == null) {
			statusManager = new LoggingStatusManager(LOGGER);
		}
		final StatusManager status = statusManager;

		final EnvoiSommationsDIsPPResults rapportFinal = new EnvoiSommationsDIsPPResults(tiersService, adresseService);
		rapportFinal.setDateTraitement(dateTraitement);
		rapportFinal.setMiseSousPliImpossible(miseSousPliImpossible);
		rapportFinal.setNombreMaxSommations(nombreMax);

		status.setMessage(
			String.format(
				"Envoi des sommations pour les DI PP au %s (Récupération de la liste de DI)",
				RegDateHelper.dateToDisplayString(dateTraitement))
		);

		final List<IdentifiantDeclaration> dis = retrieveListIdDIs(dateTraitement);

		final BatchTransactionTemplateWithResults<IdentifiantDeclaration, EnvoiSommationsDIsPPResults> t = new BatchTransactionTemplateWithResults<>(dis, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		t.execute(rapportFinal, new BatchWithResultsCallback<IdentifiantDeclaration, EnvoiSommationsDIsPPResults>() {

			@Override
			public EnvoiSommationsDIsPPResults createSubRapport() {
				return new EnvoiSommationsDIsPPResults(tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<IdentifiantDeclaration> batch, EnvoiSommationsDIsPPResults r) {
				final List<Long> numerosDis = getListNumerosDis(batch);
				final Set<DeclarationImpotOrdinairePP> declarations = declarationImpotOrdinaireDAO.getDeclarationsAvecDelaisEtEtats(DeclarationImpotOrdinairePP.class, numerosDis);
				final Iterator<DeclarationImpotOrdinairePP> iter = declarations.iterator();
				while (iter.hasNext() && ! status.isInterrupted() && (nombreMax == 0 || (rapportFinal.getTotalDisSommees()  + r.getTotalDisSommees()) < nombreMax)) {
					final DeclarationImpotOrdinairePP di = iter.next();
					traiterDI(di, r, dateTraitement, miseSousPliImpossible);
				}
				return  (nombreMax == 0 || (rapportFinal.getTotalDisSommees()  + r.getTotalDisSommees() ) < nombreMax) && !status.isInterrupted();
			}

			@Override
			public void afterTransactionCommit() {
				int nombreTotal = dis.size();
				int percent = (100 * rapportFinal.getTotalDisTraitees()) / nombreTotal;
				status.setMessage(String.format(
						"%d sur %d déclarations d'impôt analysées : %d sommée(s), %d en erreur",
						rapportFinal.getTotalDisTraitees(), nombreTotal, rapportFinal.getTotalDisSommees(), rapportFinal.getTotalSommationsEnErreur()), percent);
			}
		}, null);

		final String msg = String.format(
				"Envoi des sommations pour les DI PP au %s (traitement terminé; %d sommées, %d en erreur))",
				RegDateHelper.dateToDisplayString(dateTraitement),
				rapportFinal.getTotalDisSommees(),
				rapportFinal.getTotalSommationsEnErreur());
		LOGGER.info(msg);
		statusManager.setMessage(msg);
		rapportFinal.setInterrompu(statusManager.isInterrupted());
		rapportFinal.end();
		return rapportFinal;
	}

	private List<Long> getListNumerosDis(List<IdentifiantDeclaration> batch) {
		List<Long> ids = new ArrayList<>();

		for (IdentifiantDeclaration identifiantDeclaration : batch) {
			ids.add(identifiantDeclaration.getIdDeclaration());
		}
		return ids;
	}

	protected void traiterDI(DeclarationImpotOrdinairePP di, EnvoiSommationsDIsPPResults r, RegDate dateTraitement, boolean miseSousPliImpossible) {
		// Verification des pré-requis avant la sommation
		if (checkEtat(di, r) && checkDateDelai(di, r) && checkContribuable(di, r)) {

			final RegDate finDelai = delaisService.getDateFinDelaiEnvoiSommationDeclarationImpotPP(di.getDelaiAccordeAu());
			if (finDelai.isBefore(dateTraitement)) {
				try {
					final List<Assujettissement> assujettissements = assujettissementService.determine(di.getTiers(), di.getPeriode().getAnnee());
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
			}
			else {
				LOGGER.info(String.format("le délai de la DI (au %s) + le délai de sommation effective (au %s) n'est pas dépassé pour le contribuable %d",
				                          RegDateHelper.dateToDisplayString(di.getDelaiAccordeAu()), RegDateHelper.dateToDisplayString(finDelai), di.getTiers().getNumero()));
				r.addDelaiEffectifNonEchu(di, finDelai);
			}
		}
	}

	private boolean checkDateDelai(DeclarationImpotOrdinaire di, EnvoiSommationsDIsPPResults r) {
		if (di.getDelaiAccordeAu() == null) {
			// Ce cas ne devrait plus se produire, toute les di devraient avoir un délai
			final String msg = String.format("La di [id: %s] n'a pas de délai, cela ne devrait pas être possible !", di.getNumero());
			LOGGER.error(msg);
			r.addError(di, msg);
			return false;
		}
		return true;
	}

	private boolean checkContribuable(DeclarationImpotOrdinaire di, EnvoiSommationsDIsPPResults r) {
		if (!(di.getTiers() instanceof ContribuableImpositionPersonnesPhysiques)) {
			final String msg = String.format("Le tiers [%s] n'est pas un contribuable PP, il n'est donc pas concerné par ce traitement.", di.getId().toString());
			LOGGER.error(msg);
			r.addError(di, msg);
			return false;
		}
		return true;
	}

	private boolean checkEtat(DeclarationImpotOrdinaire di, EnvoiSommationsDIsPPResults r) {
		if (TypeEtatDocumentFiscal.EMIS != di.getDernierEtatDeclaration().getEtat()) {
			// Ce cas pourrait eventuellement se produire dans le cas ou une di aurait 2 états à la même date,
			// il s'agirait alors de donnée corrompue ...
			final String msg = String.format("La di [id: %s] n'est pas à l'état 'EMIS' et ne peut donc être sommée",	di.getId().toString());
			LOGGER.error(msg);
			r.addError(di, msg);
			return false;
		}
		return true;
	}

	private void sommerDI(final DeclarationImpotOrdinairePP di, boolean miseSousPliImpossible, final RegDate dateTraitement) throws DeclarationException {
		final RegDate dateExpedition = delaisService.getDateFinDelaiCadevImpressionDeclarationImpot(dateTraitement);
		final ParametrePeriodeFiscaleEmolument paramEmolument = di.getPeriode().getParametrePeriodeFiscaleEmolument(TypeDocumentEmolument.SOMMATION_DI_PP);
		final Integer emolument = paramEmolument != null ? paramEmolument.getMontant() : null;
		final EtatDeclarationSommee etat = new EtatDeclarationSommee(dateTraitement, dateExpedition, emolument);
		AddAndSaveHelper.addAndSave(di, etat, declarationImpotOrdinaireDAO::save, new EtatDocumentFiscalAddAndSaveAccessor<>());
		diService.envoiSommationDIPPForBatch(di, miseSousPliImpossible, dateTraitement, emolument);
	}

	/**
	 * Si la DI était optionelle (ou remplacée par une note), alors il ne faut pas la sommer
	 */
	private boolean isOptionnelle(DeclarationImpotOrdinaire di, List<Assujettissement> assujettissements) {
		final DecompositionForsAnneeComplete fors = new DecompositionForsAnneeComplete(di.getTiers(), di.getPeriode().getAnnee());
		boolean optionnel = true;
		for (Assujettissement a : assujettissements) {
			final PeriodeImpositionPersonnesPhysiques periodeImposition = periodeImpositionService.determinePeriodeImposition(fors, a);
			//Ajout du test afin de detecter les periodes d'imposition null
			//exemple: contribuable avec 2 types d'assujettissements sur la même période, sourcier et ensuite à l'ordinaire
			//la méthode doit prendre en compte les assujettissements autre que le type sourcier
			//  qui est caractérisé par un rsultat null lor du calcul de la période d'imposition.
			if (periodeImposition != null) {
				if (DateRangeHelper.intersect(di, periodeImposition)) {
					optionnel = periodeImposition.isDeclarationOptionnelle() || periodeImposition.isDeclarationRemplaceeParNote();
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
				final List<Object[]> declarationsASommer = hibernateTemplate.execute(new HibernateCallback<List<Object[]>>() {
					@Override
					public List<Object[]> doInHibernate(Session session) throws HibernateException {

						final StringBuilder b = new StringBuilder();
						b.append("SELECT di.id, di.tiers.id FROM DeclarationImpotOrdinairePP AS di");
						b.append(" WHERE di.annulationDate IS NULL");
						b.append(" AND EXISTS (SELECT etat.declaration.id FROM EtatDeclaration AS etat WHERE di.id = etat.declaration.id AND etat.annulationDate IS NULL AND etat.class = EtatDeclarationEmise)");
						b.append(" AND NOT EXISTS (SELECT etat.declaration.id FROM EtatDeclaration AS etat WHERE di.id = etat.declaration.id AND etat.annulationDate IS NULL AND etat.class IN (EtatDeclarationRetournee, EtatDeclarationSommee, EtatDeclarationRappelee, EtatDeclarationSuspendue))");
						b.append(" AND EXISTS (SELECT delai.declaration.id FROM DelaiDeclaration AS delai WHERE di.id = delai.declaration.id AND delai.annulationDate IS NULL AND delai.delaiAccordeAu IS NOT NULL AND delai.etat = 'ACCORDE'");
						b.append(" GROUP BY delai.declaration.id HAVING MAX(delai.delaiAccordeAu) < :dateLimite)");
						final String sql = b.toString();
						final Query query = session.createQuery(sql);
						query.setParameter("dateLimite", dateLimite);
						return query.list();
					}
				});
				final List<IdentifiantDeclaration> identifiantDi = new ArrayList<>(declarationsASommer.size());
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