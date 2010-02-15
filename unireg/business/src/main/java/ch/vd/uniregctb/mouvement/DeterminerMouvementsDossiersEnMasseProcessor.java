package ch.vd.uniregctb.mouvement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class DeterminerMouvementsDossiersEnMasseProcessor {

	private static final int BATCH_SIZE = 100;

	public static final Logger LOGGER = Logger.getLogger(DeterminerMouvementsDossiersEnMasseProcessor.class);

	private final TiersService tiersService;
	private final TiersDAO tiersDAO;
	private final MouvementDossierDAO mouvementDossierDAO;
	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;

	public DeterminerMouvementsDossiersEnMasseProcessor(TiersService tiersService, TiersDAO tiersDAO, MouvementDossierDAO mouvementDossierDAO, HibernateTemplate hibernateTemplate,
	                                                   PlatformTransactionManager transactionManager) {
		this.tiersService = tiersService;
		this.tiersDAO = tiersDAO;
		this.mouvementDossierDAO = mouvementDossierDAO;
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
	}

	protected static class RangesUtiles {

		public final DateRange rangeGlobal;
		public final DateRange rangeAnneeN;
		public final DateRange rangeAnneeNMoinsUn;
		public final DateRange rangeAnneeNMoinsDeux;

		public RangesUtiles(RegDate dateTraitement) {
			final int annee = dateTraitement.year();
			final RegDate finRange = RegDate.get(annee, 1, 1);
			final RegDate debutRange = RegDate.get(annee - 2, 1, 1);

			rangeGlobal = new DateRangeHelper.Range(debutRange, finRange);
			rangeAnneeN = new DateRangeHelper.Range(RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31));
			rangeAnneeNMoinsUn = new DateRangeHelper.Range(RegDate.get(annee - 1, 1, 1), RegDate.get(annee - 1, 12, 31));
			rangeAnneeNMoinsDeux = new DateRangeHelper.Range(RegDate.get(annee - 2, 1, 1), RegDate.get(annee - 2, 12, 31));
		}
	}

	public DeterminerMouvementsDossiersEnMasseResults run(final RegDate dateTraitement, StatusManager s) {

		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));
		final DeterminerMouvementsDossiersEnMasseResults rapportFinal = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement);

		status.setMessage("Récupération des contribuables...");
		final List<Long> ctbs = getListeIdsContribuablesAvecFors();

		status.setMessage("Traitement des contribuables...");

		// différentes périodes utiles (il ne sert à rien de les calculer des milliers de fois...)
		final RangesUtiles rangesUtiles = new RangesUtiles(dateTraitement);

		final BatchTransactionTemplate<Long, DeterminerMouvementsDossiersEnMasseResults> template = new BatchTransactionTemplate<Long, DeterminerMouvementsDossiersEnMasseResults>(ctbs, BATCH_SIZE, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		template.execute(new BatchTransactionTemplate.BatchCallback<Long, DeterminerMouvementsDossiersEnMasseResults>() {

			@Override
			public DeterminerMouvementsDossiersEnMasseResults createSubRapport() {
				return new DeterminerMouvementsDossiersEnMasseResults(dateTraitement);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, DeterminerMouvementsDossiersEnMasseResults rapport) throws Exception {

				final String message = String.format("Traitement du lot [%s, %s], mouvements générés : %d (%d erreur(s))",
													FormatNumeroHelper.numeroCTBToDisplay(batch.get(0)),
													FormatNumeroHelper.numeroCTBToDisplay(batch.get(batch.size() - 1)),
													rapportFinal.mouvements.size(), rapportFinal.erreurs.size());
				status.setMessage(message, percent);

				// cache des collectivités administratives (il y a 20 OID, et nous travaillons par
				// groupes de BATCH_SIZE contribuables, autant essayer de cacher les appels à la base)
				final Map<Integer, CollectiviteAdministrative> caCache = new HashMap<Integer, CollectiviteAdministrative>(25);
				
				for (Long id : batch) {

					if (status.interrupted()) {
						break;
					}

					final Contribuable ctb = (Contribuable) tiersDAO.get(id);       // la requête est ainsi faite que seuls des Contribuables devraient sortir...

					try {
						traiterContribuable(ctb, rangesUtiles, caCache, rapport);
					}
					catch (AssujettissementException e) {
						// histoire de ne pas devoir refaire tout le lot sur des erreurs connues...
						// (en revanche, cela ne fonctionne que parce que la méthode traiterContribuable()
						// ne fait rien - pas de création/modification - avant d'avoir calculé tous les
						// assujettissements nécessaires)
						rapport.addErrorException(ctb.getNumero(), e);
					}
				}

				return !status.interrupted();
			}
		});

		rapportFinal.setInterrompu(status.interrupted());
		rapportFinal.end();
		return rapportFinal;
	}

	protected void traiterContribuable(Contribuable ctb, RangesUtiles rangesUtiles, Map<Integer, CollectiviteAdministrative> caCache, DeterminerMouvementsDossiersEnMasseResults rapport) throws AssujettissementException {

		rapport.addContribuableInspecte();

		final List<Assujettissement> assujettissements = Assujettissement.determine(ctb, rangesUtiles.rangeGlobal, true);

		// que dit la spec ?
		// 1. si assujettissement année n-1
		// 1.1 si pas d'assujettissement au premier janvier de l'année n -> rien à faire
		// 1.2 sinon, si assujettissement année n-2, alors mouvement à générer si changement d'OID entre début n-2 et fin n-1
		// 1.3 sinon (assujettissement n mais pas n-2), alors mouvement à générer si changement d'OID entre début n-1 et fin n-1
		// 2. sinon (pas d'assujettissement n-1) si pas d'assujettissement n-2 non plus -> rien à faire
		// 3. sinon (assujettissement n-2 mais pas n-1 ni n), mouvement vers les archives sauf si dernier mouvement déjà comme ça

		// rien à faire si pas d'assujettissement du tout depuis plus de deux ans!
		if (assujettissements != null) {

			if (DateRangeHelper.intersect(rangesUtiles.rangeAnneeNMoinsUn, assujettissements)) {
				// assujettissement n-1

				if (DateRangeHelper.intersect(rangesUtiles.rangeAnneeN, assujettissements)) {
					// assujettissement n aussi

					// déterminons les deux dates auxquelles il faut comparer les OID de gestion

					// de toute façon, la date "après" pour la comparaison est toujours la date de fin de l'assujettissement
					// sur l'année n-1
					final List<DateRange> intersectionAnneeNMoinsUn = DateRangeHelper.intersections(rangesUtiles.rangeAnneeNMoinsUn, assujettissements);
					final RegDate apres = intersectionAnneeNMoinsUn.get(intersectionAnneeNMoinsUn.size() - 1).getDateFin();

					final RegDate avant;
					final List<DateRange> intersectionAnneeNMoinsDeux = DateRangeHelper.intersections(rangesUtiles.rangeAnneeNMoinsDeux, assujettissements);
					if (intersectionAnneeNMoinsDeux != null) {
						// assujettissement n-2 -> "avant" est la date de début de la période d'assujettissement sur n-2
						avant = intersectionAnneeNMoinsDeux.get(0).getDateDebut();
					}
					else {
						avant = intersectionAnneeNMoinsUn.get(0).getDateDebut();
					}

					// calculons les OID de gestion à ces dates là...
					final Integer oidAvant = tiersService.getOfficeImpotAt(ctb, avant);
					final Integer oidApres = tiersService.getOfficeImpotAt(ctb, apres);
					if (oidAvant == null || oidApres == null) {
						// erreur -> on ne peut pas déterminer l'office d'impôt de gestion alors que le contribuable est assujetti
						if (oidAvant == null) {
							onAbsenseOidGestion(ctb, avant, assujettissements, rapport);
						}
						if (oidApres == null) {
							onAbsenseOidGestion(ctb, apres, assujettissements, rapport);
						}
					}
					else if (!oidAvant.equals(oidApres)) {
						// générons un mouvement de dossier

						final CollectiviteAdministrative caApres = getCollectiviteAdministrativeByNumeroTechnique(oidApres, caCache);
						final CollectiviteAdministrative caAvant = getCollectiviteAdministrativeByNumeroTechnique(oidAvant, caCache);

						final EnvoiDossier envoiDossier = new EnvoiDossierVersCollectiviteAdministrative(caApres);
						envoiDossier.setContribuable(ctb);
						envoiDossier.setCollectiviteAdministrativeEmettrice(caAvant);
						envoiDossier.setEtat(EtatMouvementDossier.A_TRAITER);
						ctb.addMouvementDossier(envoiDossier);

						rapport.addMouvementVersAutreCollectiviteAdministrative(ctb.getNumero(), oidAvant, oidApres);
					}
				}

			}
			else if (DateRangeHelper.intersect(rangesUtiles.rangeAnneeNMoinsDeux, assujettissements)) {
				// pas d'assujettisement ni n-1 ni n, mais n-2 -> mouvement vers les archives si n'existe pas déjà

				final List<MouvementDossier> mvts = mouvementDossierDAO.findByNumeroDossier(ctb.getNumero(), false);

				// trouvons le dernier mouvement
				MouvementDossier dernierMouvement = null;
				for (MouvementDossier mvt : mvts) {
					if (!mvt.isAnnule() && mvt.getEtat() != EtatMouvementDossier.RETIRE) {
						if (dernierMouvement == null || mvt.getLogCreationDate().after(dernierMouvement.getLogCreationDate())) {
							dernierMouvement = mvt;
						}
					}
				}

				// pas de mouvement, ou dernier pas réception vers les archives ?
				if (dernierMouvement == null || !(dernierMouvement instanceof ReceptionDossierArchives)) {

					// il faut trouver l'OID de gestion du contribuable
					final RegDate dateFinAssujettissement = assujettissements.get(assujettissements.size() - 1).getDateFin();
					final Integer oid = tiersService.getOfficeImpotAt(ctb, dateFinAssujettissement);
					if (oid == null) {
						// erreur -> on ne peut pas déterminer l'office d'impôt de gestion au moment du dernier assujettissement
						onAbsenseOidGestion(ctb, dateFinAssujettissement, assujettissements, rapport);
					}
					else {
						final CollectiviteAdministrative ca = getCollectiviteAdministrativeByNumeroTechnique(oid, caCache);

						final ReceptionDossier receptionDossier = new ReceptionDossierArchives();
						receptionDossier.setContribuable(ctb);
						receptionDossier.setEtat(EtatMouvementDossier.A_TRAITER);
						receptionDossier.setCollectiviteAdministrativeReceptrice(ca);
						ctb.addMouvementDossier(receptionDossier);

						rapport.addMouvementVersArchives(ctb.getNumero(), oid);
					}
				}
			}
		}
	}

	private void onAbsenseOidGestion(Contribuable ctb, RegDate date, List<Assujettissement> assujettissements, DeterminerMouvementsDossiersEnMasseResults rapport) {
		final Assujettissement courant = DateRangeHelper.rangeAt(assujettissements, date);
		Assert.notNull(courant);        // toutes les dates sont prises comme des dates de début ou de fin d'assujettissement, depuis la collection

		if (courant instanceof SourcierPur) {
			// normal, les sourciers purs n'ont pas d'office de gestion
			rapport.addSourcierPurIgnore(ctb.getNumero(), date);
		}
		else {
			rapport.addErreurDeterminationOidGestion(ctb.getNumero(), date);
		}
	}

	private CollectiviteAdministrative getCollectiviteAdministrativeByNumeroTechnique(int noCa, Map<Integer, CollectiviteAdministrative> caCache) {
		final CollectiviteAdministrative cachee = caCache.get(noCa);
		if (cachee != null) {
			return cachee;
		}

		final CollectiviteAdministrative ca = tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(noCa);
		if (ca != null) {
			caCache.put(noCa, ca);
		}
		return ca;
	}

	@SuppressWarnings({"unchecked"})
	private List<Long> getListeIdsContribuablesAvecFors() {

		final List<Long> ids = (List<Long>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				final String hql = "SELECT ctb.id FROM Contribuable ctb WHERE ctb.annulationDate IS NULL AND EXISTS (SELECT ff.id FROM ForFiscal ff WHERE ff.tiers=ctb AND ff.annulationDate IS NULL) ORDER BY ctb.id ASC";
				final Query query = session.createQuery(hql);
				return query.list();
			}
		});
		return ids;

	}
}
