package ch.vd.uniregctb.evenement.organisation.interne;

import java.util.ArrayList;
import java.util.List;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Classe utile pour les evenements donnant lieu a la creation de plusieurs
 * evenements internes
 */
public final class EvenementOrganisationInterneComposite extends EvenementOrganisationInterne {

	private List<EvenementOrganisationInterne> listEvtOrganisation;

	public EvenementOrganisationInterneComposite(EvenementOrganisation evenement,
	                                             Organisation organisation,
	                                             Entreprise entreprise, 
	                                             EvenementOrganisationContext context,
	                                             EvenementOrganisationOptions options,
	                                             List<EvenementOrganisationInterne> listEvtOrganisation) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
		if (listEvtOrganisation == null) {
			throw new NullPointerException("Impossible de construire un événement composite sans une liste d'événements le composant");
		}
		if (listEvtOrganisation.size() < 2) {
			throw new IllegalArgumentException("Un événement composite doit être constitué d'au moins 2 événements");
		}
		this.listEvtOrganisation = listEvtOrganisation;
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		for (EvenementOrganisationInterne evt : listEvtOrganisation) {
			raiseStatusTo(evt.handle(warnings,suivis));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final EvenementOrganisationInterne seulementEvenementsFiscaux() throws EvenementOrganisationException {
		/*
		 Notre cas est un peu plus compliqué, car on délègue. On ne doit renvoyer un nouvel evenement
		 composite avec uniquemenent ceux qui envoient uniquement des evts fiscaux.
		  */
		List<EvenementOrganisationInterne> listFiltree = new ArrayList<>();
		for (EvenementOrganisationInterne evtInterne : this.listEvtOrganisation) {
			EvenementOrganisationInterne e = evtInterne.seulementEvenementsFiscaux();
			if (e != null) {
				listFiltree.add(e);
			}
		}
		if (listFiltree.isEmpty()) {
			return null;
		} else if (listFiltree.size() == 1) {
			return listFiltree.get(0);
		}
		return new EvenementOrganisationInterneComposite(getEvenement(), getOrganisation(), getEntreprise(), getContext(), getOptions(), listFiltree);
	}

	@Override
	protected void validateCommon(EvenementOrganisationErreurCollector erreurs) {
		for (EvenementOrganisationInterne evt : listEvtOrganisation) {
			evt.validateCommon(erreurs);
		}
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		for (EvenementOrganisationInterne evt : listEvtOrganisation) {
			evt.validateSpecific(erreurs, warnings, suivis);
		}
	}
}
