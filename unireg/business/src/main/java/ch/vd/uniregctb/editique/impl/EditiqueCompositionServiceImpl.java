package ch.vd.uniregctb.editique.impl;

import javax.jms.JMSException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import noNamespace.FichierImpressionDocument;
import noNamespace.TypFichierImpression;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.declaration.ordinaire.pm.ImpressionDeclarationImpotPersonnesMoralesHelper;
import ch.vd.uniregctb.declaration.ordinaire.pm.ImpressionLettreDecisionDelaiPMHelper;
import ch.vd.uniregctb.declaration.ordinaire.pm.ImpressionLettreDecisionDelaiPMHelperParams;
import ch.vd.uniregctb.declaration.ordinaire.pm.ImpressionSommationDeclarationImpotPersonnesMoralesHelper;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImpressionConfirmationDelaiHelperParams;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImpressionConfirmationDelaiPPHelper;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImpressionDeclarationImpotPersonnesPhysiquesHelper;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImpressionSommationDIHelperParams;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImpressionSommationDeclarationImpotPersonnesPhysiquesHelper;
import ch.vd.uniregctb.declaration.ordinaire.pp.InformationsDocumentAdapter;
import ch.vd.uniregctb.declaration.snc.ImpressionQuestionnaireSNCHelper;
import ch.vd.uniregctb.declaration.snc.ImpressionRappelQuestionnaireSNCHelper;
import ch.vd.uniregctb.declaration.source.ImpressionListeRecapHelper;
import ch.vd.uniregctb.declaration.source.ImpressionSommationLRHelper;
import ch.vd.uniregctb.documentfiscal.ImpressionLettreBienvenueHelper;
import ch.vd.uniregctb.documentfiscal.ImpressionRappelHelper;
import ch.vd.uniregctb.documentfiscal.LettreBienvenue;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.editique.FormatDocumentEditique;
import ch.vd.uniregctb.editique.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.efacture.ImpressionDocumentEfactureHelper;
import ch.vd.uniregctb.efacture.ImpressionDocumentEfactureParams;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.interfaces.service.host.Operateur;
import ch.vd.uniregctb.mouvement.BordereauMouvementDossier;
import ch.vd.uniregctb.mouvement.ImpressionBordereauMouvementDossierHelper;
import ch.vd.uniregctb.mouvement.ImpressionBordereauMouvementDossierHelperParams;
import ch.vd.uniregctb.security.IfoSecProfil;
import ch.vd.uniregctb.tache.ImpressionNouveauxDossiersHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.GroupeTypesDocumentBatchLocal;
import ch.vd.uniregctb.type.ModeleFeuille;
import ch.vd.uniregctb.type.TypeDocument;

public class EditiqueCompositionServiceImpl implements EditiqueCompositionService {

	public static final Logger LOGGER = LoggerFactory.getLogger(EditiqueCompositionServiceImpl.class);

	private EditiqueService editiqueService;

	private ImpressionDeclarationImpotPersonnesPhysiquesHelper impressionDIPPHelper;
	private ImpressionDeclarationImpotPersonnesMoralesHelper impressionDIPMHelper;
	private ImpressionListeRecapHelper impressionLRHelper;
	private ImpressionSommationDeclarationImpotPersonnesPhysiquesHelper impressionSommationDIPPHelper;
	private ImpressionSommationDeclarationImpotPersonnesMoralesHelper impressionSommationDIPMHelper;
	private ImpressionSommationLRHelper impressionSommationLRHelper;
	private ImpressionNouveauxDossiersHelper impressionNouveauxDossiersHelper;
	private ImpressionConfirmationDelaiPPHelper impressionConfirmationDelaiPPHelper;
	private ImpressionLettreDecisionDelaiPMHelper impressionLettreDecisionDelaiPMHelper;
	private ServiceSecuriteService serviceSecurite;
	private ImpressionBordereauMouvementDossierHelper impressionBordereauMouvementDossierHelper;
	private ImpressionDocumentEfactureHelper impressionEfactureHelper;
	private ImpressionLettreBienvenueHelper impressionLettreBienvenueHelper;
	private ImpressionRappelHelper impressionRappelHelper;
	private ImpressionQuestionnaireSNCHelper impressionQSNCHelper;
	private ImpressionRappelQuestionnaireSNCHelper impressionRappelQSNCHelper;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEditiqueService(EditiqueService editiqueService) {
		this.editiqueService = editiqueService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionDIPPHelper(ImpressionDeclarationImpotPersonnesPhysiquesHelper impressionDIPPHelper) {
		this.impressionDIPPHelper = impressionDIPPHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionDIPMHelper(ImpressionDeclarationImpotPersonnesMoralesHelper impressionDIPMHelper) {
		this.impressionDIPMHelper = impressionDIPMHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionLRHelper(ImpressionListeRecapHelper impressionLRHelper) {
		this.impressionLRHelper = impressionLRHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionSommationDIPPHelper(ImpressionSommationDeclarationImpotPersonnesPhysiquesHelper impressionSommationDIPPHelper) {
		this.impressionSommationDIPPHelper = impressionSommationDIPPHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionSommationDIPMHelper(ImpressionSommationDeclarationImpotPersonnesMoralesHelper impressionSommationDIPMHelper) {
		this.impressionSommationDIPMHelper = impressionSommationDIPMHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionSommationLRHelper(ImpressionSommationLRHelper impressionSommationLRHelper) {
		this.impressionSommationLRHelper = impressionSommationLRHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionNouveauxDossiersHelper(ImpressionNouveauxDossiersHelper impressionNouveauxDossiersHelper) {
		this.impressionNouveauxDossiersHelper = impressionNouveauxDossiersHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionConfirmationDelaiPPHelper(ImpressionConfirmationDelaiPPHelper impressionConfirmationDelaiPPHelper) {
		this.impressionConfirmationDelaiPPHelper = impressionConfirmationDelaiPPHelper;
	}

	public void setImpressionLettreDecisionDelaiPMHelper(ImpressionLettreDecisionDelaiPMHelper impressionLettreDecisionDelaiPMHelper) {
		this.impressionLettreDecisionDelaiPMHelper = impressionLettreDecisionDelaiPMHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecurite(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionBordereauMouvementDossierHelper(ImpressionBordereauMouvementDossierHelper impressionBordereauMouvementDossierHelper) {
		this.impressionBordereauMouvementDossierHelper = impressionBordereauMouvementDossierHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionEfactureHelper(ImpressionDocumentEfactureHelper impressionEfactureHelper) {
		this.impressionEfactureHelper = impressionEfactureHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionLettreBienvenueHelper(ImpressionLettreBienvenueHelper impressionLettreBienvenueHelper) {
		this.impressionLettreBienvenueHelper = impressionLettreBienvenueHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionRappelHelper(ImpressionRappelHelper impressionRappelHelper) {
		this.impressionRappelHelper = impressionRappelHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionQSNCHelper(ImpressionQuestionnaireSNCHelper impressionQSNCHelper) {
		this.impressionQSNCHelper = impressionQSNCHelper;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImpressionRappelQSNCHelper(ImpressionRappelQuestionnaireSNCHelper impressionRappelQSNCHelper) {
		this.impressionRappelQSNCHelper = impressionRappelQSNCHelper;
	}

	private static List<ModeleFeuilleDocumentEditique> buildDefaultAnnexes(DeclarationImpotOrdinaire di) {
		final Set<ModeleFeuilleDocument> listFeuille = di.getModeleDocument().getModelesFeuilleDocument();
		return buildDefaultAnnexes(listFeuille);
	}

	@Nullable
	private static ModeleDocument getModeleDocument(PeriodeFiscale pf, TypeDocument typeDocument) {
		final Set<ModeleDocument> modeles = pf.getModelesDocument();
		for (ModeleDocument modele : modeles) {
			if (!modele.isAnnule() && modele.getTypeDocument() == typeDocument) {
				return modele;
			}
		}
		return null;
	}

	private static List<ModeleFeuilleDocumentEditique> buildDefaultAnnexesForBatch(DeclarationImpotOrdinaire di) {
		// il faut trouver le bon type de document
		final TypeDocument typeDocument;

		// de quel groupe de DI fait partie la DI donnée...
		final TypeDocument typeDocumentOriginal = di.getTypeDeclaration();
		final GroupeTypesDocumentBatchLocal groupe = GroupeTypesDocumentBatchLocal.of(typeDocumentOriginal);
		if (groupe != null) {
			typeDocument = groupe.getBatch();
		}
		else {
			typeDocument = typeDocumentOriginal;
		}

		// récupération du modèle pour le type de document donné dans la PF de la déclaration
		final ModeleDocument modele = getModeleDocument(di.getPeriode(), typeDocument);
		if (modele != null) {
			return buildDefaultAnnexes(modele.getModelesFeuilleDocument());
		}
		else {
			return Collections.emptyList();
		}
	}

	private static List<ModeleFeuilleDocumentEditique> buildDefaultAnnexes(Set<ModeleFeuilleDocument> listFeuille) {
		final List<ModeleFeuilleDocumentEditique> annexes = new ArrayList<>();
		for (ModeleFeuilleDocument feuille : listFeuille) {
			final ModeleFeuilleDocumentEditique feuilleEditique = new ModeleFeuilleDocumentEditique(feuille, 1);
			annexes.add(feuilleEditique);
		}
		return annexes;
	}

	private static List<ModeleFeuilleDocumentEditique> buildAnnexesImmeuble(Set<ModeleFeuilleDocument> listFeuille, int nombreAnnexes) {
		final List<ModeleFeuilleDocumentEditique> annexes = new ArrayList<>();
		for (ModeleFeuilleDocument feuille : listFeuille) {
			if (ModeleFeuille.ANNEXE_320.getNoCADEV() == feuille.getNoCADEV()) {
				final ModeleFeuilleDocumentEditique feuilleEditique = new ModeleFeuilleDocumentEditique(feuille, nombreAnnexes);
				annexes.add(feuilleEditique);
			}
		}
		return annexes;
	}

	@Override
	public EditiqueResultat imprimeDIOnline(DeclarationImpotOrdinairePP declaration) throws EditiqueException, JMSException {
		return imprimeDIOnline(declaration, declaration.getTypeDeclaration(), buildDefaultAnnexes(declaration), false);
	}

	@Override
	public EditiqueResultat imprimeDIOnline(DeclarationImpotOrdinairePM declaration) throws EditiqueException, JMSException {
		return imprimeDIOnline(declaration, buildDefaultAnnexes(declaration), false);
	}

	@Override
	public EditiqueResultat imprimeDuplicataDIOnline(DeclarationImpotOrdinairePP declaration, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException, JMSException {
		return imprimeDIOnline(declaration, typeDocument, annexes, true);
	}

	@Override
	public EditiqueResultat imprimeDuplicataDIOnline(DeclarationImpotOrdinairePM declaration, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException, JMSException {
		return imprimeDIOnline(declaration, annexes, true);
	}

	private EditiqueResultat imprimeDIOnline(DeclarationImpotOrdinairePP declaration, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes, boolean isDuplicata) throws EditiqueException, JMSException {
		final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
		final TypFichierImpression editiqueDI = mainDocument.addNewFichierImpression();
		final TypFichierImpression.Document document = impressionDIPPHelper.remplitEditiqueSpecifiqueDI(declaration, editiqueDI, typeDocument, annexes);
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
		final TypeDocumentEditique typeDocumentMessage = impressionDIPPHelper.getTypeDocumentEditique(typeDocument);
		final String nomDocument = impressionDIPPHelper.construitIdDocument(declaration);

		final String description = String.format("Document '%s %d' du contribuable %s",
		                                         typeDocument.getDescription(),
		                                         declaration.getPeriode().getAnnee(),
		                                         FormatNumeroHelper.numeroCTBToDisplay(declaration.getTiers().getNumero()));
		return editiqueService.creerDocumentImmediatementSynchroneOuInbox(nomDocument, typeDocumentMessage, FormatDocumentEditique.PCL, mainDocument, false, description);
	}

	private EditiqueResultat imprimeDIOnline(DeclarationImpotOrdinairePM declaration, List<ModeleFeuilleDocumentEditique> annexes, boolean isDuplicata) throws EditiqueException, JMSException {
		final FichierImpression root = new FichierImpression();
		final FichierImpression.Document document = impressionDIPMHelper.buildDocument(declaration, annexes);
		final TypeDocumentEditique typeDocument = impressionDIPMHelper.getTypeDocumentEditique(declaration);
		root.getDocument().add(document);
		if (!isDuplicata && impressionOrignalDoubleExemplaire(typeDocument)) {
			// 2 exemplaires pour les envois originaux APM
			root.getDocument().add(document);
		}
		final String nomDocument = impressionDIPMHelper.getIdDocument(declaration);

		final String description = String.format("Document '%s %d' du contribuable %s",
		                                         declaration.getTypeDeclaration().getDescription(),
		                                         declaration.getPeriode().getAnnee(),
		                                         FormatNumeroHelper.numeroCTBToDisplay(declaration.getTiers().getNumero()));

		return editiqueService.creerDocumentImmediatementSynchroneOuInbox(nomDocument, typeDocument, FormatDocumentEditique.PCL, root, false, description);
	}

	@Override
	public void imprimeDIForBatch(DeclarationImpotOrdinairePP declaration) throws EditiqueException {
		final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
		final TypFichierImpression editiqueDI = mainDocument.addNewFichierImpression();
		final TypeDocumentEditique typeDocument = impressionDIPPHelper.getTypeDocumentEditique(declaration);
		final TypFichierImpression.Document document = impressionDIPPHelper.remplitEditiqueSpecifiqueDI(declaration, editiqueDI, null, buildDefaultAnnexes(declaration));
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
		final String nomDocument = impressionDIPPHelper.construitIdDocument(declaration);
		editiqueService.creerDocumentParBatch(nomDocument, typeDocument, mainDocument, false);
	}

	@Override
	public void imprimeDIForBatch(DeclarationImpotOrdinairePM declaration) throws EditiqueException {
		final FichierImpression root = new FichierImpression();
		final FichierImpression.Document document = impressionDIPMHelper.buildDocument(declaration, buildDefaultAnnexesForBatch(declaration));
		final TypeDocumentEditique typeDocument = impressionDIPMHelper.getTypeDocumentEditique(declaration);
		root.getDocument().add(document);
		if (impressionOrignalDoubleExemplaire(typeDocument)) {
			root.getDocument().add(document);       // 2 exemplaires pour les envois originaux
		}
		final String nomDocument = impressionDIPMHelper.getIdDocument(declaration);
		editiqueService.creerDocumentParBatch(nomDocument, typeDocument, root, false);
	}

	/**
	 * [SIFISC-19498] Les originaux de DI PM ne sont imprimés qu'en un seul exemplaire, alors que les originaux
	 * de DI APM sont imprimés en deux exemplaires
	 * @param typeDocument le type de document à imprimer en original
	 * @return <code>true</code> si l'impression doit se faire en deux exemplaires
	 */
	private static boolean impressionOrignalDoubleExemplaire(TypeDocumentEditique typeDocument) {
		return typeDocument == TypeDocumentEditique.DI_APM;
	}

	@Override
	public int imprimeAnnexeImmeubleForBatch(InformationsDocumentAdapter infosDocument, Set<ModeleFeuilleDocument> listeModele, int nombreAnnexesImmeuble) throws EditiqueException {

		final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
		final TypFichierImpression editiqueDI = mainDocument.addNewFichierImpression();
		final TypeDocument typeDoc = infosDocument.getTypeDocument();
		final TypeDocumentEditique typeDocument = impressionDIPPHelper.getTypeDocumentEditique(typeDoc);
		final TypFichierImpression.Document document = impressionDIPPHelper.remplitEditiqueSpecifiqueDI(infosDocument, editiqueDI, buildAnnexesImmeuble(listeModele, nombreAnnexesImmeuble), true);
		Assert.notNull(document);
		final TypFichierImpression.Document[] documents = new TypFichierImpression.Document[]{document};
		editiqueDI.setDocumentArray(documents);
		final int annee = infosDocument.getAnnee();
		final Integer idDoc = infosDocument.getIdDocument();
		final Tiers tiers = infosDocument.getTiers();
		final String nomDocument = impressionDIPPHelper.construitIdDocument(annee, idDoc, tiers);
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
		return editiqueService.creerDocumentImmediatementSynchroneOuRien(nomDocument, prefixe, FormatDocumentEditique.PDF, document, false);
	}

	@Override
	public void imprimeSommationDIForBatch(DeclarationImpotOrdinairePP declaration, boolean miseSousPliImpossible, RegDate dateEvenement, @Nullable Integer emolument) throws EditiqueException {
		final TypeDocumentEditique typeDocument = impressionSommationDIPPHelper.getTypeDocumentEditique();
		final ImpressionSommationDIHelperParams params = ImpressionSommationDIHelperParams.batch(declaration, miseSousPliImpossible, dateEvenement, emolument);
		final FichierImpressionDocument document = impressionSommationDIPPHelper.remplitSommationDI(params);
		final String nomDocument = impressionSommationDIPPHelper.construitIdDocument(declaration);
		editiqueService.creerDocumentParBatch(nomDocument, typeDocument, document, true);
	}

	@Override
	public void imprimeSommationDIForBatch(DeclarationImpotOrdinairePM declaration, RegDate dateTraitement, RegDate dateOfficielleEnvoi) throws EditiqueException {
		final TypeDocumentEditique typeDocument = impressionSommationDIPMHelper.getTypeDocumentEditique();
		final FichierImpression root = new FichierImpression();
		final FichierImpression.Document original = impressionSommationDIPMHelper.buildDocument(declaration, dateTraitement, dateOfficielleEnvoi, true);
		final FichierImpression.Document copieMandataire = impressionSommationDIPMHelper.buildCopieMandataire(original, declaration.getTiers(), RegDate.get());
		root.getDocument().add(original);
		if (copieMandataire != null) {
			root.getDocument().add(copieMandataire);
		}

		final String nomDocument = impressionSommationDIPMHelper.construitIdDocument(declaration);
		editiqueService.creerDocumentParBatch(nomDocument, typeDocument, root, true);
	}

	@Override
	public void imprimeSommationLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException {
		final TypeDocumentEditique typeDocument = impressionSommationLRHelper.getTypeDocumentEditique();
		final FichierImpressionDocument document = impressionSommationLRHelper.remplitSommationLR(lr, dateEvenement);
		final String nomDocument = impressionSommationLRHelper.construitIdDocument(lr);
		editiqueService.creerDocumentParBatch(nomDocument, typeDocument, document, true);
	}

	@Override
	public EditiqueResultat imprimeSommationDIOnline(DeclarationImpotOrdinairePP declaration, RegDate dateEvenement, @Nullable Integer emolument) throws EditiqueException, JMSException {
		final TypeDocumentEditique typeDocument = impressionSommationDIPPHelper.getTypeDocumentEditique();
		final String[] infoOperateur = getInfoOperateur();
		final ImpressionSommationDIHelperParams params = ImpressionSommationDIHelperParams.online(declaration, infoOperateur[0], infoOperateur[1], getNumeroTelephoneOperateur(), dateEvenement, emolument);
		final FichierImpressionDocument document = impressionSommationDIPPHelper.remplitSommationDI(params);
		final String nomDocument = impressionSommationDIPPHelper.construitIdDocument(declaration);

		final String description = String.format("Sommation de la déclaration d'impôt %d du contribuable %s",
		                                         declaration.getPeriode().getAnnee(),
		                                         FormatNumeroHelper.numeroCTBToDisplay(declaration.getTiers().getNumero()));
		return editiqueService.creerDocumentImmediatementSynchroneOuInbox(nomDocument, typeDocument, FormatDocumentEditique.PDF, document, true, description);
	}

	@Override
	public EditiqueResultat imprimeSommationDIOnline(DeclarationImpotOrdinairePM declaration, RegDate dateTraitement, RegDate dateOfficielleEnvoi) throws EditiqueException, JMSException {
		final TypeDocumentEditique typeDocument = impressionSommationDIPMHelper.getTypeDocumentEditique();

		final FichierImpression root = new FichierImpression();
		final FichierImpression.Document original = impressionSommationDIPMHelper.buildDocument(declaration, dateTraitement, dateOfficielleEnvoi, false);
		final FichierImpression.Document copieMandataire = impressionSommationDIPMHelper.buildCopieMandataire(original, declaration.getTiers(), RegDate.get());
		root.getDocument().add(original);
		if (copieMandataire != null) {
			root.getDocument().add(copieMandataire);
		}

		final String nomDocument = impressionSommationDIPMHelper.construitIdDocument(declaration);
		final String description = String.format("Sommation de la déclaration d'impôt %d du contribuable %s",
		                                         declaration.getPeriode().getAnnee(),
		                                         FormatNumeroHelper.numeroCTBToDisplay(declaration.getTiers().getNumero()));
		return editiqueService.creerDocumentImmediatementSynchroneOuInbox(nomDocument, typeDocument, FormatDocumentEditique.PDF, root, true, description);
	}

	@Override
	public void imprimeLettreBienvenueForBatch(LettreBienvenue lettre, RegDate dateTraitement) throws EditiqueException {
		final TypeDocumentEditique typeDocument = impressionLettreBienvenueHelper.getTypeDocumentEditique();

		final FichierImpression root = new FichierImpression();
		final FichierImpression.Document original = impressionLettreBienvenueHelper.buildDocument(lettre, dateTraitement, true);
		final FichierImpression.Document copieMandataire = impressionLettreDecisionDelaiPMHelper.buildCopieMandataire(original, lettre.getEntreprise(), RegDate.get());
		root.getDocument().add(original);
		if (copieMandataire != null) {
			root.getDocument().add(copieMandataire);
		}

		// sauvegarde de la clé d'archivage
		lettre.setCleArchivage(original.getInfoArchivage() != null ? original.getInfoArchivage().getIdDocument() : null);

		final String nomDocument = impressionLettreBienvenueHelper.construitIdDocument(lettre);
		editiqueService.creerDocumentParBatch(nomDocument, typeDocument, root, true);
	}

	@Override
	public void imprimeRappelLettreBienvenueForBatch(LettreBienvenue lettre, RegDate dateTraitement) throws EditiqueException {
		final TypeDocumentEditique typeDocument = impressionRappelHelper.getTypeDocumentEditique();

		final FichierImpression root = new FichierImpression();
		final FichierImpression.Document original = impressionRappelHelper.buildDocument(lettre, dateTraitement, true);
		final FichierImpression.Document copieMandataire = impressionRappelHelper.buildCopieMandataire(original, lettre.getEntreprise(), RegDate.get());
		root.getDocument().add(original);
		if (copieMandataire != null) {
			root.getDocument().add(copieMandataire);
		}

		// sauvegarde de la clé d'archivage
		lettre.setCleArchivageRappel(original.getInfoArchivage() != null ? original.getInfoArchivage().getIdDocument() : null);

		final String nomDocument = impressionRappelHelper.construitIdDocument(lettre);
		editiqueService.creerDocumentParBatch(nomDocument, typeDocument, root, true);
	}

	/**
	 * Renvoie le nom et l'e-mail de l'utilisateur connecté
	 *
	 * @return tableau contenant le nom et l'e-mail (si disponibles) de l'utilisateur connecté
	 */
	private String[] getInfoOperateur() {
		final String[] traitePar = {"ACI", null};
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
		return editiqueService.creerDocumentImmediatementSynchroneOuRien(nomDocument, typeDocument, FormatDocumentEditique.PCL, document, true);
	}

	@Override
	public Pair<EditiqueResultat, String> imprimeConfirmationDelaiOnline(DeclarationImpotOrdinairePP di, DelaiDeclaration delai) throws EditiqueException, JMSException {
		final TypeDocumentEditique typeDocument = impressionConfirmationDelaiPPHelper.getTypeDocumentEditique();
		final String[] infoOperateur = getInfoOperateur();
		final ImpressionConfirmationDelaiHelperParams params = new ImpressionConfirmationDelaiHelperParams(di, delai.getDelaiAccordeAu(),
		                                                                                                   infoOperateur[0], getNumeroTelephoneOperateur(), infoOperateur[1],
		                                                                                                   delai.getId(), delai.getLogCreationDate());
		final String cleArchivage = impressionConfirmationDelaiPPHelper.construitIdArchivageDocument(params);
		final FichierImpressionDocument document = impressionConfirmationDelaiPPHelper.remplitConfirmationDelai(params, cleArchivage);
		final String nomDocument = impressionConfirmationDelaiPPHelper.construitIdDocument(delai);

		final String description = String.format("Confirmation de délai accordé au %s de la déclaration d'impôt %d du contribuable %s",
		                                         RegDateHelper.dateToDisplayString(delai.getDelaiAccordeAu()),
		                                         di.getPeriode().getAnnee(),
		                                         FormatNumeroHelper.numeroCTBToDisplay(di.getTiers().getNumero()));

		final EditiqueResultat resultat = editiqueService.creerDocumentImmediatementSynchroneOuInbox(nomDocument, typeDocument, FormatDocumentEditique.PDF, document, true, description);
		return Pair.of(resultat, cleArchivage);
	}

	@Override
	public Pair<EditiqueResultat, String> imprimeLettreDecisionDelaiOnline(DeclarationImpotOrdinairePM di, DelaiDeclaration delai) throws EditiqueException, JMSException {
		final ImpressionLettreDecisionDelaiPMHelperParams params = new ImpressionLettreDecisionDelaiPMHelperParams(di, delai);
		final TypeDocumentEditique typeDocument = impressionLettreDecisionDelaiPMHelper.getTypeDocumentEditique(params);
		final String cleArchivage = impressionLettreDecisionDelaiPMHelper.construitIdArchivageDocument(params);

		final FichierImpression root = new FichierImpression();
		final FichierImpression.Document original = impressionLettreDecisionDelaiPMHelper.buildDocument(params, cleArchivage);
		final FichierImpression.Document copieMandataire = impressionLettreDecisionDelaiPMHelper.buildCopieMandataire(original, di.getTiers(), RegDate.get());
		root.getDocument().add(original);
		if (copieMandataire != null) {
			root.getDocument().add(copieMandataire);
		}

		final String nomDocument = impressionLettreDecisionDelaiPMHelper.construitIdDocument(params);
		final EditiqueResultat resultat = editiqueService.creerDocumentImmediatementSynchroneOuInbox(nomDocument, typeDocument, FormatDocumentEditique.PDF, root, true, params.getDescriptionDocument());
		return Pair.of(resultat, cleArchivage);
	}

	@Override
	public String imprimeLettreDecisionDelaiForBatch(DeclarationImpotOrdinairePM di, DelaiDeclaration delai) throws EditiqueException, JMSException {
		final ImpressionLettreDecisionDelaiPMHelperParams params = new ImpressionLettreDecisionDelaiPMHelperParams(di, delai);
		final TypeDocumentEditique typeDocument = impressionLettreDecisionDelaiPMHelper.getTypeDocumentEditique(params);
		final String cleArchivage = impressionLettreDecisionDelaiPMHelper.construitIdArchivageDocument(params);

		final FichierImpression root = new FichierImpression();
		final FichierImpression.Document original = impressionLettreDecisionDelaiPMHelper.buildDocument(params, cleArchivage);
		final FichierImpression.Document copieMandataire = impressionLettreDecisionDelaiPMHelper.buildCopieMandataire(original, di.getTiers(), RegDate.get());
		root.getDocument().add(original);
		if (copieMandataire != null) {
			root.getDocument().add(copieMandataire);
		}

		final String nomDocument = impressionLettreDecisionDelaiPMHelper.construitIdDocument(params);
		editiqueService.creerDocumentParBatch(nomDocument, typeDocument, root, true);
		return cleArchivage;
	}

	@Override
	public EditiqueResultat imprimeLROnline(DeclarationImpotSource lr, TypeDocument typeDocument) throws EditiqueException, JMSException {
		String[] traitePar = getInfoOperateur();
		FichierImpressionDocument document = impressionLRHelper.remplitListeRecap(lr, traitePar[0]);
		final TypeDocumentEditique typeDocumentMessage = impressionLRHelper.getTypeDocumentEditique();
		final String nomDocument = impressionLRHelper.construitIdDocument(lr);
		return editiqueService.creerDocumentImmediatementSynchroneOuRien(nomDocument, typeDocumentMessage, FormatDocumentEditique.PCL, document, false);
	}

	@Override
	public EditiqueResultat envoyerImpressionLocaleBordereau(BordereauMouvementDossier bordereau) throws EditiqueException, JMSException {
		final TypeDocumentEditique prefixe = impressionBordereauMouvementDossierHelper.getTypeDocumentEditique();
		final String[] infoOperateur = getInfoOperateur();
		final ImpressionBordereauMouvementDossierHelperParams params = new ImpressionBordereauMouvementDossierHelperParams(bordereau, infoOperateur[0], infoOperateur[1], getNumeroTelephoneOperateur());
		final FichierImpressionDocument document = impressionBordereauMouvementDossierHelper.remplitBordereau(params);
		final String nomDocument = impressionBordereauMouvementDossierHelper.construitIdDocument(bordereau);
		return editiqueService.creerDocumentImmediatementSynchroneOuRien(nomDocument, prefixe, FormatDocumentEditique.PCL, document, false);
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

	@Override
	public EditiqueResultat imprimeQuestionnaireSNCOnline(QuestionnaireSNC questionnaire) throws EditiqueException, JMSException {
		final FichierImpression root = new FichierImpression();
		final FichierImpression.Document document = impressionQSNCHelper.buildDocument(questionnaire);
		root.getDocument().add(document);
		final TypeDocumentEditique typeDocument = impressionQSNCHelper.getTypeDocumentEditique(questionnaire);
		final String nomDocument = impressionQSNCHelper.getIdDocument(questionnaire);
		return editiqueService.creerDocumentImmediatementSynchroneOuRien(nomDocument, typeDocument, FormatDocumentEditique.PCL, root, false);
	}

	@Override
	public EditiqueResultat imprimeDuplicataQuestionnaireSNCOnline(QuestionnaireSNC questionnaire) throws EditiqueException, JMSException {
		final FichierImpression root = new FichierImpression();
		final FichierImpression.Document document = impressionQSNCHelper.buildDocument(questionnaire);
		root.getDocument().add(document);
		final TypeDocumentEditique typeDocument = impressionQSNCHelper.getTypeDocumentEditique(questionnaire);
		final String nomDocument = impressionQSNCHelper.getIdDocument(questionnaire);

		final String description = String.format("Questionnaire SNC %d du contribuable %s",
		                                         questionnaire.getPeriode().getAnnee(),
		                                         FormatNumeroHelper.numeroCTBToDisplay(questionnaire.getTiers().getNumero()));

		return editiqueService.creerDocumentImmediatementSynchroneOuInbox(nomDocument, typeDocument, FormatDocumentEditique.PCL, root, false, description);
	}

	@Override
	public void imprimerQuestionnaireSNCForBatch(QuestionnaireSNC questionnaire) throws EditiqueException {
		final FichierImpression root = new FichierImpression();
		final FichierImpression.Document document = impressionQSNCHelper.buildDocument(questionnaire);
		root.getDocument().add(document);
		final TypeDocumentEditique typeDocument = impressionQSNCHelper.getTypeDocumentEditique(questionnaire);
		final String nomDocument = impressionQSNCHelper.getIdDocument(questionnaire);
		editiqueService.creerDocumentParBatch(nomDocument, typeDocument, root, false);
	}

	@Override
	public void imprimeRappelQuestionnaireSNCForBatch(QuestionnaireSNC questionnaire, RegDate dateTraitement, RegDate dateOfficielleEnvoi) throws EditiqueException {
		final FichierImpression root = new FichierImpression();
		final FichierImpression.Document original = impressionRappelQSNCHelper.buildDocument(questionnaire, dateTraitement, dateOfficielleEnvoi);
		final FichierImpression.Document copieMandataire = impressionRappelQSNCHelper.buildCopieMandataire(original, questionnaire.getTiers(), RegDate.get());
		root.getDocument().add(original);
		if (copieMandataire != null) {
			root.getDocument().add(copieMandataire);
		}
		final TypeDocumentEditique typeDocument = impressionRappelQSNCHelper.getTypeDocumentEditique(questionnaire);
		final String nomDocument = impressionRappelQSNCHelper.getIdDocument(questionnaire);
		editiqueService.creerDocumentParBatch(nomDocument, typeDocument, root, true);
	}

	@Override
	public EditiqueResultat imprimeRappelQuestionnaireSNCOnline(QuestionnaireSNC questionnaire, RegDate dateTraitement) throws EditiqueException {
		final FichierImpression root = new FichierImpression();
		final FichierImpression.Document original = impressionRappelQSNCHelper.buildDocument(questionnaire, dateTraitement, dateTraitement);
		final FichierImpression.Document copieMandataire = impressionRappelQSNCHelper.buildCopieMandataire(original, questionnaire.getTiers(), RegDate.get());
		root.getDocument().add(original);
		if (copieMandataire != null) {
			root.getDocument().add(copieMandataire);
		}
		final TypeDocumentEditique typeDocument = impressionRappelQSNCHelper.getTypeDocumentEditique(questionnaire);
		final String nomDocument = impressionRappelQSNCHelper.getIdDocument(questionnaire);

		final String description = String.format("Rappel du questionnaire SNC %d du contribuable %s",
		                                         questionnaire.getPeriode().getAnnee(),
		                                         FormatNumeroHelper.numeroCTBToDisplay(questionnaire.getTiers().getNumero()));
		return editiqueService.creerDocumentImmediatementSynchroneOuInbox(nomDocument, typeDocument, FormatDocumentEditique.PDF, root, true, description);
	}
}
