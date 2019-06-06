package ch.vd.unireg.evenement.entreprise.interne;

import java.util.ArrayList;
import java.util.List;

import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Classe utile pour les evenements donnant lieu a la creation de plusieurs
 * evenements internes
 */
public final class EvenementEntrepriseInterneComposite extends EvenementEntrepriseInterne {

	private List<EvenementEntrepriseInterne> listEvtEntreprise;

	public EvenementEntrepriseInterneComposite(EvenementEntreprise evenement,
	                                           EntrepriseCivile entrepriseCivile,
	                                           Entreprise entreprise,
	                                           EvenementEntrepriseContext context,
	                                           EvenementEntrepriseOptions options,
	                                           List<EvenementEntrepriseInterne> listEvtEntreprise) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options);
		if (listEvtEntreprise == null) {
			throw new NullPointerException("Impossible de construire un événement composite sans une liste d'événements le composant");
		}
		if (listEvtEntreprise.size() < 2) {
			throw new IllegalArgumentException("Un événement composite doit être constitué d'au moins 2 événements");
		}
		this.listEvtEntreprise = listEvtEntreprise;
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		for (EvenementEntrepriseInterne evt : listEvtEntreprise) {
			raiseStatusTo(evt.handle(warnings,suivis));
		}
	}

	@Override
	public final EvenementEntrepriseInterne seulementEvenementsFiscaux() throws EvenementEntrepriseException {
		/*
		 Notre cas est un peu plus compliqué, car on délègue. On ne doit renvoyer un nouvel evenement
		 composite avec uniquemenent ceux qui envoient uniquement des evts fiscaux.
		  */
		List<EvenementEntrepriseInterne> listFiltree = new ArrayList<>();
		for (EvenementEntrepriseInterne evtInterne : this.listEvtEntreprise) {
			EvenementEntrepriseInterne e = evtInterne.seulementEvenementsFiscaux();
			if (e != null) {
				listFiltree.add(e);
			}
		}
		if (listFiltree.isEmpty()) {
			return null;
		} else if (listFiltree.size() == 1) {
			return listFiltree.get(0);
		}
		return new EvenementEntrepriseInterneComposite(getEvenement(), getEntrepriseCivile(), getEntreprise(), getContext(), getOptions(), listFiltree);
	}

	@Override
	protected void validateCommon(EvenementEntrepriseErreurCollector erreurs) {
		for (EvenementEntrepriseInterne evt : listEvtEntreprise) {
			evt.validateCommon(erreurs);
		}
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		for (EvenementEntrepriseInterne evt : listEvtEntreprise) {
			evt.validateSpecific(erreurs, warnings, suivis);
		}
	}
}
