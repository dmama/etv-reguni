package ch.vd.uniregctb.tiers.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseException;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.entreprise.EntrepriseView;
import ch.vd.uniregctb.interfaces.InterfaceDataException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.IdentificationPersonneView;
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

		if (tiers != null){
			setTiersGeneralView(tiersEditView, tiers);
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
		}
		else {
			tiersEditView.setAllowed(true);
		}

		return tiersEditView;
	}


	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public TiersEditView getDebiteursView(Long numero) throws AdresseException, InfrastructureException {
		TiersEditView tiersEditView = new TiersEditView();
		if ( numero == null) {
			return null;
		}
		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new RuntimeException( this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		if (tiers != null){
			tiersEditView.setTiers(tiers);
			setTiersGeneralView(tiersEditView, tiers);
			if(tiers instanceof Contribuable) {
				Contribuable contribuable = (Contribuable) tiers;
				tiersEditView.setDebiteurs(getDebiteurs(contribuable));
			}
			Map<String, Boolean> allowedOnglet = initAllowedOnglet();
			boolean allowed = setDroitEdition(tiers, allowedOnglet);

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
		debiteur.setPeriodiciteDecompte(PeriodiciteDecompte.TRIMESTRIEL);
		debiteur.setPeriodeDecompte(PeriodeDecompte.M12);
		debiteur.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
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

		tiers.setNumeroTelecopie(tiers.getNumeroTelecopie());
		tiers.setNumeroTelephonePortable(tiers.getNumeroTelephonePortable());
		tiers.setNumeroTelephonePrive(tiers.getNumeroTelephonePrive());
		tiers.setNumeroTelephoneProfessionnel(tiers.getNumeroTelephoneProfessionnel());
		String ibanSaisi = FormatNumeroHelper.removeSpaceAndDash(tiers.getNumeroCompteBancaire());
		if(ibanSaisi!=null){
		tiers.setNumeroCompteBancaire(ibanSaisi.toUpperCase());	
		}
		tiers.setAdresseBicSwift(FormatNumeroHelper.removeSpaceAndDash(tiers.getAdresseBicSwift()));

		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isHabitantVD()) {
				return pp;
			} else {
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

			final DebiteurPrestationImposable dpi = tiersDAO.getDebiteurPrestationImposableByNumero(tiers.getNumero());
			//On recopie les données du tiers de la session dans le tiers que l'on vient de recuperer
			//c'est pas jolie, ca devrait venir du TiersEditView mais c'est la solution retenue historiquement
			dpi.setCategorieImpotSource(((DebiteurPrestationImposable)tiers).getCategorieImpotSource());
			dpi.setModeCommunication(((DebiteurPrestationImposable)tiers).getModeCommunication());
			dpi.setSansListeRecapitulative(((DebiteurPrestationImposable)tiers).getSansListeRecapitulative());
			dpi.setSansRappel(((DebiteurPrestationImposable)tiers).getSansRappel());

			//Calcul de la date de début de validité de la nouvelle périodicité

			RegDate debutValidite = tiersService.getDateDebutNouvellePeriodicite(dpi);
			PeriodeDecompte periodeDecompte = null;
			final PeriodiciteDecompte periodiciteDecompte = tiersView.getPeriodicite().getPeriodiciteDecompte();
			if(PeriodiciteDecompte.UNIQUE.equals(periodiciteDecompte)){
				periodeDecompte = tiersView.getPeriodicite().getPeriodeDecompte();
			}

			tiersService.addPeriodicite(dpi, periodiciteDecompte,periodeDecompte,debutValidite,null);
			Assert.notNull(dpi.getDernierePeriodicite().getId());
			

			if(tiersView.getNumeroCtbAssocie() != null) { //ajout d'un débiteur IS au contribuable

				final Contribuable ctbAss = (Contribuable) getTiersDAO().get(tiersView.getNumeroCtbAssocie());

				//ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, ctbAss, dpi);
				//getTiersDAO().getHibernateTemplate().merge(contact);
				final RapportEntreTiers rapport = tiersService.addContactImpotSource(dpi, ctbAss);

				final DebiteurPrestationImposable dpiRtr = (DebiteurPrestationImposable) tiersDAO.get(rapport.getObjetId());

				return dpiRtr;
			}
			else { //mise à jour du debiteur IS
				return getTiersDAO().save(dpi);
			}
		}

		return tiers;
	}

	/**
	 * Sauvegarde du tiers en base et mise a jour de l'indexeur
	 *
	 * @param tiersEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public Tiers save(TiersEditView tiersEditView) {
		Tiers tiersEnrichi = enrichiTiers(tiersEditView);
		Tiers tiersSaved;
		if (tiersEnrichi instanceof DebiteurPrestationImposable) {
			/*
			 * Un débiteur *appartient* à un contribuable. Pour que Hibernate mette en place les liens entre objets correctement, il est
			 * nécessaire de sauver le contribuable et non le débiteur.
			 *
			 * si ajout d'un débiteur à un contribuable, il n'y a rien à faire ici, parce que le contribuable existe déjà
			 * dans la session Hibernate et sera automatiquement sauvé lors de la fermeture de la transaction.
			 * si modification d'un débiteur, la sauvegarde est faite dans enrichiTiers
			 */
			tiersSaved = tiersEnrichi;
		}
		else {
			/*
			 * Dans tous les autres cas, on sauve le (potentiellement) nouveau tiers
			 */
			tiersSaved = getTiersDAO().save((tiersEnrichi));
		}
		return tiersSaved;
	}
}
