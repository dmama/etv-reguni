package ch.vd.unireg.evenement.entreprise.interne.information;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.ComparisonHelper;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.tiers.Entreprise;

import static ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;

/**
 * Modification de capital à propager sans effet.
 *
 * @author Raphaël Marmier, 2015-11-02.
 */
public class ModificationStatutsStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public ModificationStatutsStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event un événement entreprise civile reçu de RCEnt
	 */
	@Override
	public EvenementEntrepriseInterne matchAndCreate(EvenementEntreprise event, final EntrepriseCivile entrepriseCivile, Entreprise entreprise) throws EvenementEntrepriseException {

		// On ne s'occupe que d'entités déjà connues
		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		RegDate statutsAvant = null;
		RegDate statutsApres = null;
		final DateRanged<EtablissementCivil> etablissementPrincipalAvantRange = entrepriseCivile.getEtablissementPrincipal(dateAvant);
		if (etablissementPrincipalAvantRange != null) {
			DateRanged<RegDate> statutsAvantDateRanged = DateRangeHelper.rangeAt(etablissementPrincipalAvantRange.getPayload().getDonneesRC().getDateStatuts(), dateAvant);
			if (statutsAvantDateRanged != null) {
				statutsAvant = statutsAvantDateRanged.getPayload();
			}
			final DateRanged<RegDate> statutsApresDateRanged = DateRangeHelper.rangeAt(entrepriseCivile.getEtablissementPrincipal(dateApres).getPayload().getDonneesRC().getDateStatuts(), dateApres);
			if (statutsApresDateRanged != null) {
				statutsApres = statutsApresDateRanged.getPayload();
			}
			if (!ComparisonHelper.areEqual(statutsAvant, statutsApres)) {
				Audit.info(event.getId(), "Modification des statuts de l'entreprise -> Propagation.");
				return new InformationComplementaire(event, entrepriseCivile, entreprise, context, options, TypeInformationComplementaire.MODIFICATION_STATUTS);
			}
		}
		return null;
	}
}
