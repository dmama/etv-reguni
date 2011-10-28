package ch.vd.uniregctb.situationfamille;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.DateRangeHelper.AdapterCallback;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.interfaces.InterfaceDataException;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
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

			// nullAllowed -> s'il n'y a pas de surcharge au niveau fiscal, on tolère les situations de familles avec des dates nulles
			// présentes au niveau civil. Dans le case contraire, on ne peut pas les tolérer pour être capable d'effectuer la surcharge
			// correctement
			final boolean nullAllowed = (situationsFiscales == null || situationsFiscales.isEmpty());

			final List<VueSituationFamille> situationsCiviles = buildSituationsCivilesHisto(contribuable, nullAllowed);
			resultat = DateRangeHelper.override(situationsCiviles, situationsFiscales, new VueSituationFamilleAdapterCallback());

			// si besoin on rajoute les situations de familles fiscales annulées. la liste ainsi obtenue est utilisées dans l'affichage
			// du tiers et dans le web service
			if (annuleeAllowed) {
				resultat.addAll(situationsFiscalesAnnulees);
				Collections.sort(resultat, new DateRangeComparator<VueSituationFamille>());
			}
		}
		else {
			/*
			 * par définition, tous les autres type de contribuables (entreprises, établissement, ...) ne possèdent pas de situation de
			 * famille
			 */
			resultat = new ArrayList<VueSituationFamille>();
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
	private List<VueSituationFamille> buildSituationsCivilesHisto(Contribuable contribuable, boolean nullAllowed) {

		final List<VueSituationFamille> list = new ArrayList<VueSituationFamille>();

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

		final Collection<EtatCivil> coll = individu.getEtatsCivils();
		if (coll == null || coll.isEmpty()) {
			return list;
		}
		// traitement du cas particulier où l'individu ne possède pas d'état civil
		// host-interface renvoie quand même un état civil avec date début nulle, type nul et no sequence = 0
		if (coll.size() == 1) {
			EtatCivil etat = coll.iterator().next();
			if (etat == null || etat.getTypeEtatCivil() == null) {
				return list;
			}
		}

		EtatCivil etatPrecedent = null;
		RegDate dateDebutEtatPrecedent = null;

		// la date de fin n'est pas définie au niveau de l'état civil -> on doit la déduire à partir de la date de début suivante
		for (EtatCivil etatCourant : coll) {

			RegDate dateDebutEtatCourant;
			if (etatCourant.getDateDebutValidite() == null) {
				dateDebutEtatCourant = findDateDebutEtatCivil(etatCourant, pp, individu);
			}
			else {
				dateDebutEtatCourant = etatCourant.getDateDebutValidite();
			}

			if (etatPrecedent != null) {
				if (etatPrecedent.getDateDebutValidite() == null) {
					if (etatCourant.getDateDebutValidite() == null
							|| (dateDebutEtatPrecedent != null && dateDebutEtatPrecedent.isAfterOrEqual(etatCourant.getDateDebutValidite()))) {
						dateDebutEtatPrecedent = null;
					}
					if (!nullAllowed && dateDebutEtatPrecedent == null) {
						throw new InterfaceDataException(buildExceptionMessage(pp, etatPrecedent));
					}
				}
				if (etatCourant.getDateDebutValidite() == null) {
					if (dateDebutEtatCourant != null && dateDebutEtatPrecedent != null && dateDebutEtatPrecedent.isAfterOrEqual(dateDebutEtatCourant)) {
						// la date de début de l'état-civil est nulle, on a pu déterminer une date de début à partir d'autres sources
						// mais on se rend compte que cette date est invalide => on laisse tomber la date nouvelle déterminée
						dateDebutEtatCourant = null;
					}
					if (!nullAllowed && dateDebutEtatCourant == null) {
						throw new InterfaceDataException(buildExceptionMessage(pp, etatCourant));
					}
				}
				RegDate dateFinEtatPrecedent = null;
				if (dateDebutEtatCourant != null) {
					dateFinEtatPrecedent = dateDebutEtatCourant.getOneDayBefore();
				}
				VueSituationFamille vue = new VueSituationFamillePersonnePhysiqueCivilAdapter(etatPrecedent, dateDebutEtatPrecedent,
						dateFinEtatPrecedent);
				list.add(vue);
			}
			etatPrecedent = etatCourant;
			dateDebutEtatPrecedent = dateDebutEtatCourant;
		}

		if (etatPrecedent != null) {
			if (etatPrecedent.getDateDebutValidite() == null) {
				if (!nullAllowed && dateDebutEtatPrecedent == null) {
					throw new InterfaceDataException(buildExceptionMessage(pp, etatPrecedent));
				}
			}
			VueSituationFamille vue = new VueSituationFamillePersonnePhysiqueCivilAdapter(etatPrecedent, dateDebutEtatPrecedent, null);
			list.add(vue);
		}

		// [UNIREG-823] Dans le cas d'un individu décédé, on doit fermer la dernière situation retournée par le civil à date décès
		final RegDate dateDeces = tiersService.getDateDeces(pp);
		if (dateDeces != null) {
			if (!list.isEmpty()) {
				final int last = list.size() - 1;
				VueSituationFamille vue = list.get(last);
				vue = new VueSituationFamillePersonnePhysiqueCivilAdapter(vue.getEtatCivil(), vue.getDateDebut(), dateDeces);
				list.set(last, vue);
			}
		}

		return list;
	}

	/**
	 * Construit le message de l'exception levée lorsqu'un état civil possède une date de début nulle
	 */
	private static String buildExceptionMessage(PersonnePhysique habitant, EtatCivil etat) {
		return String.format("Contribuable %s / Individu n°%d: l'état civil %s n'a pas de date de début !",
							FormatNumeroHelper.numeroCTBToDisplay(habitant.getNumero()),
							habitant.getNumeroIndividu(),
							etat.getTypeEtatCivil().asCore());
	}

	private RegDate findDateDebutEtatCivil(EtatCivil etatCivil, PersonnePhysique habitant, Individu individu) {
		RegDate dateDebutEtatCivil = null;
		if (etatCivil.getTypeEtatCivil()== TypeEtatCivil.CELIBATAIRE) {
			dateDebutEtatCivil = individu.getDateNaissance();
		}
		else if (etatCivil.getTypeEtatCivil()==TypeEtatCivil.MARIE
				|| etatCivil.getTypeEtatCivil()==TypeEtatCivil.PACS) {
			Set<RapportEntreTiers> rapports = habitant.getRapportsSujet();
			if (rapports != null) {
				for (RapportEntreTiers rapport : rapports) {
					if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType() && !rapport.isAnnule()) {
						if (dateDebutEtatCivil == null || dateDebutEtatCivil.isAfter(rapport.getDateDebut())) {
							dateDebutEtatCivil = rapport.getDateDebut();
						}
					}
				}
			}
			if (dateDebutEtatCivil == null) {
				List<ForFiscalPrincipal> fors = habitant.getForsFiscauxPrincipauxActifsSorted();
				if (fors != null && fors.size() > 0) {
					dateDebutEtatCivil = fors.get(0).getDateDebut();
				}
			}
		}
		else {
			List<ForFiscalPrincipal> fors = habitant.getForsFiscauxPrincipauxActifsSorted();
			if (fors != null && fors.size() > 0) {
				dateDebutEtatCivil = fors.get(0).getDateDebut();
			}
			else {
				Set<RapportEntreTiers> rapports = habitant.getRapportsSujet();
				if (rapports != null) {
					for (RapportEntreTiers rapport : rapports) {
						if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType() && !rapport.isAnnule()) {
							if (dateDebutEtatCivil == null
									|| (rapport.getDateFin() != null && dateDebutEtatCivil.isAfter(rapport.getDateFin()))) {
								dateDebutEtatCivil = rapport.getDateFin();
							}
						}
					}
				}
			}
		}
		return dateDebutEtatCivil;
	}

	/**
	 * Récupère la situation de famille du contribuable donné dans le registre fiscale et construit une vue de sa situation de famille à
	 * partir de ça. 2 listes sont construites la première contient les situatiosn valides et la deuxième les situations annulées.
	 *
	 * @param contribuable
	 *            le contribuable considéré
	 * @return une liste de situations de famille valide et une liste de situation de famille annulées
	 */
	private HashMap<String, List<VueSituationFamille>> buildSituationsFiscalesHisto(Contribuable contribuable) {

		HashMap<String, List<VueSituationFamille>> map_resultat = new HashMap<String, List<VueSituationFamille>>();
		List<VueSituationFamille> listValide = new ArrayList<VueSituationFamille>();
		List<VueSituationFamille> listAnnulee = new ArrayList<VueSituationFamille>();

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
				Set<MenageCommun> menages = new HashSet<MenageCommun>();
				for (RapportEntreTiers rapportSujet : rapportsSujet) {
					if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapportSujet.getType() && !rapportSujet.isAnnule()) {
						final MenageCommun menage = hibernateTemplate.get(MenageCommun.class, rapportSujet.getObjetId());
						menages.add(menage);
					}
				}
				List<VueSituationFamille> listCouple = new ArrayList<VueSituationFamille>();

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
			final ch.vd.uniregctb.interfaces.model.EtatCivil etatCivil = serviceCivil.getEtatCivilActif(individu.getNoTechnique(), date);
			return etatCivil == null ? null : etatCivil.getTypeEtatCivil().asCore();
		}

		return null;
	}

	@Override
	public SituationFamille addSituationFamille(SituationFamille situationFamille, Contribuable contribuable) {
		final RegDate dateDebut = situationFamille.getDateDebut();
		contribuable.closeSituationFamilleActive(dateDebut.getOneDayBefore());

		situationFamille = tiersDAO.addAndSave(contribuable, situationFamille);
		evenementFiscalService.publierEvenementFiscalChangementSituation(contribuable, dateDebut, situationFamille.getId());
		return situationFamille;
	}

	private SituationFamille internalAnnulerSituationFamille(long idSituationFamille, boolean rouvrirPrecedente) {

		// Situation de famille ayant comme source Unireg
		SituationFamille situationFamille = situationFamilleDAO.get(idSituationFamille);
		Assert.notNull(situationFamille);

		final Contribuable contribuable = situationFamille.getContribuable();
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
		evenementFiscalService.publierEvenementFiscalChangementSituation(situationFamille.getContribuable(), RegDate.get(), idSituationFamille);
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
		evenementFiscalService.publierEvenementFiscalChangementSituation(situationFamille.getContribuable(), RegDate.get(), situationFamille.getId());
	}

	@Override
	public void closeSituationFamille(Contribuable contribuable, RegDate date) {
		Assert.notNull(contribuable);
		// Situation de famille ayant comme source Unireg
		final SituationFamille situationFamille = contribuable.getSituationFamilleActive();
		if (situationFamille != null) {
			contribuable.closeSituationFamilleActive(date);
			evenementFiscalService.publierEvenementFiscalChangementSituation(contribuable, RegDate.get(), situationFamille.getId());
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
		ReinitialiserBaremeDoubleGainProcessor processor = new ReinitialiserBaremeDoubleGainProcessor(this, hibernateTemplate,
				transactionManager);
		return processor.run(dateTraitement, statusManager);
	}

	@Override
	public ComparerSituationFamilleResults comparerSituationFamille(RegDate dateTraitement, int nbThreads, StatusManager status) {
		ComparerSituationFamilleProcessor processor = new ComparerSituationFamilleProcessor(serviceCivil,situationFamilleDAO,tiersService,transactionManager);
		return processor.run(dateTraitement,nbThreads,status);
	}
}
