package ch.vd.uniregctb.evenement.organisation.interne.etablissement;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneDeTraitement;
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
				continue;
			}

			// Contrôle du cas ou on va crée un établissement existant mais qu'on ne connaissait pas. On le crée quand même mais on avertit.
			Domicile ancienDomicile = aCreer.getDomicile(dateAvant);
			if (ancienDomicile != null) {
				warnings.addWarning(String.format("Vérification manuelle requise: l'établissement secondaire (n°%s civil) est préexistant au civil mais inconnu d'Unireg à ce jour. " +
						                                  "La date du rapport entre tiers (%s) doit probablement être ajustée à la main.",
				                                  aCreer.getNumeroSite(), dateApres));
			}
			addEtablissementSecondaire(aCreer, dateApres, warnings, suivis);
		}
		for (EtablissementsSecondaires.Demenagement demenagement : demenagements) {
			signaleDemenagement(demenagement.etablissement, demenagement.getAncienDomicile(), demenagement.getNouveauDomicile(), demenagement.getDate(), suivis);
		}

		adapteForsSecondairesPourEtablissementsVD(getEntreprise(), null, warnings, suivis);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
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

		// Vérifier que les sites à créer n'existent pas déjà (la redondance, ce sera pour plus tard, c'est du travail)
		for (SiteOrganisation site : sitesACreer) {
			final Etablissement etablissement = getContext().getTiersDAO().getEtablissementByNumeroSite(site.getNumeroSite());
			Assert.isNull(etablissement, "L'établissement secondaire à créer existe déjà!");
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
