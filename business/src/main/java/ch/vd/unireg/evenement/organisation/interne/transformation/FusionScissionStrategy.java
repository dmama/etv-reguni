package ch.vd.unireg.evenement.organisation.interne.transformation;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.organisation.interne.TraitementManuel;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.PublicationBusiness;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Evénements portant sur la fusion et la scission.
 *
 * @author Raphaël Marmier, 2016-02-18.
 */
public class FusionScissionStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public FusionScissionStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event   un événement entreprise civile reçu de RCEnt
	 */
	@Override
	public EvenementEntrepriseInterne matchAndCreate(EvenementEntreprise event, final EntrepriseCivile entrepriseCivile, Entreprise entreprise) throws EvenementEntrepriseException {
		if (entreprise == null) {
			return null;
		}

		final RegDate dateApres = event.getDateEvenement();

		final List<PublicationBusiness> publicationBusinessesPourDate = entrepriseCivile.getEtablissementPrincipal(dateApres).getPayload().getPublications(event.getDateEvenement());
		if (publicationBusinessesPourDate != null && !publicationBusinessesPourDate.isEmpty()) {
			for (PublicationBusiness publication : publicationBusinessesPourDate) { // Partant du principe qu'un seul type de fusion ne peut avoir lieu sur un même jour, on renvoie le premier trouvé.
				if (publication.getTypeDeFusion() != null) {
					switch (publication.getTypeDeFusion()) {
					case FUSION_SOCIETES_COOPERATIVES:
					case FUSION_SOCIETES_ANONYMES:
					case FUSION_SOCIETES_ANONYMES_ET_COMMANDITE_PAR_ACTIONS:
					case AUTRE_FUSION:
					case FUSION_INTERNATIONALE:
					case FUSION_ART_25_LFUS:
					case FUSION_INSTITUTIONS_DE_PREVOYANCE:
					case FUSION_SUISSE_VERS_ETRANGER:
						Audit.info(event.getId(), "Fusion de l'entreprise détectée");
						return new Fusion(event, entrepriseCivile, entreprise, context, options);
					case SCISSION_ART_45_LFUS:
					case SCISSION_SUISSE_VERS_ETRANGER:
						Audit.info(event.getId(), "Sission de l'entreprise détectée");
						return new Scission(event, entrepriseCivile, entreprise, context, options);
					default:
						final String message = String.format("Type de fusion inconnu: %s", publication.getTypeDeFusion());
						Audit.info(event.getId(), message);
						return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
					}
				}
			}
		}
		return null;
	}
}
