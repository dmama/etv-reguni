package ch.vd.uniregctb.supergra.validators;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.supergra.view.Mc2PpView;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

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
		if (indNo == null) {
			errors.rejectValue("indNo", "error.numero.obligatoire");
		}
		else if (serviceCivil.getIndividu(view.getIndNo(), null) == null) {
			errors.rejectValue("indNo", "error.individu.inexistant", new Object[]{Long.toString(indNo)}, null);
		}
	}
}
