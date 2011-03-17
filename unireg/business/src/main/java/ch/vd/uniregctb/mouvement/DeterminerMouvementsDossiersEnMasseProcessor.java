package ch.vd.uniregctb.mouvement;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

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

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

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

	public DeterminerMouvementsDossiersEnMasseResults run(final RegDate dateTraitement, final boolean archivesSeulement, StatusManager s) {

		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));
		final DeterminerMouvementsDossiersEnMasseResults rapportFinal = new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement);

		status.setMessage("Récupération des contribuables...");
		final List<Long> ctbs = getListeIdsContribuablesAvecFors();

		status.setMessage("Traitement des contribuables...");

		// différentes périodes utiles (il ne sert à rien de les calculer des milliers de fois...)
		final RangesUtiles rangesUtiles = new RangesUtiles(dateTraitement);

		final BatchTransactionTemplate<Long, DeterminerMouvementsDossiersEnMasseResults> template = new BatchTransactionTemplate<Long, DeterminerMouvementsDossiersEnMasseResults>(ctbs, BATCH_SIZE, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, DeterminerMouvementsDossiersEnMasseResults>() {

			@Override
			public DeterminerMouvementsDossiersEnMasseResults createSubRapport() {
				return new DeterminerMouvementsDossiersEnMasseResults(dateTraitement, archivesSeulement);
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
						traiterContribuable(ctb, rangesUtiles, archivesSeulement, caCache, rapport);
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

	/**
	 * Trouve le for de gestion au début ou à la fin de la période fiscale donnée
	 * @param fors tous les fors de gestions connus
	 * @param anneeRange range de la période fiscale
	 * @param debut <code>true</code> pour le début, <code>false</code> pour la fin
	 * @return le for de gestion trouvé, ou null s'il n'y en a pas
	 */
	private static ForGestion findForGestion(List<ForGestion> fors, DateRange anneeRange, boolean debut) {
		if (debut) {
			for (ForGestion candidat : fors) {
				if (DateRangeHelper.intersect(candidat, anneeRange)) {
					return candidat;
				}
			}
		}
		else {
			final ListIterator<ForGestion> iterator = fors.listIterator(fors.size());
			while (iterator.hasPrevious()) {
				final ForGestion candidat = iterator.previous();
				if (DateRangeHelper.intersect(candidat, anneeRange)) {
					return candidat;
				}
			}
		}
		return null;
	}

	protected void traiterContribuable(Contribuable ctb, RangesUtiles rangesUtiles, boolean archivesSeulement, Map<Integer, CollectiviteAdministrative> caCache,
	                                   DeterminerMouvementsDossiersEnMasseResults rapport) throws AssujettissementException {

		rapport.addContribuableInspecte();

		final List<Assujettissement> assujettissements = Assujettissement.determine(ctb, rangesUtiles.rangeGlobal, true);

		// les sourciers purs n'ont pas de dossier ni d'OID de gestion (seuls les contribuables au rôle en ont un)
		// donc pas de mouvement à programmer pour eux...
		if (assujettissements != null) {

			// on enlève donc tous les assujettissements de type "sourcier pur"
			final Iterator<Assujettissement> iterator = assujettissements.iterator();
			while (iterator.hasNext()) {
				final Assujettissement a = iterator.next();
				if (a instanceof SourcierPur) {
					iterator.remove();
				}
			}

			// si la collection d'asujettissements est maintenant vide, cela signifie que nous avions affaire
			// à un sourcier pur qui l'a toujours été -> contribuable ignoré
			if (assujettissements.isEmpty()) {
				rapport.addSourcierPurIgnore(ctb.getNumero());
			}
		}

		// que dit la spec ?
		// 1. si assujettissement année n-1
		// 1.1 si pas d'assujettissement au premier janvier de l'année n -> rien à faire
		// 1.2 sinon, si assujettissement année n-2, alors mouvement à générer si changement d'OID entre fin n-2 et fin n-1
		// 1.3 sinon (assujettissement n mais pas n-2), alors mouvement à générer si changement d'OID entre début n-1 et fin n-1
		// 2. sinon (pas d'assujettissement n-1) si pas d'assujettissement n-2 non plus -> rien à faire
		// 3. sinon (assujettissement n-2 mais pas n-1 ni n), mouvement vers les archives sauf si dernier mouvement déjà comme ça

		// rien à faire si pas d'assujettissement du tout depuis plus de deux ans!
		if (assujettissements != null && assujettissements.size() > 0) {

			final List<ForGestion> histoForGestion = tiersService.getForsGestionHisto(ctb);

			if (DateRangeHelper.intersect(rangesUtiles.rangeAnneeNMoinsUn, assujettissements)) {
				// assujettissement n-1

				if (DateRangeHelper.intersect(rangesUtiles.rangeAnneeN, assujettissements)) {
					// assujettissement n aussi

					// déterminons les deux OID de gestion à comparer

					// de toute façon, le for de gestion "après" pour la comparaison est toujours celui qui est valide à la fin de l'année n-1
					final ForGestion apres = findForGestion(histoForGestion, rangesUtiles.rangeAnneeNMoinsUn, false);

					final RegDate dateAvant;
					final ForGestion avant;
					final List<DateRange> intersectionAnneeNMoinsDeux = DateRangeHelper.intersections(rangesUtiles.rangeAnneeNMoinsDeux, assujettissements);
					if (intersectionAnneeNMoinsDeux != null) {
						// assujettissement n-2 -> "avant" est la fin n-2
						avant = findForGestion(histoForGestion, rangesUtiles.rangeAnneeNMoinsDeux, false);
						dateAvant = DateRangeHelper.intersection(rangesUtiles.rangeAnneeNMoinsDeux, avant).getDateFin();
					}
					else {
						// pas d'assujettissement n-2 -> "avant" est le début n-1

						// [UNIREG-2854] début = début du premier for VAUDOIS de la période fiscale n-1
						// [UNIREG-2757] début = début du premier for VAUDOIS AU RÔLE de la période fiscale n-1
						ForFiscal premierForVaudoisAnneeNMoinsUn = null;
						final List<ForFiscal> ff = ctb.getForsFiscauxNonAnnules(true);
						for (ForFiscal forCandidat : ff) {
							if (forCandidat instanceof ForFiscalRevenuFortune) {
								final ForFiscalRevenuFortune forRevenuFortune = (ForFiscalRevenuFortune) forCandidat;

								// [UNIREG-2757] on ne prend pas en compte les fors principaux source
								if (forRevenuFortune instanceof ForFiscalPrincipal && ((ForFiscalPrincipal) forRevenuFortune).getModeImposition() == ModeImposition.SOURCE) {
									continue;
								}

								if (forRevenuFortune.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && DateRangeHelper.intersect(rangesUtiles.rangeAnneeNMoinsUn, forCandidat)) {
									if (premierForVaudoisAnneeNMoinsUn == null || premierForVaudoisAnneeNMoinsUn.getDateDebut().isAfter(forCandidat.getDateDebut())) {
										premierForVaudoisAnneeNMoinsUn = forCandidat;
									}
								}
							}
						}
						if (premierForVaudoisAnneeNMoinsUn == null) {
							throw new IllegalArgumentException("Assujettissement année n-1 sans for vaudois?");
						}

						// on sait qu'il y a un assujettissement dans l'année n-1, dont on va essayer de trouver la date
						avant = findForGestion(histoForGestion, premierForVaudoisAnneeNMoinsUn, true);
						dateAvant = DateRangeHelper.intersection(rangesUtiles.rangeAnneeNMoinsUn, avant).getDateDebut();
					}

					// calculons les OID de gestion pour ces fors de gestion là
					final Integer oidAvant = tiersService.getOfficeImpotId(avant);
					final Integer oidApres = tiersService.getOfficeImpotId(apres);
					if (oidAvant == null || oidApres == null) {
						// erreur -> on ne peut pas déterminer l'office d'impôt de gestion alors que le contribuable est assujetti
						if (oidAvant == null) {
							onAbsenseOidGestion(ctb, dateAvant, rapport);
						}
						if (oidApres == null) {
							final RegDate dateApres = DateRangeHelper.intersection(rangesUtiles.rangeAnneeNMoinsUn, apres).getDateFin();
							onAbsenseOidGestion(ctb, dateApres, rapport);
						}
					}
					else if (!oidAvant.equals(oidApres)) {
						// générons un mouvement de dossier

						if (archivesSeulement) {
							creerReceptionArchivesSiNecessaire(ctb, oidAvant, caCache, rapport);
						}
						else {
							final CollectiviteAdministrative caApres = getCollectiviteAdministrativeByNumeroTechnique(oidApres, caCache);
							final CollectiviteAdministrative caAvant = getCollectiviteAdministrativeByNumeroTechnique(oidAvant, caCache);

							final EnvoiDossier envoiDossier = new EnvoiDossierVersCollectiviteAdministrative(caApres);
							envoiDossier.setContribuable(ctb);
							envoiDossier.setCollectiviteAdministrativeEmettrice(caAvant);
							envoiDossier.setEtat(EtatMouvementDossier.A_TRAITER);
							hibernateTemplate.merge(envoiDossier); // force le save

							rapport.addMouvementVersAutreCollectiviteAdministrative(ctb.getNumero(), oidAvant, oidApres);
						}
					}
				}
			}
			else if (DateRangeHelper.intersect(rangesUtiles.rangeAnneeNMoinsDeux, assujettissements)) {
				// pas d'assujettisement ni n-1 ni n, mais n-2 -> mouvement vers les archives si n'existe pas déjà

				// il faut trouver le dernier OID de gestion du contribuable
				final ForGestion forGestion = findForGestion(histoForGestion, rangesUtiles.rangeAnneeNMoinsDeux, false);
				final Integer oid = tiersService.getOfficeImpotId(forGestion);
				if (oid == null) {
					// erreur -> on ne peut pas déterminer l'office d'impôt de gestion au moment du dernier assujettissement
					final RegDate dateFin = DateRangeHelper.intersection(rangesUtiles.rangeAnneeNMoinsDeux, forGestion).getDateFin();
					onAbsenseOidGestion(ctb, dateFin, rapport);
				}
				else {
					creerReceptionArchivesSiNecessaire(ctb, oid, caCache, rapport);
				}
			}
		}
	}

	private ReceptionDossierArchives creerReceptionArchivesSiNecessaire(Contribuable ctb, Integer oid, Map<Integer, CollectiviteAdministrative> caCache, DeterminerMouvementsDossiersEnMasseResults rapport) {
		
		final List<MouvementDossier> mvts = mouvementDossierDAO.findByNumeroDossier(ctb.getNumero(), false, false);

		// trouvons le dernier mouvement
		MouvementDossier dernierMouvement = null;
		for (MouvementDossier mvt : mvts) {
			if (mvt.getEtat() != EtatMouvementDossier.RETIRE) {
				if (dernierMouvement == null || mvt.getLogCreationDate().after(dernierMouvement.getLogCreationDate())) {
					dernierMouvement = mvt;
				}
			}
		}

		// pas de mouvement, ou dernier pas réception vers les archives ?
		final ReceptionDossierArchives receptionGeneree;
		if (dernierMouvement == null || !(dernierMouvement instanceof ReceptionDossierArchives)) {
			final CollectiviteAdministrative ca = getCollectiviteAdministrativeByNumeroTechnique(oid, caCache);

			final ReceptionDossier receptionDossier = new ReceptionDossierArchives();
			receptionDossier.setContribuable(ctb);
			receptionDossier.setEtat(EtatMouvementDossier.A_TRAITER);
			receptionDossier.setCollectiviteAdministrativeReceptrice(ca);
			receptionGeneree = (ReceptionDossierArchives) hibernateTemplate.merge(receptionDossier); // force le save

			rapport.addMouvementVersArchives(ctb.getNumero(), oid);
		}
		else {
			rapport.addMouvementNonGenereVersArchivesCarDejaExistant(ctb.getNumero());
			receptionGeneree = null;
		}
		return receptionGeneree;
	}

	private void onAbsenseOidGestion(Contribuable ctb, RegDate date, DeterminerMouvementsDossiersEnMasseResults rapport) {
		rapport.addErreurDeterminationOidGestion(ctb.getNumero(), date);
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

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return (List<Long>) template.execute(new TransactionCallback() {
			public List<Long> doInTransaction(TransactionStatus status) {
				return (List<Long>) hibernateTemplate.execute(new HibernateCallback() {
					public List<Long> doInHibernate(Session session) throws HibernateException {
						final String hql = "SELECT ctb.id FROM Contribuable ctb WHERE ctb.annulationDate IS NULL AND EXISTS (SELECT ff.id FROM ForFiscal ff WHERE ff.tiers=ctb AND ff.annulationDate IS NULL) ORDER BY ctb.id ASC";
						final Query query = session.createQuery(hql);
						return query.list();
					}
				});
			}
		});
	}
}
