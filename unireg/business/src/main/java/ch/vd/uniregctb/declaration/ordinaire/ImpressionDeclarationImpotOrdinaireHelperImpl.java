package ch.vd.uniregctb.declaration.ordinaire;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import noNamespace.CleRgpDocument.CleRgp;
import noNamespace.DIBase;
import noNamespace.DIDPDocument;
import noNamespace.DIDPDocument.DIDP;
import noNamespace.DIDocument;
import noNamespace.DIDocument.DI;
import noNamespace.DIDocument.DI.AdresseSuite;
import noNamespace.DIHCDocument;
import noNamespace.DIHCDocument.DIHC;
import noNamespace.DIRetour;
import noNamespace.DIRetourCivil;
import noNamespace.DIVDTAXDocument;
import noNamespace.DIVDTAXDocument.DIVDTAX;
import noNamespace.InfoDocumentDocument1;
import noNamespace.InfoDocumentDocument1.InfoDocument;
import noNamespace.InfoEnteteDocumentDocument1;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.TypAdresse;
import noNamespace.TypFichierImpression;
import noNamespace.TypFichierImpression.Document;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.TypeDocument;

public class ImpressionDeclarationImpotOrdinaireHelperImpl implements ImpressionDeclarationImpotOrdinaireHelper {

	public static final Logger LOGGER = Logger.getLogger(ImpressionDeclarationImpotOrdinaireHelperImpl.class);

	private static final Integer NBRE_COPIE_ANNEXE_DEFAUT = 1;

	private static final String DI = "DI";
	private static final String VERSION = "1.0";
	private static final String POPULATIONS_PP = "PP";
	private static final String LOGO_CANT = "CANT";
	private static final String HAUT1 = "HAUT1";
	private static final String NO_DOCUMENT_CPLT = "0801";
	private static final String NO_DOCUMENT_VT = "0802";
	private static final String NO_DOCUMENT_HC = "0803";
	private static final String NO_DOCUMENT_DEP = "0804";
	private static final String CODE_DOCUMENT_CPLT = "DI_STD";
	private static final String CODE_DOCUMENT_VT = "DI_VAUDTAX";
	private static final String CODE_DOCUMENT_HC = "DI_HC";
	private static final String CODE_DOCUMENT_DEP = "DI_DP";
	private static final String NO_PROJET = "I";
	private static final String DOCUM = "DOCUM";
	private static final String FOLDE = "FOLDE";
	private static final String P = "P";
	private static final String RG = "RG";

	private ServiceInfrastructureService infraService;
	private AdresseService adresseService;
	private TiersService tiersService;
	private SituationFamilleService situationFamilleService;
	private EditiqueHelper editiqueHelper;

	public ImpressionDeclarationImpotOrdinaireHelperImpl() {
	}

	public ImpressionDeclarationImpotOrdinaireHelperImpl(ServiceInfrastructureService infraService, AdresseService adresseService, TiersService tiersService,
	                                                     SituationFamilleService situationFamilleService, EditiqueHelper editiqueHelper) {
		this.infraService = infraService;
		this.adresseService = adresseService;
		this.tiersService = tiersService;
		this.situationFamilleService = situationFamilleService;
		this.editiqueHelper = editiqueHelper;
	}


	/**
	 * Calcul le prefixe
	 */
	public String calculPrefixe(Declaration declaration) {
		String prefixe = RG + P + NO_PROJET;
		TypeDocument typeDoc = declaration.getModeleDocument().getTypeDocument();
		if (typeDoc == TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH || typeDoc == TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL) {
			prefixe += NO_DOCUMENT_CPLT;
		}
		else if (typeDoc == TypeDocument.DECLARATION_IMPOT_VAUDTAX) {
			prefixe += NO_DOCUMENT_VT;
		}
		else if (typeDoc == TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE) {
			prefixe += NO_DOCUMENT_HC;
		}
		else if (typeDoc == TypeDocument.DECLARATION_IMPOT_DEPENSE) {
			prefixe += NO_DOCUMENT_DEP;
		}

		return prefixe;
	}

	/**
	 * Alimente la partie infoDocument du Document
	 */
	protected InfoDocument remplitInfoDocument(Declaration declaration) throws EditiqueException {
		InfoDocument infoDocument = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
		String prefixe = calculPrefixe(declaration);
		prefixe += DOCUM;
		infoDocument.setPrefixe(prefixe);
		infoDocument.setTypDoc(DI);
		String codDoc = "";
		TypeDocument typeDoc = declaration.getModeleDocument().getTypeDocument();
		if (typeDoc == TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH || typeDoc == TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL) {
			codDoc += CODE_DOCUMENT_CPLT;
		}
		else if (typeDoc == TypeDocument.DECLARATION_IMPOT_VAUDTAX) {
			codDoc += CODE_DOCUMENT_VT;
		}
		else if (typeDoc == TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE) {
			codDoc += CODE_DOCUMENT_HC;
		}
		else if (typeDoc == TypeDocument.DECLARATION_IMPOT_DEPENSE) {
			codDoc += CODE_DOCUMENT_DEP;
		}
		infoDocument.setCodDoc(codDoc);

		final CleRgp cleRgp = infoDocument.addNewCleRgp();
		cleRgp.setAnneeFiscale(Integer.toString(declaration.getPeriode().getAnnee()));

		try {
			AdresseEnvoiDetaillee adresseEnvoiDetaillee = adresseService.getAdresseEnvoi(declaration.getTiers(), null, TypeAdresseFiscale.COURRIER, false);
			String idEnvoi = "";
			if (declaration instanceof DeclarationImpotOrdinaire) {
				if (adresseEnvoiDetaillee.isSuisse()) {
					idEnvoi = "";
				}
				else {
					// [UNIREG-1257] tenir compte de l'OID valide durant la période de validité de la déclaration
					final Integer officeImpotId = tiersService.getOfficeImpotIdAt(declaration.getTiers(), declaration.getDateFin());
					if (officeImpotId != null) {
						idEnvoi = officeImpotId.toString();
					}
				}
			}
			infoDocument.setIdEnvoi(idEnvoi);
		}
		catch (Exception e) {
			String message = "Exception lors de l'identification de la provenance de l'adresse";
			LOGGER.error("Exception lors de l'identification de la provenance de l'adresse du tiers " + declaration.getTiers().getNumero(), e);
			throw new EditiqueException(message);
		}
		infoDocument.setVersion(VERSION);
		infoDocument.setLogo(LOGO_CANT);
		infoDocument.setPopulations(POPULATIONS_PP);
		return infoDocument;
	}

	/**
	 * Alimente l'entête du document
	 */
	protected InfoEnteteDocument remplitEnteteDocument(Declaration declaration) throws AdresseException, ServiceInfrastructureException {
		InfoEnteteDocument infoEnteteDocument = InfoEnteteDocumentDocument1.Factory.newInstance().addNewInfoEnteteDocument();

		String prefixe = calculPrefixe(declaration);
		prefixe += HAUT1;
		infoEnteteDocument.setPrefixe(prefixe);

		TypAdresse porteAdresse = editiqueHelper.remplitPorteAdresse(declaration.getTiers(), infoEnteteDocument);
		infoEnteteDocument.setPorteAdresse(porteAdresse);

		Expediteur expediteur = remplitExpediteur(declaration, infoEnteteDocument);
		infoEnteteDocument.setExpediteur(expediteur);

		Destinataire destinataire = editiqueHelper.remplitDestinataire(declaration.getTiers(), infoEnteteDocument);
		infoEnteteDocument.setDestinataire(destinataire);

		return infoEnteteDocument;

	}

	/**
	 * Alimente la partie expéditeur du document
	 *
	 * @param declaration        la déclaration
	 * @param infoEnteteDocument l'entête du document XML dont il faut compléter les informations
	 * @return les informations de l'expéditeur
	 * @throws ServiceInfrastructureException en cas de problème avec le service infrastructure
	 * @throws AdresseException        en cas de problème avec les adresses
	 */
	private Expediteur remplitExpediteur(Declaration declaration, InfoEnteteDocument infoEnteteDocument) throws AdresseException, ServiceInfrastructureException {
		//
		// Expediteur
		//
		// [UNIREG-1257] tenir compte de l'OID valide durant la période de validité de la déclaration
		// [UNIREG-1857] passer par le service des adresses pour bâtir l'adresse de la collectivité administrative (manquait la case postale)
		final CollectiviteAdministrative oid = tiersService.getOfficeImpotAt(declaration.getTiers(), declaration.getDateFin());
		Assert.notNull(oid);

		final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(oid, null, TypeAdresseFiscale.COURRIER, false);
		final Expediteur expediteur = infoEnteteDocument.addNewExpediteur();
		final TypAdresse.Adresse adresseExpediteur = expediteur.addNewAdresse();
		remplitAdresse(adresseExpediteur, adresse);
		expediteur.setAdresse(adresseExpediteur);

		final ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative collectivite = infraService.getOfficeImpot(oid.getNumeroCollectiviteAdministrative());

		expediteur.setAdrMes(collectivite.getAdresseEmail());
		expediteur.setNumTelephone(collectivite.getNoTelephone());
		expediteur.setNumFax(collectivite.getNoFax());
		expediteur.setNumCCP(collectivite.getNoCCP());

		final String ideUti = declaration.getLogModifUser();
		/*
		 * List<ch.vd.infrastructure.model.CollectiviteAdministrative> collectivitesUtilisateur =
		 * serviceSecuriteService.getCollectivitesUtilisateur(ideUti); ch.vd.infrastructure.model.CollectiviteAdministrative colUtil =
		 * collectivitesUtilisateur.get(0); if (colUtil != null) { String traitePar = colUtil.getNomComplet1();
		 * expediteur.setTraitePar(traitePar); }
		 */
		expediteur.setIdeUti(ideUti);

		final String dateExpedition = RegDateHelper.toIndexString(RegDate.get());
		expediteur.setDateExpedition(dateExpedition);

		return expediteur;
	}

	private static void remplitAdresse(TypAdresse.Adresse adresseExpediteur, AdresseEnvoiDetaillee adresse) {
		adresseExpediteur.setAdresseCourrierLigne1(adresse.getLigne1());
		adresseExpediteur.setAdresseCourrierLigne2(adresse.getLigne2());
		adresseExpediteur.setAdresseCourrierLigne3(adresse.getLigne3());
		adresseExpediteur.setAdresseCourrierLigne4(adresse.getLigne4());
		adresseExpediteur.setAdresseCourrierLigne5(adresse.getLigne5());
		adresseExpediteur.setAdresseCourrierLigne6(adresse.getLigne6());
	}

	/**
	 * Alimente un objet Document pour l'impression des DI
	 */
	public Document remplitEditiqueSpecifiqueDI(DeclarationImpotOrdinaire declaration, TypFichierImpression typeFichierImpression,
	                                            TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException {
		InfoDocument infoDocument = remplitInfoDocument(declaration);
		InfoEnteteDocument infoEnteteDocument;
		try {
			infoEnteteDocument = remplitEnteteDocument(declaration);
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
		Document document = typeFichierImpression.addNewDocument();
		if (typeDocument == null) {
			typeDocument = declaration.getModeleDocument().getTypeDocument();
		}
		if (typeDocument == TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH || typeDocument == TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL) {
			DI di = remplitSpecifiqueDI(declaration, annexes);
			document.setDI(di);
		}
		else if (typeDocument == TypeDocument.DECLARATION_IMPOT_DEPENSE) {
			DIDP didp = remplitSpecifiqueDIDP(declaration, annexes);
			document.setDIDP(didp);
		}
		else if (typeDocument == TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE) {
			DIHC dihc = remplitSpecifiqueDIHC(declaration, annexes);
			document.setDIHC(dihc);
		}
		else if (typeDocument == TypeDocument.DECLARATION_IMPOT_VAUDTAX) {
			DIVDTAX divdtax = remplitSpecifiqueDIVDTAX(declaration, annexes);
			document.setDIVDTAX(divdtax);
		}

		document.setInfoEnteteDocument(infoEnteteDocument);
		document.setInfoDocument(infoDocument);
		return document;
	}

	/**
	 * Alimente un objet DI
	 */
	protected DI remplitSpecifiqueDI(DeclarationImpotOrdinaire declaration, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException {

		final DI di = DIDocument.Factory.newInstance().addNewDI();
		remplitDIRetour(declaration, di);
		remplitAdresseSuite(declaration, di);

		final int nbAnnexes210 = getNbOfAnnexes(annexes, "210", 0, 0);
		final int nbAnnexes220 = getNbOfAnnexes(annexes, "220", 0, 0);
		final int nbAnnexes230 = getNbOfAnnexes(annexes, "230", 0, 0);
		final int nbAnnexes240 = getNbOfAnnexes(annexes, "240", 0, 0);
		final int nbAnnexes310 = getNbOfAnnexes(annexes, "310", 0, 0);

		// pour être certain d'imprimer toujours quelque chose!
		int correctionAnnexes210 = 0;
		if (nbAnnexes210 == 0 && nbAnnexes220 == 0 && nbAnnexes230 == 0 && nbAnnexes240 == 0 && nbAnnexes310 == 0) {
			correctionAnnexes210 = NBRE_COPIE_ANNEXE_DEFAUT;
		}

		final DI.Annexes a = di.addNewAnnexes();
		if (nbAnnexes210 + correctionAnnexes210 > 0) {
			a.setAnnexe210(nbAnnexes210 + correctionAnnexes210);
		}
		if (nbAnnexes220 > 0) {
			a.setAnnexe220(nbAnnexes220);
		}
		if (nbAnnexes230 > 0) {
			a.setAnnexe230(nbAnnexes230);
		}
		if (nbAnnexes240 > 0) {
			a.setAnnexe240(nbAnnexes240);
		}
		if (nbAnnexes310 > 0) {
			a.setAnnexe310(nbAnnexes310);
		}
		return di;
	}

	private void remplitDIBase(DeclarationImpotOrdinaire declaration, DIBase di) throws EditiqueException {
		remplitInfoDI(declaration, di);
	}

	private void remplitDIRetour(DeclarationImpotOrdinaire declaration, DIRetour di) throws EditiqueException {

		remplitDIBase(declaration, di);
		remplitAdresseRetour(declaration, di);
		if (di instanceof DIRetourCivil) {
			remplitContribuables(declaration, (DIRetourCivil) di);
		}

	}

	private void remplitAdresseRetour(DeclarationImpotOrdinaire declaration, DIRetour di) throws EditiqueException {

		final noNamespace.DIRetour.AdresseRetour adresseRetour = di.addNewAdresseRetour();

		final CollectiviteAdministrative col = getRetourCollectiviteAdministrative(declaration);
		if (col == null) {
			throw new EditiqueException("Impossible de déterminer la collectivité administrative de retour sur la DI numéro=[" + declaration.getId() + "]");
		}

		if (col.getNumeroCollectiviteAdministrative() == ServiceInfrastructureService.noCEDI) { // Cas spécial pour le CEDI
			final Integer officeImpot = getNumeroOfficeImpotRetour(declaration);


			Assert.notNull(officeImpot);
			remplitAdresseRetourCEDI(adresseRetour, officeImpot);
		}
		else if (col.getNumeroCollectiviteAdministrative() == ServiceInfrastructureService.noACI) { // Cas spécial pour l'ACI

			// [UNIREG-1741] Cas des décédés, les déclarations sont adressées au CEDI pour scannage avec le numéro de l'ACI
			remplitAdresseRetourCEDI(adresseRetour, ServiceInfrastructureService.noACI);
		}
		else { // adresse retour OID standard

			final AdresseEnvoiDetaillee adresse;
			try {
				adresse = adresseService.getAdresseEnvoi(col, null, TypeAdresseFiscale.COURRIER, false);
			}
			catch (AdresseException e) {
				throw new EditiqueException(e);
			}
			remplitAdresse(adresseRetour, adresse);
		}
	}



	/**
	 * UNIREG-3059  Pour ce qui concerne le gros numéro en gras, l'adresse CEDI XX, et le code à barre :
	*l'OID doit être l'OID de gestion valable au 31.12 de l'année N-1 (N étant la période lors de laquel l'édition du document a lieu)
	 *-> SAUF une exception : si la DI concerne la période fiscale courante (il s'agit d'une DI libre), alors l'OID doit être l'OID de gestion courant du moment de l'édition du docuement.
	 * @param declaration
	 * @return
	 */
	private Integer getNumeroOfficeImpotRetour(DeclarationImpotOrdinaire declaration) {


		final int anneeCourante = RegDate.get().year();

		final RegDate finPeriodeCourante = RegDate.get(anneeCourante, 12, 31);
		final RegDate finPeriodePrecedente = RegDate.get(anneeCourante - 1, 12, 31);
		RegDate dateRecherche = null;
		if (declaration.getPeriode().getAnnee() == anneeCourante) {
			dateRecherche = finPeriodeCourante;
		}
		else {
			dateRecherche = finPeriodePrecedente;
		}
		return tiersService.getOfficeImpotIdAt(declaration.getTiers(), dateRecherche);
	}

	private void remplitAdresseRetourCEDI(DIRetour.AdresseRetour adresseRetour, Integer officeImpotId) throws EditiqueException {

		final ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative cedi;
		try {
			cedi = infraService.getCEDI();
		}
		catch (ServiceInfrastructureException e) {
			throw new EditiqueException(e);
		}

		final Adresse adrCedi = cedi.getAdresse();
		adresseRetour.setADRES1RETOUR(cedi.getNomComplet1());
		adresseRetour.setADRES2RETOUR(cedi.getNomComplet2());

		adresseRetour.setADRES3RETOUR(cedi.getNomCourt() + " " + officeImpotId);
		adresseRetour.setADRES4RETOUR(adrCedi.getNumeroPostal() + " " + adrCedi.getLocalite());
		adresseRetour.setADRES5RETOUR(null);
		adresseRetour.setADRES6RETOUR(null);
	}

	private void remplitAdresse(DIRetour.AdresseRetour adresseRetour, AdresseEnvoiDetaillee adresse) {
		adresseRetour.setADRES1RETOUR(adresse.getLigne1());
		adresseRetour.setADRES2RETOUR(adresse.getLigne2());
		adresseRetour.setADRES3RETOUR(adresse.getLigne3());
		adresseRetour.setADRES4RETOUR(adresse.getLigne4());
		adresseRetour.setADRES5RETOUR(adresse.getLigne5());
		adresseRetour.setADRES6RETOUR(adresse.getLigne6());
	}

	private CollectiviteAdministrative getRetourCollectiviteAdministrative(DeclarationImpotOrdinaire declaration) throws EditiqueException {

		final Long collId = declaration.getRetourCollectiviteAdministrativeId();
		CollectiviteAdministrative collAdm = (collId == null ? null : (CollectiviteAdministrative) tiersService.getTiers(collId));
		if (collAdm == null) {
			// valeur par défaut
			collAdm = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
			Assert.notNull(collAdm);
		}

		// [UNIREG-1741] les DIs dépenses ne peuvent pas être scannées au CEDI, elles doivent retourner directement aux OIDs (ou éventuellement à l'ACI en cas de décès)
		if (collAdm.getNumeroCollectiviteAdministrative() == ServiceInfrastructureService.noCEDI && declaration.getTypeDeclaration() == TypeDocument.DECLARATION_IMPOT_DEPENSE) {
			collAdm = tiersService.getOfficeImpotAt(declaration.getTiers(), declaration.getDateFin());
			Assert.notNull(collAdm);
		}

		return collAdm;
	}

	private static int getNbOfAnnexes(List<ModeleFeuilleDocumentEditique> annexes, String codeFormulaire, int valeurSiAbsent, int valeurSiZero) {
		final int nbAnnexes;
		if (annexes == null) {
			nbAnnexes = valeurSiAbsent;
		}
		else {
			ModeleFeuilleDocumentEditique found = null;
			for (ModeleFeuilleDocumentEditique candidate : annexes) {
				if (codeFormulaire.equals(candidate.getNumeroFormulaire())) {
					found = candidate;
					break;
				}
			}

			if (found != null && found.getNbreIntituleFeuille() != null && found.getNbreIntituleFeuille() > 0) {
				nbAnnexes = found.getNbreIntituleFeuille();
			}
			else if (found == null) {
				nbAnnexes = valeurSiAbsent;
			}
			else {
				nbAnnexes = valeurSiZero;
			}
		}
		return nbAnnexes;
	}

	/**
	 * Alimente un objet DIVDTAX
	 */
	private DIVDTAX remplitSpecifiqueDIVDTAX(DeclarationImpotOrdinaire declaration, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException {

		final DIVDTAX divdtax = DIVDTAXDocument.Factory.newInstance().addNewDIVDTAX();
		remplitDIRetour(declaration, divdtax);

		// retrouve le nombre d'annexes 250 (seules autorisées par la DI VDTAX), au minimum une
		final int nbAnnexes = getNbOfAnnexes(annexes, "250", NBRE_COPIE_ANNEXE_DEFAUT, NBRE_COPIE_ANNEXE_DEFAUT);
		final DIVDTAX.Annexes a = divdtax.addNewAnnexes();
		a.setAnnexe250(nbAnnexes);

		return divdtax;
	}

	/**
	 * Alimente un objet DIDP
	 */
	private DIDP remplitSpecifiqueDIDP(DeclarationImpotOrdinaire declaration, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException {

		final DIDP didp = DIDPDocument.Factory.newInstance().addNewDIDP();
		remplitDIRetour(declaration, didp);

		// retrouve le nombre d'annexes 270 (seules autorisées par la DI ICCD), au minimum une
		final int nbAnnexes = getNbOfAnnexes(annexes, "270", NBRE_COPIE_ANNEXE_DEFAUT, NBRE_COPIE_ANNEXE_DEFAUT);
		final DIDP.Annexes a = didp.addNewAnnexes();
		a.setAnnexe270(nbAnnexes);

		return didp;
	}

	/**
	 * Alimente un objet DIVDTAX
	 */
	protected DIHC remplitSpecifiqueDIHC(DeclarationImpotOrdinaire declaration, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException {

		final DIHC dihc = DIHCDocument.Factory.newInstance().addNewDIHC();
		remplitDIRetour(declaration, dihc);

		// retrouve le nombre d'annexes 200 (seules autorisées par la DI HC (immeuble)), au minimum une
		final int nbAnnexes = getNbOfAnnexes(annexes, "200", NBRE_COPIE_ANNEXE_DEFAUT, NBRE_COPIE_ANNEXE_DEFAUT);
		final DIHC.Annexes a = dihc.addNewAnnexes();
		a.setAnnexe200(nbAnnexes);

		return dihc;
	}


	private void remplitAdresseSuite(DeclarationImpotOrdinaire declaration, DI di) throws EditiqueException {

		final Tiers tiers = declaration.getTiers();
		AdresseEnvoiDetaillee adresseEnvoi;
		try {
			adresseEnvoi = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
		}
		catch (AdresseException e) {
			throw new EditiqueException(e);
		}

		AdresseSuite adresseSuite = di.addNewAdresseSuite();
		final String npaLocalite = adresseEnvoi.getNpaEtLocalite();
		final List<String> nomPrenom = adresseEnvoi.getNomPrenom();
		switch (nomPrenom.size()) {
		case 0:
			adresseSuite.setAdresseCourrierLigne1(null);
			adresseSuite.setAdresseCourrierLigne2(npaLocalite);
			adresseSuite.setAdresseCourrierLigne3(null);
			break;
		case 1:
			adresseSuite.setAdresseCourrierLigne1(nomPrenom.get(0));
			adresseSuite.setAdresseCourrierLigne2(npaLocalite);
			adresseSuite.setAdresseCourrierLigne3(null);
			break;
		case 2:
			adresseSuite.setAdresseCourrierLigne1(nomPrenom.get(0));
			adresseSuite.setAdresseCourrierLigne2(nomPrenom.get(1));
			adresseSuite.setAdresseCourrierLigne3(npaLocalite);
			break;
		}
	}

	private void remplitInfoDI(DeclarationImpotOrdinaire declaration, DIBase di) throws EditiqueException {

		final Tiers tiers = declaration.getTiers();

		final noNamespace.DIBase.InfoDI infoDI = di.addNewInfoDI();
		final String codbarr = calculCodeBarre(declaration);
		final String delaiRetour = determineDelaiRetourImprime(declaration);
		final Long collId = declaration.getRetourCollectiviteAdministrativeId();
		final CollectiviteAdministrative collectiviteAdministrative = (collId == null ? null : (CollectiviteAdministrative) tiersService.getTiers(collId));

		// [UNIREG-1655] Il faut recalculer la commune du for de gestion
		final String nomCommuneGestion;
		try {
			final ForGestion forGestion = tiersService.getForGestionActif(declaration.getTiers(), declaration.getDateFin());
			final int noOfsCommune;
			if (forGestion != null) {
				noOfsCommune = forGestion.getNoOfsCommune();
			}
			else {
				// au cas où on voudrait imprimer, je ne sais pas, moi, une DI annulée pour quelqu'un qui n'a
				// plus de for de gestion, on prend le défaut sur la déclaration quand-même (c'est mieux qu'un crash, non ?)
				noOfsCommune = declaration.getNumeroOfsForGestion();
			}
			final Commune commune = infraService.getCommuneByNumeroOfsEtendu(noOfsCommune, declaration.getDateFin());
			nomCommuneGestion = commune.getNomMinuscule();
		}
		catch (ServiceInfrastructureException e) {
			throw new EditiqueException(e);
		}

		infoDI.setANNEEFISCALE(Integer.toString(declaration.getPeriode().getAnnee()));
		infoDI.setDESCOM(nomCommuneGestion);
		infoDI.setDELAIRETOUR(delaiRetour);
		infoDI.setNOCANT(FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()));

		final Integer numeroCA = (collectiviteAdministrative == null ? null : collectiviteAdministrative.getNumeroCollectiviteAdministrative());
		// [UNIREG-1741] le numéro d'OID doit être renseignée en cas de retour au CEDI *ou* au CEDI-22
		if (numeroCA != null) {
			if (numeroCA == ServiceInfrastructureService.noCEDI) {
				final String nooid = calculNooid(declaration, tiers);
				infoDI.setNOOID(nooid);
			}
			else if (numeroCA == ServiceInfrastructureService.noACI) {
				// [UNIREG-1741] Cas des décédés, les déclarations sont adressées au CEDI pour scannage avec le numéro de l'ACI
				infoDI.setNOOID(String.valueOf(ServiceInfrastructureService.noACI));
			}
		}

		infoDI.setCODBARR(codbarr);
	}

	/**
	 * [UNIREG-1740] Détermine le délai de retour de la déclaration. Pour les déclarations créées en masse après UNIREG-1740, ce délai est stocké dans la déclaration elle-même; pour les autres, on
	 * utilise le délai accordé.
	 *
	 * @param declaration une déclaration
	 * @return une chaîne de caractères représentant la date du délai de retour.
	 */
	private static String determineDelaiRetourImprime(DeclarationImpotOrdinaire declaration) {
		RegDate dateRetour = declaration.getDelaiRetourImprime();
		if (dateRetour == null) {
			dateRetour = declaration.getDelaiAccordeAu();
		}
		return RegDateHelper.dateToDisplayString(dateRetour);
	}

	private String calculNooid(DeclarationImpotOrdinaire declaration, Tiers tiers) {

		// [UNIREG-1257] tenir compte de l'OID valide durant la période de validité de la déclaration
		final Integer officeImpotId = getNumeroOfficeImpotRetour(declaration);
		Assert.notNull(officeImpotId);

		// [UNIREG-2965] nouveau mapping A, SA et SM (c'est lui le nouveau) -> XX-0, les autres : XX-1)
		final int suffixe;
		if (declaration.getQualification() != null) {
			switch (declaration.getQualification()) {
			case AUTOMATIQUE:
			case SEMI_AUTOMATIQUE:
			case SEMI_MANUEL:
				suffixe = 0;
				break;
			default:
				suffixe = 1;
				break;
			}
		}
		else {
			suffixe = 1;
		}
		return String.format("%02d-%d", officeImpotId, suffixe);
	}

	private void remplitContribuables(DeclarationImpotOrdinaire declaration, DIRetourCivil didp) throws EditiqueException {

		final PersonnePhysique principal;
		final PersonnePhysique conjoint;

		final Tiers tiers = declaration.getTiers();
		if (tiers instanceof PersonnePhysique) {
			principal = (PersonnePhysique) tiers;
			conjoint = null;
		}
		else {
			Assert.isTrue(tiers instanceof MenageCommun);
			final MenageCommun menage = (MenageCommun) tiers;
			final EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(menage, declaration.getDateFin());
			principal = ensembleTiersCouple.getPrincipal();
			conjoint = ensembleTiersCouple.getConjoint();
		}

		if (principal != null) {
			final String displayDateNaissance = calculDateNaissance(principal);
			final EtatCivil etatCivil = calculEtatCivil(principal, declaration.getDateFin());
			final String indnomprenom = calculIndividuNomPrenom(declaration, principal);
			final String noavs = calculAVS(declaration, principal);

			final noNamespace.DIRetourCivil.Contrib1 contrib1 = didp.addNewContrib1();
			contrib1.setINDETATCIVIL1(etatCivil != null ? etatCivil.format() : null);
			contrib1.setINDNOMPRENOM1(indnomprenom);
			contrib1.setINDDATENAISS1(displayDateNaissance);
			contrib1.setNAVS13(noavs);
		}

		if (conjoint != null) {
			final String displayDateNaissance = calculDateNaissance(conjoint);
			final EtatCivil etatCivil = calculEtatCivil(conjoint, declaration.getDateFin());
			final String indnomprenom = calculIndividuNomPrenom(declaration, conjoint);
			final String noavs = calculAVS(declaration, conjoint);

			final noNamespace.DIRetourCivil.Contrib2 contrib2 = didp.addNewContrib2();
			contrib2.setINDETATCIVIL2(etatCivil != null ? etatCivil.format() : null);
			contrib2.setINDNOMPRENOM2(indnomprenom);
			contrib2.setINDDATENAISS2(displayDateNaissance);
			contrib2.setNAVS13(noavs);
		}
	}

	private String calculCodeBarre(DeclarationImpotOrdinaire declaration) {
		Tiers tiers = declaration.getTiers();
		// [UNIREG-1257] tenir compte de l'OID valide durant la période de validité de la déclaration
		final Integer officeImpotId = getNumeroOfficeImpotRetour(declaration);
		Assert.notNull(officeImpotId);
		String codbarr = StringUtils.leftPad(tiers.getNumero().toString(), 9, "0") + declaration.getPeriode().getAnnee().toString()
				+ StringUtils.leftPad(declaration.getNumero().toString(), 2, "0")
				+ StringUtils.leftPad(officeImpotId.toString(), 2, "0");
		return codbarr;
	}

	private EtatCivil calculEtatCivil(PersonnePhysique pp, RegDate date) {
		EtatCivil etatCivil = situationFamilleService.getEtatCivil(pp, date, true);
		return etatCivil;
	}

	private String calculIndividuNomPrenom(DeclarationImpotOrdinaire declaration, PersonnePhysique pp) throws EditiqueException {
		try {
			List<String> noms = adresseService.getNomCourrier(pp, null, false);
			Assert.isTrue(noms.size() == 1);
			return adresseService.getFormulePolitesse(pp).salutations() + " " + noms.get(0);
		}
		catch (AdresseException e) {
			throw new EditiqueException(e);
		}
	}

	private String calculDateNaissance(PersonnePhysique pp) {
		final RegDate dateNaissance = tiersService.getDateNaissance(pp);
		return dateNaissance != null ? RegDateHelper.dateToDisplayString(dateNaissance) : null;
	}

	private String calculAVS(DeclarationImpotOrdinaire declaration, PersonnePhysique pp) {
		String noavs = null;
		/*
		 * if (declaration.getPeriode().getAnnee().intValue() < 2009) { noavs = tiersService.getAncienNumeroAssureSocial(pp); if (noavs !=
		 * null) { noavs = FormatNumeroHelper.formatAncienNumAVS(noavs); } } else {
		 */
		if (declaration.getPeriode().getAnnee() >= 2009) {
			noavs = tiersService.getNumeroAssureSocial(pp);
			if (noavs != null) {
				noavs = FormatNumeroHelper.formatNumAVS(noavs);
			}
		}
		return noavs;
	}

	/**
	 * Construit le champ idDocument
	 */
	public String construitIdDocument(DeclarationImpotOrdinaire declaration) {
		return String.format(
				"%s %s %s %s",
				declaration.getPeriode().getAnnee().toString(),
				StringUtils.leftPad(declaration.getNumero().toString(), 2, '0'),
				StringUtils.leftPad(declaration.getTiers().getNumero().toString(), 9, '0'),
				new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate())
		);

	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setSituationFamilleService(SituationFamilleService situationFamilleService) {
		this.situationFamilleService = situationFamilleService;
	}

	public void setEditiqueHelper(EditiqueHelper editiqueHelper) {
		this.editiqueHelper = editiqueHelper;
	}
}
