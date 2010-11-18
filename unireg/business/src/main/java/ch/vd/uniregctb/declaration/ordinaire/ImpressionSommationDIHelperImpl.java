package ch.vd.uniregctb.declaration.ordinaire;

import java.text.SimpleDateFormat;

import ch.vd.uniregctb.adresse.TypeAdresseFiscale;

import noNamespace.FichierImpressionDocument;
import noNamespace.InfoArchivageDocument;
import noNamespace.InfoDocumentDocument1;
import noNamespace.InfoEnteteDocumentDocument1;
import noNamespace.SommationDIDocument;
import noNamespace.TypAdresse;
import noNamespace.TypFichierImpression;
import noNamespace.TypPeriode;
import noNamespace.CleRgpDocument.CleRgp;
import noNamespace.InfoArchivageDocument.InfoArchivage;
import noNamespace.InfoDocumentDocument1.InfoDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.SommationDIDocument.SommationDI;
import noNamespace.SommationDIDocument.SommationDI.LettreSom;
import noNamespace.TypFichierImpression.Document;
import noNamespace.TypPeriode.Entete;
import noNamespace.TypPeriode.Entete.ImpCcn;
import noNamespace.TypPeriode.Entete.Tit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.editique.impl.EditiqueServiceImpl;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

/**
 *
 * Classe utilitaire pour la génération du xml à envoyer à éditique
 *
 * @author xsifnr
 *
 */
public class ImpressionSommationDIHelperImpl implements ImpressionSommationDIHelper {

	private static final String VERSION_XSD = "1.0";

	/** Le type de document à transmettre au service pour la sommation DI */
	public static final String TYPE_DOCUMENT_SOMMATION_DI = "385";

	private static final Logger LOGGER = Logger.getLogger(ImpressionSommationDIHelperImpl.class);

	private static final char [] DIGITS = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

	private EditiqueHelper editiqueHelper;
	private AdresseService adresseService;
	private TiersService tiersService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private DelaisService delaisService;

	public ImpressionSommationDIHelperImpl() {

	}

	public ImpressionSommationDIHelperImpl(ServiceInfrastructureService serviceInfrastructureService, AdresseService adresseService, TiersService tiersService,
											EditiqueHelper editiqueHelper) {
		this.serviceInfrastructureService = serviceInfrastructureService;
		this.adresseService = adresseService;
		this.tiersService = tiersService;
		this.editiqueHelper = editiqueHelper;
	}

	public String calculPrefixe() {
		return "RGPS0801";
	}

	public FichierImpressionDocument remplitSommationDI(ImpressionSommationDIHelperParams params) throws EditiqueException {
		final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
		TypFichierImpression typeFichierImpression = mainDocument.addNewFichierImpression();
		InfoDocument infoDocument = remplitInfoDocument(params);
		InfoArchivage infoArchivage = remplitInfoArchivage(params);
		InfoEnteteDocument infoEnteteDocument;
		try {
			infoEnteteDocument = remplitEnteteDocument(params);
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

	private InfoEnteteDocument remplitEnteteDocument(ImpressionSommationDIHelperParams params) throws EditiqueException {
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
		catch (Exception e) {
			throw new EditiqueException(e);
		}

		return infoEnteteDocument;
	}

	private String getLocaliteExpedition(DeclarationImpotOrdinaire di) throws EditiqueException {

		Integer oid =  tiersService.getOfficeImpotId(di.getTiers());
		String sLocalite = "Lausanne";
		if (oid == null) {
			LOGGER.warn( String.format(
					"oid null pour le tiers %s, Localité d'expedition par defaut : %s",
					di.getTiers().getNumero(),
					sLocalite));
		} else  {
			CollectiviteAdministrative collectiviteAdministrative = null;
			try {
				collectiviteAdministrative = serviceInfrastructureService.getCollectivite(oid);
			} catch (InfrastructureException e) {
				collectiviteAdministrative = null;
				LOGGER.warn("Impossible de retrouver la collectivité administrative " + oid, e);
			}

			if (collectiviteAdministrative != null) {
				Adresse adresse = collectiviteAdministrative.getAdresse();
				if (adresse != null) {
					int onrp = adresse.getNumeroOrdrePostal();
					Localite localite;
					try {
						localite = serviceInfrastructureService.getLocaliteByONRP(onrp);
					} catch (InfrastructureException e) {
						localite = null;
						LOGGER.warn("Impossible de retrouver la localité dont l'onrp est " + onrp, e);
					}
					if (localite != null && localite.getCommuneLocalite() != null ) {
						sLocalite = localite.getCommuneLocalite().getNomMinuscule();
					} else {
						// Impossible de retrouver la localité, on se débrouille
						sLocalite = adresse.getLocalite();
						for (char c : DIGITS) {
							sLocalite = StringUtils.remove(sLocalite, c);
						}
						sLocalite = sLocalite.trim();
					}
				}
			}
		}
		return sLocalite;
	}

	private InfoDocument remplitInfoDocument(ImpressionSommationDIHelperParams params) throws EditiqueException {
		InfoDocument infoDocument = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
		String prefixe = calculPrefixe() + "DOCUM";
		infoDocument.setPrefixe(prefixe);
		infoDocument.setTypDoc("SD");
		infoDocument.setCodDoc("SOMM_DI");
		infoDocument.setVersion(VERSION_XSD);
		infoDocument.setLogo("CANT");
		infoDocument.setPopulations("PP");
		try {
			AdresseEnvoiDetaillee adresseEnvoiDetaillee = adresseService.getAdresseEnvoi(params.getDi().getTiers(), null, TypeAdresseFiscale.COURRIER, false);
			String idEnvoi = "";
			if (adresseEnvoiDetaillee.isSuisse() && !params.isMiseSousPliImpossible()) {
				idEnvoi = "";
			} else {
				final Integer officeImpotId = tiersService.getOfficeImpotId(params.getDi().getTiers());
			if (officeImpotId != null) {
					idEnvoi = officeImpotId.toString();
				} else {
					LOGGER.warn("OID null");
				}
			}
			infoDocument.setIdEnvoi(idEnvoi);
		} catch (Exception e) {
			final String message = "Exception lors de l'identification de la provenance de l'adresse";
			LOGGER.error("Exception lors de l'identification de la provenance de l'adresse du tiers " + params.getDi().getTiers().getNumero(), e);
			throw new EditiqueException(message, e);
		}
		CleRgp cleRgp = infoDocument.addNewCleRgp();
		cleRgp.addNewGenreImpot();
		return infoDocument;
	}

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

	private SommationDI remplitSpecifiqueSommationDI(ImpressionSommationDIHelperParams params) throws EditiqueException {
		SommationDI sommationDI  = SommationDIDocument.Factory.newInstance().addNewSommationDI();
		TypPeriode periode = sommationDI.addNewPeriode();
		periode.setPrefixe(calculPrefixe() + "PERIO");
		periode.setOrigDuplicat("ORG");
		periode.setHorsSuisse("");
		periode.setHorsCanton("");
		periode.setAnneeFiscale(params.getDi().getPeriode().getAnnee().toString());
		periode.setDateDecompte(
				RegDateHelper.toIndexString(
						params.getDi().getEtatDeclarationActif(TypeEtatDeclaration.EMISE).getDateObtention()
				)
		);
		periode.setDatDerCalculAc("");
		Entete entete = periode.addNewEntete();
		Tit tit = entete.addNewTit();
		tit.setPrefixe(calculPrefixe() + "TITIM");
		tit.setLibTit(
				String.format(
						"Invitation à déposer la déclaration %s - Sommation",
						params.getDi().getPeriode().getAnnee().toString()));
		ImpCcn impCcn = entete.addNewImpCcn();
		impCcn.setPrefixe(calculPrefixe() + "IMPCC");
		impCcn.setLibImpCcn("");

		LettreSom lettreSom = sommationDI.addNewLettreSom();
		lettreSom.setOFS(editiqueHelper.getCommune(params.getDi()));
		final String formuleAppel = adresseService.getFormulePolitesse(params.getDi().getTiers()).formuleAppel();
		lettreSom.setCivil(formuleAppel);
		lettreSom.setPeriodeFiscal(params.getDi().getPeriode().getAnnee().toString());
		lettreSom.setDateEnrg(RegDateHelper.toIndexString(params.getDateTraitement()));
		return sommationDI;
	}

	private InfoArchivage remplitInfoArchivage(ImpressionSommationDIHelperParams params) {
		InfoArchivage infoArchivage = InfoArchivageDocument.Factory.newInstance().addNewInfoArchivage();
		infoArchivage.setPrefixe(calculPrefixe() + "FOLDE");
		infoArchivage.setNomApplication("FOLDERS");
		infoArchivage.setTypDossier(EditiqueServiceImpl.TYPE_DOSSIER_UNIREG);
		String numeroCTB = FormatNumeroHelper.numeroCTBToDisplay(params.getDi().getTiers().getNumero());
		infoArchivage.setNomDossier(numeroCTB);
		infoArchivage.setTypDocument(TYPE_DOCUMENT_SOMMATION_DI);
		String idDocument = construitIdArchivageDocument(params.getDi());
		infoArchivage.setIdDocument(idDocument);
		infoArchivage.setDatTravail(String.valueOf(params.getDateTraitement().index()));
		return infoArchivage;
	}

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

	public void setEditiqueHelper(EditiqueHelper editiqueHelper) {
		this.editiqueHelper = editiqueHelper;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}
	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public void setDelaisService(DelaisService delaisService) {
		this.delaisService = delaisService;
	}

}
