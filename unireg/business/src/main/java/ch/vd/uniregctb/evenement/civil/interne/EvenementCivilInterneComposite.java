package ch.vd.uniregctb.evenement.civil.interne;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;

/**
 * Classe utile pour les evenements eCH donnant lieu a la creation de plusieurs
 * evenements internes
 */
public class EvenementCivilInterneComposite extends EvenementCivilInterne {

	private EvenementCivilInterne[] listEvtEch;

	public EvenementCivilInterneComposite(EvenementCivilEch evenement, EvenementCivilContext context, EvenementCivilOptions options, EvenementCivilInterne... listEvtEch) throws EvenementCivilException {
		super(evenement, context, options);
		if (listEvtEch == null) {
			throw new NullPointerException("Impossible de construire un événement composite sans une liste d'événements le composant");
		}
		if (listEvtEch.length < 2) {
			throw new IllegalArgumentException("Un événement composite doit être constitué d'au moins 2 événements");
		}
		this.listEvtEch = listEvtEch;
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		HandleStatus ret = HandleStatus.REDONDANT;
		for (EvenementCivilInterne evt : listEvtEch) {
			final HandleStatus hs = evt.handle(warnings);
			if (HandleStatus.TRAITE == hs) {
				ret = HandleStatus.TRAITE;
			}
		}
		return ret;
	}

	@Override
	protected void validateCommon(EvenementCivilErreurCollector erreurs) {
		for (EvenementCivilInterne evt : listEvtEch) {
			evt.validateCommon(erreurs);
		}
	}

	@Override
	protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		for (EvenementCivilInterne evt : listEvtEch) {
			evt.validateSpecific(erreurs, warnings);
		}
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		for (EvenementCivilInterne evt : listEvtEch) {
			evt.fillRequiredParts(parts);
		}
	}

	@Override
	protected boolean isContribuableObligatoirementConnuAvantTraitement() {
		for (EvenementCivilInterne evt : listEvtEch) {
			if (evt.isContribuableObligatoirementConnuAvantTraitement()) {
				return true;
			}
		}
		return false;
	}
}
