package ch.vd.uniregctb.tiers.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.entreprise.EntrepriseView;
import ch.vd.uniregctb.iban.IbanHelper;
import ch.vd.uniregctb.interfaces.InterfaceDataException;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.CoordonneesFinancieres;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.dao.RemarqueDAO;
import ch.vd.uniregctb.tiers.view.ComplementView;
import ch.vd.uniregctb.tiers.view.CompteBancaireView;
import ch.vd.uniregctb.tiers.view.DebiteurEditView;
import ch.vd.uniregctb.tiers.view.IdentificationPersonneView;
import ch.vd.uniregctb.tiers.view.PeriodiciteView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.utils.BeanUtils;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Service qui fournit les methodes pour editer un tiers
 *
 */
public class TiersEditManagerImpl extends TiersManager implements TiersEditManager {

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
	public TiersEditView getCivilView(Long numero) throws AdresseException, ServiceInfrastructureException {

		TiersEditView tiersEditView = new TiersEditView();
		if ( numero == null) {
			return null;
		}
		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}

		setTiersGeneralView(tiersEditView, tiers);
		tiersEditView.setComplement(buildComplement(tiers));

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
	 * @throws AdressesResolutionException
	 * @throws ServiceInfrastructureException
	 */
	@Override
	@Transactional(readOnly = true)
	public DebiteurEditView getDebiteurEditView(Long numero) throws AdresseException, ServiceInfrastructureException {
		if (numero == null) {
			return null;
		}

		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}
		Assert.isInstanceOf(DebiteurPrestationImposable.class, tiers);
		return new DebiteurEditView((DebiteurPrestationImposable) tiers, ibanValidator);
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
	public TiersEditView getView(Long numero) throws AdresseException, ServiceInfrastructureException{
		TiersEditView tiersEditView = new TiersEditView();
		refresh(tiersEditView, numero);
		return tiersEditView;
	}


	/**
	 * Rafraichissement de la vue
	 *
	 */
	@Override
	public TiersEditView refresh(TiersEditView tiersEditView, Long numero) throws AdresseException, ServiceInfrastructureException {
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
		tiersEditView.setComplement(buildComplement(tiers));

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
	 * @throws ServiceInfrastructureException
	 */
	private void setNonHabitant(TiersEditView tiersEditView, PersonnePhysique nonHabitant) throws ServiceInfrastructureException {

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
	 * Cree une nouvelle instance de TiersView correspondant a une organisation
	 *
	 * @return un objet TiersView
	 */
	@Override
	public TiersEditView creeOrganisation() {
		TiersEditView tiersView = new TiersEditView();
		AutreCommunaute autreCommunaute = new AutreCommunaute();
		tiersView.setTiers(autreCommunaute);
		return tiersView;
	}

	/**
	 * Cree une nouvelle instance de TiersView correspondant a un debiteur
	 *
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 */
	@Override
	public TiersEditView creeDebiteur(Long numeroCtbAssocie) throws AdressesResolutionException {
		final TiersEditView tiersView = new TiersEditView();
		final DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
		final Contribuable ctbAssocie = tiersDAO.getContribuableByNumero(numeroCtbAssocie);
		if (ctbAssocie instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) ctbAssocie;
			final String nomPrenom = tiersService.getNomPrenom(pp);
			debiteur.setNom1(nomPrenom);
			debiteur.setNom2(pp.getComplementNom());
		}
		else if (ctbAssocie instanceof MenageCommun) {
			final MenageCommun menage = (MenageCommun) ctbAssocie;
			final EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(menage, null);

			final PersonnePhysique principal = ensembleTiersCouple.getPrincipal();
			if (principal != null) {
				final String nomPrenom = tiersService.getNomPrenom(principal);
				debiteur.setNom1(nomPrenom);
			}

			final PersonnePhysique conjoint = ensembleTiersCouple.getConjoint();
			if (conjoint != null) {
				final String nomPrenom2 = tiersService.getNomPrenom(conjoint);
				debiteur.setNom2(nomPrenom2);
			}
		}
		else if (ctbAssocie instanceof AutreCommunaute) {
			final AutreCommunaute autreCommunaute =(AutreCommunaute) ctbAssocie;
			debiteur.setNom1(autreCommunaute.getNom());
		}
		else if (ctbAssocie instanceof Entreprise) {
			final Entreprise entreprise = (Entreprise) ctbAssocie;
			final EntrepriseView entrepriseView = getEntrepriseService().getEntreprise(entreprise);
			debiteur.setNom1(CollectionsUtils.getLastElement(entrepriseView.getRaisonsSociales()).getRaisonSociale());
		}
	
		debiteur.setModeCommunication(ModeCommunication.PAPIER);	
		debiteur.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		setPeriodiciteCourante(tiersView, debiteur);
		tiersView.setTiers(debiteur);
		tiersView.setNumeroCtbAssocie(numeroCtbAssocie);
		return tiersView;
	}

	/**
	 * Enrichir un objet Tiers en fonction d'un objet TiersEditView
	 */
	private Tiers enrichiTiers(TiersEditView tiersView) {

		final Tiers tiers = tiersView.getTiers();

		enrichiComplement(tiers, tiersView.getComplement());

		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isHabitantVD()) {
				return pp;
			}
			else {
				// MAJ num AVS
				pp.setNumeroAssureSocial(FormatNumeroHelper.removeSpaceAndDash(pp.getNumeroAssureSocial()));

				// Set Identification Personne View
				final IdentificationPersonneView idPersonneView = tiersView.getIdentificationPersonne();
				if (idPersonneView != null) {
					tiersService.setIdentifiantsPersonne(pp, idPersonneView.getAncienNumAVS(), idPersonneView.getNumRegistreEtranger());
				}

				return pp;
			}
		}
		else if (tiers instanceof DebiteurPrestationImposable) {

			DebiteurPrestationImposable dpiFromView = (DebiteurPrestationImposable) tiers;

			//Test de la saisie d'une periodicite dans la vue
			final PeriodiciteView periodicite = tiersView.getPeriodicite();
			if (periodicite != null) {
				// L'appel de addperiodicite permet de sauver le tiers et la périodicité
				final RegDate debutValidite = tiersService.getDateDebutNouvellePeriodicite(dpiFromView, periodicite.getPeriodiciteDecompte());
				final Periodicite periodiciteAjoutee = changePeriodicite(dpiFromView, periodicite.getPeriodiciteDecompte(), periodicite.getPeriodeDecompte(), debutValidite);

				// permet de recuperer l'id dans le cas d'un débiteur nouvellement créé
				Assert.notNull(periodiciteAjoutee.getId());
				dpiFromView = periodiciteAjoutee.getDebiteur();
			}

			if (tiersView.getNumeroCtbAssocie() != null) { //ajout d'un débiteur IS au contribuable

				final Contribuable ctbAss = (Contribuable) getTiersDAO().get(tiersView.getNumeroCtbAssocie());

				//ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, ctbAss, dpi);
				final RapportEntreTiers rapport = tiersService.addContactImpotSource(dpiFromView, ctbAss);

				return tiersDAO.get(rapport.getObjetId());
			}
			else {
				return tiersDAO.save(dpiFromView);
			}
		}

		return tiers;
	}

	private void enrichiComplement(Tiers tiers, ComplementView complement) {

		if (tiers instanceof Entreprise) {
			// les PMs ne peuvent pas être éditées dans Unireg pour l'instant
			return;
		}

		// nom
		tiers.setPersonneContact(StringUtils.trimToNull(complement.getPersonneContact()));
		tiers.setComplementNom(StringUtils.trimToNull(complement.getComplementNom()));

		// téléphone
		tiers.setNumeroTelecopie(StringUtils.trimToNull(complement.getNumeroTelecopie()));
		tiers.setNumeroTelephonePortable(StringUtils.trimToNull(complement.getNumeroTelephonePortable()));
		tiers.setNumeroTelephonePrive(StringUtils.trimToNull(complement.getNumeroTelephonePrive()));
		tiers.setNumeroTelephoneProfessionnel(StringUtils.trimToNull(complement.getNumeroTelephoneProfessionnel()));
		tiers.setAdresseCourrierElectronique(StringUtils.trimToNull(complement.getAdresseCourrierElectronique()));

		// compte bancaire
		final CompteBancaireView compteBancaire = complement.getCompteBancaire();
		if (compteBancaire != null) {
			tiers.setTitulaireCompteBancaire(StringUtils.trimToNull(compteBancaire.getTitulaireCompteBancaire()));

			final String iban = IbanHelper.normalize(compteBancaire.getIban());
			final String bicSwift = StringUtils.trimToNull(FormatNumeroHelper.removeSpaceAndDash(compteBancaire.getAdresseBicSwift()));
			if (iban != null || bicSwift != null) {
				tiers.setCoordonneesFinancieres(new CoordonneesFinancieres(iban, bicSwift));
			}
			else {
				tiers.setCoordonneesFinancieres(null);
			}
		}
	}

	@Override
	public Tiers save(TiersEditView tiersEditView) {
		final Tiers tiersEnrichi = enrichiTiers(tiersEditView);
		return getTiersDAO().save(tiersEnrichi);
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
