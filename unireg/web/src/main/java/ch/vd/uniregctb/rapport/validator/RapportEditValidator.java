package ch.vd.uniregctb.rapport.validator;

import ch.vd.uniregctb.adresse.AdresseException;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.SensRapportEntreTiers;
import ch.vd.uniregctb.type.TypeAdresseTiers;

/**
 * Validateur de RapportEditController
 *
 * @author xcifde
 *
 */
public class RapportEditValidator implements Validator {

	private TiersDAO tiersDAO;

	private AdresseService adresseService;

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return RapportView.class.equals(clazz) ;
	}

	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {

		RapportView rapportView = (RapportView) obj;

		if (rapportView.getDateDebut() == null) {
			ValidationUtils.rejectIfEmpty(errors, "dateDebut", "error.date.debut.vide");
		}
		else {
			//vérifier que le tuteur, le curateur ou le conseil légal a une adresse de représentation
			Tiers tiers = null;
			if(rapportView.getSensRapportEntreTiers().equals(SensRapportEntreTiers.OBJET)){
				tiers = tiersDAO.get(rapportView.getTiersLie().getNumero());
			}
			else {
				tiers = tiersDAO.get(rapportView.getTiers().getNumero());
			}
			try {
				AdresseGenerique adrTuteur = adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.REPRESENTATION, rapportView
						.getRegDateDebut(), false);
				if (adrTuteur == null) {
					errors.reject("error.rapport.adresse");
				}
			} catch (AdresseException e) {
				errors.reject("error.rapport.adresse");
			}
		}
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

}