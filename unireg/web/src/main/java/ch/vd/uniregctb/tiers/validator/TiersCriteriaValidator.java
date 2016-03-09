package ch.vd.uniregctb.tiers.validator;


import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.utils.ValidatorUtils;

/**
 * Validateur de l'objet du m�me nom.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 *
 */
public class TiersCriteriaValidator implements Validator {

	//private static final Logger LOGGER = LoggerFactory.getLogger(TiersCriteriaValidator.class);

	/**
	 * @see org.springframework.validation.Validator#supports(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return TiersCriteriaView.class.isAssignableFrom(clazz);
	}

	/**
	 * @see org.springframework.validation.Validator#validate(java.lang.Object,
	 *      org.springframework.validation.Errors)
	 */
	@Override
	public void validate(Object target, Errors errors) {
		// s'il y a deja des erreurs (ce sont des erreurs de binding), pas continuer les vérifications
		if (!errors.hasErrors()) {
			final TiersCriteriaView bean = (TiersCriteriaView) target;
			if (bean.isEmpty()) {
				errors.reject("error.criteres.vide");
			}
			if (StringUtils.isNotBlank(bean.getNumeroFormatte()) && !ValidatorUtils.isNumber(FormatNumeroHelper.removeSpaceAndDash(bean.getNumeroFormatte()))) {
				errors.rejectValue("numeroFormatte", "error.numero");
			}

			// Si le noOfsFor est null, mais que forAll est pas null
			// C'est que l'autocomplete a pas marché
			//   => Avertir l'utilisateur
			if (	(bean.getNoOfsFor() == null || bean.getNoOfsFor().isEmpty())
					&&
					(bean.getForAll() != null && !bean.getForAll().isEmpty())
				)
			{
				bean.setForAll(null);
				errors.rejectValue("forAll", "error.tiers.noOfsFor.invalide");
				//errors.reject("error.tiers.noOfsFor.invalide");
			}

			if (bean.getDateNaissanceInscriptionRC() != null) {
				RegDate dateDuJour = RegDate.get();
				if (bean.getDateNaissanceInscriptionRC().isAfter(dateDuJour)) {
					errors.rejectValue("dateNaissanceInscriptionRC", "error.date.naissance.posterieure.date.jour");
				}
			}
		}
	}
}
