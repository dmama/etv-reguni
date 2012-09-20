package ch.vd.uniregctb.declaration.source;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;

import noNamespace.BVRSTDDocument.BVRSTD;
import noNamespace.CleRgpDocument;
import noNamespace.FichierImpressionDocument;
import noNamespace.InfoArchivageDocument.InfoArchivage;
import noNamespace.InfoDocumentDocument1;
import noNamespace.InfoDocumentDocument1.InfoDocument;
import noNamespace.InfoEnteteDocumentDocument1;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.SLRLCBVRDocument;
import noNamespace.SLRLCBVRDocument.SLRLCBVR;
import noNamespace.SLRLCBVRDocument.SLRLCBVR.InfBVR;
import noNamespace.SLRLCBVRDocument.SLRLCBVR.SLRLC;
import noNamespace.SLRLCBVRDocument.SLRLCBVR.SLRLC.TitreDoc;
import noNamespace.TypAdresse;
import noNamespace.TypFichierImpression;
import noNamespace.TypFichierImpression.Document;
import noNamespace.TypPeriode;
import noNamespace.TypPeriode.Entete;
import noNamespace.TypPeriode.Entete.ImpCcn;
import noNamespace.TypPeriode.Entete.Tit;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrDemande;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrReponse;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.webservice.sipf.BVRPlusClient;

public class ImpressionSommationLRHelperImpl extends EditiqueAbstractHelper implements ImpressionSommationLRHelper {

	public static final Logger LOGGER = Logger.getLogger(ImpressionSommationLRHelperImpl.class);

	private static final String VERSION = "1.0";
	private static final String TYPE_DOC_SOMMATION_LR = "SL";

	private static final String LISTE_RECAPITULATIVE_MAJ = "LISTE RECAPITULATIVE";
	private static final String IMPOT_A_LA_SOURCE_MIN = "Impôt à la source";
	private static final String IMPOT_A_LA_SOURCE_MAJ = "IMPOT A LA SOURCE";

	private static final String HYP_MIN = "concernant l’impôt à la source perçu sur les intérêts hypothécaires";
	private static final String PRE_MIN = "pour les prestations de prévoyance";
	private static final String ADM_MIN = "pour les prestations versées aux administrateurs domiciliés à l’étranger";
	private static final String CAS_MIN = "pour les artistes, sportifs et conférenciers domiciliés à l’étranger";
	private static final String DECOMPTE_LR_MIN = "Décompte liste récapitulative";

	private static final String HYP_MAJ = "CREANCIERS HYPOTHECAIRES";
	private static final String PRE_MAJ = "PRESTATION PREVOYANCE";
	private static final String CAS_MAJ = "CONFERENCIERS ARTISTES SPORTIFS";
	private static final String ADM_MAJ = "ADMINISTRATEURS";

	private static final String CODE_DOC_SOMMATION_LR_REG = "SLR_SREG";
	private static final String CODE_DOC_SOMMATION_LR_REG_SANS_PREIMP = "SLR_SREG_NO_PRE";
	private static final String CODE_DOC_SOMMATION_LR_CAS = "SLR_SCAS";
	private static final String CODE_DOC_SOMMATION_LR_ADM = "SLR_SADM";
	private static final String CODE_DOC_SOMMATION_LR_PRE = "SLR_SPRE";
	private static final String CODE_DOC_SOMMATION_LR_HYP = "SLR_SHYP";
	private static final String CODE_DOC_SOMMATION_LR_LTN = "SLR_SLTN";

	private BVRPlusClient bvrPlusClient;
	private EditiqueHelper editiqueHelper;
	private AdresseService adresseService;

	public void setBvrPlusClient(BVRPlusClient bvrPlusClient) {
		this.bvrPlusClient = bvrPlusClient;
	}

	public void setEditiqueHelper(EditiqueHelper editiqueHelper) {
		this.editiqueHelper = editiqueHelper;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.SOMMATION_LR;
	}

	@Override
	public String construitIdArchivageDocument(DeclarationImpotSource lr) {
		return String.format(
				"%s%s %s %s",
				lr.getPeriode().getAnnee().toString(),
				StringUtils.leftPad(Integer.toString(lr.getDateDebut().month()), 2, "0"),
				StringUtils.rightPad("Sommation LR", 19, ' '),
				new SimpleDateFormat("MMddHHmmssSSS").format(
						lr.getLogCreationDate()
				)
		);
	}

	@Override
	public String construitIdDocument(DeclarationImpotSource lr) {
		return String.format(
				"%s %s %s %s",
				lr.getDateDebut().toString(),
				lr.getDateFin().toString(),
				StringUtils.leftPad(lr.getTiers().getNumero().toString(), 9, '0'),
				new SimpleDateFormat("yyyyMMddHHmmssSSS").format(
						lr.getLogCreationDate()
				)
		);
	}

	/**
	 * Génère l'objet pour l'impression de la sommation LR
	 * @param lr
	 * @param dateTraitement
	 * @return
	 * @throws EditiqueException
	 */
	@Override
	public FichierImpressionDocument remplitSommationLR(DeclarationImpotSource lr, RegDate dateTraitement) throws EditiqueException {
		final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
		TypFichierImpression impressionIS = mainDocument.addNewFichierImpression();
		Document[] documents = new Document[1];

		InfoDocument infoDocument = remplitInfoDocument(lr);
		InfoArchivage infoArchivage = remplitInfoArchivage(lr, dateTraitement);
		InfoEnteteDocument infoEnteteDocument;
		try {
			infoEnteteDocument = remplitEnteteDocument(lr);
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}

		SLRLCBVR slrlcbvr = remplitSpecifiqueSommationLR(lr, RegDate.get());
		Document document = impressionIS.addNewDocument();

		document.setInfoEnteteDocument(infoEnteteDocument);
		document.setInfoDocument(infoDocument);
		document.setSLRLCBVR(slrlcbvr);
		document.setInfoArchivage(infoArchivage);
		documents[0] = document;

		impressionIS.setDocumentArray(documents);

		return mainDocument;
	}


	/**
	 * Alimente la partie infoDocument du Document
	 *
	 * @return
	 */
	private InfoDocument remplitInfoDocument(DeclarationImpotSource lr) throws EditiqueException {
		final InfoDocument infoDocument = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
		infoDocument.setPrefixe(buildPrefixeInfoDocument(getTypeDocumentEditique()));
		infoDocument.setTypDoc(TYPE_DOC_SOMMATION_LR);

		final String codeDoc;
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) lr.getTiers();
		switch (dpi.getCategorieImpotSource()) {
			case REGULIERS:
				// [UNIREG-2054] seuls les débiteurs "Papier" doivent recevoir le pré-imprimé
				if (dpi.getModeCommunication() == ModeCommunication.PAPIER) {
					codeDoc = CODE_DOC_SOMMATION_LR_REG;
				}
				else {
					codeDoc = CODE_DOC_SOMMATION_LR_REG_SANS_PREIMP;
				}
				break;

			case CONFERENCIERS_ARTISTES_SPORTIFS:
				codeDoc = CODE_DOC_SOMMATION_LR_CAS;
				break;

			case ADMINISTRATEURS:
				codeDoc = CODE_DOC_SOMMATION_LR_ADM;
				break;

			case PRESTATIONS_PREVOYANCE:
				codeDoc = CODE_DOC_SOMMATION_LR_PRE;
				break;

			case CREANCIERS_HYPOTHECAIRES:
				codeDoc = CODE_DOC_SOMMATION_LR_HYP;
				break;

			case LOI_TRAVAIL_AU_NOIR:
				codeDoc = CODE_DOC_SOMMATION_LR_LTN;
				break;

			default:
				codeDoc = null;
				break;
		}
		infoDocument.setCodDoc(codeDoc);
		final CleRgpDocument.CleRgp cleRgp = infoDocument.addNewCleRgp();
		cleRgp.setAnneeFiscale(Integer.toString(lr.getPeriode().getAnnee()));
		infoDocument.setVersion(VERSION);
		infoDocument.setLogo(LOGO_CANTON);
		infoDocument.setPopulations(POPULATION_IS);
		editiqueHelper.remplitAffranchissement(infoDocument, dpi);
		return infoDocument;
	}

	/**
	 * Alimente l'entête du document
	 *
	 * @return
	 * @throws AdressesResolutionException
	 * @throws ServiceInfrastructureException
	 * @throws RemoteException
	 * @throws AdressesResolutionException
	 * @throws ServiceInfrastructureException
	 */
	protected InfoEnteteDocument remplitEnteteDocument(Declaration declaration) throws AdresseException, ServiceInfrastructureException {
		final InfoEnteteDocument infoEnteteDocument = InfoEnteteDocumentDocument1.Factory.newInstance().addNewInfoEnteteDocument();
		infoEnteteDocument.setPrefixe(buildPrefixeEnteteDocument(getTypeDocumentEditique()));

		TypAdresse porteAdresse = editiqueHelper.remplitPorteAdresse(declaration.getTiers(), infoEnteteDocument);
		infoEnteteDocument.setPorteAdresse(porteAdresse);

		Expediteur expediteur = editiqueHelper.remplitExpediteurPourSommationLR(declaration, infoEnteteDocument, null);
		infoEnteteDocument.setExpediteur(expediteur);

		Destinataire destinataire = editiqueHelper.remplitDestinataire(declaration.getTiers(), infoEnteteDocument);
		infoEnteteDocument.setDestinataire(destinataire);

		return infoEnteteDocument;

	}

	private SLRLCBVR remplitSpecifiqueSommationLR(DeclarationImpotSource lr, RegDate dateTraitement) throws EditiqueException {

		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) lr.getTiers();

		final SLRLCBVR slrlcbvr = SLRLCBVRDocument.Factory.newInstance().addNewSLRLCBVR();

		final TypPeriode typPeriode = slrlcbvr.addNewPeriode();
		final TypeDocumentEditique typeDocumentEditique = getTypeDocumentEditique();
		final String prefixePerio = buildPrefixePeriode(typeDocumentEditique);

		typPeriode.setPrefixe(prefixePerio);
		typPeriode.setOrigDuplicat(ORIGINAL);
		typPeriode.setHorsSuisse("");
		typPeriode.setHorsCanton("");
		typPeriode.setAnneeFiscale(lr.getPeriode().getAnnee().toString());
		typPeriode.setDateDecompte(String.valueOf(lr.getDateExpedition().index()));
		Entete entete = typPeriode.addNewEntete();
		final Tit tit = entete.addNewTit();
		final String prefixeTitim = buildPrefixeTitreEntete(typeDocumentEditique);
		tit.setPrefixe(prefixeTitim);

		String libTit = IMPOT_A_LA_SOURCE_MAJ;
		if (dpi.getCategorieImpotSource() == CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS) {
			libTit = libTit + ' ' + CAS_MAJ ;
		}
		else if (dpi.getCategorieImpotSource() == CategorieImpotSource.ADMINISTRATEURS) {
			libTit = libTit + ' ' + ADM_MAJ ;
		}
		else if (dpi.getCategorieImpotSource() == CategorieImpotSource.PRESTATIONS_PREVOYANCE) {
			libTit = libTit + ' ' + PRE_MAJ ;
		}
		else if (dpi.getCategorieImpotSource() == CategorieImpotSource.CREANCIERS_HYPOTHECAIRES) {
			libTit = libTit + ' ' + HYP_MAJ ;
		}
		libTit = libTit + ' ' + lr.getPeriode().getAnnee().toString();
		tit.setLibTit(libTit);
		final Tit[] tits = new Tit[1];
		tits[0] = tit;
		entete.setTitArray(tits);

		final ImpCcn impCcn = entete.addNewImpCcn();
		final String prefixeImpCcn = buildPrefixeImpCcnEntete(typeDocumentEditique);
		impCcn.setPrefixe(prefixeImpCcn);
		impCcn.setLibImpCcn(DECOMPTE_LR_MIN);
		//
		// InfBVR
		//
		InfBVR infBVR = slrlcbvr.addNewInfBVR();
		infBVR.setPeriodeDu(String.valueOf(lr.getDateDebut().index()));
		infBVR.setPeriodeAu(String.valueOf(lr.getDateFin().index()));
		if (lr.getDelaiAccordeAu() != null) {
			infBVR.setDateDelai(String.valueOf(lr.getDelaiAccordeAu().index()));
		}
		//
		// LRLC
		//
		final SLRLC slrlc = slrlcbvr.addNewSLRLC();
		final TitreDoc titreDoc = slrlc.addNewTitreDoc();
		titreDoc.setLibelle1(LISTE_RECAPITULATIVE_MAJ);
		if (dpi.getCategorieImpotSource() == CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS) {
			titreDoc.setLibelle2(CAS_MIN);
		}
		else if (dpi.getCategorieImpotSource() == CategorieImpotSource.ADMINISTRATEURS) {
			titreDoc.setLibelle2(ADM_MIN);
		}
		else if (dpi.getCategorieImpotSource() == CategorieImpotSource.PRESTATIONS_PREVOYANCE) {
			titreDoc.setLibelle2(PRE_MIN);
		}
		else if (dpi.getCategorieImpotSource() == CategorieImpotSource.CREANCIERS_HYPOTHECAIRES) {
			titreDoc.setLibelle2(HYP_MIN);
		}

		slrlc.setNoRef(FormatNumeroHelper.numeroCTBToDisplay(dpi.getNumero()));
		slrlc.setPeriodeDu(String.valueOf(lr.getDateDebut().index()));
		slrlc.setPeriodeAu(String.valueOf(lr.getDateFin().index()));
		slrlc.setResponsable(dpi.getPersonneContact());
		slrlc.setTel(dpi.getNumeroTelephoneProfessionnel());
		slrlc.setPeriodeDu(String.valueOf(lr.getDateDebut().index()));
		slrlc.setPeriodeAu(String.valueOf(lr.getDateFin().index()));
		//TODO (FDE) Le renseigner dès que possible
		final String codeBar = calculCodeBarre(lr);
		slrlc.setCodeBar(codeBar);

		//
		// BVRSTD
		//
		final BvrDemande bvrDemande = new BvrDemande();
		bvrDemande.setNdc(dpi.getNumero().toString());
		final Integer anneeAsInteger = lr.getPeriode().getAnnee();
		final BigInteger annee = BigInteger.valueOf(anneeAsInteger.longValue());
		bvrDemande.setAnneeTaxation(annee);
		bvrDemande.setTypeDebiteurIS(dpi.getCategorieImpotSource().toString());

		final BvrReponse bvrReponse = bvrPlusClient.getBVRDemande(bvrDemande);
//		final BvrReponse bvrReponse = new BvrReponse();
//		bvrReponse.setLigneCodage("0100003949753>210000000003139471430009017+ 01010391391>");
//		bvrReponse.setNoAdherent("10-1245-8");

		if (bvrReponse.getLigneCodage() == null) {
			throw new EditiqueException("Le service 'SipfBVRPlus' ne renvoie pas d'information pour le débiteur " + dpi.getNumero().toString() + " de type " + dpi.getCategorieImpotSource()  + " et l'année " + annee );
		}
		final String noReference = FormatNumeroHelper.extractNoReference(bvrReponse.getLigneCodage());

		final BVRSTD bvrstd = slrlcbvr.addNewBVRSTD();
		final String prefixeBVRST = buildPrefixeBvrStandard(typeDocumentEditique);
		bvrstd.setPrefixe(prefixeBVRST);
		bvrstd.setLibImp(IMPOT_A_LA_SOURCE_MIN + ' ' + lr.getPeriode().getAnnee().toString());
		//TODO (FDE) Se renseigner sur le numéro de collectivité concerné
		bvrstd.setVersPourLigne1("Département des finances");
		bvrstd.setVersPourLigne2("Administration des impôts");
		bvrstd.setVersPourLigne3("");
		bvrstd.setVersPourNpaLoc("1014 Lausanne");
		bvrstd.setLigneReference(noReference);
		bvrstd.setNoCompte(bvrReponse.getNoAdherent());
		bvrstd.setIBAN(dpi.getNumeroCompteBancaire());
		try {
			AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(dpi, null, TypeAdresseFiscale.COURRIER, false);
			bvrstd.setVerseParLigne1(adresseEnvoi.getLigne1());
			bvrstd.setVerseParLigne2(adresseEnvoi.getLigne2());
			bvrstd.setVerseParLigne3(adresseEnvoi.getLigne3());
			bvrstd.setVerseParLigne4(adresseEnvoi.getLigne4());
			bvrstd.setVerseParLigne5(adresseEnvoi.getLigne5());
			bvrstd.setVerseParLigne6(adresseEnvoi.getLigne6());
		}
		catch (AdresseException e) {
			LOGGER.error("Exception lors de l'identification de la provenance de l'adresse du tiers " + dpi.getNumero(), e);
			throw new EditiqueException("Exception lors de l'identification de la provenance de l'adresse");
		}
		bvrstd.setLigneCodage(bvrReponse.getLigneCodage());

		// [SIFISC-753] Tout débiteur "papier" reçoit son enveloppe retour, pas les autres
		slrlcbvr.setEnveloppeRetour(dpi.getModeCommunication() == ModeCommunication.PAPIER ? "O" : "N");

		return slrlcbvr;
	}

	private String calculCodeBarre(DeclarationImpotSource lr) {
		Tiers tiers = lr.getTiers();
		String codbarr = 	StringUtils.leftPad(tiers.getNumero().toString(), 8, "0")
							+ String.valueOf(lr.getDateDebut().index())
							+ String.valueOf(lr.getDateFin().index())
							+ "01";
		return codbarr;
	}

	private InfoArchivage remplitInfoArchivage(DeclarationImpotSource lr, RegDate dateTraitement) {
		return editiqueHelper.buildInfoArchivage(getTypeDocumentEditique(), lr.getTiers().getNumero(), construitIdArchivageDocument(lr), dateTraitement);
	}
}
