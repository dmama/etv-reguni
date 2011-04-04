package ch.vd.uniregctb.couple.validator;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.ValidatorHelper;
import ch.vd.uniregctb.couple.CoupleHelper;
import ch.vd.uniregctb.couple.CoupleHelper.Couple;
import ch.vd.uniregctb.couple.CoupleRecapPickerFilter;
import ch.vd.uniregctb.couple.view.CoupleRecapView;
import ch.vd.uniregctb.couple.view.TypeUnion;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.utils.ValidatorUtils;

/**
 * Validation du fait que les éléments du couple sont déjà en ménage
 *
 * @author xcifde
 *
 */
public class CoupleRecapValidator implements Validator {

	private MetierService metierService;
	private TiersService tiersService;
	private CoupleHelper coupleHelper;
	private ValidatorHelper validatorHelper;

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setCoupleHelper(CoupleHelper coupleHelper) {
		this.coupleHelper = coupleHelper;
	}

	public void setValidatorHelper(ValidatorHelper validatorHelper) {
		this.validatorHelper = validatorHelper;
	}

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return CoupleRecapView.class.equals(clazz) ;
	}

	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {

		Assert.isTrue(obj instanceof CoupleRecapView);
		final CoupleRecapView coupleRecapView = (CoupleRecapView) obj;

		final TypeUnion typeUnion = coupleRecapView.getTypeUnion();
		final RegDate dateDebut;
		final String dateDebutField;
		if ((typeUnion == TypeUnion.COUPLE || typeUnion == TypeUnion.SEUL) && !coupleRecapView.isNouveauCtb()) {
			if (coupleRecapView.getNumeroTroisiemeTiers() == null) {
				errors.rejectValue("numeroTroisiemeTiers", "error.aucun.contribuable.existant");
				dateDebut = coupleRecapView.getDateCoupleExistant();
			}
			else {
				final Tiers troisieme = tiersService.getTiers(coupleRecapView.getNumeroTroisiemeTiers());
				if (troisieme == null) {
					errors.rejectValue("numeroTroisiemeTiers", "error.tiers.inexistant");
				}
				else {
					if (!CoupleRecapPickerFilter.isValideCommeTroisiemeTiers(troisieme)) {
						errors.rejectValue("numeroTroisiemeTiers", "error.troisieme.tiers.non.valide");
					}
				}

				if (troisieme != null && troisieme.getNatureTiers() == NatureTiers.NonHabitant) {
					dateDebut = troisieme.getDateDebutActivite(); // [UNIREG-3297] on ne tient pas compte de la date affichée à l'écran
				}
				else {
					dateDebut = coupleRecapView.getDateCoupleExistant();
				}
			}

			dateDebutField = "dateCoupleExistant";
		}
		else {
			dateDebut = RegDate.get(coupleRecapView.getDateDebut());
			dateDebutField = "dateDebut";
		}
		
		if (dateDebut == null) {
			ValidationUtils.rejectIfEmpty(errors, dateDebutField, "error.date.debut.vide");
		}
		else {
			if (RegDate.get().isBefore(dateDebut)) {
				errors.rejectValue(dateDebutField, "error.date.debut.future");
			}

			final ValidationResults validationResults = new ValidationResults();

			//Validation du mariage
			final PersonnePhysique principal = (PersonnePhysique) tiersService.getTiers(coupleRecapView.getPremierePersonne().getNumero());
			validatorHelper.validateSexeConnu(principal, validationResults);

			PersonnePhysique conjoint = null;
			if (coupleRecapView.getSecondePersonne() != null) {
				conjoint = (PersonnePhysique) tiersService.getTiers(coupleRecapView.getSecondePersonne().getNumero());
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
					validationResults.merge(metierService.validateReconciliation(principal, conjoint, dateDebut));
					break;

				case RECONSTITUTION_MENAGE:
					{
						final Couple couple = coupleHelper.getCoupleForReconstitution(principal, conjoint, dateDebut);
						validationResults.merge(metierService.validateReconstitution((MenageCommun) couple.getPremierTiers(), (PersonnePhysique) couple.getSecondTiers(), dateDebut));
					}
					break;
					
				case FUSION_MENAGES:
					{
						final Couple couple = coupleHelper.getCoupleForFusion(principal, conjoint, null);
						validationResults.merge(metierService.validateFusion((MenageCommun) couple.getPremierTiers(), (MenageCommun) couple.getSecondTiers()));
					}
					break;
					
				default:
					Assert.fail("Type d'union non supporté : " + typeUnion);
					break;
			}
			
			final List<String> validationErrors = validationResults.getErrors();
			ValidatorUtils.rejectErrors(validationErrors, errors);
		}
	}
}