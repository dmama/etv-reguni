package ch.vd.uniregctb.evenement.organisation.interne.etablissement;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
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

	private List<Etablissement> etablissementsPresentsEtPasses;

	private List<Etablissement> etablissementsAFermer;
	private List<SiteOrganisation> sitesACreer;

	private List<EtablissementsSecondaires.Demenagement> demenagements;

	public EtablissementsSecondaires(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                 EvenementOrganisationContext context,
	                                 EvenementOrganisationOptions options,
	                                 List<Etablissement> etablissementsAFermer,
	                                 List<SiteOrganisation> sitesACreer,
	                                 List<EtablissementsSecondaires.Demenagement> demenagements) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		dateApres = evenement.getDateEvenement();
		dateAvant = dateApres.getOneDayBefore();

		this.etablissementsPresentsEtPasses = context.getTiersService().getEtablissementsSecondairesEntrepriseSansRange(entreprise);

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
			if (siteQuiFerme.isInscritAuRC(dateApres)) {
				RegDate dateRadiation = siteQuiFerme.getDateRadiationRC(dateApres); // Vaudois ou HC, il doit être radié au niveau global.

				// exception APM

				if (dateRadiation == null) {
					throw new EvenementOrganisationException(
							String.format("Impossible de déterminer la date de fin d'activité de l'établissement n°%d: la date de radiation au RC CH n'est pas disponible.",
							              siteQuiFerme.getNumeroSite())
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
				suivis.addSuivi(String.format("L'établissement secondaire civil %d est hors canton et ne sera donc pas créé dans Unireg.",
				                              aCreer.getNumeroSite()));
				continue;
			}
			/*
				Ne pas créer les établissements secondaires non succursales
			 */
			if (!aCreer.isSuccursale(getDateEvt())) {
				suivis.addSuivi(String.format("L'établissement secondaire civil %d n'est pas une succursale ou est une succursale radiée du RC et ne sera donc pas créé dans Unireg.",
				                              aCreer.getNumeroSite()));
				raiseStatusTo(HandleStatus.TRAITE);
				continue;
			}

			// Vérifier que le site à créer n'existe pas déjà.
			final Etablissement etablissement = getContext().getTiersDAO().getEtablissementByNumeroSite(aCreer.getNumeroSite());
			if (etablissement == null) {
				RegDate dateCreation = dateApres;
				if (aCreer.isInscritAuRC(dateApres)) {
					final RegDate dateInscriptionRCVd = aCreer.getDateInscriptionRCVd(dateApres);
					if (dateInscriptionRCVd != null) {
						dateCreation = dateInscriptionRCVd;
					} else {
						dateCreation = aCreer.getDateInscriptionRC(dateApres);
					}
				}
				addEtablissementSecondaire(aCreer, dateCreation, warnings, suivis);
				final Etablissement nouvelEtablissement = getContext().getTiersDAO().getEtablissementByNumeroSite(aCreer.getNumeroSite());
				if (dateCreation.isBefore(dateApres)) {
					appliqueDonneesCivilesSurPeriode(nouvelEtablissement, new DateRangeHelper.Range(dateCreation, dateApres.getOneDayBefore()), dateApres, warnings, suivis);
				}
				// Contrôle du cas ou on va crée un établissement existant mais qu'on ne connaissait pas. On le crée quand même mais on avertit.
				String ancienNom = aCreer.getNom(dateAvant);
				if (ancienNom != null) {
					warnings.addWarning(String.format("Vérification manuelle requise: l'établissement secondaire (n°%d civil) est préexistant au civil (depuis le %s) mais inconnu d'Unireg à ce jour. " +
							                                  "La date du rapport entre tiers (%s) doit probablement être ajustée à la main.",
					                                  aCreer.getNumeroSite(), RegDateHelper.dateToDisplayString(aCreer.connuAuCivilDepuis()), RegDateHelper.dateToDisplayString(dateApres)));
				}

			} else {
				suivis.addSuivi(String.format("Nouvel établissement secondaire civil n°%d déjà connu de Unireg en tant que tiers n°%s. Ne sera pas créé.",
				                              aCreer.getNumeroSite(), FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())));
			}
		}
		for (Demenagement demenagement : demenagements) {
			RegDate dateDemenagement = dateApres;
			final Domicile ancienDomicile = demenagement.getAncienDomicile();
			final Domicile nouveauDomicile = demenagement.getNouveauDomicile();
			/* Départ VD */
			if (ancienDomicile.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD
					&& nouveauDomicile.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				throw new EvenementOrganisationException("Le déménagement HC/HS d'une succursale n'est pas censé se produire.");
			}
			/* Arrivee HC */
			else if (ancienDomicile.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD
					&& nouveauDomicile.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				throw new EvenementOrganisationException("L'arrivée HC/HS d'une succursale n'est pas censé se produire.");
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
			signaleDemenagement(demenagement.etablissement, demenagement.getAncienDomicile(), demenagement.getNouveauDomicile(), dateDemenagement, suivis);
		}

		adapteForsSecondairesPourEtablissementsVD(getEntreprise(), null, warnings, suivis);

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
			Assert.isTrue(aFermer.getNumero() != null, "L'établissement secondaire ne peut être fermé: il n'existe pas en base.");
			Assert.isTrue(aFermer.getAnnulationDate() == null, "L'établissement secondaire ne peut être fermé: il est annulé.");
			Assert.isTrue(aFermer.isConnuAuCivil(), "L'établissement secondaire ne peut être fermé: il n'est pas connu au civil.");
			final RapportEntreTiers rapportSujet = aFermer.getRapportObjetValidAt(dateApres, TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE);
			Assert.notNull(rapportSujet, "L'établissement secondaire ne peut être fermé: il n'y a déjà plus de rapport à la date demandée.");
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
		private final Domicile ancienDomicile;
		private final Domicile nouveauDomicile;
		private final RegDate date;

		public Demenagement(Etablissement etablissement, Domicile ancienDomicile, Domicile nouveauDomicile, RegDate date) {
			this.etablissement = etablissement;
			this.ancienDomicile = ancienDomicile;
			this.nouveauDomicile = nouveauDomicile;
			this.date = date;
		}

		public Etablissement getEtablissement() {
			return etablissement;
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
