package ch.vd.unireg.evenement.organisation.interne.donneeinvalide;

import java.util.EnumSet;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.organisation.interne.TraitementManuel;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Evénements portant sur une succursale au RC rapportée de manière erronée comme une entreprise par RCEnt.
 *
 * @author Raphaël Marmier, 2016-04-12.
 */
public class FormeJuridiqueInvalideStrategy extends AbstractEntrepriseStrategy {

	private static final Set<FormeLegale> FORMES_LEGALES_INVALIDES = EnumSet.of(
																				FormeLegale.N_0111_FILIALE_ETRANGERE_AU_RC,
	                                                                            FormeLegale.N_0113_FORME_JURIDIQUE_PARTICULIERE,
	                                                                            FormeLegale.N_0118_PROCURATIONS_NON_COMMERCIALES,
	                                                                            FormeLegale.N_0119_CHEF_INDIVISION,
	                                                                            FormeLegale.N_0151_SUCCURSALE_SUISSE_AU_RC, // Erreur de données dans RCEnt, établissement secondaire présenté comme établissement principal.
	                                                                            FormeLegale.N_0312_FILIALE_ETRANGERE_NON_AU_RC
	);

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public FormeJuridiqueInvalideStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		super(context, options);
	}


	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event un événement entreprise civile reçu de RCEnt
	 */
	@Override
	public EvenementEntrepriseInterne matchAndCreate(EvenementEntreprise event, final EntrepriseCivile entrepriseCivile, Entreprise entreprise) throws EvenementEntrepriseException {
		final RegDate dateApres = event.getDateEvenement();

		final FormeLegale formeLegale = entrepriseCivile.getFormeLegale(dateApres);
		if (formeLegale == null) {
			Audit.info(event.getId(), "La forme juridique (LegalForm) est absente des données civiles.");
			return null;
		}

		if (FORMES_LEGALES_INVALIDES.contains(formeLegale)) {
			final String message;
			if (entreprise == null) {
				message = String.format("L'entreprise civile n°%d, nom: '%s', possède dans RCEnt une forme juridique non-acceptée par Unireg. Elle ne peut aboutir à la création d'un contribuable.",
				                        entrepriseCivile.getNumeroEntreprise(),
				                        entrepriseCivile.getNom(dateApres));
			}
			else {
				message = String.format("L'entreprise civile n°%d, nom: '%s', possède dans RCEnt une forme juridique non-acceptée par Unireg. Elle est pourtant associée à l'entreprise n°%s. Ce cas doit être corrigé.",
				                        entrepriseCivile.getNumeroEntreprise(),
				                        entrepriseCivile.getNom(dateApres),
				                        FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
			}
			Audit.info(event.getId(), message);
			return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
		}

		return null;
	}
}
