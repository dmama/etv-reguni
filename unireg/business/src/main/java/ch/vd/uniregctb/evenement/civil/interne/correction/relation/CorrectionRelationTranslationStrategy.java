package ch.vd.uniregctb.evenement.civil.interne.correction.relation;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilEchTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

public class CorrectionRelationTranslationStrategy implements  EvenementCivilEchTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {

		throw new EvenementCivilException("Evenement à traiter manuellement");
	}

	private boolean isCorrectionConjoint(EvenementCivilEch event, EvenementCivilContext context) {
		final Long numeroIndividu = event.getNumeroIndividu();
		final RegDate dateEvenement = event.getDateEvenement();
		final Individu individuAvant = context.getServiceCivil().getIndividu(numeroIndividu, dateEvenement.getOneDayBefore(), AttributeIndividu.CONJOINTS);
		final Individu individuApres = context.getServiceCivil().getIndividu(numeroIndividu, dateEvenement, AttributeIndividu.CONJOINTS);
		List<RelationVersIndividu> lesConjointsAvant = individuAvant.getConjoints();
		List<RelationVersIndividu> lesConjointsApres = individuApres.getConjoints();
		if (isChangementDansRelationConjoint(lesConjointsAvant, lesConjointsApres)) {
			return true;
		}

		return false;
	}

	private boolean isChangementDansRelationConjoint(List<RelationVersIndividu> lesConjointsAvant, List<RelationVersIndividu> lesConjointsApres) {
		//nombre de relations différent
		if (lesConjointsAvant.size() != lesConjointsApres.size()) {
			return true;
		}

		//les 2 listes ont la même taille, on peut comaprer leurs éléments
		trierListeConjoints(lesConjointsAvant);
		trierListeConjoints(lesConjointsApres);
		
		for (int i = 0; i < lesConjointsAvant.size(); i++) {
			RelationVersIndividu relationVersIndividuAvant = lesConjointsAvant.get(i);
			RelationVersIndividu relationVersIndividuApres = lesConjointsApres.get(i);
			if (!relationVersIndividuAvant.equals(relationVersIndividuApres)) {
				return true;
			}
		}

		return false;
	}

	private void trierListeConjoints(List<RelationVersIndividu> lesConjoints) {
		Collections.sort(lesConjoints, new Comparator<RelationVersIndividu>() {
			@Override
			public int compare(RelationVersIndividu relationVersIndividu, RelationVersIndividu relationVersIndividu1) {
				return NullDateBehavior.EARLIEST.compare(relationVersIndividu.getDateDebut(), relationVersIndividu1.getDateDebut());
			}
		});
	}


	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilContext context) throws EvenementCivilException {
		return false;
	}

}
