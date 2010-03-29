package ch.vd.uniregctb.declaration.ordinaire;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.Indigent;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.Contribuable;
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

	public EnvoiSommationsDIsProcessor(
			HibernateTemplate hibernateTemplate,
			DeclarationImpotOrdinaireDAO declarationImpotOrdinaireDAO,
			DelaisService delaisService,
			DeclarationImpotService diService,
			PlatformTransactionManager transactionManager
	) {
		this.hibernateTemplate = hibernateTemplate;
		this.declarationImpotOrdinaireDAO = declarationImpotOrdinaireDAO;
		this.delaisService = delaisService;
		this.diService = diService;
		this.transactionManager = transactionManager;
	}

	public EnvoiSommationsDIsResults run(final RegDate dateTraitement, final boolean miseSousPliImpossible, final Integer nombreMax, StatusManager statusManager) {

		if (statusManager == null) {
			statusManager = new LoggingStatusManager(LOGGER);
		}
		final StatusManager status = statusManager;

		final EnvoiSommationsDIsResults rapportFinal = new EnvoiSommationsDIsResults();
		rapportFinal.setDateTraitement(dateTraitement);
		rapportFinal.setMiseSousPliImpossible(miseSousPliImpossible);

		status.setMessage(
			String.format(
				"Envoi des sommations pour les DI au %s (Récupération de la liste de DI)",
				RegDateHelper.dateToDisplayString(dateTraitement))
		);

		final List<Long> dis = retrieveListIdDIs(dateTraitement, nombreMax);

		final BatchTransactionTemplate<Long, EnvoiSommationsDIsResults> t = new BatchTransactionTemplate<Long, EnvoiSommationsDIsResults>(dis, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		t.execute(rapportFinal, new BatchCallback<Long, EnvoiSommationsDIsResults>() {

			int currentBatch = 0;
			private EnvoiSommationsDIsResults rapport;

			@Override
			public EnvoiSommationsDIsResults createSubRapport() {
				return new EnvoiSommationsDIsResults();
			}

			public boolean doInTransaction(List<Long> batch, EnvoiSommationsDIsResults r) {
				rapport = r;
				currentBatch++;
				try {
					final Set<DeclarationImpotOrdinaire> declarations = declarationImpotOrdinaireDAO.getDIsForSommation(batch);
					final Iterator<DeclarationImpotOrdinaire> iter = declarations.iterator();
					while (iter.hasNext() && ! status.interrupted() && ( nombreMax == 0 || (rapportFinal.getTotalDisSommees()  + this.rapport.getTotalDisSommees() )< nombreMax) ) {
						DeclarationImpotOrdinaire di = iter.next();
						// Verification des pré-requis avant la sommation
						if ( !checkEtat(di) || !checkDateDelai(di) || !checkContribuable(di) ) {
							continue;
						}
						RegDate finDelai;
						finDelai = delaisService.getDateFinDelaiEnvoiSommationDeclarationImpot(di.getDelaiAccordeAu());
						if (finDelai.isBefore(dateTraitement)) {
							try {
								List<Assujettissement> assujettissements = Assujettissement.determine((Contribuable)di.getTiers(), di.getPeriode().getAnnee());
								if (assujettissements == null || assujettissements.isEmpty()) {
									String msg = String.format(
											"La di [id: %s] n'a pas été sommée car le contribuable [%s] n'est pas assujetti pour la période fiscale %s",
											di.getId().toString(), di.getTiers().getNumero(), di.getPeriode().getAnnee());
									LOGGER.info(msg);
									this.rapport.addNonAssujettissement(di);
								} else {
									// Le contribuable est assujetti
									if (isIndigent(di,	assujettissements)) {
										String msg = String.format(
												"La di [id: %s] n'a pas été sommée car le contribuable [%s] est indigent au %s",
												di.getId().toString(), di.getTiers().getNumero(), RegDateHelper.dateToDisplayString(di.getDateFin()));
										LOGGER.info(msg);
										this.rapport.addIndigent(di);
									} else {
										sommerDI(di, miseSousPliImpossible, dateTraitement);
										LOGGER.info(
												String.format(
														"La di [id: %s; ctb: %s; periode: %s; debut: %s; fin: %s] a été sommée",
														di.getId().toString(),
														di.getTiers().getNumero().toString(),
														di.getPeriode().getAnnee(),
														di.getDateDebut(),
														di.getDateFin()));
										this.rapport.addDiSommee(di.getPeriode().getAnnee(), di);
									}
								}
							}
							catch (RuntimeException e) {
								this.rapport.addError(di,e.getMessage());
								LOGGER.error(e.getMessage(), e);
								throw e;
							} catch (Exception e) {
								this.rapport.addError(di,e.getMessage());
								LOGGER.error(e.getMessage(), e);
								throw new RuntimeException(e);
							}
						} else {
							LOGGER.info(
									String.format(
											"le délai de la DI (au %s) + le délai de sommation effictive (au %s) n'est pas dépassé",
											di.getDelaiAccordeAu().toString(), finDelai.toString()));
						}
					}
				} finally {
					LOGGER.debug("Batch no " + currentBatch + " terminé");
				}
				return  (nombreMax == 0 || (rapportFinal.getTotalDisSommees()  + this.rapport.getTotalDisSommees() ) < nombreMax) && !status.interrupted();
			}

			private boolean checkDateDelai(DeclarationImpotOrdinaire di) {
				if (di.getDelaiAccordeAu() == null) {
					// Ce cas ne devrait plus se produire, toute les di devraient avoir un délai
					String msg = String.format("La di [id: %s] n'a pas de délai, cela ne devrait pas être possible !", di.getNumero());
					LOGGER.error(msg);
					rapport.addError(di, msg);
					return false;
				}
				return true;
			}

			private boolean checkContribuable(DeclarationImpotOrdinaire di) {
				if (!(di.getTiers() instanceof Contribuable)) {
					// Ce cas ne devrait pas se produire, une di est forcement rattaché à un tiers qui est un contribuable.
					String msg = String.format(
							"Le tiers [%s] n'est pas un contribuable, on ne peut pas calculer son assujettisement",
							di.getId().toString());
					LOGGER.error(msg);
					rapport.addError(di, msg);
					return false;
				}
				return true;
			}

			private boolean checkEtat(DeclarationImpotOrdinaire di) {
				if (TypeEtatDeclaration.EMISE != di.getDernierEtat().getEtat()) {
					// Ce cas pourrait eventuellement se produire dans le cas ou une di aurait 2 états à la même date,
					// il s'agirait alors de donnée corrompue ...
					String msg = String.format("La di [id: %s] n'est pas à l'état 'EMISE' et ne peut donc être sommée",	di.getId().toString());
					LOGGER.error(msg);
					rapport.addError(di, msg);
					return false;
				}
				return true;
			}

			@Override
			public void afterTransactionCommit() {
				int nombreTotal = (nombreMax == 0 || dis.size() < nombreMax ? dis.size() : nombreMax);
				int percent = (100 * rapportFinal.getTotalDisTraitees()) / nombreTotal;
				status.setMessage(String.format(
						"%d sur %d des déclarations d'impôt traitées (%d%%) : (%d sommées, %d en erreur)",
						rapportFinal.getTotalDisTraitees(), nombreTotal, percent, rapportFinal.getTotalDisSommees(), rapportFinal.getTotalSommationsEnErreur()));
			}
		});

		String msg = String.format(
				"Envoi des sommations pour les DI au %s (traitement terminé; %d sommées, %d en erreur))",
				RegDateHelper.dateToDisplayString(dateTraitement),
				rapportFinal.getTotalDisSommees(),
				rapportFinal.getTotalSommationsEnErreur()
			);
		LOGGER.info(msg);
		statusManager.setMessage(msg);
		rapportFinal.setInterrompu(statusManager.interrupted());
		rapportFinal.end();
		return rapportFinal;
	}

	private void sommerDI(final DeclarationImpotOrdinaire di, boolean miseSousPliImpossible, final RegDate dateTraitement) throws DeclarationException {

		EtatDeclaration etat = new EtatDeclaration();
		etat.setDeclaration(di);
		etat.setEtat(TypeEtatDeclaration.SOMMEE);
		etat.setAnnule(false);
		RegDate dateExpedition = delaisService.getDateFinDelaiCadevImpressionDeclarationImpot(dateTraitement);
		etat.setDateObtention(dateExpedition);
		di.addEtat(etat);

		diService.envoiSommationDIForBatch(di, miseSousPliImpossible, dateTraitement);
	}
	
	/**
	 * [UNIREG-1472] Verification que l'assujettissement ne soit pas indigent à la date de la fin de di
	 */
	protected boolean isIndigent(DeclarationImpotOrdinaire di,
			List<Assujettissement> assujettissements) {
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
	private List<Long> retrieveListIdDIs(final RegDate dateLimite, final Integer nombreMax) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return (List<Long>) template.execute(new TransactionCallback() {
			public List<Long> doInTransaction(TransactionStatus status) {

				final List<Long> ids = (List<Long>) hibernateTemplate.execute(new HibernateCallback() {
					public Object doInHibernate(Session session) throws HibernateException {
						SQLQuery sqlQuery = session.createSQLQuery("alter session set optimizer_index_caching = 90");
						sqlQuery.executeUpdate();
						sqlQuery = session.createSQLQuery("alter session set optimizer_index_cost_adj = 10");
						sqlQuery.executeUpdate();
						Query query = session.createQuery(
							"SELECT di.id"
							+ " FROM DeclarationImpotOrdinaire AS di"
							+ " WHERE di.annulationDate IS NULL "
							+ " AND EXISTS ("
							+ "   SELECT etat1.declaration.id"
							+ "   FROM EtatDeclaration AS etat1"
							+ "   WHERE di.id = etat1.declaration.id"
							+ "     AND etat1.annulationDate IS NULL"
							+ "     AND etat1.etat = 'EMISE'"
							+ "     AND etat1.dateObtention IN ("
							+ "       SELECT MAX(etat2.dateObtention) "
							+ "       FROM EtatDeclaration AS etat2 "
							+ "       WHERE etat1.declaration.id = etat2.declaration.id AND etat2.annulationDate IS NULL))"
							+ " AND EXISTS ( "
							+ "   SELECT delai.declaration.id  FROM DelaiDeclaration AS delai"
							+ "   WHERE delai.declaration.id = di.id"
							+ "   AND delai.annulationDate IS NULL"
							+ "   AND delai.delaiAccordeAu IS NOT NULL"
							+ "   GROUP BY delai.declaration.id"
							+ "   HAVING MAX(delai.delaiAccordeAu) < :dateLimite"
							+ " )"
						);

						query.setParameter("dateLimite", dateLimite.index());
						if (nombreMax > 0) {
							// On fait une query sur 2 x le nombre max. Pour etre statistiquement sûr
							// de sommer le nombreMax de DI
							query.setMaxResults(nombreMax * 2);
						}
						return query.list();

					}
				});
				return ids;
			}
		});
	}

}