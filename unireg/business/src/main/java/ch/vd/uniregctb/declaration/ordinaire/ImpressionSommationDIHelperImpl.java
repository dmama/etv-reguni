package ch.vd.uniregctb.declaration.ordinaire;

import java.text.SimpleDateFormat;

import noNamespace.CleRgpDocument.CleRgp;
import noNamespace.FichierImpressionDocument;
import noNamespace.InfoArchivageDocument.InfoArchivage;
import noNamespace.InfoDocumentDocument1;
import noNamespace.InfoDocumentDocument1.InfoDocument;
import noNamespace.InfoEnteteDocumentDocument1;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.SommationDIDocument;
import noNamespace.SommationDIDocument.SommationDI;
import noNamespace.SommationDIDocument.SommationDI.LettreSom;
import noNamespace.TypAdresse;
import noNamespace.TypFichierImpression;
import noNamespace.TypFichierImpression.Document;
import noNamespace.TypPeriode;
import noNamespace.TypPeriode.Entete;
import noNamespace.TypPeriode.Entete.ImpCcn;
import noNamespace.TypPeriode.Entete.Tit;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.StatutMenageCommun;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

/**
 *
 * Classe utilitaire pour la génération du xml à envoyer à éditique
 *
 * @author xsifnr
 *
 */
public class ImpressionSommationDIHelperImpl extends EditiqueAbstractHelper implements ImpressionSommationDIHelper {

	private static final String VERSION_XSD = "1.0";

	private static final Logger LOGGER = Logger.getLogger(ImpressionSommationDIHelperImpl.class);

	private ServiceInfrastructureService serviceInfrastructureService;
	private DelaisService delaisService;

	public ImpressionSommationDIHelperImpl() {
	}

	public ImpressionSommationDIHelperImpl(ServiceInfrastructureService serviceInfrastructureService, AdresseService adresseService, TiersService tiersService,
											EditiqueHelper editiqueHelper, DelaisService delaisService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
		this.adresseService = adresseService;
		this.tiersService = tiersService;
		this.editiqueHelper = editiqueHelper;
		this.delaisService = delaisService;
	}

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.SOMMATION_DI;
	}


	@Override
	public FichierImpressionDocument remplitSommationDI(final ImpressionSommationDIHelperParams params) throws EditiqueException {
		TraitementRemplissageSommation traitement = determineStrategieDeRemplissageDeLaSommation(params);
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
		if (params.getDi().getTiers() instanceof MenageCommun) {
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

		InfoArchivage remplitInfoArchivage(ImpressionSommationDIHelperParams params) {
			final DeclarationImpotOrdinaire di = params.getDi();
			return editiqueHelper.buildInfoArchivage(getTypeDocumentEditique(), di.getTiers().getNumero(), construitIdArchivageDocument(di), params.getDateTraitement());
		}

		InfoEnteteDocument remplitEnteteDocument(ImpressionSommationDIHelperParams params) throws EditiqueException {
			InfoEnteteDocument infoEnteteDocument = InfoEnteteDocumentDocument1.Factory.newInstance().addNewInfoEnteteDocument();

			try {
				TypAdresse porteAdresse = editiqueHelper.remplitPorteAdresse(params.getDi().getTiers(), infoEnteteDocument);
				infoEnteteDocument.setPorteAdresse(porteAdresse);
				Expediteur expediteur = editiqueHelper.remplitExpediteurACI(infoEnteteDocument);
				if (params.isBatch()) {
					expediteur.setDateExpedition(
							RegDateHelper.toIndexString(
									delaisService.getDateFinDelaiCadevImpressionDeclarationImpot(params.getDateTraitement())));
				} else {
					expediteur.setDateExpedition(RegDateHelper.toIndexString(params.getDateTraitement()));
				}
				if (params.isOnline() && !StringUtils.isEmpty(params.getNoTelephone())) {
					expediteur.setNumTelephone(params.getNoTelephone());
				} else {
					expediteur.setNumTelephone(serviceInfrastructureService.getCAT().getNoTelephone());
					expediteur.setNumFax(serviceInfrastructureService.getCAT().getNoFax());
				}
				expediteur.setTraitePar(params.getTraitePar());
				expediteur.setLocaliteExpedition(getLocaliteExpedition(params.getDi()));
				expediteur.setAdrMes(params.getAdrMsg());
				infoEnteteDocument.setExpediteur(expediteur);
				Destinataire destinataire = editiqueHelper.remplitDestinataire(params.getDi().getTiers(), infoEnteteDocument);
				infoEnteteDocument.setDestinataire(destinataire);
			}
			catch (EditiqueException e) {
				throw e;
			}
			catch (Exception e) {
				throw new EditiqueException(e);
			}

			return infoEnteteDocument;
		}

		SommationDI remplitSpecifiqueSommationDI(ImpressionSommationDIHelperParams params) throws EditiqueException {
			final SommationDI sommationDI  = SommationDIDocument.Factory.newInstance().addNewSommationDI();
			final TypPeriode periode = sommationDI.addNewPeriode();
			final TypeDocumentEditique typeDocumentEditique = getTypeDocumentEditique();
			periode.setPrefixe(buildPrefixePeriode(typeDocumentEditique));
			periode.setOrigDuplicat(ORIGINAL);
			periode.setHorsSuisse("");
			periode.setHorsCanton("");
			periode.setAnneeFiscale(params.getDi().getPeriode().getAnnee().toString());
			periode.setDateDecompte(
					RegDateHelper.toIndexString(
							params.getDi().getDernierEtatOfType(TypeEtatDeclaration.EMISE).getDateObtention()
					)
			);
			periode.setDatDerCalculAc("");
			final Entete entete = periode.addNewEntete();
			final Tit tit = entete.addNewTit();
			tit.setPrefixe(buildPrefixeTitreEntete(typeDocumentEditique));
			tit.setLibTit(
					String.format(
							"Invitation à déposer la déclaration %s - Sommation",
							params.getDi().getPeriode().getAnnee().toString()));
			final ImpCcn impCcn = entete.addNewImpCcn();
			impCcn.setPrefixe(buildPrefixeImpCcnEntete(typeDocumentEditique));
			impCcn.setLibImpCcn("");

			final LettreSom lettreSom = sommationDI.addNewLettreSom();
			lettreSom.setOFS(editiqueHelper.getCommune(params.getDi()));
			final String formuleAppel = adresseService.getFormulePolitesse(params.getDi().getTiers()).formuleAppel();
			lettreSom.setCivil(formuleAppel);
			lettreSom.setPeriodeFiscal(params.getDi().getPeriode().getAnnee().toString());
			lettreSom.setDateEnrg(RegDateHelper.toIndexString(params.getDateTraitement()));
			return sommationDI;
		}

		private String getLocaliteExpedition(DeclarationImpotOrdinaire di) throws EditiqueException {

			final Integer oid =  tiersService.getOfficeImpotId(di.getTiers());
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
						final int onrp = adresse.getNumeroOrdrePostal();
						Localite localite = null;
						try {
							localite = serviceInfrastructureService.getLocaliteByONRP(onrp);
						} catch (ServiceInfrastructureException e) {
							LOGGER.warn("Impossible de retrouver la localité dont l'onrp est " + onrp, e);
						}
						if (localite != null) {
							sLocalite = localite.getNomCompletMinuscule();
						}
						else {
							// Impossible de retrouver la localité, on se débrouille
							sLocalite = adresse.getLocalite();
							sLocalite = sLocalite.replaceAll("[0-9]+", "");
							sLocalite = sLocalite.trim();
						}
					}
				}
			}
			return sLocalite;
		}

		InfoDocument remplitInfoDocument(ImpressionSommationDIHelperParams params) throws EditiqueException {
			final InfoDocument infoDocument = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
			final String prefixe = buildPrefixeInfoDocument(getTypeDocumentEditique());
			infoDocument.setPrefixe(prefixe);
			infoDocument.setTypDoc("SD");
			infoDocument.setCodDoc("SOMM_DI");
			infoDocument.setVersion(VERSION_XSD);
			infoDocument.setLogo(LOGO_CANTON);
			infoDocument.setPopulations(POPULATION_PP);
			remplitAffranchissement(infoDocument, params.getDi().getTiers(), null, params.isMiseSousPliImpossible());
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
			InfoDocument infoDocument = remplitInfoDocument(params);
			InfoArchivage infoArchivage = remplitInfoArchivage(params);
			InfoEnteteDocument infoEnteteDocument;
			try {
				infoEnteteDocument = remplitEnteteDocument(params);
			}
			catch (EditiqueException e) {
				throw e;
			}
			catch (Exception e) {
				throw new EditiqueException(e);
			}
			Document document = typeFichierImpression.addNewDocument();
			document.setSommationDI(remplitSpecifiqueSommationDI(params));
			document.setInfoEnteteDocument(infoEnteteDocument);
			document.setInfoDocument(infoDocument);
			document.setInfoArchivage(infoArchivage);
			typeFichierImpression.setDocumentArray(new Document[]{ document });
			return mainDocument;
		}


	}

	/**
	 * Deuxieme stratégie de remplissage: Pour les ménages séparés (lorsque ils se sont séparés entre l'émission de la DI et sa sommation),
	 * On envoie le même document aux 2 membres à leures adresses respectives
	 */
	class TraitementRemplissageSommationSepares extends TraitementRemplissageSommation {

		private PersonnePhysique separe1;
		private PersonnePhysique separe2;

		TraitementRemplissageSommationSepares (PersonnePhysique separe1, PersonnePhysique separe2) {
			this.separe1 = separe1;
			this.separe2 = separe2;
		}

		@Override
		public FichierImpressionDocument remplitSommationDI(ImpressionSommationDIHelperParams params) throws EditiqueException {
			final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
			TypFichierImpression typeFichierImpression = mainDocument.addNewFichierImpression();
			InfoDocument infoDocument1 = remplitInfoDocument(params, separe1, separe2, 1);
			InfoDocument infoDocument2 = remplitInfoDocument(params, separe2, separe1, 2);
			InfoArchivage infoArchivage1 = remplitInfoArchivage(params);
			InfoArchivage infoArchivage2 = remplitInfoArchivage(params);
			InfoEnteteDocument infoEnteteDocument1, infoEnteteDocument2;
			try {
				infoEnteteDocument1 = remplitEnteteDocument(params);
				infoEnteteDocument2 = remplitEnteteDocument(params);
			}
			catch (EditiqueException e) {
				throw e;
			}
			catch (Exception e) {
				throw new EditiqueException(e);
			}

			Document document1 = typeFichierImpression.addNewDocument();
			document1.setSommationDI(remplitSpecifiqueSommationDI(params));
			document1.setInfoEnteteDocument(infoEnteteDocument1);
			document1.setInfoDocument(infoDocument1);
			document1.setInfoArchivage(infoArchivage1);

			Document document2 = typeFichierImpression.addNewDocument();
			document2.setSommationDI(remplitSpecifiqueSommationDI(params));
			document2.setInfoEnteteDocument(infoEnteteDocument2);
			document2.setInfoDocument(infoDocument2);
			document2.setInfoArchivage(infoArchivage2);

			typeFichierImpression.setDocumentArray(new Document[]{ document1, document2 });
			return mainDocument;
		}

		private InfoDocument remplitInfoDocument(ImpressionSommationDIHelperParams params, PersonnePhysique destinataire, PersonnePhysique exConjoint, int noSepare) throws EditiqueException {
			// remplissage de la partie commune
			final InfoDocument infoDocument = remplitInfoDocument(params);

			// remplissage de la partie spécifique aux séparés
			InfoDocument.Separes separes = infoDocument.addNewSepares();
			separes.setNumero(Integer.toString(noSepare));
			separes.setEnvoieA(tiersService.getNomPrenom(exConjoint));
			TypAdresse.Adresse adresse = separes.addNewAdresse();
			try {
				AdresseEnvoi adresseEnvoi = adresseService.getAdresseEnvoi(destinataire, null, TypeAdresseFiscale.COURRIER, false);
				editiqueHelper.remplitAdresse(adresseEnvoi, adresse);
			}
			catch (AdresseException e) {
				throw new EditiqueException(e);
			}
			return infoDocument;
		}
	}

}
