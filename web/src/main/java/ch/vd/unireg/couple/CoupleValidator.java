package ch.vd.unireg.couple;

import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.common.ValidatorHelper;
import ch.vd.unireg.metier.MetierService;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.utils.ValidatorUtils;

public class CoupleValidator implements Validator {

	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private ValidatorHelper validatorHelper;
	private MetierService metierService;
	private CoupleManager coupleManager;
	private SecurityProviderInterface securityProvider;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setValidatorHelper(ValidatorHelper validatorHelper) {
		this.validatorHelper = validatorHelper;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCoupleManager(CoupleManager coupleManager) {
		this.coupleManager = coupleManager;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return CoupleView.class.equals(clazz);
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public void validate(Object target, Errors errors) {
		final CoupleView view = (CoupleView) target;

		// on détermine quelques informations sur le futur ménage commun
		final CoupleManager.CoupleInfo info = coupleManager.determineInfoFuturCouple(view.getPp1Id(), view.getPp2Id(), view.getMcId());

		// on récupère les paramètres de la requête, en forçant si nécessaire les valeurs qui vont bien
		final Long pp1Id = view.getPp1Id();
		final Long pp2Id = view.getPp2Id();
		final Long mcId = (info.getForceMcId() == null ? view.getMcId() : info.getForceMcId());
		final RegDate dateDebut = (info.getForceDateDebut() == null ? view.getDateDebut() : info.getForceDateDebut());

		// validation du numéro de la première personne physique
		validatePP(pp1Id, "pp1Id", errors);

		// validation du numéro de la seconde personne physique
		if (!view.isMarieSeul()) {
			validatePP(pp2Id, "pp2Id", errors);

			if (pp1Id != null && pp1Id.equals(pp2Id)) {
				errors.rejectValue("pp2Id", "error.seconde.pp.identique");
			}
		}

		// validation du numéro du contribuable existant
		if (!view.isNouveauMC()) {
			validateFuturMC(mcId, errors, info.getForceMcId() != null);

			if (mcId != null) {
				if (mcId.equals(pp1Id)) {
					errors.rejectValue("mcId", "error.mc.identique.premiere.pp");
				}
				if (mcId.equals(pp2Id)) {
					errors.rejectValue("mcId", "error.mc.identique.seconde.pp");
				}
			}
		}

		// validation de la date de début
		// [SIFISC-18086] blindage en cas de mauvais format de date, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateDebut")) {
			if (dateDebut == null) {
				errors.rejectValue("dateDebut", "error.date.debut.vide");
			}
			else if (RegDate.get().isBefore(dateDebut)) {
				errors.rejectValue("dateDebut", "error.date.debut.future");
			}
		}

		// validation du futur ménage-commun
		if (!errors.hasErrors()) {
			validateMariage(pp1Id, pp2Id, dateDebut, info.getType(), errors);
		}
	}

	private void validateMariage(Long pp1Id, Long pp2Id, RegDate dateDebut, TypeUnion typeUnion, Errors errors) {

		final ValidationResults validationResults = new ValidationResults();

		//Validation du mariage
		final PersonnePhysique principal = (PersonnePhysique) tiersService.getTiers(pp1Id);
		validatorHelper.validateSexeConnu(principal, validationResults);

		PersonnePhysique conjoint = null;
		if (pp2Id != null) {
			conjoint = (PersonnePhysique) tiersService.getTiers(pp2Id);
			validatorHelper.validateSexeConnu(conjoint, validationResults);
		}

		if (TypeUnion.RECONCILIATION != typeUnion) {
			validatorHelper.validatePretPourMariage(principal, dateDebut, validationResults);
			validatorHelper.validatePretPourMariage(conjoint, dateDebut, validationResults);
		}

		switch (typeUnion) {
		case SEUL:
		case COUPLE:
			validationResults.merge(metierService.validateMariage(dateDebut, principal, conjoint));
			break;

		case RECONCILIATION:
			validationResults.merge(metierService.validateReconciliation(principal, conjoint, dateDebut, false));
			break;

		case RECONSTITUTION_MENAGE: {
			final CoupleManager.Couple couple = coupleManager.getCoupleForReconstitution(principal, conjoint, dateDebut);
			validationResults.merge(metierService.validateReconstitution((MenageCommun) couple.getPremierTiers(), (PersonnePhysique) couple.getSecondTiers(), dateDebut));
		}
		break;

		case FUSION_MENAGES: {
			final CoupleManager.Couple couple = coupleManager.getCoupleForFusion(principal, conjoint, null);
			validationResults.merge(metierService.validateFusion((MenageCommun) couple.getPremierTiers(), (MenageCommun) couple.getSecondTiers()));
		}
		break;

		default:
			throw new IllegalArgumentException("Type d'union non supporté : " + typeUnion);
		}

		final List<String> validationErrors = validationResults.getErrors();
		ValidatorUtils.rejectErrors(validationErrors, errors);
	}

	private void validatePP(Long ppId, String field, Errors errors) {
		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors(field)) {
			if (ppId == null) {
				errors.rejectValue(field, "error.tiers.obligatoire");
			}
			else if (ppId > ContribuableImpositionPersonnesPhysiques.CTB_GEN_LAST_ID) {
				errors.rejectValue(field, "error.numero.tiers.trop.grand");
			}
			else {
				final Tiers tiers = tiersDAO.get(ppId);
				if (tiers == null) {
					errors.rejectValue(field, "error.tiers.inexistant");
				}
				else if (!(tiers instanceof PersonnePhysique)) {
					errors.rejectValue(field, "error.tiers.doit.etre.personne.physique");
				}
				else if (!isModifGranted(tiers)) {
					errors.rejectValue(field, "error.tiers.interdit");
				}
			}
		}
	}


	private void validateFuturMC(Long futurMcId, Errors errors, boolean forcedMc) {
		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("mcId")) {
			if (futurMcId == null) {
				errors.rejectValue("mcId", "error.tiers.obligatoire");
			}
			else if (futurMcId > ContribuableImpositionPersonnesPhysiques.CTB_GEN_LAST_ID) {
				errors.rejectValue("mcId", "error.numero.tiers.trop.grand");
			}
			else {
				final Tiers tiers = tiersDAO.get(futurMcId);
				if (tiers == null) {
					errors.rejectValue("mcId", "error.tiers.inexistant");
				}
				else if (!forcedMc && !CoupleMcPickerFilter.isValideCommeTroisiemeTiers(tiers)) {
					errors.rejectValue("mcId", "error.troisieme.tiers.non.valide");
				}
				else if (!isModifGranted(tiers)) {
					errors.rejectValue("mcId", "error.tiers.interdit");
				}
			}
		}
	}

	/**
	 * @param tiers une personne physique ou un ménage commun
	 * @return <b>vrai</b> si l'utilisateur courant possède les droits de modification de la personne physique spécifiée; <b>faux</b> autrement.
	 */
	private boolean isModifGranted(Tiers tiers) {

		final Niveau acces = SecurityHelper.getDroitAcces(securityProvider, tiers);
		if (acces == null || acces == Niveau.LECTURE) {
			return false;
		}

		boolean droitTout = SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS);
		if (droitTout) {
			return true;
		}

		boolean habitantVD = isSomewhatHabitantVD(tiers);

		if (habitantVD && SecurityHelper.isGranted(securityProvider, Role.MODIF_HAB_DEBPUR)) {
			return true;
		}

		//noinspection RedundantIfStatement
		if (!habitantVD && SecurityHelper.isGranted(securityProvider, Role.MODIF_NONHAB_DEBPUR)) {
			return true;
		}

		return false;
	}

	/**
	 * @param tiers une personne physique ou un ménage commun
	 * @return <b>vrai</b> si le tiers est plutôt considéré comme habitant le canton de Vaud; <b>faux</b> si le tiers est plutôt considéré comme un non-habitant.
	 */
	private boolean isSomewhatHabitantVD(Tiers tiers) {
		if (tiers instanceof PersonnePhysique) {
			return ((PersonnePhysique) tiers).isHabitantVD();
		}
		else if (tiers instanceof MenageCommun) {
			boolean habitantVD = false;
			final Set<PersonnePhysique> pps = tiersService.getPersonnesPhysiques((MenageCommun) tiers);
			for (PersonnePhysique pp : pps) {
				habitantVD = habitantVD || pp.isHabitantVD();
			}
			return habitantVD;
		}
		else {
			throw new IllegalArgumentException("Type de tiers non géré [" + tiers.getClass().getSimpleName() + ']');
		}
	}

}
