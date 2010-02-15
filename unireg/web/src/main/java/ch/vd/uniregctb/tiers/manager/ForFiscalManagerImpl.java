package ch.vd.uniregctb.tiers.manager;

import java.util.HashMap;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalDAO;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.tiers.view.TiersVisuView;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Service à disposition du controller pour gérer un for fiscal
 *
 * @author xcifde
 *
 */
public class ForFiscalManagerImpl extends TiersManager implements ForFiscalManager {

	private ForFiscalDAO forFiscalDAO;

	private EvenementFiscalService evenementFiscalService;

	private ServiceInfrastructureService serviceInfra;

	public EvenementFiscalService getEvenementFiscalService() {
		return evenementFiscalService;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public ForFiscalDAO getForFiscalDAO() {
		return forFiscalDAO;
	}

	public void setForFiscalDAO(ForFiscalDAO forFiscalDAO) {
		this.forFiscalDAO = forFiscalDAO;
	}

	public void setServiceInfra(ServiceInfrastructureService service) {
		serviceInfra = service;
	}

	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	public TiersEditView getView(Long numero) throws AdressesResolutionException, InfrastructureException{
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
			tiersEditView.setTiers(tiers);
			if(tiers instanceof Contribuable) {
				Contribuable contribuable = (Contribuable) tiers;
				setSituationsFamille(tiersEditView, contribuable);
				setForsFiscaux(tiersEditView, contribuable);
			}
			if (tiers instanceof DebiteurPrestationImposable) {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
				setForsFiscauxDebiteur(tiersEditView, dpi);
			}
		}
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
	 * initialise les droits d'édition des onglets du tiers
	 * @return la map de droit d'édition des onglets
	 */
	private Map<String, Boolean> initAllowedOnglet(){
		Map<String, Boolean> allowedOnglet = new HashMap<String, Boolean>();
		allowedOnglet.put(TiersVisuView.MODIF_FISCAL, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.FISCAL_FOR_PRINC, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.FISCAL_FOR_SEC, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.FISCAL_FOR_AUTRE, Boolean.FALSE);

		return allowedOnglet;
	}


	/**
	 * Recupere la vue ForFiscalView
	 *
	 * @param id
	 * @return
	 */
	public ForFiscalView get(Long id) throws Exception {
		final ForFiscal forFiscal = forFiscalDAO.get(id);

		if (forFiscal == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.for.fiscal.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		Tiers tiers = forFiscal.getTiers();

		ForFiscalView forFiscalView = new ForFiscalView();
		forFiscalView.setChangementModeImposition("false");
		forFiscalView.setId(forFiscal.getId());
		forFiscalView.setNumeroCtb(tiers.getNumero());
		forFiscalView.setGenreImpot(forFiscal.getGenreImpot());
		if(tiers.getNatureTiers().equals(Tiers.NATURE_MENAGECOMMUN)){
			MenageCommun menage = (MenageCommun)tiers;
			boolean isHabitant = false;
			for (PersonnePhysique pp : menage.getPersonnesPhysiques()) {
				if (pp.isHabitant()) {
					isHabitant = true;
					break;
				}
			}
			if(isHabitant)
				forFiscalView.setNatureTiers(Tiers.NATURE_HABITANT);
			else forFiscalView.setNatureTiers(Tiers.NATURE_NONHABITANT);
		}
		else {
			forFiscalView.setNatureTiers(tiers.getNatureTiers());
		}
		forFiscalView.setAnnule(forFiscal.isAnnule());
		if (forFiscal instanceof ForFiscalAutreImpot) {
			forFiscalView.setDateEvenement(forFiscal.getDateDebut());
		} else {
			forFiscalView.setDateOuverture(forFiscal.getDateDebut());
			forFiscalView.setDateFermeture(forFiscal.getDateFin());
		}
		TypeAutoriteFiscale typeForFiscal = forFiscal.getTypeAutoriteFiscale();
		forFiscalView.setTypeAutoriteFiscale(typeForFiscal);
		switch (typeForFiscal) {
		case COMMUNE_OU_FRACTION_VD:
			forFiscalView.setNumeroForFiscalCommune(forFiscal.getNumeroOfsAutoriteFiscale());
			if (forFiscal.getNumeroOfsAutoriteFiscale() != null) {
				forFiscalView.setLibFractionCommune(serviceInfra.getCommuneByNumeroOfsEtendu(forFiscal.getNumeroOfsAutoriteFiscale()).getNomMinuscule());
			}
			break;
		case COMMUNE_HC:
			forFiscalView.setNumeroForFiscalCommuneHorsCanton(forFiscal.getNumeroOfsAutoriteFiscale());
			if (forFiscal.getNumeroOfsAutoriteFiscale() != null) {
				forFiscalView.setLibCommuneHorsCanton(serviceInfra.getCommuneByNumeroOfsEtendu(forFiscal.getNumeroOfsAutoriteFiscale()).getNomMinuscule());
			}
			break;
		case PAYS_HS:
			forFiscalView.setNumeroForFiscalPays(forFiscal.getNumeroOfsAutoriteFiscale());
			if (forFiscal.getNumeroOfsAutoriteFiscale() != null) {
				forFiscalView.setLibPays(serviceInfra.getPays(forFiscal.getNumeroOfsAutoriteFiscale()).getNomMinuscule());
			}
			break;
		default:
			break;
		}
		if (forFiscal instanceof ForFiscalRevenuFortune) {
			ForFiscalRevenuFortune forFiscalRevenuFortune = (ForFiscalRevenuFortune) forFiscal;
			forFiscalView.setMotifOuverture(forFiscalRevenuFortune.getMotifOuverture());
			forFiscalView.setMotifFermeture(forFiscalRevenuFortune.getMotifFermeture());
			forFiscalView.setMotifRattachement(forFiscalRevenuFortune.getMotifRattachement());
		}
		if (forFiscal instanceof ForFiscalPrincipal) {
			ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) forFiscal;
			forFiscalView.setModeImposition(forFiscalPrincipal.getModeImposition());
		}

		forFiscalView.setNatureForFiscal(forFiscal.getClass().getSimpleName());

		return forFiscalView;
	}

	/**
	 * Cree une nouvelle vue ForFiscalView
	 *
	 * @param id
	 * @return
	 */
	public ForFiscalView create(Long numeroCtb, boolean dpi) {
		ForFiscalView forFiscalView = new ForFiscalView();
		forFiscalView.setChangementModeImposition("false");
		forFiscalView.setNumeroCtb(numeroCtb);
		Tiers tiers = tiersDAO.get(numeroCtb);
		if(Tiers.NATURE_MENAGECOMMUN.equals(tiers.getNatureTiers())){
			MenageCommun menage = (MenageCommun)tiers;
			boolean isHabitant = false;
			for (PersonnePhysique pp : menage.getPersonnesPhysiques()) {
				if (pp.isHabitant()) {
					isHabitant = true;
					break;
				}
			}
			if(isHabitant)
				forFiscalView.setNatureTiers(Tiers.NATURE_HABITANT);
			else forFiscalView.setNatureTiers(Tiers.NATURE_NONHABITANT);
		}
		else {
			forFiscalView.setNatureTiers(tiers.getNatureTiers());
		}
		if (dpi) {
			forFiscalView.setNatureForFiscal("ForDebiteurPrestationImposable");
			forFiscalView.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscalView.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		} else {
			forFiscalView.setNatureForFiscal("ForFiscalPrincipal");
			forFiscalView.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscalView.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscalView.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscalView.setModeImposition(ModeImposition.ORDINAIRE);
		}
		forFiscalView.setDateOuverture(RegDate.get());
		return forFiscalView;
	}

	/**
	 * Enrichi le for en fonction de ForFiscalView (partie ForFiscal)
	 *
	 * @param forFiscal
	 * @param forFiscalView
	 */
	private void enrichiFor(final ForFiscal forFiscal, ForFiscalView forFiscalView) {

		forFiscal.setAnnule(forFiscalView.isAnnule());
		forFiscal.setGenreImpot(forFiscalView.getGenreImpot());

		TypeAutoriteFiscale typeForFiscal = forFiscalView.getTypeAutoriteFiscale();
		forFiscal.setTypeAutoriteFiscale(typeForFiscal);
		switch (typeForFiscal) {
		case COMMUNE_OU_FRACTION_VD:
			forFiscal.setNumeroOfsAutoriteFiscale(forFiscalView.getNumeroForFiscalCommune());
			break;
		case COMMUNE_HC:
			forFiscal.setNumeroOfsAutoriteFiscale(forFiscalView.getNumeroForFiscalCommuneHorsCanton());
			break;
		case PAYS_HS:
			forFiscal.setNumeroOfsAutoriteFiscale(forFiscalView.getNumeroForFiscalPays());
			break;
		default:
			break;
		}
	}

	/**
	 * Enrichi le for en fonction de ForFiscalView (partie ForFiscalAutreImpot)
	 *
	 * @param forFiscal
	 * @param forFiscalView
	 */
	private void enrichiForAutreImpot(ForFiscal forFiscal, ForFiscalView forFiscalView) {
		forFiscal.setDateDebut(forFiscalView.getRegDateEvenement());
		forFiscal.setDateFin(forFiscalView.getRegDateEvenement());
	}

	/**
	 * Enrichi le for en fonction de ForFiscalView (partie ForRevenuFortune)
	 *
	 * @param forFiscal
	 * @param forFiscalView
	 */
	private void enrichiForRevenuFortune(ForFiscalRevenuFortune forRevenuFortune, ForFiscalView forFiscalView) {

		if (forFiscalView.getDateChangement() == null) {
			forRevenuFortune.setDateDebut(forFiscalView.getRegDateOuverture());
			forRevenuFortune.setDateFin(forFiscalView.getRegDateFermeture());
		} else {
			forRevenuFortune.setDateDebut(forFiscalView.getRegDateChangement());
			forRevenuFortune.setDateFin(null);
		}
		forRevenuFortune.setMotifRattachement(forFiscalView.getMotifRattachement());
		forRevenuFortune.setMotifOuverture(forFiscalView.getMotifOuverture());

		forRevenuFortune.setMotifFermeture(forFiscalView.getMotifFermeture());


		if (forRevenuFortune instanceof ForFiscalPrincipal) {
			enrichiForPrincipal((ForFiscalPrincipal) forRevenuFortune, forFiscalView);
		}
	}

	/**
	 * Enrichi le for en fonction de ForFiscalView (partie ForDebiteurPrestationImposable)
	 *
	 * @param forDebiteurPrestationImposable
	 * @param forFiscalView
	 */
	private void enrichiForDebiteurPrestationImposable(ForDebiteurPrestationImposable forDebiteurPrestationImposable, ForFiscalView forFiscalView) {
		forDebiteurPrestationImposable.setDateDebut(forFiscalView.getRegDateOuverture());
		forDebiteurPrestationImposable.setDateFin(forFiscalView.getRegDateFermeture());
	}

	/**
	 * Enrichi le for en fonction de ForFiscalView (partie ForPrincipal)
	 *
	 * @param forFiscal
	 * @param forFiscalView
	 */
	private void enrichiForPrincipal(ForFiscalPrincipal forFiscalPrincipal, ForFiscalView forFiscalView) {
		forFiscalPrincipal.setModeImposition(forFiscalView.getModeImposition());
	}

	/**
	 * Sauvegarde du for
	 *
	 * @param forFiscalView
	 * @return
	 */
	@Transactional(rollbackFor = Throwable.class)
	public ForFiscal save(ForFiscalView forFiscalView) {

		if (forFiscalView.getId() == null) {

			if (forFiscalView.getGenreImpot().equals(GenreImpot.REVENU_FORTUNE)){
				if(forFiscalView.getMotifRattachement().equals(MotifRattachement.DOMICILE) ||
						forFiscalView.getMotifRattachement().equals(MotifRattachement.DIPLOMATE_SUISSE) ||
						forFiscalView.getMotifRattachement().equals(MotifRattachement.DIPLOMATE_ETRANGER)) {
					ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal();
					Contribuable contribuable = (Contribuable) tiersDAO.get(forFiscalView.getNumeroCtb());
					enrichiFor(forFiscalPrincipal, forFiscalView);
					enrichiForRevenuFortune(forFiscalPrincipal, forFiscalView);
					enrichiForPrincipal(forFiscalPrincipal, forFiscalView);
					ForFiscalPrincipal dernierForPrincipal = contribuable.getDernierForFiscalPrincipal();
					if ((dernierForPrincipal != null) && (dernierForPrincipal.getDateFin() == null)) {

						if (forFiscalView.getRegDateFermeture()==null || forFiscalView.getRegDateFermeture().isAfter(dernierForPrincipal.getDateDebut()) ) {
							tiersService.closeForFiscalPrincipal(contribuable, forFiscalPrincipal.getDateDebut().getOneDayBefore(), forFiscalView.getMotifOuverture());
						}

					}
					ForFiscalPrincipal forRtr = null;
					if (forFiscalView.getDateFermeture() == null) {

						forRtr = tiersService.openForFiscalPrincipal(contribuable, forFiscalPrincipal.getDateDebut(), forFiscalPrincipal.getMotifRattachement(),
									forFiscalPrincipal.getNumeroOfsAutoriteFiscale(), forFiscalPrincipal.getTypeAutoriteFiscale(),
									forFiscalPrincipal.getModeImposition(), forFiscalPrincipal.getMotifOuverture(), true);
					}
					else {
						forRtr = tiersService.openAndCloseForFiscalPrincipal(contribuable, forFiscalPrincipal.getDateDebut(), forFiscalPrincipal.getMotifRattachement(),
								forFiscalPrincipal.getNumeroOfsAutoriteFiscale(), forFiscalPrincipal.getTypeAutoriteFiscale(),
								forFiscalPrincipal.getModeImposition(), forFiscalPrincipal.getMotifOuverture(),forFiscalView.getRegDateFermeture(), 
								forFiscalView.getMotifFermeture(), true);
					}


					return forRtr;
				}
				else if (forFiscalView.getMotifRattachement().equals(MotifRattachement.ACTIVITE_INDEPENDANTE) ||
						forFiscalView.getMotifRattachement().equals(MotifRattachement.IMMEUBLE_PRIVE) ||
						forFiscalView.getMotifRattachement().equals(MotifRattachement.SEJOUR_SAISONNIER) ||
						forFiscalView.getMotifRattachement().equals(MotifRattachement.DIRIGEANT_SOCIETE)) {
					ForFiscalSecondaire forFiscalSecondaire = new ForFiscalSecondaire();
					Contribuable contribuable = (Contribuable) tiersDAO.get(forFiscalView.getNumeroCtb());
					enrichiFor(forFiscalSecondaire, forFiscalView);
					enrichiForRevenuFortune(forFiscalSecondaire, forFiscalView);
					ForFiscalSecondaire forRtr = tiersService.openForFiscalSecondaire(contribuable, forFiscalSecondaire.getGenreImpot(),
						forFiscalSecondaire.getDateDebut(),forFiscalSecondaire.getDateFin(), forFiscalSecondaire.getMotifRattachement(),
						forFiscalSecondaire.getNumeroOfsAutoriteFiscale(), forFiscalSecondaire.getTypeAutoriteFiscale(),
						forFiscalSecondaire.getMotifOuverture(), forFiscalSecondaire.getMotifFermeture());
					if (forFiscalView.getDateFermeture() != null) {
						forRtr = tiersService.closeForFiscalSecondaire(contribuable, forRtr, forFiscalView.getRegDateFermeture(), forFiscalView.getMotifFermeture());
					}
					return forRtr;
				}
				else {
					ForFiscalAutreElementImposable forFiscalAutreElement = new ForFiscalAutreElementImposable();
					Contribuable contribuable = (Contribuable) tiersDAO.get(forFiscalView.getNumeroCtb());
					enrichiFor(forFiscalAutreElement, forFiscalView);
					enrichiForRevenuFortune(forFiscalAutreElement, forFiscalView);
					ForFiscalAutreElementImposable forRtr = tiersService.openForFiscalAutreElementImposable(contribuable, forFiscalAutreElement.getGenreImpot(),
							forFiscalAutreElement.getDateDebut(), forFiscalAutreElement.getMotifRattachement(), forFiscalAutreElement.getNumeroOfsAutoriteFiscale(),
							forFiscalAutreElement.getTypeAutoriteFiscale(), forFiscalAutreElement.getMotifOuverture());
					if (forFiscalView.getDateFermeture() != null) {
						forRtr = tiersService.closeForFiscalAutreElementImposable(contribuable, forRtr, forFiscalView.getRegDateFermeture(), forFiscalView.getMotifFermeture());
					}
					return forRtr;
				}
			}
			else if (forFiscalView.getGenreImpot().equals(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE)) {
				ForDebiteurPrestationImposable forDebiteur = new ForDebiteurPrestationImposable();
				DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersDAO.get(forFiscalView.getNumeroCtb());
				enrichiFor(forDebiteur, forFiscalView);
				enrichiForDebiteurPrestationImposable(forDebiteur, forFiscalView);
				ForDebiteurPrestationImposable dernierForDebiteur = debiteur.getDernierForDebiteur();
				if ((dernierForDebiteur != null) && (dernierForDebiteur.getDateFin() == null)) {
					tiersService.closeForDebiteurPrestationImposable(debiteur, dernierForDebiteur, forDebiteur.getDateDebut().getOneDayBefore());
				}
				ForDebiteurPrestationImposable forRtr = tiersService.openForDebiteurPrestationImposable(debiteur, forDebiteur.getDateDebut(),
						forDebiteur.getNumeroOfsAutoriteFiscale(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				if (forFiscalView.getDateFermeture() != null) {
					forRtr = tiersService.closeForDebiteurPrestationImposable(debiteur, forRtr, forDebiteur.getDateFin());
				}
				return forRtr;
			}
			else {
				ForFiscalAutreImpot forFiscalAutreImpot = new ForFiscalAutreImpot();
				Contribuable contribuable = (Contribuable) tiersDAO.get(forFiscalView.getNumeroCtb());
				enrichiFor(forFiscalAutreImpot, forFiscalView);
				enrichiForAutreImpot(forFiscalAutreImpot, forFiscalView);
				ForFiscalAutreImpot forRtr = tiersService.openForFiscalAutreImpot(contribuable, forFiscalAutreImpot.getGenreImpot(), forFiscalAutreImpot.getDateDebut(),
						forFiscalAutreImpot.getNumeroOfsAutoriteFiscale(), forFiscalAutreImpot.getTypeAutoriteFiscale());
				return forRtr;
			}
		}
		else {

			if (forFiscalView.getRegDateChangement() != null) {
				ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal();
				Contribuable contribuable = (Contribuable) tiersDAO.get(forFiscalView.getNumeroCtb());
				enrichiFor(forFiscalPrincipal, forFiscalView);
				enrichiForRevenuFortune(forFiscalPrincipal, forFiscalView);
				enrichiForPrincipal(forFiscalPrincipal, forFiscalView);
				ForFiscalPrincipal forRtr = tiersService.changeModeImposition(contribuable, forFiscalPrincipal.getDateDebut(),
						forFiscalPrincipal.getModeImposition(), forFiscalView.getMotifImposition());

				return forRtr;
			}
			else {
				if (forFiscalView.getRegDateFermeture() != null) {
					ForFiscal forFiscal = forFiscalDAO.get(forFiscalView.getId());
					if (forFiscalView.getGenreImpot().equals(GenreImpot.REVENU_FORTUNE)){
						if(forFiscalView.getMotifRattachement().equals(MotifRattachement.DOMICILE) ||
								forFiscalView.getMotifRattachement().equals(MotifRattachement.DIPLOMATE_SUISSE) ||
								forFiscalView.getMotifRattachement().equals(MotifRattachement.DIPLOMATE_ETRANGER)) {
							ForFiscal forRtr = tiersService.closeForFiscalPrincipal((ForFiscalPrincipal) forFiscal,
									forFiscalView.getRegDateFermeture(), forFiscalView.getMotifFermeture());
							return forRtr;
						}
						else if (forFiscalView.getMotifRattachement().equals(MotifRattachement.ACTIVITE_INDEPENDANTE) ||
								forFiscalView.getMotifRattachement().equals(MotifRattachement.IMMEUBLE_PRIVE) ||
								forFiscalView.getMotifRattachement().equals(MotifRattachement.SEJOUR_SAISONNIER) ||
								forFiscalView.getMotifRattachement().equals(MotifRattachement.DIRIGEANT_SOCIETE)) {
							ForFiscal forRtr = tiersService.closeForFiscalSecondaire((Contribuable)forFiscal.getTiers(),
									(ForFiscalSecondaire) forFiscal,
									forFiscalView.getRegDateFermeture(), forFiscalView.getMotifFermeture());
							return forRtr;
						}
						else {
							ForFiscal forRtr = tiersService.closeForFiscalAutreElementImposable((Contribuable)forFiscal.getTiers(),
									(ForFiscalAutreElementImposable) forFiscal,
									forFiscalView.getRegDateFermeture(), forFiscalView.getMotifFermeture());
							return forRtr;
						}
					}
					else if (forFiscalView.getGenreImpot().equals(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE)) {
						ForFiscal forRtr = tiersService.closeForDebiteurPrestationImposable((DebiteurPrestationImposable) forFiscal.getTiers() , (ForDebiteurPrestationImposable) forFiscal, forFiscalView.getRegDateFermeture());
						return forRtr;
					}
					//else les fors autreimpot ne sont éditables
				}
			}
		}

		return null;
	}



	/**
	 * Annulation du for
	 *
	 * @param idFor
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerFor(Long idFor) {
		ForFiscal forFiscal = forFiscalDAO.get(idFor);
		if (forFiscal == null) {
			throw new ObjectNotFoundException("Le for fiscal n°" + idFor + " n'existe pas.");
		}
		tiersService.annuleForFiscal(forFiscal, true);
	}
}
