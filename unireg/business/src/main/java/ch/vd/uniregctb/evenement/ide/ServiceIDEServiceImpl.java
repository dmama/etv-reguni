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
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.organisation.data.AdresseAnnonceIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.ModeleAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.ModeleAnnonceIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.NumeroIDE;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeRadiationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesFiscales;
import ch.vd.uniregctb.adresse.AdressesFiscalesHisto;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DomicileHisto;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.RaisonSocialeHisto;
import ch.vd.uniregctb.tiers.Source;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

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
	public boolean isServiceEtendu(Entreprise entreprise, RegDate date) {

		final FormeLegaleHisto formeLegaleHisto = getFormeLegale(entreprise, date);
		final FormeLegale formeLegale = formeLegaleHisto == null ? null : formeLegaleHisto.getFormeLegale();
		final DomicileHisto siege = getSiege(entreprise, date);
		final boolean actifAuRC = isActifAuRC(entreprise, date);

		final String AUDIT__BASE_FORMAT = "Entreprise n°%s, domicile %s, type %s, %s";

		if (!actifAuRC &&
				siege != null && siege.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
				(formeLegale == FormeLegale.N_0109_ASSOCIATION || formeLegale == FormeLegale.N_0110_FONDATION)) {
			Audit.info(
					String.format(AUDIT__BASE_FORMAT + " --> Unireg est responsable de l'entité. Annonce au registre IDE.",
					              FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
					              infraService.getCommuneByNumeroOfs(siege.getNumeroOfsAutoriteFiscale(), date).getNomOfficielAvecCanton(),
					              formeLegale.getLibelle(),
					              "non active au RC"
			           )
			);
			return true;
		} else {
			Audit.info(
					String.format(AUDIT__BASE_FORMAT + " --> Unireg non responsable de l'entité. Pas d'annonce au registre IDE.",
					              FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
					              siege != null ? infraService.getCommuneByNumeroOfs(siege.getNumeroOfsAutoriteFiscale(), date).getNomOfficielAvecCanton() : "<siège inconnu>",
					              formeLegale != null ? formeLegale.getLibelle() : "<forme juridique inconnue>",
					              actifAuRC ? " active au RC" : "non active au RC"
					)
			);
			return false;
		}
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
			throw new ServiceIDEException("Problème rencontré lors de la recherche des adresses de l'entreprise.", e);
		}

		final AdressesFiscales adressesFiscales = adressesFiscalHisto.at(date);

		if (adressesFiscales.domicile != null && !adressesFiscales.domicile.isAnnule()) {
			return adressesFiscales.domicile;
		} else if (adressesFiscales.courrier != null && !adressesFiscales.courrier.isAnnule()) {
			return adressesFiscales.courrier;
		} else if (adressesFiscales.representation != null && !adressesFiscales.representation.isAnnule()) {
			return adressesFiscales.representation;
		} else if (adressesFiscales.poursuite != null && !adressesFiscales.poursuite.isAnnule()) {
			return adressesFiscales.poursuite;
		}
		return null;
	}

	private boolean isActifAuRC(Entreprise entreprise, RegDate date) {
		final Organisation organisation = tiersService.getOrganisation(entreprise);
		return organisation != null && OrganisationHelper.isInscriteAuRC(organisation, date) && !OrganisationHelper.isRadieeDuRC(organisation, date);
	}

	@Override
	public ModeleAnnonceIDE synchroniseIDE(Entreprise entreprise, boolean validateOnly) throws ServiceIDEException {
		Assert.notNull(entreprise, "Impossible de synchroniser l'IDE sans une entreprise!");

		// On agit en terme du présent
		final RegDate notreDate = RegDate.get();

		LOGGER.info(String.format("Annonce des changements sur l'entreprise n°%s à l'IDE, s'il y a lieu. Seul l'établissement principal est supporté.", entreprise.getNumero()));

		// Obtenir l'établissement principal:
		final ActiviteEconomique rapportEntreTiers = (ActiviteEconomique) entreprise.getRapportSujetValidAt(notreDate, TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE);
		final Etablissement etablissement = (Etablissement) tiersService.getTiers(rapportEntreTiers.getObjetId());

		Assert.notNull(etablissement,
		               String.format("Aucun établissement trouvé pour l'entreprise n°%s en date d'aujourd'hui (%s)!",
		                             FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
		                             RegDateHelper.dateToDisplayString(notreDate)
		               )
		);

		ModeleAnnonceIDE resultat = null;
		if (isServiceEtendu(entreprise, notreDate)) {

			/*
				Il est bon d'avoir à l'esprit que pour le moment, on ne supporte que l'modèle de l'établissement principal
				de l'entreprise, quel que soit l'établissement réellement modifié.
			 */
			if (rapportEntreTiers.isPrincipal()) {

				final Organisation organisation = tiersService.getOrganisation(entreprise);
				final SiteOrganisation site = tiersService.getSiteOrganisationPourEtablissement(etablissement);
				if (site != null) {
					Assert.isTrue(site.getTypeDeSite(notreDate) == TypeDeSite.ETABLISSEMENT_PRINCIPAL,
					              String.format("Le site apparié à l'établissement n°%s n'est pas un établissement principal.", etablissement.getNumero()));
				}

				final StatusRegistreIDE statusRegistreIDE = site == null ? null : site.getDonneesRegistreIDE().getStatus(notreDate);
				final String numeroIDEFiscalEntreprise = getNumeroIDEFiscalActuel(entreprise);
				final String numeroIDEFiscalEtablissement = getNumeroIDEFiscalActuel(etablissement);

				final EtatEntreprise etatActuel = entreprise.getEtatActuel();

				final TypeAnnonce typeAnnonce;
				final String noIde;
				final RaisonDeRadiationRegistreIDE raisonDeRadiationRegistreIDE = null; // TODO: Aller chercher la raison de radiation dans les remarques du tiers.

				/*
					Déterminer le type d'annonce et le numéro IDE
				 */
				if (site != null) {
					final String numeroIDESite = site.getNumeroIDE(notreDate);
					if (numeroIDESite != null) {
						/*
							Etablissement connnu de RCEnt pour être à l'IDE
						 */
						if (numeroIDEFiscalEntreprise != null) {
							/*
								On connait déjà le numéro IDE
							 */
							if (!numeroIDEFiscalEntreprise.equals(numeroIDESite)) {
								throw new ServiceIDEException(
										String.format("Les numéro IDE de Unireg [%s] et du registre civil [%s] ne correspondent pas pour l'établissement n°%s!",
										              numeroIDEFiscalEntreprise, numeroIDESite, FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())
										));
							}
							noIde = numeroIDESite;
						} else {
							/*
								Cas spécial: la désinscription du RC d'une association. On n'a pas (encore?) le numéro IDE.
							 */
							noIde = numeroIDESite;
						}
						/*
						    Ce qui suit n'est pas satisfaisant: il faudrait pouvoir détecter qu'il faut réactiver avant le retour via RCEnt de la radiation / annulation.
							Le prix à payer est l'envoi d'annonces de modifications sur une entité radiée de l'IDE ou annulée.
						 */
						if (statusRegistreIDE == StatusRegistreIDE.RADIE || statusRegistreIDE == StatusRegistreIDE.ANNULE) {
							/*
								L'état actuel à l'IDE est le seul moyen pour nous de comprendre qu'il faut demander la radiation de l'entité.
							 */
							typeAnnonce = TypeAnnonce.REACTIVATION;
						} else if (etatActuel != null && etatActuel.getType() == TypeEtatEntreprise.DISSOUTE) {
							typeAnnonce = TypeAnnonce.RADIATION;
						} else {
							typeAnnonce = TypeAnnonce.MUTATION;
						}
					} else {
						/*
							Etablissement connnu de RCEnt mais dont RCEnt ignore une éventuelle présence à l'IDE
						 */
						if (numeroIDEFiscalEntreprise != null) {
							/*
								Cas de figure hypothétique d'une entité pure REE déjà connue qu'on a annoncé à l'IDE et qu'on tente de modifier
								avant que l'IDE n'est émis d'événement de nouvelle inscription IDE.
							 */
							typeAnnonce = TypeAnnonce.MUTATION;
							noIde = numeroIDEFiscalEntreprise;
						} else {
							/*
								Cas de figure hypothétique d'une entité pure REE déjà apparillée qu'on annonce à l'IDE.
							 */
							typeAnnonce = TypeAnnonce.CREATION;
							noIde = null;
						}
					}
				} else {
					if (numeroIDEFiscalEntreprise != null) {
							/*
								Modification par Unireg d'une entité non encore apparillée.
							 */
						typeAnnonce = TypeAnnonce.MUTATION;
						noIde = numeroIDEFiscalEntreprise;
					} else {
							/*
								Création pure par Unireg.
							 */
						typeAnnonce = TypeAnnonce.CREATION;
						noIde = null;
					}
				}

				final RaisonSocialeHisto raisonsSocialeHisto = getRaisonSociale(entreprise, notreDate);
				final FormeLegaleHisto formeLegaleHisto = getFormeLegale(entreprise, notreDate);
//				final DomicileHisto siege = getSiege(entreprise, notreDate);
				final AdresseGenerique adresseGenerique = getAdresse(entreprise, notreDate);
				final String secteurActiviteActuel = entreprise.getSecteurActivite();

				if (raisonsSocialeHisto == null || formeLegaleHisto == null || adresseGenerique == null) {
					throw new ServiceIDEException(
							String.format("Impossible de communiquer des changements à l'IDE car il manque des données obligatoires sur l'entreprise: %s%s%s%s.",
							              raisonsSocialeHisto != null ? "" : "[raison sociale]",
							              formeLegaleHisto != null ? "" : "[forme juridique]",
							              adresseGenerique != null ? "" : "[adresse]",
							              secteurActiviteActuel != null ? "" : "[secteur d'activite]"
							)
					);
				}

				// flags sur l'état des données
				boolean surchargeEnVigueure = false;


				// Determiner si on a des surcharges ou pas.
				if (raisonsSocialeHisto.getSource() == Source.FISCALE) {
					surchargeEnVigueure = true;
				}
				if (formeLegaleHisto.getSource() == Source.FISCALE) {
					surchargeEnVigueure = true;
				}
				if (adresseGenerique.getSource().getType() != AdresseGenerique.SourceType.CIVILE_ORG) {
					surchargeEnVigueure = true;
				}

				// On a aucune différence entre nous et RCEnt, donc rien à annoncer.
				if (!surchargeEnVigueure) {
					return null;
				}

				final String raisonSocialeActuelle = raisonsSocialeHisto.getRaisonSociale();
				final FormeLegale formeLegaleActuelle = formeLegaleHisto.getFormeLegale();
				final AdresseAnnonceIDERCEnt adresseActuelle = getAdresseAnnonceIDERCEnt(adresseGenerique, notreDate);

				ModeleAnnonceIDERCEnt modeleActuel =
						RCEntAnnonceIDEHelper.createModeleAnnonceIDERCEnt(typeAnnonce, DateHelper.getCurrentDate(), null, null, TypeDeSite.ETABLISSEMENT_PRINCIPAL, raisonDeRadiationRegistreIDE, null,
						                                                  noIde == null ? null : new NumeroIDE(noIde), null, null, etablissement.getNumeroEtablissement(), entreprise.getNumeroEntreprise(), null,
						                                                  raisonSocialeActuelle, null, formeLegaleActuelle, secteurActiviteActuel, adresseActuelle);


				// Qu'a-t'on déjà envoyé? Doit-on renvoyer?
				final ReferenceAnnonceIDE lastReferenceAnnonceIDE = referenceAnnonceIDEDAO.getLastReferenceAnnonceIDE(etablissement.getNumero());
				if (lastReferenceAnnonceIDE != null) {
					final AnnonceIDE derniereAnnonceEmise = serviceOrganisation.getAnnonceIDE(lastReferenceAnnonceIDE.getId());

					if (derniereAnnonceEmise != null) {
						// Gestion minimaliste de la radiation.
						if (modeleActuel.getType() == TypeAnnonce.RADIATION && derniereAnnonceEmise.getType() == TypeAnnonce.RADIATION) {
							return null;
						}
						// Contenu identique, on ne renvoie pas.
						if (modeleActuel.getContenu() != null && modeleActuel.getContenu().equals(derniereAnnonceEmise.getContenu())) {
							return null;
						}
					} else {
						// Cas de notre dernière annonce encore dans l'esb.
						throw new ServiceIDEException("Une annonce est en attente de reception par le registre civil des entreprises (RCEnt). " +
								                              "Ce traitement doit avoir lieu avant de pouvoir déterminer s'il faut annoncer de nouveaux changements.");
					}
				}

				// Note de développement, en vrac:
				// Comparer et décider
				// Comparer avec la derniere annonce eventuellement en cours.
				// Comparer avec les données présentes dans RCEnt (si c'est les mêmes, pas besoin de communiquer).
				// Déterminer que faire si des changements surviennent via des événements RCEnt? C'est probablement de nouvelles données apportées par d'autres services IDE.

				// Donc il s'agit, pour résumer:
				// - Déterminer les données "voulues" (notre base fait référence, à priori)
				// - Déterminer l'état de l'IDE (réel + futur sous la formes d'événements déjà envoyés)
				// - Déterminer la différence et annoncer le delta, si nécessaire, et si possible (message d'echec à l'utilisateur).
				// - La base d'Unireg est "toujours juste", pourvu que les informations annoncées par les autres services IDE soient pris en compte immédiatement.
					// --> Pose problème, l'événement n'est pas traité tout de suite. On doit donc se fier à autre chose! P. exemple la date de valeur des changements civils.
					//     Sinon on va annoncer des données obsolètes!
				// Pour aider, la surcharge ne doit être placée que lorsque RCEnt n'est pas juste. Donc au début, puis lorsqu'il y a un changement par l'ACI. Lors du retour de
				// ces changements par l'événement RCEnt, la surcharge doit être fermée!

				// Seule manière de procéder:
				// - Comparer pour chaque champ la version Unireg et la version IDE (RCEnt) pour une date donnée:
				//   -> s'il n'y a pas de version Unireg, c'est qu'il n'y a pas de surcharge, donc rien à faire.
				//   -> s'il y a une version Unireg, on a une surcharge. On doit potentiellement annoncer la valeur
				// - Contrôler si la valeur a déjà été annoncée. Pour cela, il faut rechercher le contenu de la dernière annonce
				//   émise auprès de RCEnt afin de comparer son contenu:
				//   - la dernière annonce est introuvable dans RCEnt. Elle n'a pas encore été traitée, donc impossible de connaître son contenu --> on sort du processus et on ne fait rien.
				//   - la dernière annonce est de contenu identique à Unireg. --> On ne fait rien, car on a déjà annoncé les changements.
				//   - la dernière annonce est de contenu différent à Unireg. --> Il faut emmettre une nouvelle annonce avec le contenu à jour.
				//

				// Problèmes non adressés:
				// - Lorsque des changements proviennent d'autres services IDE, ils ne sont pas visible avant notre traitement correspondant qui ferme la surcharge.
				//   Il y a un risque théorique de voir les changements saisis et annoncés une deuxième fois.
				// - Aucune annonce ne sera envoyée pour le seul changement dans la description du secteur d'activité, car il n'est pas surchargé et on n'a pas moyen de savoir s'il est changé.
				// - La détection de radiation émise fonctionne à minima et échoue si

				validerAnnonceIDE(modeleActuel);

				if (!validateOnly) {
					resultat = annonceIDEService.emettreAnnonceIDE(modeleActuel, etablissement);
				} else {
					resultat = modeleActuel;
				}
			} else {
				Audit.info(
						String.format("Etablissement secondaire non pris en compte pour annonces IDE. Etablissement analysé: n°%s, entreprise n°%s",
						              FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()),
						              FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())));
			}
		}
		return resultat;
	}


	@Override
	public void validerAnnonceIDE(ModeleAnnonceIDE modele) throws ServiceIDEException {
		final ModeleAnnonceIDE.Statut statut = serviceOrganisation.validerAnnonceIDE(modele);

		// Workaround du SIREF-9364, où le statut est "sans erreur" même lorsqu'il y en a.
		if (statut.getErreurs() != null && !statut.getErreurs().isEmpty()) {
			throw new AnnonceIDEValidationException("Le modèle d'annonce IDE a échoué à la validation (il y a des erreurs).", statut.getErreurs());
		}

		switch (statut.getStatut()) {
		case VALIDATION_SANS_ERREUR:
			break;
		case REJET_RCENT:
			throw new AnnonceIDEValidationException("Le modèle d'annonce IDE a échoué à la validation.", statut.getErreurs());
		default:
			throw new ServiceIDEException(String.format("Statut d'annonce inattendu retourné par le service de validation du registre civil: %s", statut.getStatut()));
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
	private AdresseAnnonceIDERCEnt getAdresseAnnonceIDERCEnt(AdresseGenerique adresse, RegDate date) {
		final AdresseAnnonceIDERCEnt adresseAnnonce;
		if (adresse != null) {
			final Pays pays = infraService.getPays(adresse.getNoOfsPays(), date);
			final CasePostale casePostale = adresse.getCasePostale();
			final Integer numeroPostal = Integer.parseInt(adresse.getNumeroPostal());
			adresseAnnonce = RCEntAnnonceIDEHelper
					.createAdresseAnnonceIDERCEnt(adresse.getRue(), adresse.getNumero(),
					                              adresse.getNumeroAppartement() == null ? null : adresse.getNumeroAppartement(),
					                              numeroPostal,
					                              adresse.getLocalite(),
					                              pays == null ? null : pays.getNoOfsEtatSouverain(), pays == null ? null : pays.getCodeIso2(), pays == null ? null : pays.getNomCourt(),
					                              casePostale == null ? null : casePostale.getNumero(), casePostale == null ? null : String.valueOf(casePostale.getType()),
					                              adresse.getEgid());
		} else {
			adresseAnnonce = null;
		}
		return adresseAnnonce;
	}
}
