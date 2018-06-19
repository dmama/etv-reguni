package ch.vd.unireg.evenement.organisation.interne.transformation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.organisation.interne.TraitementManuel;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeDissolutionRC;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Evénements portant sur la fusion et la scission.
 *
 * @author Raphaël Marmier, 2016-02-18.
 */
public class DissolutionStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public DissolutionStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event un événement entreprise civile reçu de RCEnt
	 */
	@Override
	public EvenementEntrepriseInterne matchAndCreate(EvenementEntreprise event, final EntrepriseCivile entrepriseCivile, Entreprise entreprise) throws EvenementEntrepriseException {
		if (entreprise == null) {
			return null;
		}

		final RegDate dateApres = event.getDateEvenement();

		final DonneesRC donneesRC = entrepriseCivile.getEtablissementPrincipal(dateApres).getPayload().getDonneesRC();
		final InscriptionRC inscriptionRC = donneesRC.getInscription(dateApres);
		final RaisonDeDissolutionRC raisonDeDissolution = inscriptionRC != null ? inscriptionRC.getRaisonDissolutionVD() : null;
		if (raisonDeDissolution != null) {
			switch (raisonDeDissolution) {
			case FUSION:
			case LIQUIDATION:
			case FAILLITE:
			case TRANSFORMATION:
			case CARENCE_DANS_ORGANISATION:
				Audit.info(event.getId(), "Dissolution de l'entreprise détectée");
				return new Dissolution(event, entrepriseCivile, entreprise, context, options);
			default:
				return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, String.format("Type de dissolution inconnu: %s", raisonDeDissolution));
			}
		}
		return null;
	}
}
