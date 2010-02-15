package ch.vd.uniregctb.lr.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.lr.view.ListeRecapListView;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

public class ListeRecapEditDebiteurValidator implements Validator {

	private TiersDAO tiersDAO;

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return ListeRecapListView.class.equals(clazz);
	}

	public void validate(Object target, Errors errors) {
		ListeRecapListView lrListView = (ListeRecapListView) target;

		Tiers tiers = tiersDAO.get(lrListView.getDpi().getNumero());
		if(tiers.getForsFiscaux() == null || tiers.getForsFiscaux().isEmpty()){
			//la dpi ne possède pas de for => création LR interdite
			errors.reject("error.lr.creation.interdit");
		}
	}

	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

}
