package ch.vd.unireg.evenement.entreprise.interne.information;

import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterneInformationPure;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

import static ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;

/**
 * @author Raphaël Marmier, 2015-10-15
 */
public class InformationComplementaire extends EvenementEntrepriseInterneInformationPure {

	private final TypeInformationComplementaire typeInfo;

	protected InformationComplementaire(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile,
	                                    Entreprise entreprise, EvenementEntrepriseContext context,
	                                    EvenementEntrepriseOptions options, TypeInformationComplementaire typeInfo) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options);
		this.typeInfo = typeInfo;
	}

	public TypeInformationComplementaire getTypeInfo() {
		return typeInfo;
	}

	@Override
	public String describe() {
		return "Information complémentaire";
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		String message = String.format("Envoi d'un événement d'information: %s. Entreprise n°%s (civil: %d).",
		                               typeInfo.toString(), FormatNumeroHelper.numeroCTBToDisplay(getEntreprise().getNumero()), getNoEntrepriseCivile());
		emetEvtFiscalInformation(getDateEvt(), getEntreprise(), typeInfo, message, suivis);
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		if (typeInfo == null) {
			throw new IllegalArgumentException();
		}
	}
}
