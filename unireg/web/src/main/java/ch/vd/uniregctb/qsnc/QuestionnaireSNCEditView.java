package ch.vd.uniregctb.qsnc;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.declaration.view.QuestionnaireSNCView;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class QuestionnaireSNCEditView extends QuestionnaireSNCView {

	private final boolean rappelable;

	public QuestionnaireSNCEditView(QuestionnaireSNC questionnaire, MessageSource messageSource, boolean allowedRappel) {
		super(questionnaire, messageSource);
		this.rappelable = allowedRappel && isRappelable(questionnaire);
	}

	private static TypeEtatDeclaration getDernierEtat(QuestionnaireSNC q) {
		final EtatDeclaration dernierEtat = q.getDernierEtat();
		return dernierEtat == null ? null : dernierEtat.getEtat();
	}

	private static boolean isRappelable(QuestionnaireSNC q) {
		final TypeEtatDeclaration dernierEtat = getDernierEtat(q);
		boolean isRappelable = false;
		if (dernierEtat == TypeEtatDeclaration.EMISE) {
			if (q.getDelaiAccordeAu() == null || RegDate.get().isAfter(q.getDelaiAccordeAu())) {
				isRappelable = true;
			}
		}
		return isRappelable;
	}

	public boolean isRappelable() {
		return rappelable;
	}
}
