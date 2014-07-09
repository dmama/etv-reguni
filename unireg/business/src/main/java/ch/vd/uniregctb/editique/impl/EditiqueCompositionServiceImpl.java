package ch.vd.uniregctb.editique.impl;

import javax.jms.JMSException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import noNamespace.FichierImpressionDocument;
import noNamespace.TypFichierImpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.InformationsDocumentAdapter;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionConfirmationDelaiHelper;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionConfirmationDelaiHelperParams;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionDeclarationImpotOrdinaireHelper;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionSommationDIHelper;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionSommationDIHelperParams;
import ch.vd.uniregctb.declaration.ordinaire.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.declaration.source.ImpressionListeRecapHelper;
import ch.vd.uniregctb.declaration.source.ImpressionSommationLRHelper;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.efacture.ImpressionDocumentEfactureHelperImpl;
import ch.vd.uniregctb.efacture.ImpressionDocumentEfactureParams;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.mouvement.BordereauMouvementDossier;
import ch.vd.uniregctb.mouvement.ImpressionBordereauMouvementDossierHelper;
import ch.vd.uniregctb.mouvement.ImpressionBordereauMouvementDossierHelperParams;
import ch.vd.uniregctb.security.IfoSecProfil;
import ch.vd.uniregctb.tache.ImpressionNouveauxDossiersHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.ModeleFeuille;
import ch.vd.uniregctb.type.TypeDocument;

public class EditiqueCompositionServiceImpl implements EditiqueCompositionService {

	public static final Logger LOGGER = LoggerFactory.getLogger(EditiqueCompositionServiceImpl.class);

	private EditiqueService editiqueService;

	private ImpressionDeclarationImpotOrdinaireHelper impressionDIHelper;
	private ImpressionListeRecapHelper impressionLRHelper;
	private ImpressionSommationDIHelper impressionSommationDIHelper;
	private ImpressionSommationLRHelper impressionSommationLRHelper;
	private ImpressionNouveauxDossiersHelper impressionNouveauxDossiersHelper;
	private ImpressionConfirmationDelaiHelper impressionConfirmationDelaiHelper;
	private ServiceSecuriteService serviceSecurite;
	private ImpressionBordereauMouvementDossierHelper impressionBordereauMouvementDossierHelper;
	private ImpressionDocumentEfactureHelperImpl impressionEfactureHelper;

	public void setEditiqueService(EditiqueService editiqueService) {
		this.editiqueService = editiqueService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionDIHelper(ImpressionDeclarationImpotOrdinaireHelper impressionDIHelper) {
		this.impressionDIHelper = impressionDIHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionLRHelper(ImpressionListeRecapHelper impressionLRHelper) {
		this.impressionLRHelper = impressionLRHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionSommationDIHelper(ImpressionSommationDIHelper impressionSommationDIHelper) {
		this.impressionSommationDIHelper = impressionSommationDIHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionSommationLRHelper(ImpressionSommationLRHelper impressionSommationLRHelper) {
		this.impressionSommationLRHelper = impressionSommationLRHelper;
	}

	public void setImpressionNouveauxDossiersHelper(ImpressionNouveauxDossiersHelper impressionNouveauxDossiersHelper) {
		this.impressionNouveauxDossiersHelper = impressionNouveauxDossiersHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionConfirmationDelaiHelper(ImpressionConfirmationDelaiHelper impressionConfirmationDelaiHelper) {
		this.impressionConfirmationDelaiHelper = impressionConfirmationDelaiHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecurite(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionBordereauMouvementDossierHelper(ImpressionBordereauMouvementDossierHelper impressionBordereauMouvementDossierHelper) {
		this.impressionBordereauMouvementDossierHelper = impressionBordereauMouvementDossierHelper;
	}

	public void setImpressionEfactureHelper(ImpressionDocumentEfactureHelperImpl impressionEfactureHelper) {
		this.impressionEfactureHelper = impressionEfactureHelper;
	}


	private static List<ModeleFeuilleDocumentEditique> buildDefaultAnnexes(DeclarationImpotOrdinaire di) {
		final Set<ModeleFeuilleDocument> listFeuille = di.getModeleDocument().getModelesFeuilleDocument();
		return buildDefaultAnnexes(listFeuille);

	}

	private static List<ModeleFeuilleDocumentEditique> buildDefaultAnnexes(Set<ModeleFeuilleDocument> listFeuille) {
		final List<ModeleFeuilleDocumentEditique> annexes = new ArrayList<>();
		for (ModeleFeuilleDocument feuille : listFeuille) {
			ModeleFeuilleDocumentEditique feuilleEditique = new ModeleFeuilleDocumentEditique();
			feuilleEditique.setIntituleFeuille(feuille.getIntituleFeuille());
			feuilleEditique.setNumeroFormulaire(feuille.getNumeroFormulaire());
			feuilleEditique.setNbreIntituleFeuille(1);
			annexes.add(feuilleEditique);
		}
		return annexes;

	}

	private static List<ModeleFeuilleDocumentEditique> buildAnnexesImmeuble(Set<ModeleFeuilleDocument> listFeuille, int nombreAnnexes) {
		final List<ModeleFeuilleDocumentEditique> annexes = new ArrayList<>();
		for (ModeleFeuilleDocument feuille : listFeuille) {
			if (ModeleFeuille.ANNEXE_320.getCode().equals(feuille.getNumeroFormulaire())) {
				ModeleFeuilleDocumentEditique feuilleEditique = new ModeleFeuilleDocumentEditique();
				feuilleEditique.setIntituleFeuille(feuille.getIntituleFeuille());
				feuilleEditique.setNumeroFormulaire(feuille.getNumeroFormulaire());
				feuilleEditique.setNbreIntituleFeuille(nombreAnnexes);
				annexes.add(feuilleEditique);
			}
		}
		return annexes;
	}

	@Override
	public EditiqueResultat imprimeDIOnline(DeclarationImpotOrdinaire declaration) throws EditiqueException, JMSException {
		return imprimeDIOnline(declaration, declaration.getTypeDeclaration(), buildDefaultAnnexes(declaration), false);
	}

	@Override
	public EditiqueResultat imprimeDuplicataDIOnline(DeclarationImpotOrdinaire declaration, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException, JMSException {
		return imprimeDIOnline(declaration, typeDocument, annexes, true);
	}

	private EditiqueResultat imprimeDIOnline(DeclarationImpotOrdinaire declaration, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes,
	                                         boolean isDuplicata) throws EditiqueException, JMSException {
		final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
		final TypFichierImpression editiqueDI = mainDocument.addNewFichierImpression();
		final TypFichierImpression.Document document = impressionDIHelper.remplitEditiqueSpecifiqueDI(declaration, editiqueDI, typeDocument, annexes);
		final TypFichierImpression.Document[] documents;
		if (isDuplicata || typeDocument == TypeDocument.DECLARATION_IMPOT_VAUDTAX) {
			documents = new TypFichierImpression.Document[1];
			documents[0] = document;
		}
		else {
			documents = new TypFichierImpression.Document[2];
			documents[0] = document;
			documents[1] = document;
		}
		editiqueDI.setDocumentArray(documents);
		final TypeDocumentEditique typeDocumentMessage = impressionDIHelper.getTypeDocumentEditique(typeDocument);
		final String nomDocument = impressionDIHelper.construitIdDocument(declaration);

		final String description = String.format("Document '%s %d' du contribuable %s", typeDocument.getDescription(), declaration.getPeriode().getAnnee(),
				FormatNumeroHelper.numeroCTBToDisplay(declaration.getTiers().getNumero()));
		return editiqueService.creerDocumentImmediatementSynchroneOuInbox(nomDocument, typeDocumentMessage, TypeFormat.PCL, mainDocument, false, description);
	}

	@Override
	public void imprimeDIForBatch(DeclarationImpotOrdinaire declaration) throws EditiqueException {
		final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
		final TypFichierImpression editiqueDI = mainDocument.addNewFichierImpression();
		final TypeDocumentEditique typeDocument = impressionDIHelper.getTypeDocumentEditique(declaration);
		final TypFichierImpression.Document document = impressionDIHelper.remplitEditiqueSpecifiqueDI(declaration, editiqueDI, null, buildDefaultAnnexes(declaration));
		final TypFichierImpression.Document[] documents;
		Assert.notNull(document);
		if (declaration.getTypeDeclaration() == TypeDocument.DECLARATION_IMPOT_VAUDTAX) {
			documents = new TypFichierImpression.Document[1];
			documents[0] = document;
		}
		else {
			documents = new TypFichierImpression.Document[2];
			documents[0] = document;
			documents[1] = document;
		}
		editiqueDI.setDocumentArray(documents);
		final String nomDocument = impressionDIHelper.construitIdDocument(declaration);
		editiqueService.creerDocumentParBatch(nomDocument, typeDocument, mainDocument, false);
	}

	@Override
	public int imprimeAnnexeImmeubleForBatch(InformationsDocumentAdapter infosDocument, Set<ModeleFeuilleDocument> listeModele, int nombreAnnexesImmeuble) throws EditiqueException {

		final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
		final TypFichierImpression editiqueDI = mainDocument.addNewFichierImpression();
		final TypeDocument typeDoc = infosDocument.getTypeDocument();
		final TypeDocumentEditique typeDocument = impressionDIHelper.getTypeDocumentEditique(typeDoc);
		final TypFichierImpression.Document document = impressionDIHelper.remplitEditiqueSpecifiqueDI(infosDocument, editiqueDI, buildAnnexesImmeuble(listeModele, nombreAnnexesImmeuble), true);
		Assert.notNull(document);
		final TypFichierImpression.Document[] documents = new TypFichierImpression.Document[]{document};
		editiqueDI.setDocumentArray(documents);
		final int annee = infosDocument.getAnnee();
		final Integer idDoc = infosDocument.getIdDocument();
		final Tiers tiers = infosDocument.getTiers();
		final String nomDocument = impressionDIHelper.construitIdDocument(annee, idDoc, tiers);
		editiqueService.creerDocumentParBatch(nomDocument, typeDocument, mainDocument, false);
		return nombreAnnexesImmeuble;
	}

	@Override
	public void imprimeLRForBatch(DeclarationImpotSource lr) throws EditiqueException {
		final FichierImpressionDocument document = impressionLRHelper.remplitListeRecap(lr, null);
		final TypeDocumentEditique typeDocument = impressionLRHelper.getTypeDocumentEditique();
		final String nomDocument = impressionLRHelper.construitIdDocument(lr);
		editiqueService.creerDocumentParBatch(nomDocument, typeDocument, document, false);
	}

	@Override
	public EditiqueResultat imprimeNouveauxDossiers(List<Contribuable> contribuables) throws EditiqueException, JMSException {
		final TypeDocumentEditique prefixe = impressionNouveauxDossiersHelper.getTypeDocumentEditique();
		final FichierImpressionDocument document = impressionNouveauxDossiersHelper.remplitNouveauDossier(contribuables);
		final String nomDocument = impressionNouveauxDossiersHelper.construitIdDocument(contribuables.get(0));
		return editiqueService.creerDocumentImmediatementSynchroneOuRien(nomDocument, prefixe, TypeFormat.PDF, document, false);
	}

	@Override
	public void imprimeSommationDIForBatch(DeclarationImpotOrdinaire declaration, boolean miseSousPliImpossible, RegDate dateEvenement) throws EditiqueException {
		final TypeDocumentEditique typeDocument = impressionSommationDIHelper.getTypeDocumentEditique();
		final ImpressionSommationDIHelperParams params = ImpressionSommationDIHelperParams.createBatchParams(declaration, miseSousPliImpossible, dateEvenement);
		final FichierImpressionDocument document = impressionSommationDIHelper.remplitSommationDI(params);
		final String nomDocument = impressionSommationDIHelper.construitIdDocument(declaration);
		editiqueService.creerDocumentParBatch(nomDocument, typeDocument, document, true);
	}

	@Override
	public void imprimeSommationLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException {
		final TypeDocumentEditique typeDocument = impressionSommationLRHelper.getTypeDocumentEditique();
		final FichierImpressionDocument document = impressionSommationLRHelper.remplitSommationLR(lr, dateEvenement);
		final String nomDocument = impressionSommationLRHelper.construitIdDocument(lr);
		editiqueService.creerDocumentParBatch(nomDocument, typeDocument, document, true);
	}

	@Override
	public EditiqueResultat imprimeSommationDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException, JMSException {
		final TypeDocumentEditique typeDocument = impressionSommationDIHelper.getTypeDocumentEditique();
		final String[] infoOperateur = getInfoOperateur();
		final ImpressionSommationDIHelperParams params =
				ImpressionSommationDIHelperParams.createOnlineParams(declaration, infoOperateur[0], infoOperateur[1], getNumeroTelephoneOperateur(), dateEvenement);
		final FichierImpressionDocument document = impressionSommationDIHelper.remplitSommationDI(params);
		final String nomDocument = impressionSommationDIHelper.construitIdDocument(declaration);

		final String description = String.format("Sommation de la déclaration d'impôt %d du contribuable %s", declaration.getPeriode().getAnnee(),
				FormatNumeroHelper.numeroCTBToDisplay(declaration.getTiers().getNumero()));
		return editiqueService.creerDocumentImmediatementSynchroneOuInbox(nomDocument, typeDocument, TypeFormat.PDF, document, true, description);
	}

	/**
	 * Renvoie le nom et l'e-mail de l'utilisateur connecté
	 *
	 * @return tableau contenant le nom et l'e-mail (si disponibles) de l'utilisateur connecté
	 */
	private String[] getInfoOperateur() {
		final String traitePar[] = {"ACI", null};
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		if (visa != null) {
			final Operateur operateur = serviceSecurite.getOperateur(visa);
			if (operateur != null) {
				traitePar[0] = String.format("%s %s", operateur.getPrenom() == null ? "" : operateur.getPrenom(), operateur.getNom() == null ? "" : operateur.getNom());
				traitePar[1] = operateur.getEmail();
			}
			else {
				LOGGER.warn(String.format("Impossible de récupérer l'opérateur [%s]", visa));
			}
		}
		else {
			LOGGER.warn("Impossible de récupérer le principal courant");
		}
		return traitePar;
	}

	private String getNumeroTelephoneOperateur() {
		String tel = "";
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		final Integer oid = AuthenticationHelper.getCurrentOID();
		if (visa != null && oid != null) {
			IfoSecProfil po = serviceSecurite.getProfileUtilisateur(visa, oid);
			if (po != null) {
				tel = po.getNoTelephone();
			}
		}
		else {
			LOGGER.warn("Impossible de récupérer le principal courant ou l'oid courant");
		}
		return tel;
	}

	@Override
	public EditiqueResultat imprimeSommationLROnline(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException, JMSException {
		final TypeDocumentEditique typeDocument = impressionSommationLRHelper.getTypeDocumentEditique();
		final FichierImpressionDocument document = impressionSommationLRHelper.remplitSommationLR(lr, dateEvenement);
		final String nomDocument = impressionSommationLRHelper.construitIdDocument(lr);
		return editiqueService.creerDocumentImmediatementSynchroneOuRien(nomDocument, typeDocument, TypeFormat.PCL, document, true);
	}

	@Override
	public EditiqueResultat imprimeConfirmationDelaiOnline(DeclarationImpotOrdinaire di, DelaiDeclaration delai) throws EditiqueException, JMSException {
		final TypeDocumentEditique typeDocument = impressionConfirmationDelaiHelper.getTypeDocumentEditique();
		final String[] infoOperateur = getInfoOperateur();
		final ImpressionConfirmationDelaiHelperParams params =
				new ImpressionConfirmationDelaiHelperParams(di, delai.getDelaiAccordeAu(), infoOperateur[0], getNumeroTelephoneOperateur(), infoOperateur[1],
						delai.getId(), delai.getLogCreationDate());
		final FichierImpressionDocument document = impressionConfirmationDelaiHelper.remplitConfirmationDelai(params);
		final String nomDocument = impressionConfirmationDelaiHelper.construitIdDocument(delai);

		final String description = String.format("Confirmation de délai accordé au %s de la déclaration d'impôt %d du contribuable %s",
				RegDateHelper.dateToDisplayString(delai.getDelaiAccordeAu()), di.getPeriode().getAnnee(), FormatNumeroHelper.numeroCTBToDisplay(di.getTiers().getNumero()));


		return editiqueService.creerDocumentImmediatementSynchroneOuInbox(nomDocument, typeDocument, TypeFormat.PDF, document, true, description);
	}

	@Override
	public EditiqueResultat imprimeLROnline(DeclarationImpotSource lr, TypeDocument typeDocument) throws EditiqueException, JMSException {
		String[] traitePar = getInfoOperateur();
		FichierImpressionDocument document = impressionLRHelper.remplitListeRecap(lr, traitePar[0]);
		final TypeDocumentEditique typeDocumentMessage = impressionLRHelper.getTypeDocumentEditique();
		final String nomDocument = impressionLRHelper.construitIdDocument(lr);
		return editiqueService.creerDocumentImmediatementSynchroneOuRien(nomDocument, typeDocumentMessage, TypeFormat.PCL, document, false);
	}

	@Override
	public EditiqueResultat envoyerImpressionLocaleBordereau(BordereauMouvementDossier bordereau) throws EditiqueException, JMSException {
		final TypeDocumentEditique prefixe = impressionBordereauMouvementDossierHelper.getTypeDocumentEditique();
		final String[] infoOperateur = getInfoOperateur();
		final ImpressionBordereauMouvementDossierHelperParams params = new ImpressionBordereauMouvementDossierHelperParams(bordereau, infoOperateur[0], infoOperateur[1], getNumeroTelephoneOperateur());
		final FichierImpressionDocument document = impressionBordereauMouvementDossierHelper.remplitBordereau(params);
		final String nomDocument = impressionBordereauMouvementDossierHelper.construitIdDocument(bordereau);
		return editiqueService.creerDocumentImmediatementSynchroneOuRien(nomDocument, prefixe, TypeFormat.PCL, document, false);
	}

	@Override
	public String imprimeDocumentEfacture(Tiers tiers, TypeDocument typeDoc, Date dateTraitement, RegDate dateDemande, BigInteger noAdherent, RegDate dateDemandePrecedente, BigInteger noAdherentPrecedent) throws EditiqueException, JMSException {
		final TypeDocumentEditique prefixe = impressionEfactureHelper.getTypeDocumentEditique(typeDoc);
		ImpressionDocumentEfactureParams params = new ImpressionDocumentEfactureParams(tiers, typeDoc, dateTraitement, dateDemande, noAdherent, dateDemandePrecedente, noAdherentPrecedent);
		final FichierImpressionDocument document = impressionEfactureHelper.remplitDocumentEfacture(params);
		final String nomDocument = impressionEfactureHelper.construitIdDocument(params);
		editiqueService.creerDocumentParBatch(nomDocument, prefixe, document, true);
		return impressionEfactureHelper.construitIdArchivageDocument(params);
	}
}
