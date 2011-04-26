package ch.vd.uniregctb.common;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.utils.WebContextUtils;

public class ValidatorHelperImpl implements ValidatorHelper, MessageSourceAware {

	private MessageSource messageSource;
	private TiersService tiersService;
	private SituationFamilleService situationFamilleService;

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSituationFamilleService(SituationFamilleService situationFamilleService) {
		this.situationFamilleService = situationFamilleService;
	}

	/**
	 * Ajoute une erreur de validation à l'ensemble donné si le sexe de la personne physique donnée est inconnu
	 * @param pp la personne physique à tester
	 * @param vr le container de l'éventuelle erreur générée
	 */
	public void validateSexeConnu(PersonnePhysique pp, ValidationResults vr) {
		final Sexe sexe = tiersService.getSexe(pp);
		if (pp != null && sexe == null) {
			final Object[] params = new Object[] {FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero())};
			vr.addError(messageSource.getMessage("error.personne.sexe.inconnnu", params, WebContextUtils.getDefaultLocale()));
		}
	}

	private boolean estPretPourMariage(EtatCivil etatCivil) {
		return etatCivil == null || (EtatCivil.SEPARE != etatCivil && EtatCivil.MARIE != etatCivil);
	}

	/**
	 * Ajoute une erreur de validation à l'ensemble donné si la personne physique ne peut pas se marier à la date prévue
	 * @param pp la personne physique à tester
	 * @param dateMariagePrevu date prévue pour le mariage
	 * @param vr le container de l'éventuelle erreur générée
	 */
	public void validatePretPourMariage(PersonnePhysique pp, RegDate dateMariagePrevu, ValidationResults vr) {
		if (pp != null) {
			final EtatCivil etatCivil = situationFamilleService.getEtatCivil(pp, dateMariagePrevu, false);
			if (!estPretPourMariage(etatCivil)) {
				final Object[] params = new Object[] {FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()), etatCivil.format().toLowerCase()};
				vr.addError(messageSource.getMessage("error.impossible.marier.contribuable", params, WebContextUtils.getDefaultLocale()));
			}
		}
	}
}
