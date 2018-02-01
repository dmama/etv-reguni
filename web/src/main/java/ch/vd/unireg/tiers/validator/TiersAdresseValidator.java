package ch.vd.unireg.tiers.validator;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.AdressesFiscales;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.HorsCanton;
import ch.vd.unireg.metier.assujettissement.HorsSuisse;
import ch.vd.unireg.metier.assujettissement.SourcierPur;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.NatureTiers;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.view.AdresseView;
import ch.vd.unireg.tiers.view.TiersEditView;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.TypeAdresseTiers;

public class TiersAdresseValidator implements Validator {

	protected final Logger LOGGER = LoggerFactory.getLogger(TiersAdresseValidator.class);

	private AdresseService adresseService;
	private TiersService tiersService;
	private ServiceInfrastructureService serviceInfra;
	private AssujettissementService assujettissementService;
	private SecurityProviderInterface securityProvider;

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
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
						Localite loc = serviceInfra.getLocaliteByONRP(noOrdre, RegDateHelper.get(adresseView.getDateFin()));
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


		final RegDate dateDebut = adresseView.getDateDebut();
		final TypeAdresseTiers usage = adresseView.getUsage();
		final Tiers tiers = tiersService.getTiers(adresseView.getNumCTB());

		final Niveau acces = SecurityHelper.getDroitAcces(securityProvider, tiers);
		if (acces == null || acces == Niveau.LECTURE) {
			errors.reject("error.tiers.interdit");
		}

		// Vérification de la date de début
		if (dateDebut == null) {
			// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
			if (!errors.hasFieldErrors("dateDebut")) {
				errors.rejectValue("dateDebut", "error.date.debut.vide");
			}
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
				LOGGER.debug(e.getMessage(), e);
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
						if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_C_DCD) && tiersService.isDecede(pp)) {
							// on peut modifier l'adresse courrier d'un décédé uniquement si ont possède le rôle correspondant
							isAllowed = true;
						}
						else if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_C) && !tiersService.isDecede(pp)) {
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
							if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_C_DCD)) {
								isAllowed = true;
							}
						}
						else {
							if(SecurityHelper.isGranted(securityProvider, Role.ADR_PP_C)) {
								isAllowed = isTiersModifiable(tiers);
							}
						}
					}
					break;
				case DOMICILE :
					if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_D)) {
						isAllowed = isTiersModifiable(tiers);
					}
					break;
				case POURSUITE :
					if (SecurityHelper.isGranted(securityProvider, Role.ADR_P)) {
						isAllowed = true;
					}
					break;
				case REPRESENTATION :
					if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_B)) {
						isAllowed = isTiersModifiable(tiers);
					}
					break;
				default:
					throw new IllegalArgumentException("Valeur non supportée : " + usage);
			}
		}
		else if(tiers.getNatureTiers() == NatureTiers.DebiteurPrestationImposable) {
			if (usage == TypeAdresseTiers.POURSUITE) {
				isAllowed = SecurityHelper.isGranted(securityProvider, Role.ADR_P);
			}
			else {
				isAllowed = isTiersModifiable(tiers);
			}
		}
		else {
			//PM
			switch (usage) {
				case COURRIER:
					if (SecurityHelper.isGranted(securityProvider, Role.ADR_PM_C)) {
						isAllowed = isTiersModifiable(tiers);
					}
					break;
				case DOMICILE:
					if (SecurityHelper.isGranted(securityProvider, Role.ADR_PM_D))
						isAllowed = isTiersModifiable(tiers);
					break;
				case POURSUITE:
					if (SecurityHelper.isGranted(securityProvider, Role.ADR_P))
						isAllowed = true;
					break;
				case REPRESENTATION:
					if (SecurityHelper.isGranted(securityProvider, Role.ADR_PM_B))
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
				return SecurityHelper.isGranted(securityProvider, Role.CREATE_DPI);
			}
			else if (tiers instanceof PersonnePhysique || tiers instanceof MenageCommun) {
				final Contribuable ctb = (Contribuable) tiers;
				final boolean isHabitant = isHabitant(ctb);
				final Contribuable assujettissable = getContribuableAssujettissable(ctb);
				final List<Assujettissement> assujettissement = assujettissementService.determine(assujettissable);
				final Assujettissement assujettissementCourant = assujettissement != null ? DateRangeHelper.rangeAt(assujettissement, RegDate.get()) : null;
				if (assujettissementCourant == null) {
					// non-assujetti
					return SecurityHelper.isGranted(securityProvider, isHabitant ? Role.MODIF_HAB_DEBPUR : Role.MODIF_NONHAB_DEBPUR);
				}
				else if (assujettissementCourant instanceof HorsCanton || assujettissementCourant instanceof HorsSuisse) {
					return SecurityHelper.isGranted(securityProvider, Role.MODIF_HC_HS);
				}
				else if (assujettissementCourant instanceof SourcierPur) {
					return SecurityHelper.isGranted(securityProvider, Role.MODIF_VD_SOURC);
				}
				else {
					return SecurityHelper.isGranted(securityProvider, Role.MODIF_VD_ORD);
				}
			}
			else if (tiers instanceof Entreprise || tiers instanceof Etablissement) {
				return SecurityHelper.isGranted(securityProvider, Role.MODIF_PM);
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
