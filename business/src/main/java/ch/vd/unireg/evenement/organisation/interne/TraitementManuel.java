package ch.vd.unireg.evenement.organisation.interne;

import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

public class TraitementManuel extends EvenementEntrepriseInterneDeTraitement {

	private static final String DEFAULT_MSG = "Cette opération doit être effectuée manuellement.";

	private String message = DEFAULT_MSG;

	public TraitementManuel(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise, EvenementEntrepriseContext context,
	                        EvenementEntrepriseOptions options, String message) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options);
		this.message = message;
	}

	@Override
	public String describe() {
		return "Traitement manuel";
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		throw new IllegalStateException("Le traitement n'aurait jamais dû arriver jusqu'ici !");
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		erreurs.addErreur(message);
	}
}
