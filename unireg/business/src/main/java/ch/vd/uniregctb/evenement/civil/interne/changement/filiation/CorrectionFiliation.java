package ch.vd.uniregctb.evenement.civil.interne.changement.filiation;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.interne.changement.ChangementBase;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

public class CorrectionFiliation extends ChangementBase {

	protected CorrectionFiliation(EvenementCivilRegPP evenement, EvenementCivilContext context, CorrectionFiliationTranslationStrategy handler, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Testing only
	 */
	public CorrectionFiliation(Individu individu, Individu conjoint, RegDate dateEvenement,
	                           Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, dateEvenement, numeroOfsCommuneAnnonce, context);
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

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		Audit.info(getNumeroEvenement(), String.format("Correction de filiation de l'individu : %d", getNoIndividu()));

		// [SIFISC-855] Il faut invalider les caches des individus civils parents de cet individu
		final List<RelationVersIndividu> parents = getIndividu().getParents();
		if (parents != null) {
			for (RelationVersIndividu parent : parents) {
				context.getDataEventService().onIndividuChange(parent.getNumeroAutreIndividu());
			}
		}

		//les événements de correction de filiation n'ont aucun impact sur le fiscal ==> rien à faire
		return HandleStatus.TRAITE;
	}
}
