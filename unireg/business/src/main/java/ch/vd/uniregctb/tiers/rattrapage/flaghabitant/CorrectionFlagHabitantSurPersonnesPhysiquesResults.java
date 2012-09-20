package ch.vd.uniregctb.tiers.rattrapage.flaghabitant;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ch.vd.uniregctb.tiers.PersonnePhysique;

public class CorrectionFlagHabitantSurPersonnesPhysiquesResults extends CorrectionFlagHabitantAbstractResults<CorrectionFlagHabitantSurPersonnesPhysiquesResults> {

	private final List<ContribuableInfo> nouveauxHabitants = new LinkedList<ContribuableInfo>();

	private final List<ContribuableInfo> nouveauxNonHabitants = new LinkedList<ContribuableInfo>();

	private final List<ContribuableErreur> erreurs = new LinkedList<ContribuableErreur>();

	public void addHabitantChangeEnNonHabitant(PersonnePhysique pp) {
		nouveauxNonHabitants.add(new ContribuableInfo(pp.getNumero(), Message.PP_NOUVEAU_NON_HABITANT));
	}

	public void addNonHabitantChangeEnHabitant(PersonnePhysique pp) {
		nouveauxHabitants.add(new ContribuableInfo(pp.getNumero(), Message.PP_NOUVEL_HABITANT));
	}

	public void addNonHabitantForVaudoisSansNumeroIndividu(PersonnePhysique pp) {
		erreurs.add(new ContribuableErreur(pp.getNumero(), Message.PP_NON_HABITANT_SANS_NUMERO_INDIVIDU));
	}

	@Override
	public void addErrorException(Long noCtb, Exception e) {
		final String message = (StringUtils.isEmpty(e.getMessage()) ? e.getClass().getName() : e.getMessage());
		erreurs.add(new ContribuableException(noCtb, message));
	}

	@Override
	public void addAll(CorrectionFlagHabitantSurPersonnesPhysiquesResults contribution) {
		nouveauxHabitants.addAll(contribution.nouveauxHabitants);
		nouveauxNonHabitants.addAll(contribution.nouveauxNonHabitants);
		erreurs.addAll(contribution.erreurs);
	}

	public List<ContribuableInfo> getNouveauxHabitants() {
		return nouveauxHabitants;
	}

	public List<ContribuableInfo> getNouveauxNonHabitants() {
		return nouveauxNonHabitants;
	}

	public List<ContribuableErreur> getErreurs() {
		return erreurs;
	}

	public void sort() {
		Collections.sort(nouveauxHabitants, COMPARATOR);
		Collections.sort(nouveauxNonHabitants, COMPARATOR);
		Collections.sort(erreurs, COMPARATOR);
	}

	public final int getNombreElementsInspectes() {
		return getNombrePersonnesPhysiquesModifiees() + erreurs.size();
	}

	public final int getNombrePersonnesPhysiquesModifiees() {
		return nouveauxHabitants.size() + nouveauxNonHabitants.size();
	}
}
