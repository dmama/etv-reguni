package ch.vd.uniregctb.couple.validator;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.couple.CoupleHelper;
import ch.vd.uniregctb.couple.CoupleHelper.Couple;
import ch.vd.uniregctb.couple.CoupleRecapPickerFilter;
import ch.vd.uniregctb.couple.view.CoupleRecapView;
import ch.vd.uniregctb.couple.view.TypeUnion;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.utils.ValidateHelper;

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
	private SituationFamilleService situationFamilleService;

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setCoupleHelper(CoupleHelper coupleHelper) {
		this.coupleHelper = coupleHelper;
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
				return;
			}
			final Tiers troisieme = tiersService.getTiers(coupleRecapView.getNumeroTroisiemeTiers());
			if (troisieme == null) {
				errors.rejectValue("numeroTroisiemeTiers", "error.tiers.inexistant");
			}
			else {
				if (!CoupleRecapPickerFilter.isValideCommeTroisiemeTiers(troisieme)) {
					errors.rejectValue("numeroTroisiemeTiers", "error.troisieme.tiers.non.valide");
				}
			}

			dateDebut = coupleRecapView.getDateCoupleExistant();
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

			//Validation du mariage
			final PersonnePhysique principal = (PersonnePhysique) tiersService.getTiers(coupleRecapView.getPremierePersonne().getNumero());
			final Sexe sexePrincipal = tiersService.getSexe(principal);
			if (principal != null && sexePrincipal == null) {
				errors.rejectValue("premierePersonne", "error.premiere.personne.sexe.inconnnu");
			}

			PersonnePhysique conjoint = null;
			if (coupleRecapView.getSecondePersonne() != null) {
				conjoint = (PersonnePhysique) tiersService.getTiers(coupleRecapView.getSecondePersonne().getNumero());
				final Sexe sexeConjoint = tiersService.getSexe(conjoint);
				if (conjoint != null && sexeConjoint == null) {
					errors.rejectValue("secondePersonne", "error.seconde.personne.sexe.inconnnu");
				}
			}
			
			if (TypeUnion.RECONCILIATION != typeUnion) {
				final ch.vd.uniregctb.type.EtatCivil etatCivilPrincipal = situationFamilleService.getEtatCivil(principal, dateDebut, false);
				if (!estPretPourMariage(etatCivilPrincipal)) {
					errors.rejectValue("premierePersonne", "error.impossible.marier.contribuable", new Object[] { FormatNumeroHelper.numeroCTBToDisplay(principal.getNumero()), etatCivilPrincipal.format().toLowerCase() }, "");
				}
					
				// [UNIREG-1076] cas d'un mariage avec conjoint inconnu
				if (conjoint != null) {
					final ch.vd.uniregctb.type.EtatCivil etatCivilConjoint = situationFamilleService.getEtatCivil(conjoint, dateDebut, false);
					if (!estPretPourMariage(etatCivilConjoint)) {
						errors.rejectValue("secondePersonne", "error.impossible.marier.contribuable", new Object[] { FormatNumeroHelper.numeroCTBToDisplay(conjoint.getNumero()), etatCivilConjoint.format().toLowerCase() }, "");
					}
				}
			}

			final ValidationResults validationResults;
			switch (typeUnion) {
				case SEUL:
				case COUPLE:
					if (!coupleRecapView.isNouveauCtb()) {
						checkFors(principal, "premierePersonne", dateDebut, errors);
						if (conjoint != null && conjoint.getForsFiscaux() != null) {
							checkFors(conjoint, "secondePersonne", dateDebut, errors);
						}
					}
					validationResults = metierService.validateMariage(dateDebut, principal, conjoint);
					break;
					
				case RECONCILIATION:
					validationResults = metierService.validateReconciliation(principal, conjoint, dateDebut);
					break;

				case RECONSTITUTION_MENAGE:
				{
					Couple couple = coupleHelper.getCoupleForReconstitution(principal, conjoint, dateDebut); 
					validationResults = metierService.validateReconstitution((MenageCommun) couple.getPremierTiers(), (PersonnePhysique) couple.getSecondTiers(), dateDebut);
				}
					break;
					
				case FUSION_MENAGES:
				{
					Couple couple = coupleHelper.getCoupleForFusion(principal, conjoint, null); 
					validationResults = metierService.validateFusion((MenageCommun) couple.getPremierTiers(), (MenageCommun) couple.getSecondTiers());
				}
					break;
					
				default:
					validationResults = new ValidationResults();
					break;
			}
			
			final List<String> validationErrors = validationResults.getErrors();
			ValidateHelper.rejectErrors(validationErrors, errors);
		}
	}

	private boolean estPretPourMariage(ch.vd.uniregctb.type.EtatCivil etatCivil) {
		return etatCivil == null || ch.vd.uniregctb.type.EtatCivil.SEPARE != etatCivil;
	}

	private void checkFors(PersonnePhysique pp, String field, RegDate date, Errors errors) {
		if (pp.getForsFiscaux() != null) {
			for (ForFiscal forFiscal : pp.getForsFiscaux()) {
				if (!forFiscal.isAnnule() && (forFiscal.getDateDebut().isAfter(date) || (forFiscal.getDateFin() != null && forFiscal.getDateFin().isAfter(date)))) {
					errors.rejectValue(field, "error.fors.posterieurs.date.mariage", new Object[] {FormatNumeroHelper.numeroCTBToDisplay(forFiscal.getTiers().getNumero()), RegDateHelper.dateToDisplayString(date)}, "");
				}
			}
		}
	}
	
	public void setSituationFamilleService(SituationFamilleService situationFamilleService) {
		this.situationFamilleService = situationFamilleService;
	}
}