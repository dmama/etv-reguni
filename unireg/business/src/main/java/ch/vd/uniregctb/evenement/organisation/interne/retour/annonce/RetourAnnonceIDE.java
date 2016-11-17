package ch.vd.uniregctb.evenement.organisation.interne.retour.annonce;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneDeTraitement;
import ch.vd.uniregctb.evenement.organisation.interne.HandleStatus;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DomicileHisto;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.RaisonSocialeHisto;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.rattrapage.appariement.CandidatAppariement;
import ch.vd.uniregctb.utils.RangeUtil;

/**
 * Traitement du retour d'annonce à l'IDE. C'est-à-dire de l'annonce à l'IDE dont l'origine est notre propre annonce.
 *
 * @author Raphaël Marmier, 2016-09-22
 */
public class RetourAnnonceIDE extends EvenementOrganisationInterneDeTraitement {

	private static final Logger LOGGER = LoggerFactory.getLogger(RetourAnnonceIDE.class);

	private final RegDate dateAvant;
	private final RegDate dateApres;

	private final AnnonceIDEEnvoyee annonceIDE;
	private final ReferenceAnnonceIDE referenceAnnonceIDE;

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
		this.referenceAnnonceIDE = evenement.getReferenceAnnonceIDE();

		sitePrincipal = organisation.getSitePrincipal(dateApres).getPayload();
		etablissementPrincipal = referenceAnnonceIDE.getEtablissement();
	}

	@Override
	public String describe() {
		return "Retour d'annonce à l'IDE";
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		/*
			- On rattache les entités, sans fermer les surcharge (demande de l'ACI).
			- On comparer une à une toutes les données de l'annonce (à rechercher dans RCEnt) et on vérifie que le résultat obtenu dans RCEnt correspond bien. (ou faut-il comparer avec l'état actuel d'Unireg?)
			- Les différences sont signalées à l'utilisateur avec un message à vérifier. Il ne devrait pas y avoir de différence, sauf en cas de collision avec un autre registre lors du traitement à l'IDE.
		 */
		suivis.addSuivi(
				String.format(
						"Retour de l'annonce à l'IDE n°%s du %s concernant l'entreprise n°%s suite à création ou modification dans Unireg. L'état à l'IDE est maintenant être aligné sur celui d'Unireg.",
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
		// Fermeture des surcharges d'adresse, sauf les permanentes. On "détourne" les méthodes du changement d'adresse, car il doit se passer la même chose que lorsque l'adresse change.
		traiteTransitionAdresseEffective(warnings, suivis, this.dateApres, false);
		traiteTransitionAdresseLegale(warnings, suivis, this.dateApres);

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

		// Vérifier qu'on a bien une annonce à l'IDE
		Assert.notNull(annonceIDE);

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
