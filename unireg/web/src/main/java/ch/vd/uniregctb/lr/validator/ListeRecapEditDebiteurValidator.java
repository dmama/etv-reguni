package ch.vd.uniregctb.lr.validator;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.lr.view.ListeRecapListView;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

public class ListeRecapEditDebiteurValidator implements Validator {

	private TiersDAO tiersDAO;

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return ListeRecapListView.class.equals(clazz);
	}

	@Override
	@Transactional(readOnly = true)
	public void validate(Object target, Errors errors) {
		final ListeRecapListView lrListView = (ListeRecapListView) target;
		final Tiers tiers = tiersDAO.get(lrListView.getDpi().getNumero());
		final List<ForFiscal> forsFiscauxNonAnnules = tiers.getForsFiscauxNonAnnules(false);
		if (forsFiscauxNonAnnules == null || forsFiscauxNonAnnules.isEmpty()) {
			//la dpi ne possède pas de for => création LR interdite
			errors.reject("error.lr.creation.interdit");
		}
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

}
