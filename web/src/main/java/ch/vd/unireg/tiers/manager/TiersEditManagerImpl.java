package ch.vd.unireg.tiers.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.interfaces.InterfaceDataException;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Remarque;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.dao.RemarqueDAO;
import ch.vd.unireg.tiers.view.DebiteurEditView;
import ch.vd.unireg.tiers.view.IdentificationPersonneView;
import ch.vd.unireg.tiers.view.TiersEditView;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.PeriodeDecompte;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.utils.BeanUtils;
import ch.vd.unireg.utils.WebContextUtils;

/**
 * Service qui fournit les methodes pour editer un tiers
 *
 */
public class TiersEditManagerImpl extends TiersManager implements TiersEditManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersEditManagerImpl.class);

	private RemarqueDAO remarqueDAO;

	public void setRemarqueDAO(RemarqueDAO remarqueDAO) {
		this.remarqueDAO = remarqueDAO;
	}

	/**
	 * Charge les informations dans TiersView
	 *
	 */
	@Override
	@Transactional(readOnly = true)
	public TiersEditView getCivilView(Long numero) throws AdresseException, InfrastructureException {

		TiersEditView tiersEditView = new TiersEditView();
		if ( numero == null) {
			return null;
		}
		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}

		setTiersGeneralView(tiersEditView, tiers);
		tiersEditView.setComplement(buildComplement(tiers, false));

		if (tiers instanceof PersonnePhysique) {
			PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isHabitantVD()) {
				setHabitant(tiersEditView, pp);
			} else {
				setNonHabitant(tiersEditView, pp);
			}
		}
		else if (tiers instanceof MenageCommun) {
			MenageCommun menageCommun = (MenageCommun) tiers;
			setMenageCommun(tiersEditView, menageCommun);
		}
		else if (tiers instanceof AutreCommunaute) {
			AutreCommunaute autreCommunaute = (AutreCommunaute) tiers;
			tiersEditView.setTiers(autreCommunaute);
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
			tiersEditView.setTiers(dpi);
			if (dpi.getContribuableId() == null) {
				tiersEditView.setAddContactISAllowed(true);
			}
			else {
				tiersEditView.setAddContactISAllowed(false);
			}
		}

		return tiersEditView;
	}


	/**
	 * Charge les informations dans un objet qui servira de view
	 *
	 * @param numero numéro de tiers du débiteur recherché
	 * @return un objet DebiteurEditView
	 * @throws InfrastructureException
	 */
	@Override
	@Transactional(readOnly = true)
	public DebiteurEditView getDebiteurEditView(Long numero) throws InfrastructureException {
		if (numero == null) {
			return null;
		}

		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}
		if (!(tiers instanceof DebiteurPrestationImposable)) {
			throw new IllegalArgumentException();
		}
		return new DebiteurEditView((DebiteurPrestationImposable) tiers);
	}

	@Override
	public List<RegDate> getDatesPossiblesPourDebutNouvellePeriodicite(long dpiId, PeriodiciteDecompte nouvellePeriodicite, RegDate maxDate, boolean descendingOnly) {
		final Tiers tiers = tiersDAO.get(dpiId);
		if (tiers == null) {
			throw new TiersNotFoundException(dpiId);
		}
		if (tiers instanceof DebiteurPrestationImposable) {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
			final RegDate minDate = tiersService.getDateDebutNouvellePeriodicite(dpi, nouvellePeriodicite);

			final Set<RegDate> datesPossibles = new TreeSet<>();

			// périodicité active à la veille de la date minimale = périodicité avec laquelle il faut composer
			final List<Periodicite> periodicites = dpi.getPeriodicitesSorted();
			Periodicite active = DateRangeHelper.rangeAt(periodicites, minDate.getOneDayBefore());
			if (active == null) {
				active = DateRangeHelper.rangeAt(periodicites, minDate);
			}
			if (active == null) {
				datesPossibles.add(minDate);
			}
			else {

				// on avance dans le temps tant qu'on est avant (ou le jour même) la date maximale
				RegDate current = minDate;
				while (current.isBeforeOrEqual(maxDate)) {
					// on n'ajoute la date dans la liste que si la périodicité n'est pas déjà active à cette date (sauf la date de début, justement, pour indiquer que rien ne change)
					if (active.getPeriodiciteDecompte() == PeriodiciteDecompte.UNIQUE
							|| nouvellePeriodicite == PeriodiciteDecompte.UNIQUE
							|| active.getPeriodiciteDecompte() != nouvellePeriodicite
							|| current == active.getDateDebut()) {
						datesPossibles.add(current);
					}

					if (active.getPeriodiciteDecompte() == PeriodiciteDecompte.UNIQUE || nouvellePeriodicite == PeriodiciteDecompte.UNIQUE) {
						current = current.addYears(1);
					}
					else {

						// si on essaie de ré-agrandir la périodicité sans en avoir le droit, cela ne doit pas fonctionner -> one ne propose pas de date ultérieure au remplacement
						if (descendingOnly && nouvellePeriodicite.getShorterPeriodicities().contains(active.getPeriodiciteDecompte())) {
							break;
						}

						while (true) {
							final RegDate candidate = active.getPeriodiciteDecompte().getDebutPeriodeSuivante(current);
							if (active.getDateFin() != null && candidate.isAfter(active.getDateFin())) {
								active = DateRangeHelper.rangeAt(periodicites, candidate);
								current = active.getDateDebut();
							}
							else {
								current = candidate;
							}
							if (nouvellePeriodicite.getDebutPeriode(candidate) == candidate) {
								break;
							}
						}
					}
				}
			}

			return new ArrayList<>(datesPossibles);
		}
		else {
            return Collections.emptyList();
		}
	}

	/**
	 * Charge les informations dans TiersView
	 *
	 */
	@Override
	public TiersEditView getView(Long numero) throws AdresseException, InfrastructureException {
		TiersEditView tiersEditView = new TiersEditView();
		refresh(tiersEditView, numero);
		return tiersEditView;
	}


	/**
	 * Rafraichissement de la vue
	 *
	 */
	@Override
	public TiersEditView refresh(TiersEditView tiersEditView, Long numero) throws AdresseException, InfrastructureException {
		if ( numero == null) {
			return null;
		}
		Tiers  oldTiers = tiersEditView.getTiers();
		tiersEditView.clear();
		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}

		setTiersGeneralView(tiersEditView, tiers);
		tiersEditView.setComplement(buildComplement(tiers, false));

		if( oldTiers != null) {
			BeanUtils.simpleMerge(tiers, oldTiers);
		}

		if (tiers instanceof PersonnePhysique) {
			PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isHabitantVD()) {
				setHabitant(tiersEditView, pp);
			} else {
				setNonHabitant(tiersEditView, pp);
			}
		}
		else if (tiers instanceof MenageCommun) {
			MenageCommun menageCommun = (MenageCommun) tiers;
			setMenageCommun(tiersEditView, menageCommun);
		}
		/* les entreprises ne sont pas éditables
		else if (tiers instanceof Entreprise) {
			Entreprise entreprise = (Entreprise) tiers;
			setEntreprise(tiersEditView, entreprise);
		}*/
		else if (tiers instanceof AutreCommunaute) {
			AutreCommunaute autreCommunaute = (AutreCommunaute) tiers;
			tiersEditView.setTiers(autreCommunaute);
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
			tiersEditView.setTiers(dpi);
			setForsFiscauxDebiteur(tiersEditView, dpi);
			setContribuablesAssocies(tiersEditView, dpi, false);
			setPeriodiciteCourante(tiersEditView,dpi);
			if (dpi.getContribuableId() == null) {
				tiersEditView.setAddContactISAllowed(true);
			}
			else {
				tiersEditView.setAddContactISAllowed(false);
			}
		}

		if (tiersEditView.getTiers() != null){
			if (tiers instanceof Entreprise || tiers instanceof PersonnePhysique) {
				tiersEditView.setRapportsEtablissements(getRapportsEtablissements(tiers));
			}
			if(tiers instanceof Contribuable) {
				final Contribuable contribuable = (Contribuable) tiers;
				tiersEditView.setDebiteurs(getDebiteurs(contribuable));
				setForsFiscaux(tiersEditView, contribuable);

				try {
					setSituationsFamille(tiersEditView, contribuable);
					tiersEditView.setSituationFamilleActive(isSituationFamilleActive(contribuable));
				}
				catch (InterfaceDataException e) {
					LOGGER.warn(String.format("Exception lors de la récupération des situations de familles du contribuable %d", numero), e);
					tiersEditView.setSituationsFamilleEnErreurMessage(e.getMessage());
				}
			}

			tiersEditView.setDossiersApparentes(getRapports(tiers));
			setAdressesActives(tiersEditView, tiers);
			setAdressesFiscalesModifiables(tiersEditView,tiers);
		}

		return tiersEditView;
	}

	/**
	 * Met a jour le non-habitant pour l'edition
	 *
	 * @throws InfrastructureException
	 */
	private void setNonHabitant(TiersEditView tiersEditView, PersonnePhysique nonHabitant) throws InfrastructureException {

		final IdentificationPersonneView idPersonneView = new IdentificationPersonneView(nonHabitant);
		final Integer numeroOfsNationalite = nonHabitant.getNumeroOfsNationalite();
		if (numeroOfsNationalite != null) {
			tiersEditView.setLibelleOfsPaysOrigine(getServiceInfrastructureService().getPays(numeroOfsNationalite, null).getNomCourt());
		}

		tiersEditView.setIdentificationPersonne(idPersonneView);
		tiersEditView.setTiers(nonHabitant);
	}

	/**
	 * Cree une nouvelle instance de TiersView correspondant a une personne
	 *
	 * @return un objet TiersView
	 */
	@Override
	public TiersEditView creePersonne() {
		TiersEditView tiersView = new TiersEditView();
		IdentificationPersonneView idpersonneView = new IdentificationPersonneView();
		tiersView.setIdentificationPersonne(idpersonneView);
		PersonnePhysique nonHab = new PersonnePhysique(false);
		tiersView.setTiers(nonHab);
		return tiersView;
	}

	/**
	 * Cree une nouvelle instance de TiersView correspondant a une entreprise
	 *
	 * @return un objet TiersView
	 */
	@Override
	public TiersEditView creeEntreprise() {
		TiersEditView tiersView = new TiersEditView();
		AutreCommunaute autreCommunaute = new AutreCommunaute();
		tiersView.setTiers(autreCommunaute);
		return tiersView;
	}

	private Remarque addRemarque(Tiers tiers, String visa, String texteRemarque) {
		final Remarque remarque = new Remarque();
		remarque.setTiers(tiers);
		remarque.setTexte(texteRemarque);

		AuthenticationHelper.pushPrincipal(visa);
		try {
			return remarqueDAO.save(remarque);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private String getModeCommunicationDisplayString(ModeCommunication modeCommunication) {
		if (modeCommunication == null) {
			return StringUtils.EMPTY;
		}
		final String key = String.format("option.mode.communication.%s", modeCommunication.name());
		return messageSource.getMessage(key, null, WebContextUtils.getDefaultLocale());
	}

	@Override
	public void save(DebiteurEditView view) {
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(view.getId());
		if (dpi.isSansLREmises()) {
			dpi.setCategorieImpotSource(view.getCategorieImpotSource());
		}

		// [SIFISC-12197] en cas de changement de mode de communication, il faut inscrire une remarque sur le débiteur
		if (dpi.getModeCommunication() != view.getModeCommunication()) {
			final String texteRemarque = String.format("Changement de mode de communication :\n'%s' --> '%s'",
			                                           getModeCommunicationDisplayString(dpi.getModeCommunication()),
			                                           getModeCommunicationDisplayString(view.getModeCommunication()));
			addRemarque(dpi, String.format("%s-auto", AuthenticationHelper.getCurrentPrincipal()), texteRemarque);
			dpi.setModeCommunication(view.getModeCommunication());
		}

		dpi.setLogicielId(view.getLogicielId());
		changePeriodicite(dpi, view.getNouvellePeriodicite(), view.getPeriodeDecompte(), view.getDateDebutNouvellePeriodicite());
	}

	private Periodicite changePeriodicite(DebiteurPrestationImposable dpi, PeriodiciteDecompte nouvellePeriodicite, PeriodeDecompte nouvellePeriode, RegDate dateDebut) {
		final PeriodeDecompte periodeDecompte = (nouvellePeriodicite == PeriodiciteDecompte.UNIQUE ? nouvellePeriode : null);
		return tiersService.addPeriodicite(dpi, nouvellePeriodicite, periodeDecompte, dateDebut, null);
	}

	/**
	 * Compte le nombre de rapports prestation imposable pour un débiteur
	 */
	@Transactional(readOnly = true)
	public int countRapportsPrestationImposable(Long numeroDebiteur, boolean rapportsPrestationHisto) {
		return rapportEntreTiersDAO.countRapportsPrestationImposable(numeroDebiteur, !rapportsPrestationHisto);
	}

	/**
	 * Annule un tiers
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerTiers(Long numero) {
		final Tiers tiers = tiersService.getTiers(numero);
		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}
		tiersService.annuleTiers(tiers);
	}
}
