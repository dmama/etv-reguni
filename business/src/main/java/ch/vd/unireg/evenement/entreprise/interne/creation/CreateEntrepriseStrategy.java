package ch.vd.unireg.evenement.entreprise.interne.creation;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.entreprise.interne.MessageSuiviPreExecution;
import ch.vd.unireg.evenement.entreprise.interne.TraitementManuel;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.Domicile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntrepriseStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public CreateEntrepriseStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne {@link CreateEntreprise} est
	 * pertinente.
	 * <p>
	 * Spécifications:
	 * - Ti01SE03-Identifier et traiter les mutations organisation.doc - Version 1.1 - 23.09.2015
	 *
	 * @param event un événement entreprise civile reçu de RCEnt
	 */
	@Override
	public EvenementEntrepriseInterne matchAndCreate(EvenementEntreprise event, final EntrepriseCivile entrepriseCivile, Entreprise entreprise) throws EvenementEntrepriseException {

		// On décide qu'on a affaire à une création uniquement selon la présence d'un tiers entreprise dans Unireg, et rien d'autre.
		if (entreprise != null) {
			return null;
		}

		final RegDate dateEvenement = event.getDateEvenement();

		final long numeroEntreprise = entrepriseCivile.getNumeroEntreprise();

		try {

			EtablissementCivil etablissementPrincipal = getEtablissementCivilPrincipal(entrepriseCivile, dateEvenement);

			/*
			    EntrepriseCivile hors canton
			  */
			final Domicile domicile = etablissementPrincipal.getDomicile(dateEvenement);
			if (domicile == null) {
				// Commune de siège non renseignée au civil. Selon toute probabilité l'entreprise civile se trouve hors canton.
				return handleSiegeVide(event, entrepriseCivile, context, options, dateEvenement, etablissementPrincipal);
			}

			final Commune communeDomicile = getCommuneByNumeroOfs(entrepriseCivile, etablissementPrincipal, context, dateEvenement, domicile);

			final FormeLegale formeLegale = entrepriseCivile.getFormeLegale(dateEvenement);

			// Traitement manuel pour les entreprises qui n'existent qu'au REE (ou autre registre non utile à la fiscalité).
			// Si la forme juridique est présente dans les données RCEnt, ne pas s'occuper des entreprises individuelles et les sociétés simples qui sont ignorées plus loin (SIFISC-25308).
			if (entrepriseCivile.getNumeroIDE(dateEvenement) == null && formeLegale != FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE && formeLegale != FormeLegale.N_0302_SOCIETE_SIMPLE) {
				final String message;

				if (entrepriseCivile.isInscriteAuRC(dateEvenement)) {
					message = String.format("Numéro IDE manquant pour l'entreprise %s (civil: n°%d), domiciliée à %s, pourtant inscrite au RC. Impossible de continuer.",
					                        entrepriseCivile.getNom(dateEvenement), numeroEntreprise, communeDomicile.getNomOfficielAvecCanton());
				}
				else if (entrepriseCivile.isInscriteIDE(dateEvenement)) {
					message = String.format("Numéro IDE manquant pour l'entreprise %s (civil: n°%d), domiciliée à %s, pourtant inscrite à l'IDE. Impossible de continuer.",
					                        entrepriseCivile.getNom(dateEvenement), numeroEntreprise, communeDomicile.getNomOfficielAvecCanton());
				}
				else {
					message = String.format("L'entreprise %s (civil: n°%d), domiciliée à %s, n'existe pas à l'IDE ni au RC. Pas de création automatique.",
					                        entrepriseCivile.getNom(dateEvenement), numeroEntreprise, communeDomicile.getNomOfficielAvecCanton());
				}
				Audit.info(event.getId(), message);
				return new TraitementManuel(event, entrepriseCivile, null, context, options, message);
			}

			// S'assurer qu'on a bien une forme juridique à ce stade.
			if (formeLegale == null) {
				final String message = String.format("L'entreprise civile n°%d n'a pas de forme juridique (legalForm). Pas de création.", numeroEntreprise);
				Audit.info(event.getId(), message);
				return null;
			}

			// Entreprises à ignorer
			if (formeLegale == FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE) {
				return handleRaisonIndividuelle(event, entrepriseCivile, context, options);
			}
			// SIFISC-19863 - Ignorer les sociétés simples
			else if (formeLegale == FormeLegale.N_0302_SOCIETE_SIMPLE) {
				return handleSocieteSimple(event, entrepriseCivile, context, options);
			}

			InformationDeDateEtDeCreation info;

			// Entreprises VD
			if (entrepriseCivile.hasEtablissementPrincipalVD(dateEvenement)) {

				// SIFISC-19723 Pour éviter les doublons lors de la mauvaise identification d'association/fondation créées à la main par l'ACI et simultanément enregistrée par SiTi,
				// pas de création automatique des association/fondation, sauf lorsque l'inscription provient du RC, qui dans ce cas est nécessairement l'institution émettrice.
				// SIFISC-21588 et SIFISC-19660: le traitement manuel s'impose car on ne peut établir automatiquement si l'association doit être créée ou non.
				if (entrepriseCivile.isAssociationFondation(dateEvenement) && !entrepriseCivile.isInscriteAuRC(dateEvenement)) {
					final String message = String.format("Pas de création automatique de l'association/fondation n°%d [%s] non inscrite au RC (risque de création de doublon). " +
							                                     "Veuillez vérifier et le cas échéant créer le tiers associé à la main.",
					                                     numeroEntreprise, entrepriseCivile.getNom(dateEvenement));
					Audit.info(event.getId(), message);
					return new TraitementManuel(event, entrepriseCivile, null, context, options, message);
				}

				final String message = String.format("Création du tiers entreprise pour l'entreprise civile vaudoise n°%s.", numeroEntreprise);
				Audit.info(event.getId(), message);
				info = extraireInformationDeDateEtDeCreation(event, entrepriseCivile);
				return new CreateEntrepriseVD(event, entrepriseCivile, null, context, options, info.getDateDeCreation(), info.getDateOuvertureFiscale(), info.isCreation());
			}
			// Entreprises hors VD avec présence VD
			else if (entrepriseCivile.hasEtablissementVD(dateEvenement)) {
				final List<EtablissementCivil> succursalesRCVD = entrepriseCivile.getSuccursalesRCVD(dateEvenement);
				// On ne crée l'entreprise que si elle a une présence vaudoise concrétisée par une succursale au RC VD active. Ceci pour éviter les établissements REE.
				if (succursalesRCVD.isEmpty()){
					final String message = String.format("L'entreprise civile n°%d n'a pas de succursale active au RC Vaud (inscrite et non radiée). Pas de création.", numeroEntreprise);
					Audit.info(event.getId(), message);
					return new MessageSuiviPreExecution(event, entrepriseCivile, null, context, options, message);
				}
				final String message = String.format("Création du tiers entreprise pour l'entreprise civile non-vaudoise n°%s avec succursale vaudoise active.", numeroEntreprise);
				Audit.info(event.getId(), message);
				info = extraireInformationDeDateEtDeCreation(event, entrepriseCivile);
				return new CreateEntrepriseHorsVD(event, entrepriseCivile, null, context, options, info.isCreation(), succursalesRCVD);
			}
			// Entreprises strictement hors VD
			final String message = String.format("L'entreprise civile n°%d n'a pas de présence sur Vaud. Pas de création.", numeroEntreprise);
			Audit.info(event.getId(), message);
			return new MessageSuiviPreExecution(event, entrepriseCivile, null, context, options, message);
		}
		catch (EvenementEntrepriseException e) {
			final String message = String.format("Une erreur est survenue lors de l'analyse de l'événement RCEnt: %s", e.getMessage());
			Audit.info(event.getId(), message);
			return new TraitementManuel(event, entrepriseCivile, null, context, options, message);
		}
	}

	@NotNull
	private EvenementEntrepriseInterne handleRaisonIndividuelle(EvenementEntreprise event, EntrepriseCivile entrepriseCivile, EvenementEntrepriseContext context, EvenementEntrepriseOptions options)
			throws EvenementEntrepriseException {
		final String message = String.format("L'entreprise civile n°%d est une entreprise individuelle %s. Pas de création.", entrepriseCivile.getNumeroEntreprise(), getQualificatifLieu(entrepriseCivile, event.getDateEvenement()));
		Audit.info(event.getId(), message);
		return new MessageSuiviPreExecution(event, entrepriseCivile, null, context, options, message);
	}

	@NotNull
	private String getQualificatifLieu(EntrepriseCivile entrepriseCivile, RegDate dateEvenement) {
		return entrepriseCivile.hasEtablissementPrincipalVD(dateEvenement) ? "vaudoise" : entrepriseCivile.hasEtablissementVD(dateEvenement) ? "hors canton avec présence sur VD" :  "strictement hors canton";
	}

	@NotNull
	private EvenementEntrepriseInterne handleSocieteSimple(EvenementEntreprise event, EntrepriseCivile entrepriseCivile, EvenementEntrepriseContext context, EvenementEntrepriseOptions options)
			throws EvenementEntrepriseException {
		final String message = String.format("L'entreprise civile n°%d est une société simple %s. Pas de création.", entrepriseCivile.getNumeroEntreprise(), getQualificatifLieu(entrepriseCivile, event.getDateEvenement()));
		Audit.info(event.getId(), message);
		return new MessageSuiviPreExecution(event, entrepriseCivile, null, context, options, message);
	}

	@NotNull
	private EvenementEntrepriseInterne handleSiegeVide(EvenementEntreprise event, EntrepriseCivile entrepriseCivile, EvenementEntrepriseContext context, EvenementEntrepriseOptions options,
	                                                   RegDate dateEvenement, EtablissementCivil etablissementPrincipal) throws EvenementEntrepriseException {
		final String message = String.format(
				"Autorité fiscale (siège) introuvable pour l'établissement civil principal %s de l'entreprise civile %s %s, en date du %s. Etablissement probablement à l'étranger. Impossible de créer le domicile de l'établissement principal.",
				etablissementPrincipal.getNumeroEtablissement(), entrepriseCivile.getNumeroEntreprise(), entrepriseCivile.getNom(dateEvenement), RegDateHelper.dateToDisplayString(dateEvenement));
		Audit.info(event.getId(), message);
		return new TraitementManuel(event, entrepriseCivile, null, context, options, message);
	}

	@NotNull
	private EtablissementCivil getEtablissementCivilPrincipal(EntrepriseCivile entrepriseCivile, RegDate dateEvenement) throws EvenementEntrepriseException {
		final DateRanged<EtablissementCivil> etablissementPrincipalRange = entrepriseCivile.getEtablissementPrincipal(dateEvenement);
		if (etablissementPrincipalRange == null) {
			final String message =
					String.format("Etablissement civil principal introuvable pour l'entreprise civile n°%d en date du %s.", entrepriseCivile.getNumeroEntreprise(), RegDateHelper.dateToDisplayString(dateEvenement));
			throw new EvenementEntrepriseException(message);
		}
		return etablissementPrincipalRange.getPayload();
	}

	@NotNull
	private Commune getCommuneByNumeroOfs(EntrepriseCivile entrepriseCivile, EtablissementCivil etablissement, EvenementEntrepriseContext context, RegDate dateEvenement, Domicile domicile) throws EvenementEntrepriseException {
		try {
			return context.getServiceInfra().getCommuneByNumeroOfs(domicile.getNumeroOfsAutoriteFiscale(), dateEvenement);
		}
		catch (ServiceInfrastructureException e) {
			final String message = String.format("Une erreur est survenue lors de la récupération des information de la commune n°%d (Ofs) de l'établissement civil n°%d de l'entreprise civile n°%d: %s",
			                                     domicile.getNumeroOfsAutoriteFiscale(), etablissement.getNumeroEtablissement(), entrepriseCivile.getNumeroEntreprise(), e.getMessage());
			throw new EvenementEntrepriseException(message);
		}
	}
}
