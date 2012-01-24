package ch.vd.uniregctb.evenement.civil.interne.changement.filiation;

import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.changement.ChangementBase;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.RelationVersIndividu;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class CorrectionFiliation extends ChangementBase {

	protected CorrectionFiliation(EvenementCivilRegPP evenement, EvenementCivilContext context, CorrectionFiliationTranslationStrategy handler, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Testing only
	 */
	public CorrectionFiliation(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate dateEvenement,
	                           Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, principalPPId, conjoint, conjointPPId, dateEvenement, numeroOfsCommuneAnnonce, context);
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PARENTS);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		//les événements de correction de filiation n'ont aucun impact sur le fiscal ==> rien à faire
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		Audit.info(getNumeroEvenement(), String.format("Correction de filiation de l'individu : %d", getNoIndividu()));

		// [SIFISC-855] Il faut invalider les caches des individus civils parents de cet individu
		final List<RelationVersIndividu> parents = getIndividu().getParents();
		if (parents != null) {
			for (RelationVersIndividu parent : parents) {
				context.getDataEventService().onIndividuChange(parent.getNumeroAutreIndividu());
			}
		}

		//les événements de correction de filiation n'ont aucun impact sur le fiscal ==> rien à faire
		return null;
	}
}
