package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Stratégie applicable aux événements civils dont le traitement consiste en une ré-indexation seule
 */
public class IndexationPureTranslationStrategy implements EvenementCivilEchTranslationStrategy {

	private static final String MESSAGE_INDEXATION_PURE = "Événemement traité sans modification Unireg.";

	@Override
	public EvenementCivilInterne create(final EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new EvenementCivilInterne(event, context, options) {
			@NotNull
			@Override
			public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
				final PersonnePhysique pp = getPrincipalPP();
				if (pp != null) {
					context.getIndexer().schedule(pp.getNumero());
				}
				if (!StringUtils.isBlank(event.getCommentaireTraitement())) {
					event.setCommentaireTraitement(event.getCommentaireTraitement() + " " + MESSAGE_INDEXATION_PURE);
				} else {
					event.setCommentaireTraitement(MESSAGE_INDEXATION_PURE);
				}
				return HandleStatus.TRAITE;
			}

			@Override
			protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
				// rien à valider
			}

			@Override
			protected boolean isContribuableObligatoirementConnuAvantTraitement() {
				return false;
			}
		};
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilContext context) {
		return true;
	}
}
