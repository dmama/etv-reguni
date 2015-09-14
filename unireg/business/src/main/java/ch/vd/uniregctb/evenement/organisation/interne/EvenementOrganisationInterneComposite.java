package ch.vd.uniregctb.evenement.organisation.interne;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Classe utile pour les evenements donnant lieu a la creation de plusieurs
 * evenements internes
 */
public class EvenementOrganisationInterneComposite extends EvenementOrganisationInterne {

	private List<EvenementOrganisationInterne> listEvtEch;

	public EvenementOrganisationInterneComposite(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,  EvenementOrganisationContext context, EvenementOrganisationOptions options, List<EvenementOrganisationInterne> listEvtEch) throws
			EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
		if (listEvtEch == null) {
			throw new NullPointerException("Impossible de construire un événement composite sans une liste d'événements le composant");
		}
		if (listEvtEch.size() < 2) {
			throw new IllegalArgumentException("Un événement composite doit être constitué d'au moins 2 événements");
		}
		this.listEvtEch = listEvtEch;
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		HandleStatus ret = HandleStatus.REDONDANT;
		for (EvenementOrganisationInterne evt : listEvtEch) {
			final HandleStatus hs = evt.handle(warnings);
			if (HandleStatus.TRAITE == hs) {
				ret = HandleStatus.TRAITE;
			}
		}
		return ret;
	}

	@Override
	protected void validateCommon(EvenementOrganisationErreurCollector erreurs) {
		for (EvenementOrganisationInterne evt : listEvtEch) {
			evt.validateCommon(erreurs);
		}
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		for (EvenementOrganisationInterne evt : listEvtEch) {
			evt.validateSpecific(erreurs, warnings);
		}
	}
}
