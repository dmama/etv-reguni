package ch.vd.unireg.evenement.entreprise.interne.demenagement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterneComposite;
import ch.vd.unireg.evenement.entreprise.interne.MessageWarning;
import ch.vd.unireg.evenement.entreprise.interne.TraitementManuel;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.Domicile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseHelper;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.utils.RangeUtil;

/**
 * @author Raphaël Marmier, 2015-09-02
 */
public class DemenagementSiegeStrategy extends AbstractEntrepriseStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(DemenagementSiegeStrategy.class);

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public DemenagementSiegeStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est pertinente.
	 *
	 * @param event un événement entreprise civile reçu de RCEnt
	 */
	@Override
	public EvenementEntrepriseInterne matchAndCreate(EvenementEntreprise event, final EntrepriseCivile entrepriseCivile, Entreprise entreprise) throws EvenementEntrepriseException {

		// On vérifie qu'on a bien retrouvé l'entreprise concernée par ce type de changement
		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		MessageWarning messageWarning = null;

		Domicile communeDeSiegeAvant = entrepriseCivile.getSiegePrincipal(dateAvant);
		final Domicile communeDeSiegeApres = entrepriseCivile.getSiegePrincipal(dateApres);


		if (communeDeSiegeApres == null) {
			final String message = String.format("Autorité fiscale (siège) introuvable l'entreprise civile n°%s %s. On est peut-être en présence d'un déménagement vers l'étranger.",
			                                     entrepriseCivile.getNumeroEntreprise(), entrepriseCivile.getNom(dateApres));
			context.audit.info(event.getId(), message);
			return new TraitementManuel(event, entrepriseCivile, null, context, options,
			                            message
			);
		}

		if (communeDeSiegeAvant == null) {
			if (isExisting(entrepriseCivile, dateApres)) {
				final String message = String.format("Autorité fiscale (siège) introuvable l'entreprise civile n°%s %s. On est peut-être en présence d'un déménagement depuis l'étranger.",
				                                     entrepriseCivile.getNumeroEntreprise(), entrepriseCivile.getNom(dateApres));
				context.audit.info(event.getId(), message);
				return new TraitementManuel(event, entrepriseCivile, null, context, options, message);
			}
			else {
				/*
				   L'entreprise civile vient seulement d'être connue au civil (on n'a qu'une photo dans RCEnt)

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
				     -> sérieux doute sur le rapprochement de l'entreprise Unireg avec l'entreprise civile RCEnt. Traitement manuel.
				   - Le siège précédant est vaudois et le nouveau ne l'est pas. On a un départ. C'est potentiellement valable, mais douteux car
				     RCEnt aurait dû connaître l'entreprise civile depuis longtemps.
				     -> Doute sur le rapprochement de l'entreprise Unireg avec l'entreprise civile RCEnt. Traitement manuel.
				   - l'entreprise est nouvelle (l'événement survient en fait à la fondation de l'entreprise).
				     -> Le rapprochement de l'entreprise Unireg avec l'entreprise civile RCEnt est erroné. Traitement manuel.

				   Le problème est d'identifier correctement lorsque l'entreprise "existante" a en fait été créée au moyen du bouton "créer tiers associé".
				   En fait on y renonce: si l'entreprise trouvée dans Unireg n'est pas plus ancienne que NB_JOURS_TOLERANCE_DE_DECALAGE_RC jours,
				   on met l'événement en traitement manuel.

				   Passé ces vérifications, on peut comparer le siège venant d'Unireg (précédant) et le siège donné par RCEnt (nouveau) pour déterminer la
				   nature du déménagement, s'il y en a un.

				   Par sécurité, on met le traitement "à vérifier" avec un message.
 				 */

				// On peut déjà partager le monde en deux, ce qui existait avant le seuil, sur lequel on peut compter, et ce qui est récent
				// qui peut avoir été créé à la main.
				final RegDate newnessThresholdDate = dateApres.addDays( - EntrepriseHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC);
				final List<DateRanged<Etablissement>> etablissementsPrincipauxEntreprise = context.getTiersService().getEtablissementsPrincipauxEntreprise(entreprise);
				final DateRanged<Etablissement> etablissementPreexistant = DateRangeHelper.rangeAt(etablissementsPrincipauxEntreprise, newnessThresholdDate);
				if (etablissementPreexistant == null) {
					final String message = String.format("Données RCEnt insuffisantes pour déterminer la situation de l'entreprise (une seule photo) " +
							                                     "alors qu'une entreprise est déjà présente dans Unireg depuis moins de %d jours. Entreprise créée à la main?",
					                                     EntrepriseHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC);
					context.audit.info(event.getId(), message);
					return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
				}

				try {
					final DateRanged<Etablissement> etablissementPrincipalRange = RangeUtil.getAssertLast(context.getTiersService().getEtablissementsPrincipauxEntreprise(entreprise), dateAvant);
					final Set<DomicileEtablissement> domiciles = etablissementPrincipalRange.getPayload().getDomiciles();
					final DomicileEtablissement domicilePrincipal = RangeUtil.getAssertLast(new ArrayList<>(domiciles), dateApres);
					if (domicilePrincipal == null) {
						final String message = String.format(
								"Fatal: Impossible de traiter l'événement de changement de  siege sociale n°%d: impossible d'avoir le dernier siége sociale  de l'entreprise %s pour faire la comparaison avec le nouveau! "
								, event.getNoEvenement(), FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())
						);
						LOGGER.error(message);
						throw new EvenementEntrepriseException(message);
					}

					Integer noOfsAutoriteFiscale = ((DomicileEtablissement) domicilePrincipal).getNumeroOfsAutoriteFiscale();
					TypeAutoriteFiscale typeAutoriteFiscale = ((DomicileEtablissement) domicilePrincipal).getTypeAutoriteFiscale();

					communeDeSiegeAvant = new Domicile(domicilePrincipal.getDateDebut(), domicilePrincipal.getDateFin(), typeAutoriteFiscale, noOfsAutoriteFiscale);

					// Quelques garde fous (expliqués dans le commentaire plus haut)
					if (communeDeSiegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						if (communeDeSiegeApres.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
							final String message = String.format("L'entreprise %s vaudoise est rattachée à une entreprise civile qui n'était pas connu de RCEnt avant aujourd'hui. RCEnt aurait du déjà la connaître." +
									                                     "Possible erreur d'identification / rattachement? Veuillez traiter le cas à la main.", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
							context.audit.info(event.getId(), message);
							return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
						}
						else {
							final String message = String.format("L'entreprise %s vaudoise est rattachée à une entreprise civile vaudoise nouvelle dans RCEnt. Il faut " +
									                                     "craindre une erreur d'identification / rattachement. Veuillez traiter le cas à la main.", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
							context.audit.info(event.getId(), message);
							return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
						}
					}
					else {
						// On est bien parti pour pouvoir utiliser les données Unireg. Un dernier contrôle sur le contenu de RCEnt pour vérifier qu'on n'a pas un cas avéré de création.
						try {
							final InformationDeDateEtDeCreation info = extraireInformationDeDateEtDeCreation(event, entrepriseCivile);
							if (info.isCreation()) {
								final String message = String.format("L'entreprise %s vaudoise n'est pas rattachée à la bonne entreprise civile RCEnt. L'entreprise civile n°%d actuellement rattachée" +
										                                     " est en cours de fondation et ne peut correspondre à l'entreprise %s. Une intervention est nécessaire.", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
								                                     entrepriseCivile.getNumeroEntreprise(), FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
								context.audit.info(event.getId(), message);
								return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
							}
							final String message = "Utilisation des données fiscales d'Unireg comme point de départ d'un éventuel déménagement.";
							context.audit.warn(message);
							messageWarning = new MessageWarning(event, entrepriseCivile, entreprise, context, options, message);
						}
						catch (EvenementEntrepriseException e) {
							final String message = e.getMessage();
							context.audit.error(message);
							return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
						}
					}
				}
				catch (RangeUtil.RangeUtilException e) {
					final String message = String.format("Problème pour déterminer le siège précédant de l'entreprise: %s", e.getMessage());
					context.audit.error(event.getId(), message);
					return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
				}
			}
		}

		// Passé ce point on a forcément un déménagement

		EvenementEntrepriseInterne aRenvoyer;
		if (Objects.equals(communeDeSiegeAvant.getNumeroOfsAutoriteFiscale(), communeDeSiegeApres.getNumeroOfsAutoriteFiscale())) { // Pas un changement, pas de traitement
			return null;
		}
		else if (isDemenagementVD(communeDeSiegeAvant, communeDeSiegeApres)) {
			context.audit.info(event.getId(), String.format("Déménagement VD -> VD: commune %d vers commune %d.", communeDeSiegeAvant.getNumeroOfsAutoriteFiscale(), communeDeSiegeApres.getNumeroOfsAutoriteFiscale()));
			aRenvoyer = new DemenagementVD(event, entrepriseCivile, entreprise, context, options, communeDeSiegeAvant, communeDeSiegeApres);
		}
		else if (isDemenagementHC(communeDeSiegeAvant, communeDeSiegeApres)) {
			context.audit.info(event.getId(), String.format("Déménagement HC -> HC: commune %d vers commune %d.", communeDeSiegeAvant.getNumeroOfsAutoriteFiscale(), communeDeSiegeApres.getNumeroOfsAutoriteFiscale()));
			aRenvoyer = new DemenagementHC(event, entrepriseCivile, entreprise, context, options, communeDeSiegeAvant, communeDeSiegeApres);
		}
		else if (isDepart(communeDeSiegeAvant, communeDeSiegeApres)) {
			context.audit.info(event.getId(), String.format("Départ VD -> HC: commune %d vers commune %d.", communeDeSiegeAvant.getNumeroOfsAutoriteFiscale(), communeDeSiegeApres.getNumeroOfsAutoriteFiscale()));
			aRenvoyer = new DemenagementDepart(event, entrepriseCivile, entreprise, context, options, communeDeSiegeAvant, communeDeSiegeApres);
		}
		else if (isArrivee(communeDeSiegeAvant, communeDeSiegeApres)) {
			context.audit.info(event.getId(), String.format("Arrivée HC -> VD: commune %d vers commune %d.", communeDeSiegeAvant.getNumeroOfsAutoriteFiscale(), communeDeSiegeApres.getNumeroOfsAutoriteFiscale()));
			aRenvoyer = new DemenagementArrivee(event, entrepriseCivile, entreprise, context, options, communeDeSiegeAvant, communeDeSiegeApres);
		}
		else {
			final String message = String.format("Il existe manifestement un type de siège qu'Unireg ne sait pas traiter. Type avant: %s. Type après: %s. Impossible de continuer.",
			                                     communeDeSiegeAvant.getTypeAutoriteFiscale(), communeDeSiegeApres.getTypeAutoriteFiscale());
			context.audit.error(event.getId(), message);
			throw new EvenementEntrepriseException(message);
		}

		if (messageWarning != null) {
			final List<EvenementEntrepriseInterne> evtList = Arrays.asList(messageWarning, aRenvoyer);
			return new EvenementEntrepriseInterneComposite(event, entrepriseCivile, entreprise, context, options, evtList);
		}
		else {
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
