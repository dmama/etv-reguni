package ch.vd.unireg.decision.aci;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.DecisionAci;

public class EditDecisionAciValidator implements Validator {

	private final HibernateTemplate hibernateTemplate;


	protected EditDecisionAciValidator(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return EditDecisionAciView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final EditDecisionAciView view = (EditDecisionAciView) target;


		final DecisionAci decisionAci = hibernateTemplate.get(DecisionAci.class, view.getId());
		if (decisionAci == null) {
			throw new ObjectNotFoundException("Impossible de trouver la décision avec l'id=" + view.getId());
		}
		view.initReadOnlyData(decisionAci); // on ré-initialise les données en lecture-seule parce qu'elles ne font pas partie du formulaire (et ne doivent pas l'être pour des raisons de sécurité)


		// validation de la date de fin
		final RegDate dateDebut = view.getDateDebut();
		final RegDate dateFin = view.getDateFin();
		if (dateFin != null) {
			if (RegDate.get().isBefore(dateFin)) {
				errors.rejectValue("dateFin", "error.date.fin.dans.futur");
			}
			else if (dateDebut != null && dateFin.isBefore(dateDebut)) {
				errors.rejectValue("dateFin", "error.date.fin.avant.debut");
			}
		}

		if (view.getNumeroAutoriteFiscale() == null) {
			errors.rejectValue("numeroAutoriteFiscale", "error.autorite.fiscale.vide");
		}
		// [SIFISC-7381] si aucun changement n'a été saisi, on réaffiche le formulaire
		if (aucunChangement(decisionAci,view)) {
			errors.reject("global.error.aucun.changement");
		}
	}

	/**
	 * Vérifie si un changement à été fait dans le formulaire d'édition
	 * @param d la décision de reference
	 * @param v la vue de modification
	 * @return <b>true</b> si aucun changement, <b>false</b>sinon
	 */
	private boolean aucunChangement(DecisionAci d, EditDecisionAciView v){
		final boolean remarqueIdentique = StringUtils.equals(StringUtils.trimToNull(d.getRemarque()), StringUtils.trimToNull(v.getRemarque()));
		final boolean dateFinIdentique = d.getDateFin() == v.getDateFin();
		final boolean numeroAutoriteIdentique = d.getNumeroOfsAutoriteFiscale().equals(v.getNumeroAutoriteFiscale());
		return remarqueIdentique && dateFinIdentique && numeroAutoriteIdentique;
	}
}
