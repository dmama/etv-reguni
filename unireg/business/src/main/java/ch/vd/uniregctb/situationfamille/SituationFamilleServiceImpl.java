package ch.vd.uniregctb.situationfamille;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.DateRangeHelper.AdapterCallback;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.EtatCivilList;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.IndividuNotFoundException;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.SituationFamilleDAO;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class SituationFamilleServiceImpl implements SituationFamilleService {

	private TiersDAO tiersDAO;

	private TiersService tiersService;

	private ServiceCivilService serviceCivil;

	private SituationFamilleDAO situationFamilleDAO;

	private HibernateTemplate hibernateTemplate;

	private PlatformTransactionManager transactionManager;

	private static final String valide = "VALIDE";

	private static final String annulee = "ANNULEE";

	/** Service des événements fiscaux */
	private EvenementFiscalService evenementFiscalService;
	private AdresseService adresseService;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	/**
	 * Callback spécialisé dans l'adaptation de situation de familles
	 */
	private static final class VueSituationFamilleAdapterCallback implements AdapterCallback<VueSituationFamille> {

		@Override
		public VueSituationFamille adapt(VueSituationFamille range, RegDate debut, RegDate fin) {

			if (range instanceof VueSituationFamillePersonnePhysique) {
				return new VueSituationFamillePersonnePhysiqueAdapter((VueSituationFamillePersonnePhysique) range, debut, fin);
			}
			else {
				Assert.isTrue(range instanceof VueSituationFamilleMenageCommun);
				return new VueSituationFamilleMenageCommunAdapter((VueSituationFamilleMenageCommun) range, debut, fin);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public VueSituationFamille getVue(Contribuable contribuable, RegDate date, boolean yComprisCivil) {

		/*
		 * Pour l'instant, on va stupidemment extraire la situation à la date demandée dans l'historique complet. On pourra re-écrire cette
		 * méthode par la suite pour ne faire que le minimum de choses si des problèmes de performances surviennent.
		 */
		final List<VueSituationFamille> histo = getVueHisto(contribuable, false);
		for (VueSituationFamille v : histo) {
			if (v.isValidAt(date) && (v.getSource() != VueSituationFamille.Source.CIVILE || yComprisCivil)) {
				return v;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<VueSituationFamille> getVueHisto(Contribuable contribuable) {
		return getVueHisto(contribuable, true);
	}

	/**
	 * permet de retourner l'historique des situations de familles des contribuables avec possibilité de rajouter celles qui sont annulées
	 *
	 * @param contribuable
	 * @param annuleeAllowed
	 * @return une liste des vues de situations de familles
	 */
	private List<VueSituationFamille> getVueHisto(Contribuable contribuable, boolean annuleeAllowed) {

		List<VueSituationFamille> resultat;

		if (contribuable instanceof PersonnePhysique || contribuable instanceof MenageCommun) {

			// Construit les vues 'civiles' et 'fiscales' des situations de famille du contribuable
			final HashMap<String, List<VueSituationFamille>> map = buildSituationsFiscalesHisto(contribuable);
			final List<VueSituationFamille> situationsFiscales = map.get(valide);
			final List<VueSituationFamille> situationsFiscalesAnnulees = map.get(annulee);

			// Construit la vue finale en surchargeant les situations civiles avec les situations fiscales
			final List<VueSituationFamille> situationsCiviles = buildSituationsCivilesHisto(contribuable);
			resultat = DateRangeHelper.override(situationsCiviles, situationsFiscales, new VueSituationFamilleAdapterCallback());

			// si besoin on rajoute les situations de familles fiscales annulées. la liste ainsi obtenue est utilisées dans l'affichage
			// du tiers et dans le web service
			if (annuleeAllowed) {
				resultat.addAll(situationsFiscalesAnnulees);
				resultat.sort(new DateRangeComparator<>());
			}
		}
		else {
			/*
			 * par définition, tous les autres type de contribuables (entreprises, établissement, ...) ne possèdent pas de situation de
			 * famille
			 */
			resultat = Collections.emptyList();
		}
		return resultat;
	}

	/**
	 * Récupère l'état civil du contribuable donné dans le registre civil et construit une vue de sa situation de famille à partir de ça.
	 *
	 * @param contribuable
	 *            le contribuable considéré
	 * @return une liste de situations de famille.
	 */
	private List<VueSituationFamille> buildSituationsCivilesHisto(Contribuable contribuable) {

		final List<VueSituationFamille> list = new ArrayList<>();

		if (contribuable instanceof MenageCommun) {
			// aucune information n'est disponible dans le civil
			return list;
		}

		Assert.isTrue(contribuable instanceof PersonnePhysique);
		final PersonnePhysique pp = (PersonnePhysique) contribuable;
		if (pp.getNumeroIndividu() == null || pp.getNumeroIndividu() == 0) {
			// aucune information n'est disponible dans le civil
			return list;
		}

		// else habitant
		final Individu individu = tiersService.getIndividu(pp);
		if (individu == null) {
			throw new IndividuNotFoundException(pp);
		}

		final EtatCivilList coll = individu.getEtatsCivils();
		final List<EtatCivil> ecList = coll != null ? coll.asList() : null;
		if (ecList == null || ecList.isEmpty()) {
			return list;
		}

		// [UNIREG-823] Dans le cas d'un individu décédé, on doit fermer la dernière situation retournée par le civil à date décès
		RegDate dateFin = tiersService.getDateDeces(pp);

		// on parcourt la liste à l'envers et on prend les états civils connus
		for (EtatCivil ec : CollectionsUtils.revertedOrder(ecList)) {
			final RegDate dateDebutCivile = ec.getDateDebut();
			final RegDate dateDebut = dateDebutCivile == null ? findDateDebutEtatCivil(ec, pp, dateFin) : dateDebutCivile;
			if (dateDebut == null || RegDateHelper.isBeforeOrEqual(dateDebut, dateFin, NullDateBehavior.LATEST)) {
				final VueSituationFamille vue = new VueSituationFamillePersonnePhysiqueCivilAdapter(ec, dateDebut, dateFin);
				list.add(0, vue);
				if (dateDebut == null) {
					// c'est fini, on sort, tous les autres états civils sont ignorés
					break;
				}
				dateFin = dateDebut.getOneDayBefore();
			}
		}

		return list;
	}

	private RegDate findDateDebutEtatCivil(EtatCivil etatCivil, PersonnePhysique pp, @Nullable RegDate max) {

		// CELIBATAIRE -> défaut à la date de naissance de l'individu (si la date est partielle, on prend le premier jour du mois/année correspondant)
		// MARIE/PACSE... -> défaut à la date de début du dernier rapport d'appartenance ménage qui commence avant (ou à) la date "max"
		// autres -> défaut à la date de début de la dernière période continue de fors principaux qui commence avant (ou à) la date "max"
		//        -> si toujours rien, alors lendemain de la dernière date de fin du dernier rapport d'appartenance ménage qui se termine avant la date "max"

		RegDate dateDebutEtatCivil = null;
		if (etatCivil.getTypeEtatCivil() == TypeEtatCivil.CELIBATAIRE) {
			dateDebutEtatCivil = tiersService.getDateNaissance(pp);
			if (dateDebutEtatCivil != null && dateDebutEtatCivil.isPartial()) {
				dateDebutEtatCivil = FiscalDateHelper.getDateComplete(dateDebutEtatCivil);
			}
		}
		else if (EtatCivilHelper.estMarieOuPacse(etatCivil)) {
			// date de début du dernier rapport d'appartenance ménage qui commence avant (ou à) la date "max"
			final List<RapportEntreTiers> ams = getAppartenancesMenagesTriees(pp);
			for (RapportEntreTiers am : CollectionsUtils.revertedOrder(ams)) {
				if (RegDateHelper.isAfterOrEqual(max, am.getDateDebut(), NullDateBehavior.LATEST)) {
					dateDebutEtatCivil = am.getDateDebut();
					break;
				}
			}
		}
		else {
			final List<ForFiscalPrincipalPP> fors = pp.getForsFiscauxPrincipauxActifsSorted();
			if (fors != null && !fors.isEmpty()) {
				// récupération des zones continues de fors principaux
				final List<CollatableDateRange> ranges = new ArrayList<>(fors.size());
				for (ForFiscalPrincipal ffp : fors) {
					ranges.add(new DateRangeHelper.Range(ffp));
				}
				final List<CollatableDateRange> collated = DateRangeHelper.collate(ranges);

				// on prend la première date de début d'une zone continue qui se situe avant (ou à) la date "max"
				for (DateRange ff : CollectionsUtils.revertedOrder(collated)) {
					if (RegDateHelper.isBeforeOrEqual(ff.getDateDebut(), max, NullDateBehavior.LATEST)) {
						dateDebutEtatCivil = ff.getDateDebut();
						break;
					}
				}
			}
			else {
				// on prend donc le lendemain de la date de fin du dernier rapport d'appartenance ménage qui se termine avant la date "max"
				final List<RapportEntreTiers> ams = getAppartenancesMenagesTriees(pp);
				for (RapportEntreTiers am : CollectionsUtils.revertedOrder(ams)) {
					if (RegDateHelper.isBefore(am.getDateFin(), max, NullDateBehavior.LATEST)) {
						dateDebutEtatCivil = am.getDateFin().getOneDayAfter();
						break;
					}
				}
			}
		}
		return dateDebutEtatCivil;
	}

	@NotNull
	private static List<RapportEntreTiers> getAppartenancesMenagesTriees(PersonnePhysique pp) {
		final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
		if (rapports != null && !rapports.isEmpty()) {

			// récupération des rapports d'appartenance ménage du contribuable
			final List<RapportEntreTiers> tries = new ArrayList<>();
			for (RapportEntreTiers rapport : rapports) {
				if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType() && !rapport.isAnnule()) {
					tries.add(rapport);
				}
			}

			// tri
			tries.sort(new DateRangeComparator<>());
			return tries;
		}

		return Collections.emptyList();
	}

	/**
	 * Récupère la situation de famille du contribuable donné dans le registre fiscal et construit une vue de sa situation de famille à
	 * partir de ça. 2 listes sont construites la première contient les situatiosn valides et la deuxième les situations annulées.
	 *
	 * @param contribuable
	 *            le contribuable considéré
	 * @return une liste de situations de famille valide et une liste de situation de famille annulées
	 */
	private HashMap<String, List<VueSituationFamille>> buildSituationsFiscalesHisto(Contribuable contribuable) {

		HashMap<String, List<VueSituationFamille>> map_resultat = new HashMap<>();
		List<VueSituationFamille> listValide = new ArrayList<>();
		List<VueSituationFamille> listAnnulee = new ArrayList<>();

		if (contribuable instanceof PersonnePhysique) {
			PersonnePhysique personne = (PersonnePhysique) contribuable;

			Set<SituationFamille> situations = personne.getSituationsFamille();
			if (situations != null) {
				for (SituationFamille s : situations) {

					VueSituationFamille vue = new VueSituationFamillePersonnePhysiqueFiscalAdapter(s,
							VueSituationFamille.Source.FISCALE_TIERS);
					if (s.getAnnulationDate() == null) {
						listValide.add(vue);
					}
					else {
						listAnnulee.add(vue);
					}
				}
			}
			Set<RapportEntreTiers> rapportsSujet = personne.getRapportsSujet();
			if (rapportsSujet != null) {
				Set<MenageCommun> menages = new HashSet<>();
				for (RapportEntreTiers rapportSujet : rapportsSujet) {
					if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapportSujet.getType() && !rapportSujet.isAnnule()) {
						final MenageCommun menage = hibernateTemplate.get(MenageCommun.class, rapportSujet.getObjetId());
						menages.add(menage);
					}
				}
				List<VueSituationFamille> listCouple = new ArrayList<>();

				for (MenageCommun menage : menages) {
					situations = menage.getSituationsFamille();
					if (situations != null) {
						for (SituationFamille s : situations) {

							VueSituationFamille vue = new VueSituationFamilleMenageCommunFiscalAdapter((SituationFamilleMenageCommun) s,
									VueSituationFamille.Source.FISCALE_AUTRE_TIERS);
							if (s.getAnnulationDate() == null) {
								listCouple.add(vue);
							}

						}
					}
				}

				listValide = DateRangeHelper.override(listValide, listCouple, new VueSituationFamilleAdapterCallback());

			}
		}
		else {
			Assert.isTrue(contribuable instanceof MenageCommun);
			MenageCommun menage = (MenageCommun) contribuable;

			Set<SituationFamille> situations = menage.getSituationsFamille();
			if (situations != null) {
				for (SituationFamille s : situations) {
					// On ne prend pas les situations de familles annulées

					VueSituationFamille vue = new VueSituationFamilleMenageCommunFiscalAdapter((SituationFamilleMenageCommun) s,
							VueSituationFamille.Source.FISCALE_TIERS);
					if (s.getAnnulationDate() == null) {
						listValide.add(vue);
					}
					else {
						listAnnulee.add(vue);
					}
				}
			}
		}
		map_resultat.put(valide, listValide);
		map_resultat.put(annulee, listAnnulee);

		return map_resultat;
	}

	/**
	 * @return l'état civil du point de vue fiscal d'une personne physique.
	 */
	@Override
	public ch.vd.uniregctb.type.EtatCivil getEtatCivil(PersonnePhysique pp, RegDate date, boolean takeCivilAsDefault) {
		Assert.notNull(pp, "la personne physique doit être renseignée");

		/*
		 * Mail de Thierry Declercq (18.08.2008) : "L'état civil du point de vue fiscal est celui de la situation de famille et cela dans
		 * tous les cas. Toutefois, j'apporte un bémol : il faut que cet état civil de la situation de famille puisse être restitué dans le
		 * plus de cas possibles. Comme il est le plus souvent identique à celui du registre civil, deux solutions me semblent possibles : -
		 * Avoir un mécanisme de défaut : en l'absence de situation de famille, l'état civil qui prévaut est celui du registre civil - Faire
		 * en sorte que les automates des événements civils mettent à jour la situation de famille."
		 */
		final VueSituationFamille vueSituationFamille = getVue(pp, date, takeCivilAsDefault);
		if (vueSituationFamille != null) {
			return vueSituationFamille.getEtatCivil();
		}

		// Défaut dans le cas d'un habitant: on va chercher son état civil dans le registre civil.
		if (pp.isConnuAuCivil() && takeCivilAsDefault) {
			final Individu individu = tiersService.getIndividu(pp);
			final EtatCivil etatCivil = serviceCivil.getEtatCivilActif(individu.getNoTechnique(), date);
			return etatCivil == null ? null : EtatCivilHelper.civil2core(etatCivil.getTypeEtatCivil());
		}

		return null;
	}

	@Override
	public SituationFamille addSituationFamille(SituationFamille situationFamille, ContribuableImpositionPersonnesPhysiques contribuable) {
		final RegDate dateDebut = situationFamille.getDateDebut();
		contribuable.closeSituationFamilleActive(dateDebut.getOneDayBefore());

		situationFamille = tiersDAO.addAndSave(contribuable, situationFamille);
		evenementFiscalService.publierEvenementFiscalChangementSituationFamille(dateDebut, contribuable);
		return situationFamille;
	}

	private SituationFamille internalAnnulerSituationFamille(long idSituationFamille, boolean rouvrirPrecedente) {

		// Situation de famille ayant comme source Unireg
		SituationFamille situationFamille = situationFamilleDAO.get(idSituationFamille);
		Assert.notNull(situationFamille);

		final ContribuableImpositionPersonnesPhysiques contribuable = situationFamille.getContribuable();
		Assert.notNull(contribuable);

		// Annulation de la situation de famille
		final List<SituationFamille> situations = contribuable.getSituationsFamilleSorted();
		// la situation de famille doit être la dernière non annulee
		Assert.notEmpty(situations);

		final SituationFamille lastSituationFamille = situations.get(situations.size() - 1);
		Assert.notNull(lastSituationFamille);

		if (lastSituationFamille.getId() != idSituationFamille) {
			throw new ValidationException(idSituationFamille, "Seule la dernière situation de famille peut être annulée.");
		}

		if (rouvrirPrecedente) {

			// Recherche de la situation de famille précédente si elle existe
			// on reouvre la situation de famille precedente
			SituationFamille situationFamillePrecedente;
			if (situations.size() > 1
					&& situations.get(situations.size() - 2).getDateFin() == situationFamille.getDateDebut().getOneDayBefore()) {
				situationFamillePrecedente = situations.get(situations.size() - 2);
				situationFamillePrecedente.setDateFin(null);
			}

		}

		situationFamille.setAnnule(true);

		return situationFamille;

	}

	/**
	 * Annule une situation de famille en réouvrant la précédente si elle existe
	 */
	@Override
	public void annulerSituationFamille(long idSituationFamille) {

		// Situation de famille ayant comme source Unireg
		final SituationFamille situationFamille = internalAnnulerSituationFamille(idSituationFamille, true);
		evenementFiscalService.publierEvenementFiscalChangementSituationFamille(RegDate.get(), situationFamille.getContribuable());
	}

	/**
	 * Annule une situation de famille
	 *
	 * @param idSituationFamille
	 */

	@Override
	public void annulerSituationFamilleSansRouvrirPrecedente(long idSituationFamille) {

		// Situation de famille ayant comme source Unireg
		final SituationFamille situationFamille = internalAnnulerSituationFamille(idSituationFamille, false);
		evenementFiscalService.publierEvenementFiscalChangementSituationFamille(RegDate.get(), situationFamille.getContribuable());
	}

	@Override
	public void closeSituationFamille(ContribuableImpositionPersonnesPhysiques contribuable, RegDate date) {
		Assert.notNull(contribuable);
		// Situation de famille ayant comme source Unireg
		final SituationFamille situationFamille = contribuable.getSituationFamilleActive();
		if (situationFamille != null) {
			contribuable.closeSituationFamilleActive(date);
			evenementFiscalService.publierEvenementFiscalChangementSituationFamille(RegDate.get(), contribuable);
		}
	}

	public void setSituationFamilleDAO(SituationFamilleDAO situationFamilleDAO) {
		this.situationFamilleDAO = situationFamilleDAO;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ReinitialiserBaremeDoubleGainResults reinitialiserBaremeDoubleGain(RegDate dateTraitement, StatusManager statusManager) {
		final ReinitialiserBaremeDoubleGainProcessor processor = new ReinitialiserBaremeDoubleGainProcessor(this, hibernateTemplate, transactionManager, tiersService, adresseService);
		return processor.run(dateTraitement, statusManager);
	}

	@Override
	public ComparerSituationFamilleResults comparerSituationFamille(RegDate dateTraitement, int nbThreads, StatusManager status) {
		final ComparerSituationFamilleProcessor processor = new ComparerSituationFamilleProcessor(serviceCivil, hibernateTemplate, tiersService, transactionManager, adresseService);
		return processor.run(dateTraitement,nbThreads,status);
	}
}
