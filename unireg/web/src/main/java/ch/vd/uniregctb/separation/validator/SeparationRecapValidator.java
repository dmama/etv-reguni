package ch.vd.uniregctb.separation.validator;

import java.util.List;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.separation.view.SeparationRecapView;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.utils.ValidateHelper;

public class SeparationRecapValidator implements Validator {

	private MetierService metierService;
	private TiersService tiersService;

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return SeparationRecapView.class.equals(clazz) ;
	}

	public void validate(Object obj, Errors errors) {

		Assert.isTrue(obj instanceof SeparationRecapView);
		SeparationRecapView separationRecapView = (SeparationRecapView) obj;

		final RegDate dateSeparation = separationRecapView.getDateSeparation();
		if (dateSeparation == null) {
			if (EtatCivil.DIVORCE.equals(separationRecapView.getEtatCivil())) {
				ValidationUtils.rejectIfEmpty(errors, "dateSeparation", "error.date.divorce.vide");
			}
			else {
				ValidationUtils.rejectIfEmpty(errors, "dateSeparation", "error.date.separation.vide");
			}
		}
		else {
			if (RegDate.get().isBefore(dateSeparation)) {
				if (EtatCivil.DIVORCE.equals(separationRecapView.getEtatCivil())) {
					errors.rejectValue("dateSeparation", "error.date.divorce.future");
				}
				else {
					errors.rejectValue("dateSeparation", "error.date.separation.future");
				}
			}

			//Validation de la séparation
			MenageCommun menage = (MenageCommun) tiersService.getTiers(separationRecapView.getCouple().getNumero());

			EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(menage, dateSeparation);
			PersonnePhysique principal = couple.getPrincipal();
			Sexe sexePrincipal = tiersService.getSexe(principal);
			if (principal != null && sexePrincipal == null) {
				errors.rejectValue("premierePersonne", "error.premiere.personne.sexe.inconnnu");
			}
			PersonnePhysique conjoint = couple.getConjoint();
			Sexe sexeConjoint = tiersService.getSexe(conjoint);
			if (conjoint != null && sexeConjoint == null) {
				errors.rejectValue("secondePersonne", "error.seconde.personne.sexe.inconnnu");
			}

			ValidationResults results = metierService.validateSeparation(menage, dateSeparation);
			List<String> validationErrors = results.getErrors();
			ValidateHelper.rejectErrors(validationErrors, errors);
			// mise à jour des warnings pour les afficher dans la page résultante
			separationRecapView.setWarnings(results.getWarnings());
		}
	}

	public MetierService getMetierService() {
		return metierService;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

}