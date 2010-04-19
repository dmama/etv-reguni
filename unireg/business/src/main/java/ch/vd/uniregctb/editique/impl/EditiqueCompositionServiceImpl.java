package ch.vd.uniregctb.editique.impl;

import javax.jms.JMSException;
import java.util.List;

import noNamespace.FichierImpressionDocument;
import noNamespace.TypFichierImpression;
import noNamespace.TypFichierImpressionIS;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.registre.base.date.RegDate;
import ch.vd.securite.model.Operateur;
import ch.vd.securite.model.ProfilOperateur;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionConfirmationDelaiHelper;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionConfirmationDelaiHelperParams;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionDeclarationImpotOrdinaireHelper;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionSommationDIHelper;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionSommationDIHelperParams;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionTaxationOfficeHelper;
import ch.vd.uniregctb.declaration.ordinaire.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.declaration.source.ImpressionListeRecapHelper;
import ch.vd.uniregctb.declaration.source.ImpressionSommationLRHelper;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.mouvement.BordereauMouvementDossier;
import ch.vd.uniregctb.mouvement.ImpressionBordereauMouvementDossierHelper;
import ch.vd.uniregctb.mouvement.ImpressionBordereauMouvementDossierHelperParams;
import ch.vd.uniregctb.tache.ImpressionNouveauxDossiersHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeDocument;

public class EditiqueCompositionServiceImpl implements EditiqueCompositionService {

	public static final Logger LOGGER = Logger.getLogger(EditiqueCompositionServiceImpl.class);

	private EditiqueService editiqueService;

	private ImpressionDeclarationImpotOrdinaireHelper impressionDIHelper;
	private ImpressionListeRecapHelper impressionLRHelper;
	private ImpressionSommationDIHelper impressionSommationDIHelper;
	private ImpressionSommationLRHelper impressionSommationLRHelper;
	private ImpressionNouveauxDossiersHelper impressionNouveauxDossiersHelper;
	private ImpressionConfirmationDelaiHelper impressionConfirmationDelaiHelper;
	private ServiceSecuriteService serviceSecurite;
	private ImpressionTaxationOfficeHelper impressionTaxationOfficeHelper;
	private ImpressionBordereauMouvementDossierHelper impressionBordereauMouvementDossierHelper;

	public void setEditiqueService(EditiqueService editiqueService) {
		this.editiqueService = editiqueService;
	}

	public void setImpressionDIHelper(ImpressionDeclarationImpotOrdinaireHelper impressionDIHelper) {
		this.impressionDIHelper = impressionDIHelper;
	}

	public void setImpressionLRHelper(ImpressionListeRecapHelper impressionLRHelper) {
		this.impressionLRHelper = impressionLRHelper;
	}

	public void setImpressionSommationDIHelper(ImpressionSommationDIHelper impressionSommationDIHelper) {
		this.impressionSommationDIHelper = impressionSommationDIHelper;
	}

	public void setImpressionSommationLRHelper(ImpressionSommationLRHelper impressionSommationLRHelper) {
		this.impressionSommationLRHelper = impressionSommationLRHelper;
	}

	public void setImpressionNouveauxDossiersHelper(ImpressionNouveauxDossiersHelper impressionNouveauxDossiersHelper) {
		this.impressionNouveauxDossiersHelper = impressionNouveauxDossiersHelper;
	}

	public void setImpressionConfirmationDelaiHelper(ImpressionConfirmationDelaiHelper impressionConfirmationDelaiHelper) {
		this.impressionConfirmationDelaiHelper = impressionConfirmationDelaiHelper;
	}

	public void setServiceSecurite(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}

	public void setImpressionTaxationOfficeHelper(ImpressionTaxationOfficeHelper impressionTaxationOfficeHelper) {
		this.impressionTaxationOfficeHelper = impressionTaxationOfficeHelper;
	}

	public void setImpressionBordereauMouvementDossierHelper(ImpressionBordereauMouvementDossierHelper impressionBordereauMouvementDossierHelper) {
		this.impressionBordereauMouvementDossierHelper = impressionBordereauMouvementDossierHelper;
	}

	public EditiqueResultat imprimeDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException, JMSException {
		return imprimeDIOnline(declaration, dateEvenement, null, null, false);
	}

	public EditiqueResultat imprimeDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes, boolean isDuplicata) throws EditiqueException, JMSException {
		final TypFichierImpression editiqueDI = FichierImpressionDocument.Factory.newInstance().addNewFichierImpression();
		final TypFichierImpression.Document document = impressionDIHelper.remplitEditiqueSpecifiqueDI(declaration, editiqueDI, typeDocument, annexes);
		final TypFichierImpression.Document[] documents;
		if (isDuplicata || declaration.getTypeDeclaration().equals(TypeDocument.DECLARATION_IMPOT_VAUDTAX)) {
			documents = new TypFichierImpression.Document[1];
			documents[0] = document;
		}
		else {
			documents = new TypFichierImpression.Document[2];
			documents[0] = document;
			documents[1] = document;
		}
		editiqueDI.setDocumentArray(documents);
		final String typeDocumentMessage = impressionDIHelper.calculPrefixe(declaration);
		final String nomDocument = impressionDIHelper.construitIdDocument(declaration);
		return editiqueService.creerDocumentImmediatement(nomDocument, typeDocumentMessage, TypeFormat.PCL, editiqueDI, false);
	}

	public void imprimeDIForBatch(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException {
		final TypFichierImpression editiqueDI = FichierImpressionDocument.Factory.newInstance().addNewFichierImpression();
		final String typeDocument = impressionDIHelper.calculPrefixe(declaration);
		final TypFichierImpression.Document document = impressionDIHelper.remplitEditiqueSpecifiqueDI(declaration, editiqueDI, null, null);
		final TypFichierImpression.Document[] documents;
		Assert.notNull(document);
		if (declaration.getTypeDeclaration().equals(TypeDocument.DECLARATION_IMPOT_VAUDTAX)) {
			documents = new TypFichierImpression.Document[1];
			documents[0] = document;
		}
		else {
			documents = new TypFichierImpression.Document[2];
			documents[0] = document;
			documents[1] = document;
		}
		editiqueDI.setDocumentArray(documents);
		editiqueService.creerDocumentParBatch(editiqueDI, typeDocument, false);
	}

	public void imprimeLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException {
		final TypFichierImpressionIS editiqueLR = impressionLRHelper.remplitListeRecap(lr);
		String typeDocument = impressionLRHelper.calculPrefixe();
		editiqueService.creerDocumentParBatch(editiqueLR, typeDocument, false);
	}

	public EditiqueResultat imprimeNouveauxDossiers(List<Contribuable> contribuables) throws EditiqueException, JMSException {
		final String prefixe = impressionNouveauxDossiersHelper.calculPrefixe();
		final TypFichierImpression typFichierImpression = impressionNouveauxDossiersHelper.remplitNouveauDossier(contribuables);
		final String nomDocument = impressionNouveauxDossiersHelper.construitIdDocument(contribuables.get(0));
		return editiqueService.creerDocumentImmediatement(nomDocument, prefixe, TypeFormat.PDF, typFichierImpression, false);
	}

	public void imprimeSommationDIForBatch(DeclarationImpotOrdinaire declaration, boolean miseSousPliImpossible, RegDate dateEvenement) throws EditiqueException {
		final String typeDocument = impressionSommationDIHelper.calculPrefixe();
		final ImpressionSommationDIHelperParams params = ImpressionSommationDIHelperParams.createBatchParams(declaration, miseSousPliImpossible, dateEvenement);
		final TypFichierImpression typFichierImpression = impressionSommationDIHelper.remplitSommationDI(params);
		editiqueService.creerDocumentParBatch(typFichierImpression, typeDocument, true);
	}

	public void imprimeSommationLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException {
		final String typeDocument = impressionSommationLRHelper.calculPrefixe();
		final TypFichierImpressionIS typFichierImpression = impressionSommationLRHelper.remplitSommationLR(lr, dateEvenement);
		editiqueService.creerDocumentParBatch(typFichierImpression, typeDocument, true);
	}

	public EditiqueResultat imprimeSommationDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException, JMSException {
		final String typeDocument = impressionSommationDIHelper.calculPrefixe();
		final String[] infoOperateur = getInfoOperateur();
		final ImpressionSommationDIHelperParams params = ImpressionSommationDIHelperParams.createOnlineParams(declaration, infoOperateur[0], infoOperateur[1], getNumeroTelephoneOperateur(), dateEvenement);
		final TypFichierImpression editiqueDI = impressionSommationDIHelper.remplitSommationDI(params);
		final String nomDocument = impressionSommationDIHelper.construitIdDocument(declaration);
		return editiqueService.creerDocumentImmediatement(nomDocument, typeDocument, TypeFormat.PDF, editiqueDI, true);
	}

	/**
	 * Renvoie le nom et l'e-mail de l'utilisateur connecté
	 * @return tableau contenant le nom et l'e-mail (si disponibles) de l'utilisateur connecté
	 */
	private String[] getInfoOperateur () {
		final String traitePar[] = {"ACI", null};
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		if (visa != null) {
			final Operateur operateur = serviceSecurite.getOperateur(visa);
			if (operateur != null) {
				traitePar[0] = String.format("%s %s", operateur.getPrenom() == null ? "" : operateur.getPrenom(), operateur.getNom() == null ? "" : operateur.getNom());
				traitePar[1] = operateur.getEmail();
			} else {
				LOGGER.warn(String.format("Impossible de récupérer l'opérateur [%s]", visa));
			}
		} else {
			LOGGER.warn("Impossible de récupérer le principal courant");
		}
		return traitePar;
	}

	private String getNumeroTelephoneOperateur() {
		String tel = "";
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		final Integer oid = AuthenticationHelper.getCurrentOID();
		if (visa != null && oid != null) {
			ProfilOperateur po = serviceSecurite.getProfileUtilisateur(visa, oid);
			if (po != null) {
				tel = po.getNoTelephone();
			}
		} else {
			LOGGER.warn("Impossible de récupérer le principal courant ou l'oid courant");
		}
		return tel;
	}

	public EditiqueResultat imprimeSommationLROnline(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException, JMSException {
		final String typeDocument = impressionSommationLRHelper.calculPrefixe();
		final TypFichierImpressionIS editiqueDI = impressionSommationLRHelper.remplitSommationLR(lr, dateEvenement);
		final String nomDocument = impressionSommationLRHelper.construitIdDocument(lr);
		return editiqueService.creerDocumentImmediatement(nomDocument, typeDocument, TypeFormat.PCL, editiqueDI, true);
	}

	public EditiqueResultat imprimeConfirmationDelaiOnline(DeclarationImpotOrdinaire di, DelaiDeclaration delai) throws EditiqueException, JMSException {
		final String typeDocument = impressionConfirmationDelaiHelper.calculPrefixe();
		final String[] infoOperateur = getInfoOperateur();
		final ImpressionConfirmationDelaiHelperParams params = new ImpressionConfirmationDelaiHelperParams(di, delai.getDelaiAccordeAu(), infoOperateur[0], getNumeroTelephoneOperateur(), infoOperateur[1]);
		final TypFichierImpression xml = impressionConfirmationDelaiHelper.remplitConfirmationDelai(params);
		final String nomDocument = impressionConfirmationDelaiHelper.construitIdDocument(di);
		return editiqueService.creerDocumentImmediatement(nomDocument, typeDocument, TypeFormat.PDF, xml, false);
	}

	public EditiqueResultat imprimeLROnline(DeclarationImpotSource lr, RegDate dateEvenement, TypeDocument typeDocument) throws EditiqueException, JMSException {
		final TypFichierImpressionIS editiqueDI = impressionLRHelper.remplitListeRecap(lr);
		final String typeDocumentMessage = impressionLRHelper.calculPrefixe();
		final String nomDocument = impressionLRHelper.construitIdDocument(lr);
		return editiqueService.creerDocumentImmediatement(nomDocument, typeDocumentMessage, TypeFormat.PCL, editiqueDI, false);
	}

	public EditiqueResultat imprimeTaxationOfficeOnline(DeclarationImpotOrdinaire declaration) throws EditiqueException, JMSException {
		final String prefixe = impressionTaxationOfficeHelper.calculPrefixe();
		final TypFichierImpression typFichierImpression = impressionTaxationOfficeHelper.remplitTaxationOffice(declaration);
		final String nomDocument = impressionTaxationOfficeHelper.construitIdDocument(declaration);
		return editiqueService.creerDocumentImmediatement(nomDocument, prefixe, TypeFormat.PCL, typFichierImpression, false);
	}

	public void imprimeTaxationOfficeBatch(DeclarationImpotOrdinaire declaration) throws EditiqueException {
		final String typeDocument = impressionTaxationOfficeHelper.calculPrefixe();
		final TypFichierImpression typFichierImpression = impressionTaxationOfficeHelper.remplitTaxationOffice(declaration);
		editiqueService.creerDocumentParBatch(typFichierImpression, typeDocument, false);
	}

	public EditiqueResultat envoyerImpressionLocaleBordereau(BordereauMouvementDossier bordereau) throws EditiqueException, JMSException {
		final String prefixe = impressionBordereauMouvementDossierHelper.calculePrefixe();
		final String[] infoOperateur = getInfoOperateur();
		final ImpressionBordereauMouvementDossierHelperParams params = new ImpressionBordereauMouvementDossierHelperParams(bordereau, infoOperateur[0], infoOperateur[1], getNumeroTelephoneOperateur());
		final TypFichierImpression fichierImpression = impressionBordereauMouvementDossierHelper.remplitBordereau(params);
		final String nomDocument = impressionBordereauMouvementDossierHelper.construitIdDocument(bordereau);
		return editiqueService.creerDocumentImmediatement(nomDocument, prefixe, TypeFormat.PCL, fichierImpression, false);
	}
}
