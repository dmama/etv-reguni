package ch.vd.uniregctb.declaration.source;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;

import noNamespace.BVRSTDDocument.BVRSTD;
import noNamespace.CleRgpDocument.CleRgp;
import noNamespace.FichierImpressionDocument;
import noNamespace.InfoDocumentDocument1;
import noNamespace.InfoDocumentDocument1.InfoDocument;
import noNamespace.InfoEnteteDocumentDocument1;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.LRLCBVRDocument;
import noNamespace.LRLCBVRDocument.LRLCBVR;
import noNamespace.LRLCBVRDocument.LRLCBVR.InfBVR;
import noNamespace.LRLCBVRDocument.LRLCBVR.LRLC;
import noNamespace.LRLCBVRDocument.LRLCBVR.LRLC.TitreDoc;
import noNamespace.TypAdresse;
import noNamespace.TypFichierImpression;
import noNamespace.TypFichierImpression.Document;
import noNamespace.TypPeriode;
import noNamespace.TypPeriode.Entete;
import noNamespace.TypPeriode.Entete.ImpCcn;
import noNamespace.TypPeriode.Entete.Tit;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

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

public class ImpressionListeRecapHelperImpl extends EditiqueAbstractHelper implements ImpressionListeRecapHelper {

	public static final Logger LOGGER = Logger.getLogger(ImpressionListeRecapHelperImpl.class);

	private static final String TYPE_DOC_LR = "LR";

	private static final String LISTE_RECAPITULATIVE_MAJ = "LISTE RECAPITULATIVE";
	private static final String IMPOT_A_LA_SOURCE_MIN = "Impôt à la source";
	private static final String IMPOT_A_LA_SOURCE_MAJ = "IMPOT A LA SOURCE";

	private static final String CODE_DOC_LR_REG = "LR_SREG";
	private static final String CODE_DOC_LR_REG_SANS_PREIMP = "LR_SREG_NO_PRE";
	private static final String CODE_DOC_LR_CAS = "LR_SCAS";
	private static final String CODE_DOC_LR_ADM = "LR_SADM";
	private static final String CODE_DOC_LR_PRE = "LR_SPRE";
	private static final String CODE_DOC_LR_HYP = "LR_SHYP";
	private static final String CODE_DOC_LR_LTN = "LR_SLTN";

	private static final String HYP_MIN = "concernant l’impôt à la source perçu sur les intérêts hypothécaires";
	private static final String PRE_MIN = "pour les prestations de prévoyance";
	private static final String ADM_MIN = "pour les prestations versées aux administrateurs domiciliés à l’étranger";
	private static final String CAS_MIN = "pour les artistes, sportifs et conférenciers domiciliés à l’étranger";
	private static final String DECOMPTE_LR_MIN = "Décompte liste récapitulative";

	private static final String HYP_MAJ = "CREANCIERS HYPOTHECAIRES";
	private static final String PRE_MAJ = "PRESTATION PREVOYANCE";
	private static final String CAS_MAJ = "CONFERENCIERS ARTISTES SPORTIFS";
	private static final String ADM_MAJ = "ADMINISTRATEURS";

	private static final String VERSION = "1.0";

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

	/**
	 * Remplit un objet TypFichierImpression
	 *
	 * @param lr
	 * @param traitePar
	 * @return
	 * @throws Exception
	 */
	@Override
	public FichierImpressionDocument remplitListeRecap(DeclarationImpotSource lr, String traitePar) throws EditiqueException {
		final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
		TypFichierImpression impressionIS = mainDocument.addNewFichierImpression();
		Document[] documents = new Document[1];

		InfoDocument infoDocument = remplitInfoDocument(lr);
		InfoEnteteDocument infoEnteteDocument;
		try {
			infoEnteteDocument = remplitEnteteDocument(lr, traitePar);
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}

		LRLCBVR lrlcbvr = remplitSpecifiqueListeRecap(lr);
		Document document = impressionIS.addNewDocument();

		document.setInfoEnteteDocument(infoEnteteDocument);
		document.setInfoDocument(infoDocument);
		document.setLRLCBVR(lrlcbvr);
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
		infoDocument.setTypDoc(TYPE_DOC_LR);

		final String codeDoc;
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) lr.getTiers();
		switch (dpi.getCategorieImpotSource()) {
			case REGULIERS:
				// [UNIREG-2054] seuls les débiteurs "papiers" doivent recevoir le pré-imprimé
				if (dpi.getModeCommunication() == ModeCommunication.PAPIER) {
					codeDoc = CODE_DOC_LR_REG;
				}
				else {
					codeDoc = CODE_DOC_LR_REG_SANS_PREIMP;
				}
				break;

			case CONFERENCIERS_ARTISTES_SPORTIFS:
		        codeDoc = CODE_DOC_LR_CAS;
				break;

			case ADMINISTRATEURS:
				codeDoc = CODE_DOC_LR_ADM;
				break;

			case PRESTATIONS_PREVOYANCE:
				codeDoc = CODE_DOC_LR_PRE;
				break;

			case CREANCIERS_HYPOTHECAIRES:
				codeDoc = CODE_DOC_LR_HYP;
				break;

			case LOI_TRAVAIL_AU_NOIR:
				codeDoc = CODE_DOC_LR_LTN;
				break;

			default:
				codeDoc = null;
				break;
		}

		infoDocument.setCodDoc(codeDoc);

		final CleRgp cleRgp = infoDocument.addNewCleRgp();
		cleRgp.setAnneeFiscale(Integer.toString(lr.getPeriode().getAnnee()));

		infoDocument.setVersion(VERSION);
		infoDocument.setLogo(LOGO_CANTON);
		infoDocument.setPopulations(POPULATION_IS);
		editiqueHelper.remplitAffranchissement(infoDocument, dpi);
		return infoDocument;
	}

	/**
	 * Calcul le prefixe
	 * @return le prefixe
	 */

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.LR;
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
	protected InfoEnteteDocument remplitEnteteDocument(Declaration declaration, String traitePar) throws AdresseException, ServiceInfrastructureException {
		final InfoEnteteDocument infoEnteteDocument = InfoEnteteDocumentDocument1.Factory.newInstance().addNewInfoEnteteDocument();
		infoEnteteDocument.setPrefixe(buildPrefixeEnteteDocument(getTypeDocumentEditique()));

		final TypAdresse porteAdresse = editiqueHelper.remplitPorteAdresse(declaration.getTiers(), infoEnteteDocument);
		infoEnteteDocument.setPorteAdresse(porteAdresse);

		final Expediteur expediteur = editiqueHelper.remplitExpediteurPourEnvoiLR(declaration, infoEnteteDocument, traitePar);
		infoEnteteDocument.setExpediteur(expediteur);

		final Destinataire destinataire = editiqueHelper.remplitDestinataire(declaration.getTiers(), infoEnteteDocument);
		infoEnteteDocument.setDestinataire(destinataire);

		return infoEnteteDocument;

	}

	private LRLCBVR remplitSpecifiqueListeRecap(DeclarationImpotSource lr) throws EditiqueException {

		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) lr.getTiers();

		final LRLCBVR lrlcbvr = LRLCBVRDocument.Factory.newInstance().addNewLRLCBVR();
		//
		// TypPeriode
		//
		final TypPeriode typPeriode = lrlcbvr.addNewPeriode();
		final TypeDocumentEditique typeDocumentEditique = getTypeDocumentEditique();
		final String prefixePerio = buildPrefixePeriode(typeDocumentEditique);
		typPeriode.setPrefixe(prefixePerio);
		typPeriode.setOrigDuplicat(ORIGINAL);
		typPeriode.setHorsSuisse("");
		typPeriode.setHorsCanton("");
		typPeriode.setAnneeFiscale(lr.getPeriode().getAnnee().toString());
		typPeriode.setDateDecompte(String.valueOf(lr.getDateExpedition().index()));

		final Entete entete = typPeriode.addNewEntete();
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
		final InfBVR infBVR = lrlcbvr.addNewInfBVR();
		infBVR.setPeriodeDu(String.valueOf(lr.getDateDebut().index()));
		infBVR.setPeriodeAu(String.valueOf(lr.getDateFin().index()));
		if (lr.getDelaiAccordeAu() != null) {
			infBVR.setDateDelai(String.valueOf(lr.getDelaiAccordeAu().index()));
		}
		//
		// LRLC
		//
		final LRLC lrlc = lrlcbvr.addNewLRLC();
		final TitreDoc titreDoc = lrlc.addNewTitreDoc();
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
		lrlc.setNoRef(FormatNumeroHelper.numeroCTBToDisplay(dpi.getNumero()));
		lrlc.setPeriodeDu(String.valueOf(lr.getDateDebut().index()));
		lrlc.setPeriodeAu(String.valueOf(lr.getDateFin().index()));
		lrlc.setResponsable(dpi.getPersonneContact());
		lrlc.setTel(dpi.getNumeroTelephoneProfessionnel());
		lrlc.setPeriodeDu(String.valueOf(lr.getDateDebut().index()));
		lrlc.setPeriodeAu(String.valueOf(lr.getDateFin().index()));
		final String codeBar = calculCodeBarre(lr);
		lrlc.setCodeBar(codeBar);

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
		if (bvrReponse.getLigneCodage() == null) {
			throw new EditiqueException("Le service 'SipfBVRPlus' ne renvoie pas d'information pour le débiteur " + dpi.getNumero().toString() + " de type " + dpi.getCategorieImpotSource()  + " et l'année " + annee );
		}

		final String noReference = FormatNumeroHelper.extractNoReference(bvrReponse.getLigneCodage());
		final BVRSTD bvrstd = lrlcbvr.addNewBVRSTD();
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
			final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(dpi, null, TypeAdresseFiscale.COURRIER, false);
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
		lrlcbvr.setEnveloppeRetour(dpi.getModeCommunication() == ModeCommunication.PAPIER ? "O" : "N");

		return lrlcbvr;
	}

	private String calculCodeBarre(DeclarationImpotSource lr) {
		Tiers tiers = lr.getTiers();
		String codbarr = 	StringUtils.leftPad(tiers.getNumero().toString(), 8, "0")
							+ String.valueOf(lr.getDateDebut().index())
							+ String.valueOf(lr.getDateFin().index())
							+ "01";
		return codbarr;
	}

	/**
	 * Construit le champ idDocument
	 *
	 * @param lr
	 * @return
	 */
	@Override
	public String construitIdDocument(DeclarationImpotSource lr) {
		return String.format(
				"%s %s",
				StringUtils.leftPad(lr.getTiers().getNumero().toString(), 9, '0'),
				new SimpleDateFormat("yyyyMMddHHmmssSSS").format(
						lr.getLogCreationDate()
				)
		);
	}
}
