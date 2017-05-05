package ch.vd.uniregctb.evenement.ide;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.data.AdresseAnnonceIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.NumeroIDE;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.ProtoAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeRadiationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesFiscales;
import ch.vd.uniregctb.adresse.AdressesFiscalesHisto;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DomicileHisto;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.RaisonSocialeHisto;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

/**
 * @author Raphaël Marmier, 2016-09-02, <raphael.marmier@vd.ch>
 */
public class ServiceIDEServiceImpl implements ServiceIDEService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnonceIDEService.class);

	private TiersService tiersService;
	private AnnonceIDEService annonceIDEService;
	private ReferenceAnnonceIDEDAO referenceAnnonceIDEDAO;
	private AdresseService adresseService;
	private ServiceInfrastructureService infraService;
	private ServiceOrganisationService serviceOrganisation;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAnnonceIDEService(AnnonceIDEService annonceIDEService) {
		this.annonceIDEService = annonceIDEService;
	}

	public void setReferenceAnnonceIDEDAO(ReferenceAnnonceIDEDAO referenceAnnonceIDEDAO) {
		this.referenceAnnonceIDEDAO = referenceAnnonceIDEDAO;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.infraService = serviceInfra;
	}

	public void setServiceOrganisation(ServiceOrganisationService serviceOrganisation) {
		this.serviceOrganisation = serviceOrganisation;
	}

	@Override
	public boolean isServiceIDEObligEtendues(Entreprise entreprise, RegDate date) {

		final FormeLegaleHisto formeLegaleHisto = getFormeLegale(entreprise, date);
		final FormeLegale formeLegale = formeLegaleHisto == null ? null : formeLegaleHisto.getFormeLegale();
		final DomicileHisto siege = getSiege(entreprise, date);
		final boolean actifAuRC = isActifAuRC(entreprise, date);

		return isServiceIDEObligEtendues(formeLegale, siege, actifAuRC);
	}

	protected boolean isServiceIDEObligEtendues(FormeLegale formeLegale, DomicileHisto siege, boolean actifAuRC) {
		return !actifAuRC &&
				siege != null && siege.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
				(formeLegale == FormeLegale.N_0109_ASSOCIATION || formeLegale == FormeLegale.N_0110_FONDATION);
	}

	private RaisonSocialeHisto getRaisonSociale(Entreprise entreprise, RegDate date) {
		final List<RaisonSocialeHisto> liste = tiersService.getRaisonsSociales(entreprise, false);
		return DateRangeHelper.rangeAt(liste, date);
	}

	private FormeLegaleHisto getFormeLegale(Entreprise entreprise, RegDate date) {
		final List<FormeLegaleHisto> liste = tiersService.getFormesLegales(entreprise, false);
		return DateRangeHelper.rangeAt(liste, date);
	}

	private DomicileHisto getSiege(Entreprise entreprise, RegDate date) {
		final List<DomicileHisto> sieges = tiersService.getSieges(entreprise, false);
		return DateRangeHelper.rangeAt(sieges, date);
	}

	@Nullable
	private AdresseGenerique getAdresse(Entreprise entreprise, RegDate date) throws ServiceIDEException {
		AdressesFiscalesHisto adressesFiscalHisto = null;
		try {
			adressesFiscalHisto = adresseService.getAdressesFiscalHisto(entreprise, false);
		}
		catch (AdresseException e) {
			final String message = "Problème rencontré lors de la recherche des adresses de l'entreprise.";
			Audit.error(message);
			throw new ServiceIDEException(message, e);
		}

		final AdressesFiscales adressesFiscales = adressesFiscalHisto.at(date);

		if (adressesFiscales.courrier != null && !adressesFiscales.courrier.isDefault() && !adressesFiscales.courrier.isAnnule()) {
			return adressesFiscales.courrier;
		} else if (adressesFiscales.representation != null && !adressesFiscales.representation.isDefault() && !adressesFiscales.representation.isAnnule()) {
			return adressesFiscales.representation;
		} else if (adressesFiscales.poursuite != null && !adressesFiscales.poursuite.isDefault() && !adressesFiscales.poursuite.isAnnule()) {
			return adressesFiscales.poursuite;
		}
		return null;
	}

	private boolean isActifAuRC(Entreprise entreprise, RegDate date) {
		final Organisation organisation = tiersService.getOrganisation(entreprise);
		return organisation != null && OrganisationHelper.isInscriteAuRC(organisation, date) && !OrganisationHelper.isRadieeDuRC(organisation, date);
	}

	@Override
	public AnnonceIDEEnvoyee synchroniseIDE(Entreprise entreprise) throws ServiceIDEException {
		final RegDate date = RegDate.get(); // On agit en terme du présent
		final ProtoAnnonceIDE protoAnnonceIDE = evalueSynchronisationIDE(entreprise, date);
		if (protoAnnonceIDE != null) {
			protoAnnonceIDE.setCommentaire("Généré automatiquement suite à la mise à jour des données civiles du contribuable.");
			try {
				return annonceIDEService.emettreAnnonceIDE(protoAnnonceIDE, tiersService.getEtablissementPrincipal(entreprise, date));
			}
			catch (AnnonceIDEException e) {
				final String message =
						String.format("Erreur lors de l'émission de l'annonce à l'IDE de l'entreprise n°%s. %s", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), e.getMessage());
				Audit.warn(message);
				throw new ServiceIDEException(message, e);
			}
		}
		else {
			return null;
		}
	}

	@Override
	public ProtoAnnonceIDE simuleSynchronisationIDE(Entreprise entreprise) throws ServiceIDEException {
		final RegDate date = RegDate.get(); // On agit en terme du présent
		return evalueSynchronisationIDE(entreprise, date);
	}

	private ProtoAnnonceIDE evalueSynchronisationIDE(Entreprise entreprise, RegDate date) throws ServiceIDEException {
		Assert.notNull(entreprise, "Impossible de synchroniser l'IDE sans une entreprise!");

		Audit.info(String.format("Evaluation de l'entreprise n°%s dont les données civiles pertinentes ont changé.", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())));

		if (!isServiceIDEObligEtendues(entreprise, date)) {
			final String message = String.format("Unireg n'est pas responsable de l'entreprise n°%s. Pas d'annonce.", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
			Audit.info(message);
			return null;
		}

		if (entreprise.isIdeDesactive()) {
			final String message = String.format("Annonces au registre IDE désactivées en base Unireg pour l'entreprise n°%s", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
			Audit.warn(message);
			throw new ServiceIDEException(message);
		}

		final Etablissement etablissement = tiersService.getEtablissementPrincipal(entreprise, date);

		if (etablissement == null) {
			final String message = String.format("Aucun établissement trouvé pour l'entreprise n°%s en date d'aujourd'hui (%s)!",
			                                    FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), RegDateHelper.dateToDisplayString(date));
			Audit.error(message);
			throw new ServiceIDEException(message);
		}

		/*
			Récupération des données civiles
		 */

		final DomicileHisto siege = getSiege(entreprise, date);
		final boolean actifAuRC = isActifAuRC(entreprise, date);
		final RaisonSocialeHisto raisonsSocialeHisto = getRaisonSociale(entreprise, date);
		final FormeLegaleHisto formeLegaleHisto = getFormeLegale(entreprise, date);
		final AdresseGenerique adresseGenerique = getAdresse(entreprise, date);
		final AdresseEnvoiDetaillee adresseEnvoiDetaillee;
		final String secteurActiviteActuel = entreprise.getSecteurActivite();

		/*
			Vérifier qu'on a toutes les cartes en main
		 */
		if (raisonsSocialeHisto == null || formeLegaleHisto == null || adresseGenerique == null) {
			final String message = String.format("Entreprise n°%s: impossible de communiquer des changements à l'IDE car il manque des données obligatoires: %s%s%s.",
			                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
			                                     raisonsSocialeHisto != null ? "" : "[raison sociale]",
			                                     formeLegaleHisto != null ? "" : "[forme juridique]",
			                                     adresseGenerique != null ? "" : "[adresse]"
			);
			Audit.error(message);
			throw new ServiceIDEException(message);
		}

		final FormeLegale formeLegale = formeLegaleHisto.getFormeLegale();

		try {
			adresseEnvoiDetaillee = adresseService.buildAdresseEnvoi(entreprise, adresseGenerique, date);
		}
		catch (AdresseException e) {
			final String message = "Erreur lors de la récupération de l'adresse actuelle du tiers: " + e.getMessage();
			Audit.error(message);
			throw new ServiceIDEException(message, e);
		}

		final SiteOrganisation site = tiersService.getSiteOrganisationPourEtablissement(etablissement);
		if (site != null) {
			if (site.getTypeDeSite(date) != TypeDeSite.ETABLISSEMENT_PRINCIPAL) {
				final String message = String.format("Entreprise n°%s: le site apparié à l'établissement n°%s n'est pas un établissement principal.",
				                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
				                                     FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()));
				Audit.error(message);
				throw new ServiceIDEException(message);
			}
		}

		final StatusRegistreIDE statusRegistreIDE = site == null ? null : site.getDonneesRegistreIDE().getStatus(date);
		final String numeroIDEFiscalEntreprise = getNumeroIDEFiscalActuel(entreprise);

		final EtatEntreprise etatActuel = entreprise.getEtatActuel();

		/*
			Récupération de la dernière annonce envoyée par Unireg, s'il y en a une.
		 */
		final AnnonceIDEEnvoyee derniereAnnonceEmise;
		final ReferenceAnnonceIDE derniereReferenceAnnonceIDE = referenceAnnonceIDEDAO.getLastReferenceAnnonceIDE(etablissement.getNumero());
		if (derniereReferenceAnnonceIDE != null) {
			derniereAnnonceEmise = serviceOrganisation.getAnnonceIDE(derniereReferenceAnnonceIDE.getId(), null);
		}
		else {
			derniereAnnonceEmise = null;
		}

		/*
			Cas de l'annonce "in flight" (dans l'esb en attente de traitement par RCEnt)
		 */
		if (derniereReferenceAnnonceIDE != null && derniereAnnonceEmise == null) {
			final String message = String.format("Entreprise n°%s: une annonce est en attente de reception par le registre civil des entreprises (RCEnt). " +
					                                     "Ce traitement doit avoir lieu avant de pouvoir déterminer s'il faut annoncer de nouveaux changements.",
			                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())
			);
			Audit.warn(message);
			throw new ServiceIDEException(message);
		}


		final TypeAnnonce typeAnnonce;
		final String noIde;
		final RaisonDeRadiationRegistreIDE raisonDeRadiationRegistreIDE = null; // TODO: Supporter correctement les radiations

		/*
			Déterminer le type d'annonce et le numéro IDE
		 */

		/*
			L'établissement est apparié à RCEnt
		 */
		if (site != null) {
			final String numeroIDESite = site.getNumeroIDE(date);
			/*
				Etablissement est connnu de RCEnt pour être à l'IDE
			 */
			if (numeroIDESite != null) {
				if (numeroIDEFiscalEntreprise != null) {
					/*
						Comme Unireg connait le numéro IDE, on vérifie qu'il est bien identique à celui du civil. Sinon on arrête tout.
					 */
					if (!numeroIDEFiscalEntreprise.equals(numeroIDESite)) {
						final String message = String.format("Entreprise n°%s: le numéro IDE dans Unireg [%s] et celui au registre civil [%s] ne correspondent pas pour l'établissement n°%s! " +
								                                     "Soit il s'agit du numéro IDE provisoire qui aurait été oublié dans Unireg, à effacer. Soit nous sommes en présence d'une erreur d'appariement.",
						                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
						                                     numeroIDEFiscalEntreprise, numeroIDESite,
						                                     FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())
						);
						Audit.error(message);
						throw new ServiceIDEException(message);
					}
					noIde = numeroIDESite;
				}
				else {
					/*
						Cas nominal: on n'est pas censé garder un numéro IDE dans Unireg une fois apparié à RCEnt
					 */
					noIde = numeroIDESite;
				}

				/*
					Connu au civil, connu à l'IDE (on a un no IDE civil) et on a donc un statut à l'IDE à disposition.
				 */
				switch (statusRegistreIDE) {
				case AUTRE:
					final String messageAutre = String.format("L'entreprise n°%s a un statut inattendu au registre IDE: %s", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
					                                    statusRegistreIDE.toString());
					Audit.error(messageAutre);
					throw new ServiceIDEException(messageAutre);
				case EN_MUTATION:
				case EN_REACTIVATION:
				case PROVISOIRE:
				case DEFINITIF:
					if (etatActuel != null && etatActuel.getType() == TypeEtatEntreprise.DISSOUTE) {
						typeAnnonce = TypeAnnonce.RADIATION;
					}
					else {
						typeAnnonce = TypeAnnonce.MUTATION;
					}
					break;
				case RADIE:
				case ANNULE:
					typeAnnonce = TypeAnnonce.REACTIVATION;
					break;
				case DEFINITIVEMENT_RADIE:
					final String messageDefRadie = String.format("L'entreprise n°%s est définitivement radiée du registre IDE et ne peut plus être modifiée.",
					                                    FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
					Audit.error(messageDefRadie);
					throw new ServiceIDEException(messageDefRadie);
				default:
					final String message = "Type de statut à l'IDE inconnu: " + statusRegistreIDE;
					Audit.error(message);
					throw new ServiceIDEException(message);
				}

				/*
				Etablissement pas connu à l'IDE d'après RCEnt
			 */
			}
			else {
				/*
					Etablissement connnu de RCEnt mais dont RCEnt ignore la présence à l'IDE
				 */
				if (numeroIDEFiscalEntreprise != null) {
					/*
						Cas de figure d'une entité pure REE déjà connue qu'on a annoncé à l'IDE et qu'on tente de modifier
						avant que l'IDE n'est émis d'événement de nouvelle inscription IDE.
					 */
					if (derniereAnnonceEmise != null) {
						final String message = String.format("L'entreprise n°%s a été annoncée au registre IDE mais cette annonce n'a pas encore été traitée: impossible d'évaluer la situation du tiers pour l'instant.",
						                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
						Audit.warn(message);
						throw new ServiceIDEException(
								message);
					}
					else {
						final String message = String.format("L'entreprise n°%s est connue par Unireg comme étant inscrite au registre IDE (n°%s), mais RCEnt ne possède pas ses données IDE. Impossible d'annoncer le tiers à l'IDE.",
						                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), numeroIDEFiscalEntreprise);
						Audit.error(message);
						throw new ServiceIDEException(
								message);
					}
				}
				else {
					/*
						Cas de figure hypothétique d'une entité pure REE déjà apparillée qu'on annonce à l'IDE.
					*/
					typeAnnonce = TypeAnnonce.CREATION;
					noIde = null;
				}
			}
			/*
			L'établissement n'est pas apparié à RCEnt
		 */
		}
		else {
			if (numeroIDEFiscalEntreprise != null) {
				/*
					Modification par Unireg d'une entité non encore apparillée.
				 */
				if (derniereAnnonceEmise != null) {
					final String message = String.format("L'entreprise n°%s non appariée a été annoncée au registre IDE mais cette annonce n'a pas encore été traitée: impossible d'évaluer la situation du tiers pour l'instant.",
					                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
					Audit.warn(message);
					throw new ServiceIDEException(message);
				}
				else {
					final String message = String.format(
							"L'entreprise n°%s non appariée est connue par Unireg comme étant inscrite au registre IDE (n°%s). Elle doit d'abord être appariée à RCEnt avant de pouvoir annoncer les changements la concernant.",
							FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), numeroIDEFiscalEntreprise);
					Audit.error(message);
					throw new ServiceIDEException(message);
				}
			}
			else {
				/*
					Création pure dans Unireg.
				 */
				typeAnnonce = TypeAnnonce.CREATION;
				noIde = null;
			}
		}

		/*
			On peut maintenant contrôler la présence du secteur d'activité
		 */
		if (secteurActiviteActuel == null && typeAnnonce == TypeAnnonce.CREATION) {
			final String message = String.format("Entreprise n°%s: impossible de communiquer des changements à l'IDE car il manque le secteur d'activité.",
			                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())
			);
			Audit.error(message);
			throw new ServiceIDEException(message);
		}

		final String raisonSocialeActuelle = raisonsSocialeHisto.getRaisonSociale();
		final FormeLegale formeLegaleActuelle = formeLegaleHisto.getFormeLegale();
		final AdresseAnnonceIDERCEnt adresseActuelle = getAdresseAnnonceIDERCEnt(adresseEnvoiDetaillee);

		ProtoAnnonceIDE protoActuel =
				RCEntAnnonceIDEHelper.createProtoAnnonceIDE(typeAnnonce, DateHelper.getCurrentDate(), RCEntAnnonceIDEHelper.UNIREG_USER, null, TypeDeSite.ETABLISSEMENT_PRINCIPAL, raisonDeRadiationRegistreIDE, null,
				                                            noIde == null ? null : new NumeroIDE(noIde), null, null, etablissement.getNumeroEtablissement(), entreprise.getNumeroEntreprise(), null,
				                                            raisonSocialeActuelle, null, formeLegaleActuelle, secteurActiviteActuel, adresseActuelle, null,
				                                            RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);

		/*
			Faire un effort pour ne pas envoyer inutilement une annonce pour des données déjà connues ou déjà annoncées.
		 */
		if (derniereAnnonceEmise != null && derniereAnnonceEmise.getType() == TypeAnnonce.CREATION && protoActuel.getType() == TypeAnnonce.CREATION) {
			final String message = String.format("L'entreprise n°%s a déjà été annoncée à l'IDE en création.", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
			Audit.warn(message);
			throw new ServiceIDEException(message);
		}

		if (derniereAnnonceEmise != null && derniereAnnonceEmise.getType() == TypeAnnonce.RADIATION && protoActuel.getType() == TypeAnnonce.RADIATION) {
			final String message = String.format("L'entreprise n°%s a déjà été annoncée à l'IDE comme radiée.", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
			Audit.error(message);
			throw new ServiceIDEException(message);
		}

		/*
			Code de neutralisation de la réactivation, qui n'est pas supportée à ce stade.
		 */
		if (typeAnnonce == TypeAnnonce.REACTIVATION) {
			String message = String.format("L'entreprise n°%s doit être réactivée au registre IDE avant de pouvoir annoncer des modifications. La demande doit être adressée à l'IDE directement.",
			                               FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
			Audit.error(message);
			throw new ServiceIDEException(message);
		}

		if (derniereAnnonceEmise != null &&
				typeAnnonce == TypeAnnonce.MUTATION &&
				protoActuel.getContenu() != null && protoActuel.getContenu().equals(derniereAnnonceEmise.getContenu())) {
			Audit.info(String.format("L'entreprise n°%s a déjà été annoncée à l'IDE avec les mêmes données. Pas d'annonce.", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())));
			return null;
		}

		/*
			Valider l'annonce auprès de RCEnt et indirectement de l'IDE.
		 */
		try {
			validerAnnonceIDE(protoActuel, entreprise);
		}
		catch (AnnonceIDEValidationException e) {
			String message = String.format("%s %s", e.getMessage(), CollectionsUtils.toString(e.getErreurs(), new StringRenderer<Pair<String, String>>() {
				@Override
				public String toString(Pair<String, String> paire) {
					return paire.getFirst() + ": " + paire.getSecond();
				}
			}, "; "));
			Audit.error(message);
			throw new ServiceIDEException(message);
		}

		Audit.info(String.format("Annonce à l'IDE de l'entreprise (%s) n°%s %s, domiciliée à %s, type %s, %s.",
		                         protoActuel.getType().getLibelle(),
		                         FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
		                         protoActuel.getContenu() == null ? "" : protoActuel.getContenu().getNom(),
		                         infraService.getCommuneByNumeroOfs(siege.getNumeroOfsAutoriteFiscale(), date).getNomOfficielAvecCanton(),
		                         formeLegale.getLibelle(),
		                         actifAuRC ? " active au RC" : "non active au RC"
		)
		);

		return protoActuel;
	}


	@Override
	public void validerAnnonceIDE(BaseAnnonceIDE proto, Entreprise entreprise) throws ServiceIDEException {
		try {
			final BaseAnnonceIDE.Statut statut = serviceOrganisation.validerAnnonceIDE(proto);

			// Workaround du SIREF-9364, où le statut est "sans erreur" même lorsqu'il y en a.
			if (statut.getErreurs() != null && !statut.getErreurs().isEmpty()) {
				throw new AnnonceIDEValidationException(String.format("Entreprise n°%s: le modèle d'annonce à l'IDE a échoué à la validation (il y a des erreurs).",
				                                                      FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())),
				                                        statut.getErreurs()
				);
			}

			switch (statut.getStatut()) {
			case VALIDATION_SANS_ERREUR:
				break;
			case REJET_RCENT:
				throw new AnnonceIDEValidationException(String.format("Entreprise n°%s: le modèle d'annonce à l'IDE a échoué à la validation par RCEnt.", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())),
				                                        statut.getErreurs());
			default:
				final String message = String.format("Entreprise n°%s: statut d'annonce inattendu retourné par le service de validation du registre civil: %s",
				                                    FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), statut.getStatut());
				Audit.error(message);
				throw new ServiceIDEException(message);
			}
		} catch (ServiceOrganisationException e) {
			final String message = "Un problème est survenu lors de la validation du prototype de l'annonce: " + e.getMessage();
			Audit.error(message);
			throw new ServiceIDEException(message, e);
		}
	}

	/**
	 * Recherche le numéro IDE actuel d'un contribuable dans Unireg.
	 *
	 * @param contribuable le contribuable
	 * @return le numéro IDE, ou null si le contribuable n'en a pas.
	 * @throws ServiceIDEException si plusieurs numéros IDE sont trouvés sur l'entreprise.
	 */
	private String getNumeroIDEFiscalActuel(Contribuable contribuable) throws ServiceIDEException {
		final Set<IdentificationEntreprise> identificationsEntreprise = contribuable.getIdentificationsEntreprise();
		if (identificationsEntreprise == null || identificationsEntreprise.isEmpty()) {
			return null;
		} else if (identificationsEntreprise.size() == 1) {
			return identificationsEntreprise.iterator().next().getNumeroIde();
		} else {
			final String message = String.format("Plusieurs numéros IDE trouvés sur l'établissement n°%s alors qu'il ne devrait y en avoir qu'un.", FormatNumeroHelper.numeroCTBToDisplay(contribuable.getNumero()));
			Audit.info(message);
			throw new ServiceIDEException(message);
		}
	}

	@Nullable
	private AdresseAnnonceIDERCEnt getAdresseAnnonceIDERCEnt(AdresseEnvoiDetaillee adresse) {
		final AdresseAnnonceIDERCEnt adresseAnnonce;
		if (adresse != null) {
			final String npa = adresse.getNpaEtLocalite() == null ? null : adresse.getNpaEtLocalite().getNpa();
			final Pays pays = adresse.getPays();
			final CasePostale casePostale = adresse.getCasePostale();
			adresseAnnonce = RCEntAnnonceIDEHelper
					.createAdresseAnnonceIDERCEnt(adresse.getRueEtNumero() == null ? "" : adresse.getRueEtNumero().getRue(),
					                              adresse.getRueEtNumero() == null ? "" : adresse.getRueEtNumero().getNumero(),
					                              adresse.getNumeroAppartement(),
					                              adresse.isSuisse() && npa != null ? Integer.parseInt(npa) : null,
					                              adresse.isSuisse() ? null : npa,
					                              adresse.getNumeroOrdrePostal(),
					                              adresse.getNpaEtLocalite() == null ? null : adresse.getNpaEtLocalite().getLocalite(),
					                              pays == null ? null : pays.getNoOfsEtatSouverain(), pays == null ? null : pays.getCodeIso2(), pays == null ? null : pays.getNomCourt(),
					                              casePostale == null ? null : casePostale.getNumero(), casePostale == null ? null : String.valueOf(casePostale.getType()),
					                              adresse.getEgid());
		} else {
			adresseAnnonce = null;
		}
		return adresseAnnonce;
	}
}
