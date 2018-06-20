package ch.vd.unireg.evenement.entreprise.interne.adresse;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.interfaces.entreprise.data.AdresseEffectiveRCEnt;
import ch.vd.unireg.interfaces.entreprise.data.AdresseLegaleRCEnt;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Changements d'adresses
 *
 * @author Raphaël Marmier, 2016-04-11.
 */
public class AdresseStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public AdresseStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
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

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		final DateRanged<EtablissementCivil> etablissementPrincipalAvantRange = entrepriseCivile.getEtablissementPrincipal(dateAvant);
		if (etablissementPrincipalAvantRange != null) {
			AdresseEffectiveRCEnt nouvelleAdresseEffective = null;
			AdresseLegaleRCEnt nouvelleAdresseLegale = null;

			EtablissementCivil etablissementPrincipalAvant = etablissementPrincipalAvantRange.getPayload();
			final AdresseEffectiveRCEnt adresseEffectiveAvant = etablissementPrincipalAvant.getDonneesRegistreIDE().getAdresseEffective(dateAvant);
			final AdresseLegaleRCEnt adresseLegaleAvant = etablissementPrincipalAvant.getDonneesRC().getAdresseLegale(dateAvant);

			final EtablissementCivil etablissementPrincipalApres = entrepriseCivile.getEtablissementPrincipal(dateApres).getPayload();
			final AdresseEffectiveRCEnt adresseEffectiveApres = etablissementPrincipalApres.getDonneesRegistreIDE().getAdresseEffective(dateApres);
			final AdresseLegaleRCEnt adresseLegaleApres = etablissementPrincipalApres.getDonneesRC().getAdresseLegale(dateApres);

			if (adresseEffectiveAvant != adresseEffectiveApres) {
				nouvelleAdresseEffective = adresseEffectiveApres;
			}
			if (adresseLegaleAvant != adresseLegaleApres) {
				nouvelleAdresseLegale = adresseLegaleApres;
			}

			if (nouvelleAdresseEffective != null || nouvelleAdresseLegale != null) {
				final String message = String.format("Changement d'adresse %s%s%s.",
				                                    nouvelleAdresseEffective != null ? "effective" : "",
				                                    nouvelleAdresseEffective != null && nouvelleAdresseLegale != null ? " et d'adresse " : "",
				                                    nouvelleAdresseLegale != null ? "légale" : "");
				Audit.info(event.getId(), message);
				return new Adresse(event, entrepriseCivile, entreprise, context, options, nouvelleAdresseEffective, nouvelleAdresseLegale);
			}
		}
		return null;
	}
}
