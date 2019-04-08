package ch.vd.unireg.declaration.ordinaire.pp;

import java.text.SimpleDateFormat;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.editique.ConstantesEditique;
import ch.vd.unireg.editique.EditiqueAbstractHelperImpl;
import ch.vd.unireg.editique.EditiqueAbstractLegacyHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiquePrefixeHelper;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.xml.editique.pp.CTypeAffranchissement;
import ch.vd.unireg.xml.editique.pp.CTypeInfoArchivage;
import ch.vd.unireg.xml.editique.pp.CTypeInfoDocument;
import ch.vd.unireg.xml.editique.pp.CTypeInfoEnteteDocument;
import ch.vd.unireg.xml.editique.pp.FichierImpression;
import ch.vd.unireg.xml.editique.pp.STypeZoneAffranchissement;

public class ImpressionLettreDecisionDelaiPPHelperImpl extends EditiqueAbstractHelperImpl implements ImpressionLettreDecisionDelaiPPHelper {

	// msi (14.01.2018) : la lettre d'obtention de délai est encore générée à travers les anciens XSD éditique (voir projet xml-beans)
//	private static final String CODE_DOCUMENT_ACCORD = TypeDocumentEditique.ACCORD_DELAI_PP.getCodeDocumentEditique().substring(0, 4);
	private static final String CODE_DOCUMENT_REFUS = TypeDocumentEditique.REFUS_DELAI_PP.getCodeDocumentEditique().substring(0, 4);
	private  MessageHelper messageHelper;

	public TypeDocumentEditique getTypeDocumentEditique(ImpressionLettreDecisionDelaiPPHelperParams params) {
		switch (params.getTypeLettre()) {
//		case ACCORD:
//			return TypeDocumentEditique.ACCORD_DELAI_PP;
		case REFUS:
			return TypeDocumentEditique.REFUS_DELAI_PP;
		default:
			throw new IllegalArgumentException("Valeur non-supportée : " + params.getTypeLettre());
		}
	}

	@Override
	public String construitIdArchivageDocument(ImpressionLettreDecisionDelaiPPHelperParams params) {
		final String idDelai = createStringIdDelai(params);
		final String texte;
		switch (params.getTypeLettre()) {
		case ACCORD:
			texte = "Accord Delai";
			break;
		case REFUS:
			texte = "Refus Delai";
			break;
		default:
			throw new IllegalArgumentException("Type de lettre de décision non-supporté : " + params.getTypeLettre());
		}

		return String.format("%s %s %s",
		                     idDelai,
		                     StringUtils.rightPad(texte, 19, ' '),
		                     new SimpleDateFormat("MMddHHmmssSSS").format(params.getLogCreationDateDelai()));
	}

	protected String createStringIdDelai(ImpressionLettreDecisionDelaiPPHelperParams params) {
		final String stringId = params.getIdDelai().toString();
		if (stringId.length() > 6) {
			return StringUtils.substring(stringId, stringId.length() - 6, stringId.length());
		}
		else if (stringId.length() < 6) {
			return StringUtils.leftPad(stringId, 6, '0');

		}
		return stringId;
	}

	@Override
	public String construitIdDocument(ImpressionLettreDecisionDelaiPPHelperParams params) {
		final DeclarationImpotOrdinairePP di = params.getDi();
		final String principal = AuthenticationHelper.getCurrentPrincipal();
		final long ts = (System.nanoTime() % 1000000000L) / 1000L;      // fraction de seconde en microsecondes
		return String.format("%4d %02d %09d %d_%s_%d", di.getPeriode().getAnnee(), di.getNumero(), di.getTiers().getNumero(), params.getIdDelai(), principal, ts);
	}

	@Override
	public FichierImpression.Document buildDocument(ImpressionLettreDecisionDelaiPPHelperParams params, String cleArchivage, RegDate dateExpedition) throws EditiqueException {
		try {
			final DeclarationImpotOrdinairePP declaration = params.getDi();
			final ContribuableImpositionPersonnesPhysiques tiers = declaration.getTiers();
			final TypeDocumentEditique typeDocument = getTypeDocumentEditique(params);
			final CTypeInfoDocument infoDocument = buildInfoDocument(getAdresseEnvoi(tiers), tiers, typeDocument, getCodeDocument(params));
			final CTypeInfoArchivage infoArchivage = buildInfoArchivagePP(typeDocument, cleArchivage, tiers.getNumero(), params.getDateTraitement());
			final String titre = messageHelper.getMessage("lettre.demande.delai.di.editique.title");

			// [SIFISC-20149] l'expéditeur de la confirmation de délai de DI PP doit être la nouvelle entité si applicable, sinon, le CAT
			final int noCaExpeditrice = Optional
					.ofNullable(EditiqueAbstractLegacyHelper.getNoCollectiviteAdministrativeEmettriceSelonEtiquettes(params.getDi().getTiers(), RegDate.get(), tiersService))
					.orElse(ServiceInfrastructureService.noCAT);
			final CollectiviteAdministrative caExpeditrice = infraService.getCollectivite(noCaExpeditrice);

			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocumentPP(tiers,
			                                                                             dateExpedition,
			                                                                             TRAITE_PAR,
			                                                                             NOM_SERVICE_EXPEDITEUR,
			                                                                             caExpeditrice,
			                                                                             infraService.getCAT(),
			                                                                             titre);

			final FichierImpression.Document.RefusDelai refusDelai = buildRefusDelai(params);
			// final FichierImpression.Document.AccordDelai accordDelai = buildAccordDelai(params);

			final FichierImpression.Document document = new FichierImpression.Document();
			document.setInfoDocument(infoDocument);
			document.setInfoArchivage(infoArchivage);
			document.setInfoEnteteDocument(infoEnteteDocument);
			document.setRefusDelai(refusDelai);
			//document.setAccordDelai(accordDelai);

			return document;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private static CTypeInfoDocument buildInfoDocument(AdresseEnvoiDetaillee adresseEnvoi, ContribuableImpositionPersonnesPhysiques contribuable, TypeDocumentEditique typeDocument, String codeDocument) {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissementPP(adresseEnvoi,
		                                                                                                        false,
		                                                                                                        ServiceInfrastructureService.noCAT);
		final STypeZoneAffranchissement zoneAffranchissement = assigneIdEnvoiPP(infoDoc, contribuable, infosAffranchissement);
		infoDoc.setAffranchissement(new CTypeAffranchissement(zoneAffranchissement, null));
		infoDoc.setVersionXSD(VERSION_XSD_PP);

		infoDoc.setCodDoc(codeDocument);
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PP);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(typeDocument));
		infoDoc.setTypDoc(TYPE_DOCUMENT_CO);

		return infoDoc;
	}

	private static String getCodeDocument(ImpressionLettreDecisionDelaiPPHelperParams params) {
		switch (params.getTypeLettre()) {
//		case ACCORD:
//			return CODE_DOCUMENT_ACCORD;
		case REFUS:
			return CODE_DOCUMENT_REFUS;
		default:
			throw new IllegalArgumentException("Type de lettre non-supporté : " + params.getTypeLettre());
		}
	}

	@Nullable
	private static FichierImpression.Document.RefusDelai buildRefusDelai(ImpressionLettreDecisionDelaiPPHelperParams params) {
		if (params.getTypeLettre() != ImpressionLettreDecisionDelaiPPHelperParams.TypeLettre.REFUS) {
			return null;
		}

		final DeclarationImpotOrdinairePP di = params.getDi();
		return new FichierImpression.Document.RefusDelai(XmlUtils.regdate2xmlcal(RegDate.get(di.getPeriode().getAnnee())),
		                                                 String.valueOf(params.getDateDemande().index()));
	}

	public void setMessageHelper(MessageHelper messageHelper) {
		this.messageHelper = messageHelper;
	}

//	@Nullable
//	private static FichierImpression.Document.AccordDelai buildAccordDelai(ImpressionLettreDecisionDelaiPPHelperParams params) {
//		if (params.getTypeLettre() != ImpressionLettreDecisionDelaiPPHelperParams.TypeLettre.ACCORD) {
//			return null;
//		}
//
//		return new FichierImpression.Document.AccordDelai(String.valueOf(params.getDateDelaiAccorde().index()));
//	}
}
