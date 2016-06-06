package ch.vd.uniregctb.evenement.organisation.interne.demenagement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneComposite;
import ch.vd.uniregctb.evenement.organisation.interne.MessageWarning;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.utils.RangeUtil;

/**
 * @author Raphaël Marmier, 2015-09-02
 */
public class DemenagementSiegeStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(DemenagementSiegeStrategy.class);

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est pertinente.
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event,
	                                                   final Organisation organisation,
	                                                   Entreprise entreprise,
	                                                   EvenementOrganisationContext context,
	                                                   EvenementOrganisationOptions options) throws EvenementOrganisationException {

		// On vérifie qu'on a bien retrouvé l'entreprise concernée par ce type de changement
		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		MessageWarning messageWarning = null;

		Domicile communeDeSiegeAvant = organisation.getSiegePrincipal(dateAvant);
		final Domicile communeDeSiegeApres = organisation.getSiegePrincipal(dateApres);


		if (communeDeSiegeApres == null) {
			return new TraitementManuel(event, organisation, null, context, options,
			                            String.format(
					                            "Autorité fiscale (siège) introuvable l'organisation n°%s %s. On est peut-être en présence d'un déménagement vers l'étranger.",
					                            organisation.getNumeroOrganisation(), organisation.getNom(dateApres))
			);
		}

		if (communeDeSiegeAvant == null) {
			if (isExisting(organisation, dateApres)) {
				return new TraitementManuel(event, organisation, null, context, options,
				                            String.format(
						                            "Autorité fiscale (siège) introuvable l'organisation n°%s %s. On est peut-être en présence d'un déménagement depuis l'étranger.",
						                            organisation.getNumeroOrganisation(), organisation.getNom(dateApres))
				);
			} else {
				/*
				   L'organisation vient seulement d'être connue au civil (on n'a qu'une photo dans RCEnt)

				   Cette situation peut signifier:
				   - On a une entreprise associée dans Unireg, donc l'entreprise a déjà une existence avant l'événement (n'est pas en train d'être fondée).
				     Elle peut donc être:
				       a) en train d'arriver sur Vaud HC ou HS
				       b) en train d'ouvrir une nouvelle activité sur Vaud et dans ce cas, elle peut soit:
				         1. être en train de déménager HC (et HS?)
				         2. conserver son siège au même domicile
				   - L'entreprise est nouvelle et n'existait pas dans Unireg, mais elle a été créé avec le bouton "créer tiers associé".

				   Au vue de ce qui précède, on vérifie que les conditions suivantes ne sont PAS réalisées:
				   - Le siège précédant est vaudois et le nouveau siège l'est aussi.
				     -> sérieux doute sur le rapprochement de l'entreprise Unireg avec l'organisation RCEnt. Traitement manuel.
				   - Le siège précédant est vaudois et le nouveau ne l'est pas. On a un départ. C'est potentiellement valable, mais douteux car
				     RCEnt aurait dû connaître l'organisation depuis longtemps.
				     -> Doute sur le rapprochement de l'entreprise Unireg avec l'organisation RCEnt. Traitement manuel.
				   - l'entreprise est nouvelle (l'événement survient en fait à la fondation de l'entreprise).
				     -> Le rapprochement de l'entreprise Unireg avec l'organisation RCEnt est erroné. Traitement manuel.

				   Le problème est d'identifier correctement lorsque l'entreprise "existante" a en fait été créée au moyen du bouton "créer tiers associé".
				   En fait on y renonce: si l'entreprise trouvée dans Unireg n'est pas plus ancienne que NB_JOURS_TOLERANCE_DE_DECALAGE_RC jours,
				   on met l'événement en traitement manuel.

				   Passé ces vérifications, on peut comparer le siège venant d'Unireg (précédant) et le siège donné par RCEnt (nouveau) pour déterminer la
				   nature du déménagement, s'il y en a un.

				   Par sécurité, on met le traitement "à vérifier" avec un message.
 				 */

				// On peut déjà partager le monde en deux, ce qui existait avant le seuil, sur lequel on peut compter, et ce qui est récent
				// qui peut avoir été créé à la main.
				final RegDate newnessThresholdDate = dateApres.addDays( - OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC);
				final List<DateRanged<Etablissement>> etablissementsPrincipauxEntreprise = context.getTiersService().getEtablissementsPrincipauxEntreprise(entreprise);
				final DateRanged<Etablissement> etablissementPreexistant = DateRangeHelper.rangeAt(etablissementsPrincipauxEntreprise, newnessThresholdDate);
				if (etablissementPreexistant == null) {
					return new TraitementManuel(event, organisation, entreprise, context, options,
					                            String.format("Données RCEnt insuffisantes pour déterminer la situation de l'entreprise (une seule photo) alors qu'une entreprise est déjà présente dans Unireg depuis moins de %d jours. Entreprise créée à la main?",
					                                                   OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC));

				}

				try {
					final DateRanged<Etablissement> etablissementPrincipalRange = RangeUtil.getAssertLast(context.getTiersService().getEtablissementsPrincipauxEntreprise(entreprise), dateAvant);
					final Set<DomicileEtablissement> domiciles = etablissementPrincipalRange.getPayload().getDomiciles();
					final DateRange domicilePrincipal = RangeUtil.getAssertLast(new ArrayList<DateRange>(domiciles), dateApres);

					Integer noOfsAutoriteFiscale = ((DomicileEtablissement) domicilePrincipal).getNumeroOfsAutoriteFiscale();
					TypeAutoriteFiscale typeAutoriteFiscale = ((DomicileEtablissement) domicilePrincipal).getTypeAutoriteFiscale();

					communeDeSiegeAvant = new Domicile(domicilePrincipal.getDateDebut(), domicilePrincipal.getDateFin(), typeAutoriteFiscale, noOfsAutoriteFiscale);

					// Quelques garde fous (expliqués dans le commentaire plus haut)
					if (communeDeSiegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						if (communeDeSiegeAvant.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
							return new TraitementManuel(event, organisation, entreprise, context, options,
							                            String.format(
									                            "L'entreprise %s vaudoise est rattachée à une organisation qui n'était pas connu de RCEnt avant aujourd'hui. RCEnt aurait du déjà la connaître." +
											                            "Possible erreur d'identification / rattachement? Veuillez traiter le cas à la main.",
									                            FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())));
						} else {
							return new TraitementManuel(event, organisation, entreprise, context, options,
							                            String.format(
									                            "L'entreprise %s vaudoise est rattachée à une organisation vaudoise nouvelle dans RCEnt. Il faut " +
											                            "craindre une erreur d'identification / rattachement. Veuillez traiter le cas à la main.",
									                            FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())));
						}
					} else {
						// On est bien parti pour pouvoir utiliser les données Unireg. Un dernier contrôle sur le contenu de RCEnt pour vérifier qu'on n'a pas un cas avéré de création.
						try {
							final InformationDeDateEtDeCreation info = extraireInformationDeDateEtDeCreation(event, organisation);
							if (info.isCreation()) {
								return new TraitementManuel(event, organisation, entreprise, context, options,
								                            String.format(
										                            "L'entreprise %s vaudoise n'est pas rattachée à la bonne organisation RCEnt. L'organisation n°%d actuellement rattachée" +
												                            " est en cours de fondation et ne peut correspondre à l'entreprise %s. Une intervention est nécessaire.",
										                            FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
										                            organisation.getNumeroOrganisation(),
										                            FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())));
							}
							final String message = "Utilisation des données fiscales d'Unireg comme point de départ d'un éventuel déménagement.";
							Audit.info(message);
							messageWarning = new MessageWarning(event, organisation, entreprise, context, options, message);
						} catch (EvenementOrganisationException e) {
							return new TraitementManuel(event, organisation, entreprise, context, options, e.getMessage());
						}
					}
				} catch (RangeUtil.RangeUtilException e) {
					return new TraitementManuel(event, organisation, entreprise, context, options,
					                            String.format("Problème pour déterminer le siège précédant de l'entreprise: %s", e.getMessage()));
				}
			}
		}

		// Passé ce point on a forcément un déménagement

		EvenementOrganisationInterne aRenvoyer = null;
		if (communeDeSiegeAvant.getNoOfs() == communeDeSiegeApres.getNoOfs()) { // Pas un changement, pas de traitement
			LOGGER.info("Pas de changement d'autorité politique. La commune d'autorité fiscale reste no {}", communeDeSiegeAvant.getNoOfs());
			return null;
		}
		else if (isDemenagementVD(communeDeSiegeAvant, communeDeSiegeApres)) {
			LOGGER.info("Déménagement VD -> VD: commune {} vers commune {}.", communeDeSiegeAvant.getNoOfs(), communeDeSiegeApres.getNoOfs());
			aRenvoyer = new DemenagementVD(event, organisation, entreprise, context, options, communeDeSiegeAvant, communeDeSiegeApres);
		}
		else if (isDemenagementHC(communeDeSiegeAvant, communeDeSiegeApres)) {
			LOGGER.info("Déménagement HC -> HC: commune {} vers commune {}.", communeDeSiegeAvant.getNoOfs(), communeDeSiegeApres.getNoOfs());
			aRenvoyer = new DemenagementHC(event, organisation, entreprise, context, options, communeDeSiegeAvant, communeDeSiegeApres);
		}
		else if (isDepart(communeDeSiegeAvant, communeDeSiegeApres)) {
			LOGGER.info("Départ VD -> HC: commune {} vers commune {}.", communeDeSiegeAvant.getNoOfs(), communeDeSiegeApres.getNoOfs());
			aRenvoyer = new DemenagementDepart(event, organisation, entreprise, context, options, communeDeSiegeAvant, communeDeSiegeApres);
		}
		else if (isArrivee(communeDeSiegeAvant, communeDeSiegeApres)) {
			LOGGER.info("Arrivée HC -> VD: commune {} vers commune {}.", communeDeSiegeAvant.getNoOfs(), communeDeSiegeApres.getNoOfs());
			aRenvoyer = new DemenagementArrivee(event, organisation, entreprise, context, options, communeDeSiegeAvant, communeDeSiegeApres);
		}
		else {
			throw new EvenementOrganisationException(
					String.format("Il existe manifestement un type de siège qu'Unireg ne sait pas traiter. Type avant: %s. Type après: %s. Impossible de continuer.",
					              communeDeSiegeAvant.getTypeAutoriteFiscale(), communeDeSiegeApres.getTypeAutoriteFiscale()));
		}
		if (messageWarning != null) {
			List<EvenementOrganisationInterne> evtList = new ArrayList<>(2);
			evtList.add(messageWarning);
			evtList.add(aRenvoyer);
			return new EvenementOrganisationInterneComposite(event, organisation, entreprise, context, options, evtList);
		} else {
			return aRenvoyer;
		}
	}

	private boolean isDemenagementVD(Domicile siegeAvant, Domicile siegeApres) {
		return siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
				siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	private boolean isDemenagementHC(Domicile siegeAvant, Domicile siegeApres) {
		return siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC &&
				siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC;
	}

	private boolean isDepart(Domicile siegeAvant, Domicile siegeApres) {
		return siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
				(siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC || siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS);
	}

	private boolean isArrivee(Domicile siegeAvant, Domicile siegeApres) {
		return (siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC || siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) &&
				siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}
}
