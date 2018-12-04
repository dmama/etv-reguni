package ch.vd.unireg.param.online;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.param.view.DelaisAccordablesOnlinePMView;
import ch.vd.unireg.type.delai.Delai;

public class DelaisOnlinePMValidator implements Validator {

	private PeriodeFiscaleDAO periodeFiscaleDAO;

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return clazz == DelaisOnlinePMView.class;
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@Override
	public void validate(Object target, Errors errors) {
		final DelaisOnlinePMView view = (DelaisOnlinePMView) target;

		final PeriodeFiscale pf = periodeFiscaleDAO.get(view.getPeriodeFiscaleId());
		if (pf == null) {
			throw new ObjectNotFoundException("Impossible de retrouver la période fiscale id : " + view.getPeriodeFiscaleId());
		}

		final List<DelaisAccordablesOnlinePMView> periodes = view.getPeriodes();

		final Set<Delai> delaisExistants = new HashSet<>();
		for (int i = 0; i < periodes.size(); i++) {
			final DelaisAccordablesOnlinePMView periode = periodes.get(i);

			// on s'assure que toutes les périodes possèdes des délais de début non nuls
			final Delai delaiDebut = periode.getDelaiDebut();
			if (delaiDebut == null) {
				errors.rejectValue("periodes[" + i + "].delaiDebut", "error.delai.debut.obligatoire");
				continue;
			}
			// on s'assure que toutes les périodes possèdes des délais de début non dupliqués
			else if (delaisExistants.contains(delaiDebut)) {
				errors.rejectValue("periodes[" + i + "].delaiDebut", "error.delai.debut.duplique");
			}
			delaisExistants.add(delaiDebut);

			// on s'assure que les délais 2 sont vides si les délais 1 sont vides aussi
			final Delai delai1DemandeUnitaire = periode.getDelai1DemandeUnitaire();
			final Delai delai2DemandeUnitaire = periode.getDelai2DemandeUnitaire();
			if (delai1DemandeUnitaire == null && delai2DemandeUnitaire != null) {
				errors.rejectValue("periodes[" + i + "].delai2DemandeUnitaire", "error.delai2.renseigne.mais.pas.delai1");
			}

			final Delai delai1DemandeGroupee = periode.getDelai1DemandeGroupee();
			final Delai delai2DemandeGroupee = periode.getDelai2DemandeGroupee();
			if (delai1DemandeGroupee == null && delai2DemandeGroupee != null) {
				errors.rejectValue("periodes[" + i + "].delai2DemandeGroupee", "error.delai2.renseigne.mais.pas.delai1");
			}
		}
	}
}
