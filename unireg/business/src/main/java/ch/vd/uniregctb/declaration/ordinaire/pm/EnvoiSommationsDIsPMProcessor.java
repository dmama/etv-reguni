package ch.vd.uniregctb.declaration.ordinaire.pm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.IdentifiantDeclaration;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class EnvoiSommationsDIsPMProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnvoiSommationsDIsPMProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;
	private final DeclarationImpotOrdinaireDAO declarationImpotOrdinaireDAO;
	private final DelaisService delaisService;
	private final DeclarationImpotService diService;
	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;
	private final PeriodeImpositionService periodeImpositionService;
	private final AdresseService adresseService;

	public EnvoiSommationsDIsPMProcessor(HibernateTemplate hibernateTemplate,
	                                     DeclarationImpotOrdinaireDAO declarationImpotOrdinaireDAO,
	                                     DelaisService delaisService,
	                                     DeclarationImpotService diService,
	                                     TiersService tiersService,
	                                     PlatformTransactionManager transactionManager,
	                                     PeriodeImpositionService periodeImpositionService,
	                                     AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.declarationImpotOrdinaireDAO = declarationImpotOrdinaireDAO;
		this.delaisService = delaisService;
		this.diService = diService;
		this.tiersService = tiersService;
		this.transactionManager = transactionManager;
		this.periodeImpositionService = periodeImpositionService;
		this.adresseService = adresseService;
	}

	public EnvoiSommationsDIsPMResults run(final RegDate dateTraitement, @Nullable final Integer nombreMax, @Nullable StatusManager statusManager) {

		final StatusManager status = statusManager != null ? statusManager : new LoggingStatusManager(LOGGER);
		final EnvoiSommationsDIsPMResults rapportFinal = new EnvoiSommationsDIsPMResults(tiersService, adresseService, dateTraitement, nombreMax);

		status.setMessage(String.format("Envoi des sommations pour les DI PM au %s (récupération de la liste de DI)", RegDateHelper.dateToDisplayString(dateTraitement)));

		final List<IdentifiantDeclaration> dis = retrieveListIdDIs(dateTraitement);

		final BatchTransactionTemplateWithResults<IdentifiantDeclaration, EnvoiSommationsDIsPMResults>
				t = new BatchTransactionTemplateWithResults<>(dis, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		t.execute(rapportFinal, new BatchWithResultsCallback<IdentifiantDeclaration, EnvoiSommationsDIsPMResults>() {

			@Override
			public EnvoiSommationsDIsPMResults createSubRapport() {
				return new EnvoiSommationsDIsPMResults(tiersService, adresseService, dateTraitement, nombreMax);
			}

			@Override
			public boolean doInTransaction(List<IdentifiantDeclaration> batch, EnvoiSommationsDIsPMResults r) {
				final List<Long> numerosDis = getListNumerosDis(batch);
				final Set<DeclarationImpotOrdinairePM> declarations = declarationImpotOrdinaireDAO.getDeclarationsAvecDelaisEtEtats(DeclarationImpotOrdinairePM.class, numerosDis);
				final Iterator<DeclarationImpotOrdinairePM> iter = declarations.iterator();
				while (iter.hasNext() && !status.interrupted() && (nombreMax == null || nombreMax <= 0 || (rapportFinal.getTotalDisSommees()  + r.getTotalDisSommees()) < nombreMax)) {
					final DeclarationImpotOrdinairePM di = iter.next();
					traiterDI(di, r, dateTraitement);
				}
				return (nombreMax == null || nombreMax <= 0 || (rapportFinal.getTotalDisSommees() + r.getTotalDisSommees() ) < nombreMax) && !status.interrupted();
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

		final String msg = String.format("Envoi des sommations pour les DI PP au %s (traitement terminé; %d sommées, %d en erreur)",
		                                 RegDateHelper.dateToDisplayString(dateTraitement),
		                                 rapportFinal.getTotalDisSommees(),
		                                 rapportFinal.getTotalSommationsEnErreur());
		LOGGER.info(msg);
		status.setMessage(msg);
		rapportFinal.setInterrompu(status.interrupted());
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

	protected void traiterDI(DeclarationImpotOrdinairePM di, EnvoiSommationsDIsPMResults r, RegDate dateTraitement) {
		// Verification des pré-requis avant la sommation
		if (checkEtat(di, r) && checkDateDelai(di, r) && checkContribuable(di, r)) {

			final RegDate finDelai = delaisService.getDateFinDelaiEnvoiSommationDeclarationImpotPM(di.getDelaiAccordeAu());
			if (finDelai.isBefore(dateTraitement)) {
				try {
					final List<PeriodeImposition> periodesImposition = periodeImpositionService.determine(di.getTiers(), di.getPeriode().getAnnee());
					if (periodesImposition == null || periodesImposition.isEmpty()) {
						final String msg = String.format("La di [id: %d] n'a pas été sommée car le contribuable [%s] n'a pas (plus ?) de période d'imposition pour la période fiscale %s",
						                                 di.getId(), di.getTiers().getNumero(), di.getPeriode().getAnnee());
						LOGGER.info(msg);
						r.addNonAssujettissement(di);
					}
					else if (isSuspendue(di)) {
						final String msg = String.format("La di [id: %d] du contribuable [%s] n'a pas été sommée car elle était suspendue",
						                                 di.getId(), di.getTiers().getNumero());
						LOGGER.info(msg);
						r.addDiSuspendue(di);
					}
					else {
						sommerDI(di, dateTraitement);
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

	private boolean checkDateDelai(DeclarationImpotOrdinaire di, EnvoiSommationsDIsPMResults r) {
		if (di.getDelaiAccordeAu() == null) {
			// Ce cas ne devrait plus se produire, toute les di devraient avoir un délai
			final String msg = String.format("La di [id: %s] n'a pas de délai, cela ne devrait pas être possible !", di.getNumero());
			LOGGER.error(msg);
			r.addError(di, msg);
			return false;
		}
		return true;
	}

	private boolean checkContribuable(DeclarationImpotOrdinaire di, EnvoiSommationsDIsPMResults r) {
		if (!(di.getTiers() instanceof ContribuableImpositionPersonnesMorales)) {
			final String msg = String.format("Le tiers [%s] n'est pas un contribuable PM, il n'est donc pas concerné par ce traitement.", di.getId().toString());
			LOGGER.error(msg);
			r.addError(di, msg);
			return false;
		}
		return true;
	}

	private boolean checkEtat(DeclarationImpotOrdinaire di, EnvoiSommationsDIsPMResults r) {
		if (TypeEtatDeclaration.EMISE != di.getDernierEtat().getEtat() && TypeEtatDeclaration.SUSPENDUE != di.getDernierEtat().getEtat()) {
			// Ce cas pourrait eventuellement se produire dans le cas où une DI aurait 2 états à la même date,
			// il s'agirait alors de données corrompues ...
			final String msg = String.format("La di [id: %s] n'est ni à l'état 'EMISE', et ne peut donc être sommée", di.getId().toString());
			LOGGER.error(msg);
			r.addError(di, msg);
			return false;
		}
		return true;
	}

	private void sommerDI(final DeclarationImpotOrdinairePM di, final RegDate dateTraitement) throws DeclarationException {

		final RegDate dateExpedition = delaisService.getDateFinDelaiCadevImpressionDeclarationImpot(dateTraitement);
		final EtatDeclarationSommee etat = new EtatDeclarationSommee(dateTraitement, dateExpedition, null);
		etat.setDeclaration(di);
		etat.setAnnule(false);
		di.addEtat(etat);

		diService.envoiSommationDIPMForBatch(di, dateTraitement, dateExpedition);
	}

	/**
	 * Si la DI est dans un état SUSPENDUE, il ne faut pas la sommer
	 */
	private boolean isSuspendue(DeclarationImpotOrdinaire di) {
		final EtatDeclaration dernierEtat = di.getDernierEtat();
		return dernierEtat != null && dernierEtat.getEtat() == TypeEtatDeclaration.SUSPENDUE;
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
						b.append("SELECT di.id, di.tiers.id FROM DeclarationImpotOrdinairePM AS di");
						b.append(" WHERE di.annulationDate IS NULL");
						b.append(" AND EXISTS (SELECT etat.declaration.id FROM EtatDeclaration AS etat WHERE di.id = etat.declaration.id AND etat.annulationDate IS NULL AND etat.class = EtatDeclarationEmise)");
						b.append(" AND NOT EXISTS (SELECT etat.declaration.id FROM EtatDeclaration AS etat WHERE di.id = etat.declaration.id AND etat.annulationDate IS NULL AND etat.class IN (EtatDeclarationRetournee, EtatDeclarationSommee, EtatDeclarationRappelee))");
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