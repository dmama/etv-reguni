package ch.vd.unireg.evenement.organisation.interne.formejuridique;

import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterneInformationPure;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

import static ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;

/**
 * @author Raphaël Marmier, 2015-10-15
 */
public class ChangementNeutreFormeJuridique extends EvenementEntrepriseInterneInformationPure {

	private final TypeInformationComplementaire typeInfo;

	protected ChangementNeutreFormeJuridique(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile,
	                                         Entreprise entreprise, EvenementEntrepriseContext context,
	                                         EvenementEntrepriseOptions options) {
		super(evenement, entrepriseCivile, entreprise, context, options);
		typeInfo = TypeInformationComplementaire.CHANGEMENT_FORME_JURIDIQUE_MEME_CATEGORIE;
	}

	@Override
	public String describe() {
		return "Changement de forme juridique sans changement de régime fiscal.";
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		String message = String.format("Envoi d'un événement d'information: %s. Changement neutre de forme juridique. Entreprise n°%s (civil: %d.",
		                               typeInfo.toString(), FormatNumeroHelper.numeroCTBToDisplay(getEntreprise().getNumero()), getNoEntrepriseCivile());
		emetEvtFiscalInformation(getDateEvt(), getEntreprise(), typeInfo, message, suivis);
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {

	}
}
