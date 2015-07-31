package ch.vd.uniregctb.evenement.civil.interne.changement.origine;

import java.util.Collection;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.interne.changement.ChangementBase;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.tiers.OriginePersonnePhysique;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class CorrectionOrigine extends ChangementBase {

	protected CorrectionOrigine(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	public CorrectionOrigine(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.ORIGINE);
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		final long noIndividu = getNoIndividu();

		Audit.info(getNumeroEvenement(), String.format("Changement d'origine pour l'individu : %d", noIndividu));

		final PersonnePhysique pp = getPrincipalPP();
		if (pp != null && !pp.isHabitantVD()) {
			final Individu individu = getIndividu();
			if (individu != null) {
				final OriginePersonnePhysique origine;
				final Collection<Origine> origines = individu.getOrigines();
				if (origines != null && !origines.isEmpty()) {
					final Origine first = origines.iterator().next();
					origine = new OriginePersonnePhysique(first.getNomLieu(), first.getSigleCanton());
				}
				else {
					origine = null;
				}
				pp.setOrigine(origine);
			}
		}
		return super.handle(warnings);
	}

	@Override
	protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		// rien à faire ici
	}
}
