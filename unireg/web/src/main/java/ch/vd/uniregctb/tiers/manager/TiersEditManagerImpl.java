package ch.vd.uniregctb.tiers.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.entreprise.EntrepriseView;
import ch.vd.uniregctb.interfaces.InterfaceDataException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.ComplementView;
import ch.vd.uniregctb.tiers.view.DebiteurEditView;
import ch.vd.uniregctb.tiers.view.IdentificationPersonneView;
import ch.vd.uniregctb.tiers.view.PeriodiciteView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.tiers.view.TiersVisuView;
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

	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public TiersEditView getComplementView(Long numero) throws AdresseException, InfrastructureException {

		TiersEditView tiersEditView = new TiersEditView();
		if ( numero == null) {
			return null;
		}
		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new RuntimeException( this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
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
		//gestion des droits d'édition
		Map<String, Boolean> allowedOnglet = initAllowedOnglet();
		boolean allowed = setDroitEdition(tiers, allowedOnglet);

		tiersEditView.setAllowedOnglet(allowedOnglet);
		tiersEditView.setAllowed(allowed);

		if(!allowed){
			tiersEditView.setTiers(null);
		}

		return tiersEditView;
	}


	/**
	 * Charge les informations dans un objet qui servira de view
	 *
	 * @param numero numéro de tiers du débiteur recherché
	 * @return un objet DebiteurEditView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public DebiteurEditView getDebiteurEditView(Long numero) throws AdresseException, InfrastructureException {
		if (numero == null) {
			return null;
		}

		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new RuntimeException( this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		Assert.isInstanceOf(DebiteurPrestationImposable.class, tiers);
		return new DebiteurEditView((DebiteurPrestationImposable) tiers, ibanValidator);
	}



	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	public TiersEditView getView(Long numero) throws AdresseException, InfrastructureException{
		TiersEditView tiersEditView = new TiersEditView();
		refresh(tiersEditView, numero);
		return tiersEditView;
	}


	/**
	 * Rafraichissement de la vue
	 *
	 * @param numero
	 * @return
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	public TiersEditView refresh(TiersEditView tiersEditView, Long numero) throws AdresseException, InfrastructureException {
		if ( numero == null) {
			return null;
		}
		Tiers  oldTiers = tiersEditView.getTiers();
		tiersEditView.clear();
		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
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
			setContribuablesAssocies(tiersEditView, dpi);
			setPeriodiciteCourante(tiersEditView,dpi);
			if (dpi.getContribuableId() == null) {
				tiersEditView.setAddContactISAllowed(true);
			}
			else {
				tiersEditView.setAddContactISAllowed(false);
			}
		}

		if (tiersEditView.getTiers() != null){
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

			//gestion des droits d'édition
			boolean allowed = false;
			Map<String, Boolean> allowedOnglet = initAllowedOnglet();
			allowed = setDroitEdition(tiers, allowedOnglet);

			tiersEditView.setAllowedOnglet(allowedOnglet);
			tiersEditView.setAllowed(allowed);

			if(!allowed){
				tiersEditView.setTiers(null);
			}
		}
		else {
			tiersEditView.setAllowed(true);
		}

		return tiersEditView;
	}

	/**
	 * Met a jour le non-habitant pour l'edition
	 *
	 * @param tiersEditView
	 * @param nonHabitant
	 * @throws InfrastructureException
	 */
	private void setNonHabitant(TiersEditView tiersEditView, PersonnePhysique nonHabitant) throws InfrastructureException {

		IdentificationPersonneView idPersonneView = new IdentificationPersonneView();

		final Set<IdentificationPersonne> ips = nonHabitant.getIdentificationsPersonnes();
		for (IdentificationPersonne ip : ips) {
			switch (ip.getCategorieIdentifiant()) {
			case CH_AHV_AVS:
				idPersonneView.setAncienNumAVS((ip.getIdentifiant()));
				break;
			case CH_ZAR_RCE:
				idPersonneView.setNumRegistreEtranger((ip.getIdentifiant()));
				break;
			default:
				Assert.fail("Catégorie d'identifiant inconnu :" + ip.getCategorieIdentifiant());
			}
		}

		final Integer numeroOfsNationalite = nonHabitant.getNumeroOfsNationalite();
		if (numeroOfsNationalite != null) {
			tiersEditView.setLibelleOfsPaysOrigine(getServiceInfrastructureService().getPays(numeroOfsNationalite).getNomMinuscule());
		}
		final Integer numeroOfsCommuneOrigine = nonHabitant.getNumeroOfsCommuneOrigine();
		if (numeroOfsCommuneOrigine != null) {
			tiersEditView.setLibelleOfsCommuneOrigine(getServiceInfrastructureService().getCommuneByNumeroOfsEtendu(numeroOfsCommuneOrigine, null).getNomMinuscule());
		}

		tiersEditView.setIdentificationPersonne(idPersonneView);
		tiersEditView.setTiers(nonHabitant);
	}

	/**
	 * Cree une nouvelle instance de TiersView correspondant a une personne
	 *
	 * @return un objet TiersView
	 */
	public TiersEditView creePersonne() {
		TiersEditView tiersView = new TiersEditView();
		IdentificationPersonneView idpersonneView = new IdentificationPersonneView();
		tiersView.setIdentificationPersonne(idpersonneView);
		PersonnePhysique nonHab = new PersonnePhysique(false);
		tiersView.setTiers(nonHab);
		Map<String, Boolean> allowedOnglet = initAllowedOnglet();
		allowedOnglet.put(TiersVisuView.MODIF_CIVIL, Boolean.TRUE);
		allowedOnglet.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.TRUE);
		allowedOnglet.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.TRUE);
		if (SecurityProvider.isGranted(Role.COOR_FIN)) {
			allowedOnglet.put(TiersEditView.COMPLEMENT_COOR_FIN, Boolean.TRUE);
		}
		tiersView.setAllowedOnglet(allowedOnglet);
		return tiersView;
	}

	/**
	 * Cree une nouvelle instance de TiersView correspondant a une organisation
	 *
	 * @return un objet TiersView
	 */
	public TiersEditView creeOrganisation() {
		TiersEditView tiersView = new TiersEditView();
		AutreCommunaute autreCommunaute = new AutreCommunaute();
		tiersView.setTiers(autreCommunaute);
		Map<String, Boolean> allowedOnglet = initAllowedOnglet();
		allowedOnglet.put(TiersVisuView.MODIF_CIVIL, Boolean.TRUE);
		allowedOnglet.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.TRUE);
		allowedOnglet.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.TRUE);
		if (SecurityProvider.isGranted(Role.COOR_FIN)) {
			allowedOnglet.put(TiersEditView.COMPLEMENT_COOR_FIN, Boolean.TRUE);
		}
		tiersView.setAllowedOnglet(allowedOnglet);
		return tiersView;
	}

	/**
	 * Cree une nouvelle instance de TiersView correspondant a un debiteur
	 *
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 */
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
			final EntrepriseView entrepriseView = getHostPersonneMoraleService().get(entreprise.getNumero());
			debiteur.setNom1(entrepriseView.getRaisonSociale());
		}
	
		debiteur.setModeCommunication(ModeCommunication.PAPIER);	
		debiteur.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		setPeriodiciteCourante(tiersView, debiteur);
		tiersView.setTiers(debiteur);
		tiersView.setNumeroCtbAssocie(numeroCtbAssocie);
		final Map<String, Boolean> allowedOnglet = initAllowedOnglet();
		allowedOnglet.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.TRUE);
		allowedOnglet.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.TRUE);
		if (SecurityProvider.isGranted(Role.COOR_FIN)) {
			allowedOnglet.put(TiersEditView.COMPLEMENT_COOR_FIN, Boolean.TRUE);
		}
		allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.TRUE);
		tiersView.setAllowedOnglet(allowedOnglet);
		return tiersView;
	}

	/**
	 * initialise les droits d'édition des onglets du tiers
	 * @return la map de droit d'édition des onglets
	 */
	private Map<String, Boolean> initAllowedOnglet(){
		Map<String, Boolean> allowedOnglet = new HashMap<String, Boolean>();
		allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.FISCAL_FOR_PRINC, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.FISCAL_FOR_SEC, Boolean.FALSE);
		allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.ADR_B, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.ADR_C, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.ADR_P, Boolean.FALSE);
		allowedOnglet.put(TiersVisuView.MODIF_CIVIL, Boolean.FALSE);
		allowedOnglet.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.COMPLEMENT_COMMUNICATION, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.COMPLEMENT_COOR_FIN, Boolean.FALSE);
		allowedOnglet.put(TiersVisuView.MODIF_DEBITEUR, Boolean.FALSE);
		allowedOnglet.put(TiersVisuView.MODIF_DOSSIER, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.DOSSIER_NO_TRAVAIL, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.DOSSIER_TRAVAIL, Boolean.FALSE);
		allowedOnglet.put(TiersVisuView.MODIF_RAPPORT, Boolean.FALSE);
		allowedOnglet.put(TiersVisuView.MODIF_MOUVEMENT, Boolean.FALSE);

		return allowedOnglet;
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
				final Periodicite periodiciteAjoutee = changePeriodicite(dpiFromView, periodicite.getPeriodiciteDecompte(), periodicite.getPeriodeDecompte());

				// permet de recuperer l'id dans le cas d'un débiteur nouvellement créé
				Assert.notNull(periodiciteAjoutee.getId());
				dpiFromView = periodiciteAjoutee.getDebiteur();
			}

			if (tiersView.getNumeroCtbAssocie() != null) { //ajout d'un débiteur IS au contribuable

				final Contribuable ctbAss = (Contribuable) getTiersDAO().get(tiersView.getNumeroCtbAssocie());

				//ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, ctbAss, dpi);
				//getTiersDAO().getHibernateTemplate().merge(contact);
				final RapportEntreTiers rapport = tiersService.addContactImpotSource(dpiFromView, ctbAss);

				final DebiteurPrestationImposable dpiRtr = (DebiteurPrestationImposable) tiersDAO.get(rapport.getObjetId());

				return dpiRtr;
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
		tiers.setPersonneContact(complement.getPersonneContact());
		tiers.setComplementNom(complement.getComplementNom());

		// téléphone
		tiers.setNumeroTelecopie(complement.getNumeroTelecopie());
		tiers.setNumeroTelephonePortable(complement.getNumeroTelephonePortable());
		tiers.setNumeroTelephonePrive(complement.getNumeroTelephonePrive());
		tiers.setNumeroTelephoneProfessionnel(complement.getNumeroTelephoneProfessionnel());
		if (StringUtils.isNotBlank(complement.getAdresseCourrierElectronique())) {
			tiers.setAdresseCourrierElectronique(complement.getAdresseCourrierElectronique().trim());
		}

		// compte bancaire
		String ibanSaisi = FormatNumeroHelper.removeSpaceAndDash(complement.getNumeroCompteBancaire());
		if (ibanSaisi != null) {
			tiers.setNumeroCompteBancaire(ibanSaisi.toUpperCase());
		}
		tiers.setTitulaireCompteBancaire(complement.getTitulaireCompteBancaire());
		tiers.setAdresseBicSwift(FormatNumeroHelper.removeSpaceAndDash(complement.getAdresseBicSwift()));
	}

	public Tiers save(TiersEditView tiersEditView) {
		final Tiers tiersEnrichi = enrichiTiers(tiersEditView);
		return getTiersDAO().save(tiersEnrichi);
	}

	public void save(DebiteurEditView view) {
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(view.getId());
		dpi.setCategorieImpotSource(view.getCategorieImpotSource());
		dpi.setModeCommunication(view.getModeCommunication());
		dpi.setSansListeRecapitulative(view.getSansListeRecapitulative());
		dpi.setSansRappel(view.getSansSommation());
		changePeriodicite(dpi, view.getPeriodiciteCourante(), view.getPeriodeDecompte());
	}

	private Periodicite changePeriodicite(DebiteurPrestationImposable dpi, PeriodiciteDecompte nouvellePeriodicite, PeriodeDecompte nouvellePeriode) {
		final PeriodeDecompte periodeDecompte = (nouvellePeriodicite == PeriodiciteDecompte.UNIQUE ? nouvellePeriode : null);
		final RegDate debutValidite = tiersService.getDateDebutNouvellePeriodicite(dpi);
		return tiersService.addPeriodicite(dpi, nouvellePeriodicite, periodeDecompte, debutValidite, null);
	}
}
