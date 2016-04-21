package ch.vd.uniregctb.evenement.organisation.interne.etablissement;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
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
			closeEtablissement(aFermer, dateAvant, warnings, suivis);
		}
		for (SiteOrganisation aCreer : sitesACreer) {
			/*
				Ne pas créer les établissements secondaires non succursales
			 */
			if (!aCreer.isSuccursale(getDateEvt())) {
				suivis.addSuivi(String.format("L'établissement secondaire civil %d n'est pas une succursale ou est une succursale radiée du RC et ne sera donc pas créé dans Unireg.", aCreer.getNumeroSite()));
				raiseStatusTo(HandleStatus.TRAITE);
				continue;
			}

			// Contrôle du cas ou on va crée un établissement existant mais qu'on ne connaissait pas. On le crée quand même mais on avertit.
			Domicile ancienDomicile = aCreer.getDomicile(dateAvant);
			if (ancienDomicile != null) {
				warnings.addWarning(String.format("Vérification manuelle requise: l'établissement secondaire (n°%d civil) est préexistant au civil (depuis le %s) mais inconnu d'Unireg à ce jour. " +
						                                  "La date du rapport entre tiers (%s) doit probablement être ajustée à la main.",
				                                  aCreer.getNumeroSite(), aCreer.connuAuCivilDepuis(), dateApres));
			}

			// Vérifier que le site à créer n'existe pas déjà.
			final Etablissement etablissement = getContext().getTiersDAO().getEtablissementByNumeroSite(aCreer.getNumeroSite());
			if (etablissement == null) {
				addEtablissementSecondaire(aCreer, dateApres, warnings, suivis);
			} else {
				suivis.addSuivi(String.format("Nouvel établissement secondaire civil %d déjà connu de Unireg en tant que tiers %s. Ne sera pas créé.",
				                              aCreer.getNumeroSite(), FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())));
			}
		}
		for (EtablissementsSecondaires.Demenagement demenagement : demenagements) {
			signaleDemenagement(demenagement.etablissement, demenagement.getAncienDomicile(), demenagement.getNouveauDomicile(), demenagement.getDate(), suivis);
		}

		adapteForsSecondairesPourEtablissementsVD(getEntreprise(), null, warnings, suivis);
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
