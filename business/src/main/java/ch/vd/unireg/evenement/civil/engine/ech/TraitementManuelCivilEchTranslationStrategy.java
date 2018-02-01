package ch.vd.unireg.evenement.civil.engine.ech;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;

/**
 * Stratégie utilisable pour les événements civils eCH qui partent systématiquement en traitement manuel
 */
public class TraitementManuelCivilEchTranslationStrategy implements EvenementCivilEchTranslationStrategy {

	public static final String MSG = "Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.";

	@Override
	public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new EvenementCivilInterne(event, context, options) {

			@NotNull
			@Override
			public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
				throw new IllegalArgumentException("Le traitement n'aurait jamais dû arriver jusqu'ici !");
			}

			@Override
			protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
				erreurs.addErreur(MSG);
			}

			@Override
			protected boolean isContribuableObligatoirementConnuAvantTraitement() {
				return false;
			}
		};
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) {
		return false;
	}
}
