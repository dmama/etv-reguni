package ch.vd.uniregctb.tiers.rattrapage.flaghabitant;

import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CorrectionFlagHabitantSurMenagesResults extends CorrectionFlagHabitantAbstractResults<CorrectionFlagHabitantSurMenagesResults> {

	private final List<ContribuableErreur> erreurs = new LinkedList<ContribuableErreur>();

	public void sort() {
		Collections.sort(erreurs, COMPARATOR);
	}

	public List<ContribuableErreur> getErreurs() {
		return erreurs;
	}

	public int getNombreElementsInspectes() {
		return erreurs.size();
	}

	public void addMenageVaudoisSansHabitant(long mcNo) {
		this.erreurs.add(new ContribuableErreur(mcNo, Message.MC_FOR_VD_SANS_HABITANT));
	}

	public void addMenageNonVaudoisAvecHabitant(long mcNo) {
		this.erreurs.add(new ContribuableErreur(mcNo, Message.MC_FOR_HC_HS_AVEC_HABITANT));
	}

	public void addErrorException(Long noCtb, Exception e) {
		final String messageException = StringUtils.isEmpty(e.getMessage()) ? e.getClass().getName() : e.getMessage();
		this.erreurs.add(new ContribuableException(noCtb, messageException));
	}

	public void addAll(CorrectionFlagHabitantSurMenagesResults correctionFlagHabitantSurMenagesResults) {
		this.erreurs.addAll(correctionFlagHabitantSurMenagesResults.erreurs);
	}

}
