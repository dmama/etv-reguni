package ch.vd.uniregctb.evenement.organisation.interne.etablissement;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.EntreeJournalRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneDeTraitement;
import ch.vd.uniregctb.evenement.organisation.interne.HandleStatus;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * @author Raphaël Marmier, 2016-02-26
 */
public class EtablissementsSecondaires extends EvenementOrganisationInterneDeTraitement {

	private static final Logger LOGGER = LoggerFactory.getLogger(EtablissementsSecondaires.class);
	private final RegDate dateAvant;
	private final RegDate dateApres;

	private final List<Etablissement> etablissementsAFermer;
	private final List<SiteOrganisation> sitesACreer;

	private final List<EtablissementsSecondaires.Demenagement> demenagements;

	public EtablissementsSecondaires(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                 EvenementOrganisationContext context,
	                                 EvenementOrganisationOptions options,
	                                 List<Etablissement> etablissementsAFermer,
	                                 List<SiteOrganisation> sitesACreer,
	                                 List<EtablissementsSecondaires.Demenagement> demenagements) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		dateApres = evenement.getDateEvenement();
		dateAvant = dateApres.getOneDayBefore();

		this.etablissementsAFermer = etablissementsAFermer;
		this.sitesACreer = sitesACreer;

		this.demenagements = demenagements;
	}

	@Override
	public String describe() {
		return "Changement dans les établissements secondaires";
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		for (Etablissement aFermer : etablissementsAFermer) {
			SiteOrganisation siteQuiFerme = getOrganisation().getSiteForNo(aFermer.getNumeroEtablissement());
			RegDate dateFermeture = dateApres;

			/* Si l'établissement est inscrit au RC, c'est la date de radiation du RC qui nous intéresse. A certaines conditions. */
			if (siteQuiFerme.isConnuInscritAuRC(dateApres)) {
				RegDate dateRadiation = siteQuiFerme.getDateRadiationRC(dateApres); // Vaudois ou HC, il doit être radié au niveau global.

				// exception APM

				if (dateRadiation == null) {
					throw new EvenementOrganisationException(
							String.format("Impossible de déterminer la date de fin d'activité de l'établissement n°%d (%s): la date de radiation au RC CH n'est pas disponible.",
							              siteQuiFerme.getNumeroSite(),
							              afficheAttributsSite(siteQuiFerme, dateApres))
					);
				}
				/* A-t-on à faire à une véritable fin d'activité, ou sommes-nous dans un cas de radiation autorisée? */
				if (dateRadiation.isAfterOrEqual(dateApres.addDays( - OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC))) {
					dateFermeture = dateRadiation;
				}
			}
			closeEtablissement(aFermer, dateFermeture, warnings, suivis);
		}
		for (SiteOrganisation aCreer : sitesACreer) {
			final Domicile domicile = aCreer.getDomicile(dateApres);
			/* On ne traite que des établissements VD. SIFISC-19086 */
			if (domicile.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				suivis.addSuivi(String.format("L'établissement secondaire civil n°%d (%s) est hors canton et ne sera donc pas créé dans Unireg.",
				                              aCreer.getNumeroSite(),
				                              afficheAttributsSite(aCreer, dateApres)));
				continue;
			}
			/*
				Ne pas créer les établissements secondaires non succursales RC actifs. C'est le critère pour éviter les établissements REE.
			 */
			if (!aCreer.isSuccursale(getDateEvt())) {
				suivis.addSuivi(String.format("L'établissement secondaire civil n°%d (%s) n'est pas une succursale ou est une succursale radiée du RC et ne sera donc pas créé dans Unireg.",
				                              aCreer.getNumeroSite(),
				                              afficheAttributsSite(aCreer, dateApres)));
				raiseStatusTo(HandleStatus.TRAITE);
				continue;
			}

			// Vérifier que le site à créer n'existe pas déjà.
			final Etablissement etablissement = getContext().getTiersDAO().getEtablissementByNumeroSite(aCreer.getNumeroSite());
			if (etablissement == null) {
				RegDate dateCreation = dateApres;
				if (aCreer.isConnuInscritAuRC(dateApres)) {
					final RegDate dateInscriptionRCVd = aCreer.getDateInscriptionRCVd(dateApres);
					if (dateInscriptionRCVd != null) {
						dateCreation = dateInscriptionRCVd;
					} else {
						dateCreation = aCreer.getDateInscriptionRC(dateApres);
					}
				}
				// Création de la surcharge corrective s'il y a lieu
				SurchargeCorrectiveRange surchargeCorrectiveRange = null;
				if (dateCreation.isBefore(getDateEvt())) {
					surchargeCorrectiveRange = new SurchargeCorrectiveRange(dateCreation, getDateEvt().getOneDayBefore());
				}
				// On a une surcharge corrective, vérifier avant de créer.
				if (surchargeCorrectiveRange != null) {
					if (surchargeCorrectiveRange.isAcceptable()) {
						final Etablissement nouvelEtablissement = addEtablissementSecondaire(aCreer, dateCreation, warnings, suivis);
						appliqueDonneesCivilesSurPeriode(nouvelEtablissement, surchargeCorrectiveRange, getDateEvt(), warnings, suivis);
					} else {
						String message = String.format("Refus de créer dans Unireg l'établissement n°%d (%s) dont la fondation / déménagement remonte à %s, %d jours avant la date de l'événement. La tolérance étant de %d jours. " +
								                               "Il y a probablement une erreur d'identification ou un problème de date.",
						                               aCreer.getNumeroSite(),
						                               afficheAttributsSite(aCreer, dateApres),
						                               RegDateHelper.dateToDisplayString(dateCreation),
						                               surchargeCorrectiveRange.getEtendue(),
						                               OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC);

						warnings.addWarning(message);
					}
				}
				// On n'a pas de surcharge corrective.
				else {
					addEtablissementSecondaire(aCreer, dateCreation, warnings, suivis);
				}

				// Contrôle du cas ou on va crée un établissement existant mais qu'on ne connaissait pas. On le crée quand même mais on avertit.
				String ancienNom = aCreer.getNom(dateAvant);
				if (ancienNom != null) {
					warnings.addWarning(String.format("Vérification manuelle requise: l'établissement secondaire n°%d (%s) est préexistant au civil (depuis le %s) mais inconnu d'Unireg à ce jour. " +
							                                  "La date du rapport entre tiers (%s) doit probablement être ajustée à la main.",
					                                  aCreer.getNumeroSite(),
					                                  afficheAttributsSite(aCreer, dateApres),
					                                  RegDateHelper.dateToDisplayString(aCreer.connuAuCivilDepuis()),
					                                  RegDateHelper.dateToDisplayString(dateCreation)));
				}

			} else {
				suivis.addSuivi(String.format("Nouvel établissement secondaire civil n°%d (%s) déjà connu de Unireg en tant que tiers n°%s. Ne sera pas créé.",
				                              aCreer.getNumeroSite(),
				                              afficheAttributsSite(aCreer, dateApres),
				                              FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())));
			}
		}
		for (Demenagement demenagement : demenagements) {

			final Domicile ancienDomicile = demenagement.getAncienDomicile();
			final Domicile nouveauDomicile = demenagement.getNouveauDomicile();
			/* Départ VD */
			if (ancienDomicile.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD
					&& nouveauDomicile.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				throw new EvenementOrganisationException(String.format("Le déménagement HC/HS d'une succursale [n°%d, %s] n'est pas censé se produire.",
				                                                       demenagement.getSite().getNumeroSite(),
				                                                       afficheAttributsSite(demenagement.getSite(), dateApres)
				));
			}
			/* Arrivee HC */
			else if (ancienDomicile.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD
					&& nouveauDomicile.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				throw new EvenementOrganisationException(String.format("L'arrivée HC/HS d'une succursale [n°%d, %s] n'est pas censé se produire.",
				                                                       demenagement.getSite().getNumeroSite(),
				                                                       afficheAttributsSite(demenagement.getSite(), dateApres)
				));
			}
			/* On ne traite que des établissements VD. SIFISC-19086 */
			if (ancienDomicile.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				continue;
			}
/* Si des fois ça peut quand même se produire, le code pour l'application des surcharges est là.
			SiteOrganisation quiDemenage = getOrganisation().getSiteForNo(demenagement.getEtablissement().getNumeroEtablissement());
			if (quiDemenage.isInscritAuRC(dateApres)) {

				if (ancienDomicile.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD
						&& !(nouveauDomicile.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)) {
					final RegDate dateRadiationRCVd = quiDemenage.getDateRadiationRCVd(dateApres);
					if (dateRadiationRCVd != null) {
						dateDemenagement = dateRadiationRCVd;
					} else {
						throw new EvenementOrganisationException(String.format("Date de radiation du RC VD introuvable pour l'établissement %d.",
						                                                       quiDemenage.getNumeroSite())
						);
					}
					if (dateDemenagement.isBefore(dateApres)) {
						appliqueDonneesCivilesSurPeriode(demenagement.getEtablissement(), new DateRangeHelper.Range(dateDemenagement, dateApres.getOneDayBefore()), dateApres, warnings, suivis);
					}
				}

				else if (!(ancienDomicile.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)
						&& nouveauDomicile.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					final RegDate dateInscriptionRCVd = quiDemenage.getDateInscriptionRCVd(dateApres);
					if (dateInscriptionRCVd != null) {
						dateDemenagement = dateInscriptionRCVd;
					} else {
						throw new EvenementOrganisationException(String.format("Date d'inscription au RC VD introuvable pour l'établissement %d.",
						                                                       quiDemenage.getNumeroSite())
						);
					}
					if (dateDemenagement.isBefore(dateApres)) {
						appliqueDonneesCivilesSurPeriode(demenagement.getEtablissement(), new DateRangeHelper.Range(dateDemenagement, dateApres.getOneDayBefore()), dateApres, warnings, suivis);
					}
				}
			}
*/
			// On peut y aller

			final SiteOrganisation site = demenagement.getSite();

			RegDate dateDemenagement = null;
			if (site.isConnuInscritAuRC(getDateEvt()) && !site.isRadieDuRC(getDateEvt())) {
				final List<EntreeJournalRC> entreesJournal = site.getDonneesRC().getEntreesJournalPourDatePublication(getDateEvt());
				if (entreesJournal.isEmpty()) {
					throw new EvenementOrganisationException(
							String.format("Entrée de journal au RC introuvable dans l'établissement n°%s (civil: %d, %s). Impossible de traiter le déménagement.",
							              FormatNumeroHelper.numeroCTBToDisplay(demenagement.getEtablissement().getNumero()),
							              site.getNumeroSite(),
							              afficheAttributsSite(site, dateApres)));
				}
				// On prend la première entrée qui vient car il devrait y en avoir qu'une seule. S'il devait vraiment y en avoir plusieurs, on considère qu'elles renverraient toutes vers le même jour.
				dateDemenagement = entreesJournal.iterator().next().getDate();
			} else {
				dateDemenagement = getDateEvt();
			}

			// Création de la surcharge corrective s'il y a lieu
			SurchargeCorrectiveRange surchargeCorrectiveRange = null;
			if (dateDemenagement.isBefore(getDateEvt())) {
				surchargeCorrectiveRange = new SurchargeCorrectiveRange(dateDemenagement, getDateEvt().getOneDayBefore());
			}
			// On a une surcharge corrective, vérifier avant d'ajouter la surcharge sur l'établissement.
			if (surchargeCorrectiveRange != null) {
				if (surchargeCorrectiveRange.isAcceptable()) {
					appliqueDonneesCivilesSurPeriode(demenagement.getEtablissement(), surchargeCorrectiveRange, getDateEvt(), warnings, suivis);
				} else {
					String message = String.format("Refus de créer une surcharge corrective pour l'établissement n°%d (%s) dont la date déménagement remonte à %s, %d jours avant la date de l'événement. La tolérance étant de %d jours. " +
							                               "Il y a probablement une erreur d'identification ou un problème de date.",
					                               site.getNumeroSite(),
					                               afficheAttributsSite(site, dateApres),
					                               RegDateHelper.dateToDisplayString(dateDemenagement),
					                               surchargeCorrectiveRange.getEtendue(),
					                               OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC);

					warnings.addWarning(message);
				}
			}

			// C'est un déménagement sur VD. On prend acte.
			signaleDemenagement(demenagement.etablissement, demenagement.getAncienDomicile(), demenagement.getNouveauDomicile(), dateDemenagement, suivis);
		}

		adapteForsSecondairesPourEtablissementsVD(getEntreprise(), warnings, suivis);

		raiseStatusTo(HandleStatus.TRAITE);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		/*
		 Erreurs techniques fatale
		  */
		Assert.notNull(dateAvant);
		Assert.notNull(dateApres);
		Assert.isTrue(dateAvant.equals(dateApres.getOneDayBefore()));

		// Vérifier qu'il y a bien une entreprise préexistante en base ? (Ca ne devrait pas se produire ici)
		Assert.notNull(getEntreprise());

		// Vérifier que les établissements à fermer existent bien, sont connus au civil, et ne sont pas annulés.
		for (Etablissement aFermer : etablissementsAFermer) {
			Assert.isTrue(aFermer.getNumero() != null, String.format("L'établissement secondaire n°%s ne peut être fermé: il n'existe pas en base.", FormatNumeroHelper.numeroCTBToDisplay(aFermer.getNumero())));
			Assert.isTrue(aFermer.getAnnulationDate() == null, String.format("L'établissement secondaire n°%s ne peut être fermé: il est annulé.", FormatNumeroHelper.numeroCTBToDisplay(aFermer.getNumero())));
			Assert.isTrue(aFermer.isConnuAuCivil(), String.format("L'établissement secondaire  n°%s ne peut être fermé: il n'est pas connu au civil.", FormatNumeroHelper.numeroCTBToDisplay(aFermer.getNumero())));
			final RapportEntreTiers rapportSujet = aFermer.getRapportObjetValidAt(dateApres, TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE);
			// SIFISC-19230: Le rapport peut avoir été fermé dans le cadre du processus complexe "Fin d'activité"
			//Assert.notNull(rapportSujet, "L'établissement secondaire ne peut être fermé: il n'y a déjà plus de rapport à la date demandée.");
		}
	}

	public RegDate getDateAvant() {
		return dateAvant;
	}

	public RegDate getDateApres() {
		return dateApres;
	}

	public List<Demenagement> getDemenagements() {
		return demenagements;
	}

	public List<SiteOrganisation> getSitesACreer() {
		return sitesACreer;
	}

	public List<Etablissement> getEtablissementsAFermer() {
		return etablissementsAFermer;
	}

	public static class Demenagement {
		private final Etablissement etablissement;
		private final SiteOrganisation site;
		private final Domicile ancienDomicile;
		private final Domicile nouveauDomicile;
		private final RegDate date;

		public Demenagement(Etablissement etablissement, SiteOrganisation site, Domicile ancienDomicile, Domicile nouveauDomicile, RegDate date) {
			this.etablissement = etablissement;
			this.site = site;
			this.ancienDomicile = ancienDomicile;
			this.nouveauDomicile = nouveauDomicile;
			this.date = date;
		}

		public Etablissement getEtablissement() {
			return etablissement;
		}

		public SiteOrganisation getSite() {
			return site;
		}

		public Domicile getAncienDomicile() {
			return ancienDomicile;
		}

		public Domicile getNouveauDomicile() {
			return nouveauDomicile;
		}

		public RegDate getDate() {
			return date;
		}
	}
}
