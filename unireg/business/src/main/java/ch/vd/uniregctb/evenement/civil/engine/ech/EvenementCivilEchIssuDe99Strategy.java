package ch.vd.uniregctb.evenement.civil.engine.ech;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneComposite;
import ch.vd.uniregctb.evenement.civil.interne.changement.nom.ChangementNom;
import ch.vd.uniregctb.evenement.civil.interne.changement.origine.CorrectionOrigine;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

public class EvenementCivilEchIssuDe99Strategy implements EvenementCivilEchTranslationStrategy {

	private final TiersService tiersService;

	private static final EvenementCivilEchTranslationStrategy INDEXATION_ONLY = new IndexationPureTranslationStrategy();

	public EvenementCivilEchIssuDe99Strategy(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {

		// sur le principe, un eCH-99 ne devrait pas avoir d'impact fiscal -> une simple
		// ré-indexation suffit... SAUF pour ce qui concerne les anciens habitants pour lesquels certaines
		// données, modifiables par le eCH-99 (prénoms, origine) doivent être repris dans Unireg avant ré-indexation ;
		// seuls les noms (et prénoms, donc)et  l'origine

		event.setCommentaireTraitement("Evénement civil issu d'un eCH-0099 de commune.");

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(event.getNumeroIndividu());
		if (pp != null && !pp.isHabitantVD()) {
			Audit.info(event.getId(), "Evénement civil issu d'un eCH-0099 sur un ancien habitant");
			return new EvenementCivilInterneComposite(event, context, options,
					new ChangementNom(event, context, options),
					new CorrectionOrigine(event, context, options));
		}
		else {
			return INDEXATION_ONLY.create(event, context, options);
		}
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
		return true;
	}
}
