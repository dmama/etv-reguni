package ch.vd.unireg.evenement.organisation.interne.information;

import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

import static ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;

/**
 * @author Raphaël Marmier, 2015-10-15
 */
public class InformationComplementaireAVerifier extends InformationComplementaire {

	protected InformationComplementaireAVerifier(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile,
	                                             Entreprise entreprise, EvenementEntrepriseContext context,
	                                             EvenementEntrepriseOptions options, TypeInformationComplementaire typeInfo) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options, typeInfo);
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		super.doHandle(warnings, suivis);
		warnings.addWarning("Une vérification manuelle est requise pour contrôler la situation de faillite ou le transfert à l’étranger.");
	}

}
