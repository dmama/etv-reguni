package ch.vd.uniregctb.rapport.validator;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.rapport.SensRapportEntreTiers;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

/**
 * Validateur de RapportController
 *
 * @author xcifde
 *
 */
public class RapportEditValidator implements Validator {

	private TiersDAO tiersDAO;

	private AdresseService adresseService;

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return RapportView.class.equals(clazz) ;
	}

	@Override
	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {

		final RapportView rapportView = (RapportView) obj;

		if (rapportView.getDateDebut() == null) {
			// [SIFISC-18086] blindage en cas de mauvais format de date, pour éviter le double message d'erreur
			if (!errors.hasFieldErrors("dateDebut")) {
				errors.rejectValue("dateDebut", "error.date.debut.vide");
			}
		}
		else {
			//vérifier que le tuteur, le curateur ou le conseil légal a une adresse de représentation
			final Tiers tiers;
			if(rapportView.getSensRapportEntreTiers() == SensRapportEntreTiers.OBJET){
				tiers = tiersDAO.get(rapportView.getTiersLie().getNumero());
			}
			else {
				tiers = tiersDAO.get(rapportView.getTiers().getNumero());
			}
			try {
				AdresseGenerique adrTuteur = adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.REPRESENTATION, rapportView.getDateDebut(), false);
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