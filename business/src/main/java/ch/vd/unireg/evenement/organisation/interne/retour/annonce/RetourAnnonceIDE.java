package ch.vd.unireg.evenement.organisation.interne.retour.annonce;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseSupplementaire;
import ch.vd.unireg.common.ComparisonHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterneDeTraitement;
import ch.vd.unireg.evenement.organisation.interne.HandleStatus;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.organisation.data.AdresseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AdresseEffectiveRCEnt;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.tiers.DomicileHisto;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.FormeLegaleHisto;
import ch.vd.unireg.tiers.RaisonSocialeHisto;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.rattrapage.appariement.CandidatAppariement;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.utils.RangeUtil;

/**
 * Traitement du retour d'annonce à l'IDE. C'est-à-dire de l'annonce à l'IDE dont l'origine est notre propre annonce.
 *
 * @author Raphaël Marmier, 2016-09-22
 */
public class RetourAnnonceIDE extends EvenementOrganisationInterneDeTraitement {

	private final RegDate dateAvant;
	private final RegDate dateApres;

	private final AnnonceIDEEnvoyee annonceIDE;

	private final SiteOrganisation sitePrincipal;

	private final Etablissement etablissementPrincipal;

	public RetourAnnonceIDE(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                        EvenementOrganisationContext context,
	                        EvenementOrganisationOptions options,
	                        AnnonceIDEEnvoyee annonceIDE) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		dateApres = evenement.getDateEvenement();
		dateAvant = dateApres.getOneDayBefore();

		this.annonceIDE = annonceIDE;
		final ReferenceAnnonceIDE referenceAnnonceIDE = evenement.getReferenceAnnonceIDE();

		sitePrincipal = organisation.getSitePrincipal(dateApres).getPayload();
		etablissementPrincipal = referenceAnnonceIDE.getEtablissement();
	}

	@Override
	public String describe() {
		return "Retour d'annonce à l'IDE";
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		suivis.addSuivi(
				String.format(
						"Retour de l'annonce à l'IDE n°%s du %s concernant l'entreprise n°%s suite à création ou modification dans Unireg. L'état à l'IDE est maintenant aligné sur celui d'Unireg.",
						annonceIDE.getNumero(),
						DateHelper.dateTimeToDisplayString(annonceIDE.getDateAnnonce()),
						FormatNumeroHelper.numeroCTBToDisplay(getEntreprise().getNumero())
				)
		);

		final TiersService tiersService = getContext().getTiersService();

		// Rattacher ?
		if (getEntreprise().getNumeroEntreprise() == null) {
			// apparier et fermer les surcharges civiles
			tiersService.apparier(getEntreprise(), getOrganisation(), true);

			// Appariement sans fermeture de surcharge de l'établissement principal, car on doit pouvoir garder le domicile différent.
			etablissementPrincipal.setNumeroEtablissement(sitePrincipal.getNumeroSite());
			etablissementPrincipal.setIdentificationsEntreprise(null); // L'identifiant IDE est dès lors fourni par RCEnt.

			final List<CandidatAppariement> appariements = getContext().getAppariementService().rechercheAppariementsEtablissementsSecondaires(getEntreprise());
			if (!appariements.isEmpty()) {
				for (CandidatAppariement appariement : appariements) {
					tiersService.apparier(appariement.getEtablissement(), appariement.getSite());
				}
			}

			suivis.addSuivi(
					String.format("Organisation civile n°%d rattachée à l'entreprise n°%s.",
					              getOrganisation().getNumeroOrganisation(), FormatNumeroHelper.numeroCTBToDisplay(getEntreprise().getNumero()))
			);
		} else {
			// Fermer les surcharges  civiles ouvertes sur l'entreprise. Cela permet de prendre en compte d'éventuels changements survenus dans l'interval.
			tiersService.fermeSurchargesCiviles(getEntreprise(), getEvenement().getDateEvenement().getOneDayBefore());
		}
		// Fermeture des surcharges d'adresse, sauf les permanentes. A la condition qu'il doit exister une adresse effective dans RCEnt.
		final AdresseEffectiveRCEnt adresseEffective = sitePrincipal.getDonneesRegistreIDE().getAdresseEffective(this.dateApres);
		if (adresseEffective != null) {
			final AdresseSupplementaire adresseCourrier = getAdresseTiers(TypeAdresseTiers.COURRIER, this.dateApres);
			final AdresseSupplementaire adresseRepresentation = getAdresseTiers(TypeAdresseTiers.REPRESENTATION, this.dateApres);
			if (adresseCourrier != null || adresseRepresentation != null) {
				traiteTransitionAdresseEffective(warnings, suivis, this.dateApres, false);
				// S'il y a un doute sur l'équivalence de l'adresse effective par rapport à l'adresse annoncée, on met à vérifier.
				if (!checkAdressesEffectiveCommeAnnoncee(adresseEffective, annonceIDE)) {
					warnings.addWarning(
							String.format("L'adresse effective [%s] présente dans le registre civil est differente de celle annoncée [%s] à l'IDE par Unireg. " +
									              "Un autre service IDE a peut-être effectué une modification concurrente. Veuillez vérifier la situation de l'entreprise.",
							              detailSimpleAdresse(adresseEffective.getRue(), adresseEffective.getNumero(), adresseEffective.getNumeroPostal()),
							              detailSimpleAdresse(annonceIDE))
					);

				}
				final AdresseSupplementaire adressePoursuite = getAdresseTiers(TypeAdresseTiers.POURSUITE, this.dateApres);
				if (adressePoursuite != null) {
					traiteTransitionAdresseLegale(warnings, suivis, this.dateApres);
				}
			}
		}
		else {
			warnings.addWarning(
					"Pas d'adresse effective trouvée dans le registre civil. Pas de fermeture d'adresse(s) fiscale(s). (N'y a-t-il qu'une adresse de boîte postale dans RCEnt?)" +
							"Un autre service IDE a peut-être effectué une modification concurrente. Veuillez vérifier la situation de l'entreprise."
			);
		}

		final List<RaisonSocialeHisto> raisonsSociales = tiersService.getRaisonsSociales(getEntreprise(), false);
		final RaisonSocialeHisto raisonSocialeHisto = RangeUtil.getAssertLast(raisonsSociales, getDateApres());
		if (raisonSocialeHisto != null) {
			final String raisonSociale = raisonSocialeHisto.getRaisonSociale();
			final BaseAnnonceIDE.Contenu contenu = annonceIDE.getContenu();
			if (contenu != null && !raisonSociale.equals(contenu.getNom())) {
				warnings.addWarning(
						String.format("La raison sociale [%s] présente dans le registre civil est differente de celle annoncée [%s] à l'IDE par Unireg. " +
								              "Un autre service IDE a peut-être effectué une modification concurrente. Veuillez vérifier la situation de l'entreprise.",
						              raisonSociale, contenu.getFormeLegale())
				);
			}
		}

		final List<FormeLegaleHisto> formeLegaleHistos = tiersService.getFormesLegales(getEntreprise(), false);
		final FormeLegaleHisto formeLegaleHisto = RangeUtil.getAssertLast(formeLegaleHistos, getDateApres());
		if (formeLegaleHisto != null) {
			final FormeLegale formeLegale = formeLegaleHisto.getFormeLegale();
			final BaseAnnonceIDE.Contenu contenu = annonceIDE.getContenu();
			if (contenu != null && formeLegale != contenu.getFormeLegale()) {
				warnings.addWarning(
						String.format("La forme juridique [%s] présente dans le registre civil est differente de celle annoncée [%s] à l'IDE par Unireg. " +
								              "Un autre service IDE a peut-être effectué une modification concurrente. Veuillez vérifier la situation de l'entreprise.",
						              formeLegale, contenu.getFormeLegale())
				);
			}
		}

		DomicileEtablissement domicileFiscal = null;
		final List<DomicileEtablissement> domicilesFiscaux = etablissementPrincipal.getSortedDomiciles(false);
		for (DomicileEtablissement domFisc : domicilesFiscaux) {
			if (domFisc.isValidAt(getDateApres())) {
				domicileFiscal = domFisc;
			}
		}
		final List<DomicileHisto> DomicilesHistos = tiersService.getDomiciles(etablissementPrincipal, false);
		final DomicileHisto domicileCivil = RangeUtil.getAssertLast(DomicilesHistos, getDateApres());

		if (domicileFiscal != null && domicileCivil != null) {

			final Integer numeroOfsCivil = domicileCivil.getNumeroOfsAutoriteFiscale();
			final Integer numeroOfsFiscal = domicileFiscal.getNumeroOfsAutoriteFiscale();
			final Commune communeCivile = getContext().getServiceInfra().getCommuneByNumeroOfs(numeroOfsCivil, getDateApres());
			final Commune communeFiscale = getContext().getServiceInfra().getCommuneByNumeroOfs(numeroOfsFiscal, getDateApres());

			/* Si RCEnt propose la même commune qu'Unireg comme commune de siège, on peut fermer la surcharge. */
			if (numeroOfsFiscal.equals(numeroOfsCivil)) {
				tiersService.fermeSurchargesCiviles(etablissementPrincipal, getEvenement().getDateEvenement().getOneDayBefore());
			}
			/* Sinon, la surcharge doit rester ouverte. */
			else {
				warnings.addWarning(
						String.format("Le domicile [%s] présent dans le registre civil est different de celui trouvé [%s] dans Unireg. " +
								              "Le domicile Unireg prime et la surcharge fiscale reste ouverte. Veuillez vérifier la situation de l'entreprise.",
						              communeCivile == null ? "" : communeCivile.getNomOfficielAvecCanton(),
						              communeFiscale == null ? "" : communeFiscale.getNomOfficielAvecCanton())
				);
			}
		}
		raiseStatusTo(HandleStatus.TRAITE);
	}

	private boolean checkAdressesEffectiveCommeAnnoncee(AdresseEffectiveRCEnt adresseEffective, AnnonceIDEEnvoyee annonceIDE) {
		final BaseAnnonceIDE.Contenu contenu = annonceIDE.getContenu();
		if (contenu == null) {
			return false;
		}
		final AdresseAnnonceIDE adresseAnnonce = contenu.getAdresse();
		return adresseAnnonce != null &&
				ComparisonHelper.areEqual(adresseAnnonce.getNumeroOrdrePostal(), adresseEffective.getNumeroOrdrePostal()) &&
				ComparisonHelper.areEqual(adresseAnnonce.getNumero(), adresseEffective.getNumero()) &&
				ComparisonHelper.areEqual(adresseAnnonce.getRue(), adresseEffective.getRue());
	}

	private String detailSimpleAdresse(String rue, String numero, String npa) {
		String rueString = rue == null ? "" : "rue: " + rue;
		String numeroString = numero == null ? "" : "numéro: " + numero;
		String npaString = npa == null ? "" : "npa: " + npa;
		return String.join(", ", rueString, numeroString, npaString);
	}

	@NotNull
	private String detailSimpleAdresse(AnnonceIDEEnvoyee annonceIDE) {
		final BaseAnnonceIDE.Contenu contenu = annonceIDE.getContenu();
		if (contenu != null) {
			final AdresseAnnonceIDE adresse = contenu.getAdresse();
			if (adresse != null) {
				return detailSimpleAdresse(adresse.getRue(), adresse.getNumero(), adresse.getNpa().toString());
			}
		}
		return "";
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		// Erreurs techniques fatale
		if (dateAvant == null || dateApres == null || dateAvant != dateApres.getOneDayBefore()) {
			throw new IllegalArgumentException();
		}

		// Vérifier qu'il y a bien une entreprise préexistante en base ? (Ca ne devrait pas se produire ici)
		// On doit avoir deux autorités fiscales
		if (getEntreprise() == null) {
			throw new IllegalArgumentException();
		}

		// Vérifier qu'on a bien une annonce à l'IDE
		if (annonceIDE == null) {
			throw new IllegalArgumentException();
		}
	}

	public RegDate getDateAvant() {
		return dateAvant;
	}

	public RegDate getDateApres() {
		return dateApres;
	}

	public AnnonceIDEEnvoyee getAnnonceIDE() {
		return annonceIDE;
	}
}
