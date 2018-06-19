package ch.vd.unireg.evenement.organisation.interne;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

/**
 * <p>
 *     Evénement interne sans effet servant publier un message en tant que warning <strong>pendant</strong> l'exécution. Cet événement interne a pour conséquence
 *     l'ajout d'une ligne dans les messages affichés à l'utilisateur et de provoquer un état "à vérifier"
 * </p>
 * <p>
 *     Cette classe d'événement est utile pour communiquer avec l'utilisateur des informations importantes en lien avec
 *     l'exécution des événements internes. En particulier pour communiquer des informations supplémentaires qui
 *     one correspondent pas à des actions systèmes.
 * </p>
 * <p>
 *     Les messages sont ajoutés au cours de l'étape d'exécution [handle()]. De cette manière, les messages sont affichés à leur place dans la séquence
 *     des traitements.
 * </p>
 */
public class MessageWarning extends EvenementEntrepriseInterneDeTraitement {

	private String warning = null;

	public MessageWarning(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise, EvenementEntrepriseContext context,
	                      EvenementEntrepriseOptions options, String warning) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options);
		this.warning = warning;
	}

	@Override
	public String describe() {
		return null; // On ne veut pas de message descriptif sur cet événement qui n'en est pas un.
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		if (StringUtils.isNotBlank(warning)) {
			warnings.addWarning(warning);
		}
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
	}
}
