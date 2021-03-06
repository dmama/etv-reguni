package ch.vd.unireg.evenement.civil.interne.correction.identification;

import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.engine.ech.EvenementCivilEchTranslationStrategy;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterneComposite;
import ch.vd.unireg.evenement.civil.interne.changement.dateNaissance.CorrectionDateNaissance;
import ch.vd.unireg.evenement.civil.interne.changement.identificateur.AnnulationIdentificateur;
import ch.vd.unireg.evenement.civil.interne.changement.identificateur.ChangementIdentificateur;
import ch.vd.unireg.evenement.civil.interne.changement.nom.ChangementNom;
import ch.vd.unireg.evenement.civil.interne.changement.sexe.ChangementSexe;
import ch.vd.unireg.type.ActionEvenementCivilEch;

public class CorrectionIdentificationTranslationStrategy implements EvenementCivilEchTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {

		final EvenementCivilInterne chgtIdentificateur =
				event.getAction() == ActionEvenementCivilEch.ANNULATION
						? new AnnulationIdentificateur(event, context, options)
						: new ChangementIdentificateur(event, context, options);

		return new EvenementCivilInterneComposite(
				event, context, options,
				chgtIdentificateur,
				new ChangementSexe(event, context, options),
				new CorrectionDateNaissance(event, context, options),
				new ChangementNom(event, context, options)
		);
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
		return true;
	}
}
