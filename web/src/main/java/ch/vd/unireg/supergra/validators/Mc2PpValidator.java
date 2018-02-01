package ch.vd.unireg.supergra.validators;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.supergra.view.Mc2PpView;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;

public class Mc2PpValidator implements Validator {

	private TiersDAO tiersDAO;
	private ServiceCivilService serviceCivil;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return Mc2PpView.class == clazz;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public void validate(Object target, Errors errors) {
		final Mc2PpView view = (Mc2PpView) target;

		// l'id du tiers n'est pas éditable par l'utilisateur, sauf hack
		final Tiers tiers = tiersDAO.get(view.getId());
		if (tiers == null) {
			throw new TiersNotFoundException(view.getId());
		}
		if (!(tiers instanceof MenageCommun)) {
			throw new IllegalArgumentException("Le tiers doit être un ménage commun !");
		}

		// on vérifie que le numéro d'individu est valide
		final Long indNo = view.getIndNo();

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("indNo")) {
			if (indNo == null) {
				errors.rejectValue("indNo", "error.numero.obligatoire");
			}
			else {
				try {
					final Individu individu = serviceCivil.getIndividu(view.getIndNo(), null);
					if (individu == null) {
						errors.rejectValue("indNo", "error.individu.inexistant", new Object[]{Long.toString(indNo)}, null);
					}
				}
				catch (ServiceCivilException e) {
					errors.rejectValue("indNo", "error.individu.exception", new Object[]{Long.toString(indNo), e.getMessage()}, null);
				}
			}
		}
	}
}
