package ch.vd.uniregctb.declaration.ordinaire.pp;

import java.text.SimpleDateFormat;
import java.util.Optional;

import noNamespace.CleRgpDocument.CleRgp;
import noNamespace.FichierImpressionDocument;
import noNamespace.InfoDocumentDocument1.InfoDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.SommationDIDocument.SommationDI;
import noNamespace.SommationDIDocument.SommationDI.LettreSom;
import noNamespace.TypAdresse;
import noNamespace.TypFichierImpression;
import noNamespace.TypFichierImpression.Document;
import noNamespace.TypPeriode;
import noNamespace.TypPeriode.Entete;
import noNamespace.TypPeriode.Entete.ImpCcn;
import noNamespace.TypPeriode.Entete.Tit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.EditiqueAbstractLegacyHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiquePrefixeHelper;
import ch.vd.uniregctb.editique.LegacyEditiqueHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.editique.impl.ExpediteurNillableValuesFiller;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.StatutMenageCommun;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

/**
 *
 * Classe utilitaire pour la génération du xml à envoyer à éditique
 *
 * @author xsifnr
 *
 */
public class ImpressionSommationDeclarationImpotPersonnesPhysiquesHelperImpl extends EditiqueAbstractLegacyHelper implements ImpressionSommationDeclarationImpotPersonnesPhysiquesHelper {

	private static final String VERSION_XSD = "1.0";
	private static final int OID_LA_VALLEE = 8;

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpressionSommationDeclarationImpotPersonnesPhysiquesHelperImpl.class);

	private ServiceInfrastructureService serviceInfrastructureService;
	private DelaisService delaisService;

	public ImpressionSommationDeclarationImpotPersonnesPhysiquesHelperImpl() {
	}

	public ImpressionSommationDeclarationImpotPersonnesPhysiquesHelperImpl(ServiceInfrastructureService serviceInfrastructureService, AdresseService adresseService, TiersService tiersService,
	                                                                       LegacyEditiqueHelper editiqueHelper, DelaisService delaisService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
		this.adresseService = adresseService;
		this.tiersService = tiersService;
		this.legacyEditiqueHelper = editiqueHelper;
		this.delaisService = delaisService;
	}

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.SOMMATION_DI;
	}


	@Override
	public FichierImpressionDocument remplitSommationDI(final ImpressionSommationDIHelperParams params) throws EditiqueException {
		final TraitementRemplissageSommation traitement = determineStrategieDeRemplissageDeLaSommation(params);
		return traitement.remplitSommationDI(params);
	}

	private TraitementRemplissageSommation determineStrategieDeRemplissageDeLaSommation(final ImpressionSommationDIHelperParams params) {

		/*
		 * LA stratégie par défaut, la sommation est envoyée à une seule adresse
		 */
		TraitementRemplissageSommation traitement = new TraitementRemplissageSommationStandard();

		/*
 		 * SIFISC-7139: Nouvelle stratégie pour les ménages communs séparés, la sommation est envoyée
 		 * à chacun des ex-membres du ménage avec un porte-adresse different pour chaque membre.
		 */
		final Tiers tiers = params.getDi().getTiers();
		if (tiers instanceof MenageCommun) {
			final MenageCommun mc = (MenageCommun) tiers;
			final EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(mc, null);
			if (etc.getPrincipal() != null && etc.getConjoint() != null) {
				if (tiersService.getStatutMenageCommun(mc) == StatutMenageCommun.TERMINE_SUITE_SEPARATION) {
					traitement = new TraitementRemplissageSommationSepares(etc.getPrincipal(), etc.getConjoint());
				}
			}
		}

		return traitement;
	}

	@Override
	public String construitIdDocument(DeclarationImpotOrdinaire declaration) {
		return String.format(
				"%s %s %s %s",
				declaration.getPeriode().getAnnee().toString(),
				StringUtils.leftPad(declaration.getNumero().toString(), 2, '0'),
				StringUtils.leftPad(declaration.getTiers().getNumero().toString(), 9, '0'),
				new SimpleDateFormat("yyyyMMddHHmmssSSS").format(
						declaration.getLogCreationDate()
				)
		);
	}

	@Override
	public String construitIdArchivageDocument(DeclarationImpotOrdinaire declaration) {
		return String.format(
				"%s%s %s %s",
				declaration.getPeriode().getAnnee().toString(),
				StringUtils.leftPad(declaration.getNumero().toString(), 2, '0'),
				StringUtils.rightPad("Sommation DI", 19, ' '),
				new SimpleDateFormat("MMddHHmmssSSS").format(
						declaration.getLogCreationDate()
				)
		);
	}

	@Override
	public String construitAncienIdArchivageDocument(DeclarationImpotOrdinaire declaration) {
		return String.format(
				"%s%s %s %s",
				declaration.getPeriode().getAnnee().toString(),
				StringUtils.leftPad(declaration.getNumero().toString(), 2, '0'),
				"Sommation DI        ",
				new SimpleDateFormat("yyyyMMddHHmm").format(
						declaration.getLogCreationDate()
				)
		);
	}

	@Override
	public String construitAncienIdArchivageDocumentPourOnLine(DeclarationImpotOrdinaire declaration) {
		return String.format(
				"%s%s %s %s",
				declaration.getPeriode().getAnnee().toString(),
				StringUtils.leftPad(declaration.getNumero().toString(), 2, '0'),
				"Sommation DI        ",
				new SimpleDateFormat("yyyyMMddHHmmssSSS").format(
						declaration.getLogCreationDate()
				)
		);
	}

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public void setDelaisService(DelaisService delaisService) {
		this.delaisService = delaisService;
	}

	abstract class TraitementRemplissageSommation {

		abstract FichierImpressionDocument remplitSommationDI(ImpressionSommationDIHelperParams params) throws EditiqueException;

		String getLocaliteExpedition(DeclarationImpotOrdinaire di) throws EditiqueException {

			// [SIFISC-20149] l'expéditeur de la sommation de DI PP est la nouvelle entité, si applicable
			final Integer oid = Optional.ofNullable(getNoCollectiviteAdministrativeEmettriceSelonEtiquettes(di.getTiers(), RegDate.get()))
										.orElseGet(() -> tiersService.getOfficeImpotId(di.getTiers()));

			String sLocalite = "Lausanne";
			if (oid == null) {
				LOGGER.warn(String.format("oid null pour le tiers %s, Localité d'expedition par defaut : %s", di.getTiers().getNumero(), sLocalite));
			}
			else  {
				CollectiviteAdministrative collectiviteAdministrative = null;
				try {
					collectiviteAdministrative = serviceInfrastructureService.getCollectivite(oid);
				} catch (ServiceInfrastructureException e) {
					LOGGER.warn("Impossible de retrouver la collectivité administrative " + oid, e);
				}

				if (collectiviteAdministrative != null) {
					Adresse adresse = collectiviteAdministrative.getAdresse();
					if (adresse != null) {
						final Integer onrp = adresse.getNumeroOrdrePostal();
						Localite localite = null;
						if (onrp != null) {
							try {
								localite = serviceInfrastructureService.getLocaliteByONRP(onrp, null);
							}
							catch (ServiceInfrastructureException e) {
								LOGGER.warn("Impossible de retrouver la localité dont l'onrp est " + onrp, e);
							}
						}
						if (localite != null) {
							//SIFISC-3468: Exception pour la localité de La vallée, on prend le nom de la localité et non de la commune
							if (OID_LA_VALLEE == oid) {
								sLocalite = localite.getNom();
							}
							else {
								if (localite.getCommuneLocalite() != null) {
									sLocalite = localite.getCommuneLocalite().getNomCourt();
								}
								else{
									// Impossible de retrouver lacommune de la localité, on se débrouille
									sLocalite = extractLocaliteFromAdresse(adresse);
								}
							}

						}
						else {
							// Impossible de retrouver la localité, on se débrouille
							sLocalite = extractLocaliteFromAdresse(adresse);
						}
					}
				}
			}
			return sLocalite;
		}

		private String extractLocaliteFromAdresse(Adresse adresse) {
			String sLocalite = null;
			sLocalite = adresse.getLocalite();
			sLocalite = sLocalite.replaceAll("[0-9]+", "");
			sLocalite = sLocalite.trim();
			return sLocalite;
		}


		void ajouteInfoEnteteDocument(Document document, ImpressionSommationDIHelperParams params) throws AdresseException, EditiqueException {
			final InfoEnteteDocument infoEnteteDocument = document.addNewInfoEnteteDocument();
			legacyEditiqueHelper.remplitPorteAdresse(params.getDi().getTiers(), infoEnteteDocument);

			// [SIFISC-20149] l'expéditeur de la sommation de DI PP doit être la nouvelle entité si applicable, sinon, l'ACI
			final int noCaExpeditrice = Optional.ofNullable(getNoCollectiviteAdministrativeEmettriceSelonEtiquettes(params.getDi().getTiers(), RegDate.get())).orElse(ServiceInfrastructureService.noACI);
			final ch.vd.uniregctb.tiers.CollectiviteAdministrative caExpeditrice = tiersService.getCollectiviteAdministrative(noCaExpeditrice);

			final Expediteur expediteur = legacyEditiqueHelper.remplitExpediteur(caExpeditrice, infoEnteteDocument);
			final ExpediteurNillableValuesFiller expNilValues = new ExpediteurNillableValuesFiller();
			expNilValues.init(expediteur);
			if (params.isBatch()) {
				expediteur.setDateExpedition(RegDateHelper.toIndexString(delaisService.getDateFinDelaiCadevImpressionDeclarationImpot(params.getDateTraitement())));
			}
			else {
				expediteur.setDateExpedition(RegDateHelper.toIndexString(params.getDateTraitement()));
			}
			if (params.isOnline() && !StringUtils.isEmpty(params.getNoTelephone())) {
				expNilValues.setNumTelephone(params.getNoTelephone());
			}
			else {
				expNilValues.setNumTelephone(serviceInfrastructureService.getCAT().getNoTelephone());
				expNilValues.setNumFax(serviceInfrastructureService.getCAT().getNoFax());
			}
			expediteur.setTraitePar(params.getTraitePar());
			expediteur.setLocaliteExpedition(getLocaliteExpedition(params.getDi()));
			expNilValues.setAdrMes(params.getAdrMsg());
			expNilValues.fill(expediteur);
			legacyEditiqueHelper.remplitDestinataire(params.getDi().getTiers(), infoEnteteDocument);
		}

		void ajouteSommationDI(Document document, ImpressionSommationDIHelperParams params) throws EditiqueException {
			final SommationDI sommationDI = document.addNewSommationDI();
			final TypPeriode periode = sommationDI.addNewPeriode();
			final TypeDocumentEditique typeDocumentEditique = getTypeDocumentEditique();
			periode.setPrefixe(EditiquePrefixeHelper.buildPrefixePeriode(typeDocumentEditique));
			periode.setOrigDuplicat(ORIGINAL);
			periode.setHorsSuisse("");
			periode.setHorsCanton("");

			final DeclarationImpotOrdinairePP di = params.getDi();
			final PeriodeFiscale pf = di.getPeriode();
			final String pfStr = pf.getAnnee().toString();
			periode.setAnneeFiscale(pfStr);
			periode.setDateDecompte(RegDateHelper.toIndexString(di.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.EMISE).getDateObtention()));
			periode.setDatDerCalculAc("");
			final Entete entete = periode.addNewEntete();
			final Tit tit = entete.addNewTit();
			tit.setPrefixe(EditiquePrefixeHelper.buildPrefixeTitreEntete(typeDocumentEditique));
			tit.setLibTit(String.format("Invitation à déposer la déclaration %s - Sommation", pfStr));
			final ImpCcn impCcn = entete.addNewImpCcn();
			impCcn.setPrefixe(EditiquePrefixeHelper.buildPrefixeImpCcnEntete(typeDocumentEditique));
			impCcn.setLibImpCcn("");

			final LettreSom lettreSom = sommationDI.addNewLettreSom();
			lettreSom.setOFS(legacyEditiqueHelper.getCommune(di));
			final String formuleAppel = adresseService.getFormulePolitesse(di.getTiers()).formuleAppel();
			lettreSom.setCivil(formuleAppel);
			lettreSom.setPeriodeFiscal(pfStr);
			lettreSom.setDateEnrg(RegDateHelper.toIndexString(params.getDateTraitement()));
			if (pf.isShowCodeControleSommationDeclarationPP() && StringUtils.isNotBlank(di.getCodeControle())) {
				lettreSom.setCodeValidation(di.getCodeControle());
			}
			if (params.getMontantEmolument() != null && params.getMontantEmolument() > 0) {
				lettreSom.setMontantEmolument(params.getMontantEmolument());
			}
		}

		void ajouteInfoArchivage(Document document, ImpressionSommationDIHelperParams params) {
			final DeclarationImpotOrdinaire di = params.getDi();
			legacyEditiqueHelper.fillInfoArchivage(
					document.addNewInfoArchivage(),
					getTypeDocumentEditique(),
					di.getTiers().getNumero(),
					construitIdArchivageDocument(di),
					params.getDateTraitement());
		}

		InfoDocument ajouteInfoDocument(Document document, ImpressionSommationDIHelperParams params) throws EditiqueException {
			final InfoDocument infoDocument = document.addNewInfoDocument();
			final String prefixe = EditiquePrefixeHelper.buildPrefixeInfoDocument(getTypeDocumentEditique());
			infoDocument.setPrefixe(prefixe);
			infoDocument.setTypDoc("SD");
			infoDocument.setCodDoc("SOMM_DI");
			infoDocument.setVersion(VERSION_XSD);
			infoDocument.setLogo(LOGO_CANTON);
			infoDocument.setPopulations(ConstantesEditique.POPULATION_PP);
			final CleRgp cleRgp = infoDocument.addNewCleRgp();
			cleRgp.setAnneeFiscale(Integer.toString(params.getDi().getPeriode().getAnnee()));
			return infoDocument;
		}
	}

	/**
	 * Premiere stratégie de remplissage: Le standard, la sommation est envoyée à une seule adresse
	 */
	class TraitementRemplissageSommationStandard extends TraitementRemplissageSommation {

		@Override
		FichierImpressionDocument remplitSommationDI(ImpressionSommationDIHelperParams params) throws EditiqueException {
			final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
			TypFichierImpression typeFichierImpression = mainDocument.addNewFichierImpression();
			try {
				ajouteDocument(typeFichierImpression, params);
			}
			catch (EditiqueException e) {
				throw e;
			}
			catch (Exception e) {
				throw new EditiqueException(e);
			}
			return mainDocument;
		}

		private void ajouteDocument(TypFichierImpression typeFichierImpression, ImpressionSommationDIHelperParams params) throws EditiqueException, AdresseException {
			Document document = typeFichierImpression.addNewDocument();
			ajouteInfoDocument(document, params);
			ajouteInfoEnteteDocument(document, params);
			ajouteSommationDI(document, params);
			ajouteInfoArchivage(document, params);

		}

		InfoDocument ajouteInfoDocument(Document document, ImpressionSommationDIHelperParams params) throws EditiqueException {
			// remplissage de la partie commune
			final InfoDocument infoDocument = super.ajouteInfoDocument(document, params);
			// remplissage de la partie spécifique au traitement standard
			remplitAffranchissement(infoDocument, params.getDi().getTiers(), null, params.isMiseSousPliImpossible());
			return infoDocument;
		}

	}

	/**
	 * Deuxieme stratégie de remplissage: Pour les ménages séparés (lorsque ils se sont séparés entre l'émission de la DI et sa sommation),
	 * On envoie le même document aux 2 membres à leures adresses respectives
	 */
	class TraitementRemplissageSommationSepares extends TraitementRemplissageSommation {

		private final PersonnePhysique separe1;
		private final PersonnePhysique separe2;

		TraitementRemplissageSommationSepares (PersonnePhysique separe1, PersonnePhysique separe2) {
			this.separe1 = separe1;
			this.separe2 = separe2;
		}

		@Override
		public FichierImpressionDocument remplitSommationDI(ImpressionSommationDIHelperParams params) throws EditiqueException {
			final FichierImpressionDocument fichierImpression = FichierImpressionDocument.Factory.newInstance();
			TypFichierImpression typeFichierImpression = fichierImpression.addNewFichierImpression();
			try {
				ajouteDocument(typeFichierImpression, params, separe1, separe2, 1);
				ajouteDocument(typeFichierImpression, params, separe2, separe1, 2);
			}
			catch (EditiqueException e) {
				throw e;
			}
			catch (Exception e) {
				throw new EditiqueException(e);
			}
			return fichierImpression;
		}

		private void ajouteDocument(TypFichierImpression fichierImpression, ImpressionSommationDIHelperParams params, PersonnePhysique destinataire, PersonnePhysique exConjoint, int noSepare) throws
				Exception {
			Document document = fichierImpression.addNewDocument();
			ajouteSommationDI(document, params);
			ajouteInfoEnteteDocument(document, params);
			ajouteInfoDocument(document, params, destinataire, exConjoint, noSepare);
			ajouteInfoArchivage(document, params);

		}

		private void ajouteInfoDocument(Document document, ImpressionSommationDIHelperParams params, PersonnePhysique destinataire, PersonnePhysique exConjoint, int noSepare) throws
				EditiqueException, AdresseException {
			// remplissage de la partie commune
			final InfoDocument infoDocument = super.ajouteInfoDocument(document, params);

			// remplissage de la partie spécifique aux séparés
			remplitAffranchissement(infoDocument, destinataire, null, params.isMiseSousPliImpossible());
			InfoDocument.Separes separes = infoDocument.addNewSepares();
			separes.setNumero(Integer.toString(noSepare));
			separes.setEnvoieA(tiersService.getNomPrenom(exConjoint));
			TypAdresse.Adresse adresse = separes.addNewAdresse();
			AdresseEnvoi adresseEnvoi = adresseService.getAdresseEnvoi(destinataire, null, TypeAdresseFiscale.COURRIER, false);
			legacyEditiqueHelper.remplitAdresse(adresseEnvoi, adresse);
		}


	}

}
