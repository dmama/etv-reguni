package ch.vd.unireg.evenement.civil.interne.divorce;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Traitement métier des événements de dissolution de partenariat.
 * 
 */
public class DissolutionPartenariatTranslationStrategy extends DivorceTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new Divorce(event, context, this, options);
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {

		// [SIFISC-66234]
		// Verification de l'état-civil de l'individu derrière l'évenement
		// Si l'individu est non-marié, c'est que son partenariat à été dissolu
		// judiciairement. Ce cas est identique à l'annulation de mariage, il
		// faut le traiter manuellement.
		final IndividuApresEvenement iae =  context.getServiceCivil().getIndividuAfterEvent(event.getId());
		if (iae != null && iae.getIndividu() != null) {
			final Individu individu = iae.getIndividu();
			final EtatCivil ec = individu.getEtatCivil(event.getDateEvenement());
			if (ec.getTypeEtatCivil() == TypeEtatCivil.NON_MARIE) {
				return new EvenementCivilInterne(event, context, options) {
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						throw new IllegalArgumentException("Le traitement n'aurait jamais dû arriver jusqu'ici !");
					}

					@Override
					protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						erreurs.addErreur("Situation de dissolution de partenariat pour motif annulation détectée. Veuillez effectuer cette opération manuellement.");
					}

					@Override
					protected boolean isContribuableObligatoirementConnuAvantTraitement() {
						return false;
					}
				};
			}
		}
		return super.create(event, context, options);
	}

}
