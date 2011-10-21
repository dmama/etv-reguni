package ch.vd.uniregctb.tiers.validator;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesFiscales;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeAdresseTiers;

public class TiersAdresseValidator implements Validator {

	protected final Logger LOGGER = Logger.getLogger(TiersAdresseValidator.class);

	private AdresseService adresseService;
	private TiersService tiersService;
	private ServiceInfrastructureService serviceInfra;

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	/**
	 * @see org.springframework.validation.Validator#supports(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return Tiers.class.equals(clazz) || DebiteurPrestationImposable.class.equals(clazz) || TiersEditView.class.equals(clazz)
				|| AdresseView.class.equals(clazz);
	}

	/**
	 * @see org.springframework.validation.Validator#validate(java.lang.Object, org.springframework.validation.Errors)
	 */
	@Override
	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {

		if (obj == null) {
			return;
		}

		final AdresseView adresseView = (AdresseView) obj;

		if ("reprise".equals(adresseView.getMode()) || "repriseCivil".equals(adresseView.getMode())) {
			if (adresseView.isMettreAJourDecedes()) {
				// [SIFISC-156] blindage : selon la logique de l'écran, on ne devrait jamais arriver là
				errors.rejectValue("mettreAJourDecedes", "error.maj.deces.sur.adresse.reprise");
			}
		}
		else {

			if ("suisse".equals(adresseView.getTypeLocalite())) {
				// On doit avoir un NO_ORDRE
				if (StringUtils.isBlank(adresseView.getNumeroOrdrePoste())) {
					errors.rejectValue("localiteSuisse", "error.invalid.localite_suisse");
				}
				// et il doit etre valide
				else {
					try {
						Integer noOrdre = Integer.parseInt(adresseView.getNumeroOrdrePoste());
						Localite loc = serviceInfra.getLocaliteByONRP(noOrdre);
						if (loc == null) {
							errors.rejectValue("localiteSuisse", "error.invalid.localite_suisse");
						}
					}
					catch (Exception e) {
						errors.rejectValue("localiteSuisse", "error.invalid.localite_suisse");
					}
				}
			}

			if ("pays".equals(adresseView.getTypeLocalite())) {

				if (adresseView.getPaysOFS() == null) {
					errors.rejectValue("paysNpa", "error.required.pays");
				}

				if (StringUtils.isBlank(adresseView.getLocaliteNpa())) {
					errors.rejectValue("localiteNpa", "error.required.localite_npa");
				}
			}

		}


		final RegDate dateDebut = adresseView.getRegDateDebut();
		final TypeAdresseTiers usage = adresseView.getUsage();
		final Tiers tiers = tiersService.getTiers(adresseView.getNumCTB());

		final Niveau acces = SecurityProvider.getDroitAcces(tiers);
		if (acces == null || acces == Niveau.LECTURE) {
			errors.reject("error.tiers.interdit");
		}

		// Vérification de la date de début
		if (dateDebut == null) {
			errors.rejectValue("dateDebut", "error.date.debut.vide");
		}
		else if (adresseView.getId() == null) {
			if (RegDate.get().isBefore(dateDebut)) {
				errors.rejectValue("dateDebut", "error.date.debut.future");
			}
			AdressesFiscales adressesFiscales = null;
			try {
				adressesFiscales = adresseService.getAdressesFiscales(tiers, null, false);
			}
			catch (AdresseException e) {
				LOGGER.debug(e, e);
			}
			if (adressesFiscales != null) {
				final AdresseGenerique adresse = adressesFiscales.ofType(usage);
				//Vérifie que la date de début de la nouvelle adresse n'est pas *avant* la date début de la dernière adresse existante
				if (adresse != null && adresse.getDateDebut() != null && dateDebut.isBefore(adresse.getDateDebut())) {
					errors.rejectValue("dateDebut", "error.date.debut.anterieure");
				}
			}
		}

		//gestion des droits de création d'une adresse
		boolean isAllowed = false;
		if(tiers.getNatureTiers() == NatureTiers.Habitant || tiers.getNatureTiers() == NatureTiers.NonHabitant ||
				tiers.getNatureTiers() == NatureTiers.MenageCommun){
			//PP
			switch (usage) {
				case COURRIER :
					// [UNIREG-1292] - mise à jour selon la matrice des droits Unireg
					if (tiers instanceof PersonnePhysique) {
						final PersonnePhysique pp = (PersonnePhysique) tiers;
						if (SecurityProvider.isGranted(Role.ADR_PP_C_DCD) && tiersService.isDecede(pp)) {
							// on peut modifier l'adresse courrier d'un décédé uniquement si ont possède le rôle correspondant
							isAllowed = true;
						}
						else if (SecurityProvider.isGranted(Role.ADR_PP_C) && !tiersService.isDecede(pp)) {
							// on peut modifier l'adresse courrier d'un non décédé uniquement si ont possède le rôle correspondant
							isAllowed = isTiersModifiable(tiers);
						}
					}
					else if (tiers instanceof MenageCommun) {
						final MenageCommun mc = (MenageCommun) tiers;
						boolean auMoinsUnDecede = false;
						for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(mc)) {
							if (tiersService.isDecede(pp)) {
								auMoinsUnDecede = true;
							}
						}
						// pour les ménages commun le droit sur les décédés est le plus contraignant
						if (auMoinsUnDecede) {
							if (SecurityProvider.isGranted(Role.ADR_PP_C_DCD)) {
								isAllowed = true;
							}
						}
						else {
							if(SecurityProvider.isGranted(Role.ADR_PP_C)) {
								isAllowed = isTiersModifiable(tiers);
							}
						}
					}
					break;
				case DOMICILE :
					if (SecurityProvider.isGranted(Role.ADR_PP_D)) {
						isAllowed = isTiersModifiable(tiers);
					}
					break;
				case POURSUITE :
					if (SecurityProvider.isGranted(Role.ADR_P)) {
						isAllowed = true;
					}
					break;
				case REPRESENTATION :
					if (SecurityProvider.isGranted(Role.ADR_PP_B)) {
						isAllowed = isTiersModifiable(tiers);
					}
					break;
				default:
					throw new IllegalArgumentException("Valeur non supportée : " + usage);
			}
		}
		else if(tiers.getNatureTiers() == NatureTiers.DebiteurPrestationImposable) {
			if (usage == TypeAdresseTiers.POURSUITE) {
				isAllowed = SecurityProvider.isGranted(Role.ADR_P);
			}
			else {
				isAllowed = isTiersModifiable(tiers);
			}
		}
		else {
			//PM
			switch (usage) {
				case COURRIER:
					if (SecurityProvider.isGranted(Role.ADR_PM_C)) {
						isAllowed = isTiersModifiable(tiers);
					}
					break;
				case DOMICILE:
					if (SecurityProvider.isGranted(Role.ADR_PM_D))
						isAllowed = isTiersModifiable(tiers);
					break;
				case POURSUITE:
					if (SecurityProvider.isGranted(Role.ADR_P))
						isAllowed = true;
					break;
				case REPRESENTATION:
					if (SecurityProvider.isGranted(Role.ADR_PM_B))
						isAllowed = isTiersModifiable(tiers);
					break;
				default:
					throw new IllegalArgumentException("Valeur non supportée : " + usage);
			}
		}

		if(!isAllowed){
			if (adresseView.getId() == null) {//création
				errors.rejectValue("usage", "error.usage.interdit");
			}
			else {//édition (il ne devrait plus y avoir d'édition d'adresse à terme)
				errors.reject("error.tiers.interdit");
			}
		}
	}

	private boolean isTiersModifiable(Tiers tiers) {

		try {
			if (tiers instanceof DebiteurPrestationImposable) {
				return SecurityProvider.isGranted(Role.CREATE_DPI);
			}
			else if (tiers instanceof PersonnePhysique || tiers instanceof MenageCommun) {
				final Contribuable ctb = (Contribuable) tiers;
				final boolean isHabitant = isHabitant(ctb);
				final Contribuable assujettissable = getContribuableAssujettissable(ctb);
				final List<Assujettissement> assujettissement = Assujettissement.determine(assujettissable);
				final Assujettissement assujettissementCourant = assujettissement != null ? DateRangeHelper.rangeAt(assujettissement, RegDate.get()) : null;
				if (assujettissementCourant == null) {
					// non-assujetti
					return SecurityProvider.isGranted(isHabitant ? Role.MODIF_HAB_DEBPUR : Role.MODIF_NONHAB_DEBPUR);
				}
				else if (assujettissementCourant instanceof HorsCanton || assujettissementCourant instanceof HorsSuisse) {
					return SecurityProvider.isGranted(Role.MODIF_HC_HS);
				}
				else if (assujettissementCourant instanceof SourcierPur) {
					return SecurityProvider.isGranted(Role.MODIF_VD_SOURC);
				}
				else {
					return SecurityProvider.isGranted(Role.MODIF_VD_ORD);
				}
			}
			else {
				// pas de rôle particulier pour avoir le droit de modifier les PM en général
				return true;
			}
		}
		catch (AssujettissementException e) {
			throw new ActionException(e.getMessage());
		}
	}

	/**
	 * N'a de sens que sur des personnes physiques ou des ménages communs
	 * @param ctb le contribuable sur lequel on s'interroge
	 * @return <code>true</code> si le contribuble est habitant, <code>false</code> sinon
	 */
	private boolean isHabitant(Contribuable ctb) {
		final boolean isHabitant;
		if (ctb instanceof PersonnePhysique) {
			isHabitant = ((PersonnePhysique) ctb).isHabitantVD();
		}
		else if (ctb instanceof MenageCommun) {
			final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple((MenageCommun) ctb, RegDate.get());
			final PersonnePhysique principal = couple.getPrincipal();
			final PersonnePhysique conjoint = couple.getConjoint();
			isHabitant = (principal != null && principal.isHabitantVD()) || (conjoint != null && conjoint.isHabitantVD());
		}
		else {
			isHabitant = false;
		}
		return isHabitant;
	}

	private Contribuable getContribuableAssujettissable(Contribuable ctb) {
		final Contribuable assujettissable;
		if (ctb instanceof PersonnePhysique) {
			final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple((PersonnePhysique) ctb, RegDate.get());
			if (couple != null && couple.getMenage() != null) {
				assujettissable = couple.getMenage();
			}
			else {
				assujettissable = ctb;
			}
		}
		else {
			assujettissable = ctb;
		}
		return assujettissable;
	}
}
