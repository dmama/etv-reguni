package ch.vd.unireg.declaration.ordinaire.pm;

import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.editique.ConstantesEditique;
import ch.vd.unireg.editique.EditiqueAbstractHelperImpl;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiquePrefixeHelper;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.xml.editique.pm.CTypeAffranchissement;
import ch.vd.unireg.xml.editique.pm.CTypeInfoArchivage;
import ch.vd.unireg.xml.editique.pm.CTypeInfoDocument;
import ch.vd.unireg.xml.editique.pm.CTypeInfoEnteteDocument;
import ch.vd.unireg.xml.editique.pm.FichierImpression;
import ch.vd.unireg.xml.editique.pm.STypeZoneAffranchissement;

public class ImpressionLettreDecisionDelaiPMHelperImpl extends EditiqueAbstractHelperImpl implements ImpressionLettreDecisionDelaiPMHelper {

	private static final String CODE_DOCUMENT_ACCORD = TypeDocumentEditique.ACCORD_DELAI_PM.getCodeDocumentEditique().substring(0, 4);
	private static final String CODE_DOCUMENT_SURSIS = TypeDocumentEditique.SURSIS.getCodeDocumentEditique().substring(0, 4);
	private static final String CODE_DOCUMENT_REFUS = TypeDocumentEditique.REFUS_DELAI_PM.getCodeDocumentEditique().substring(0, 4);

	public TypeDocumentEditique getTypeDocumentEditique(ImpressionLettreDecisionDelaiPMHelperParams params) {
		switch (params.getTypeLettre()) {
		case ACCORD:
			return TypeDocumentEditique.ACCORD_DELAI_PM;
		case ACCORD_SURSIS:
			return TypeDocumentEditique.SURSIS;
		case REFUS:
			return TypeDocumentEditique.REFUS_DELAI_PM;
		default:
			throw new IllegalArgumentException("Valeur non-supportée : " + params.getTypeLettre());
		}
	}

	@Override
	public String construitIdArchivageDocument(ImpressionLettreDecisionDelaiPMHelperParams params) {
		final String idDelai = createStringIdDelai(params);
		final String texte;
		switch (params.getTypeLettre()) {
		case ACCORD:
			texte = "Accord Delai";
			break;
		case ACCORD_SURSIS:
			texte = "Accord Sursis";
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

	protected String createStringIdDelai(ImpressionLettreDecisionDelaiPMHelperParams params) {
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
	public String construitIdDocument(ImpressionLettreDecisionDelaiPMHelperParams params) {
		final DeclarationImpotOrdinairePM di = params.getDi();
		final String principal = AuthenticationHelper.getCurrentPrincipal();
		final long ts = (System.nanoTime() % 1000000000L) / 1000L;      // fraction de seconde en microsecondes
		return String.format("%4d %02d %09d %d_%s_%d", di.getPeriode().getAnnee(), di.getNumero(), di.getTiers().getNumero(), params.getIdDelai(), principal, ts);
	}

	@Override
	public FichierImpression.Document buildDocument(ImpressionLettreDecisionDelaiPMHelperParams params, String cleArchivage, RegDate dateExpedition) throws EditiqueException {
		try {
			final DeclarationImpotOrdinairePM declaration = params.getDi();
			final ContribuableImpositionPersonnesMorales tiers = declaration.getTiers();
			final TypeDocumentEditique typeDocument = getTypeDocumentEditique(params);
			final CTypeInfoDocument infoDocument = buildInfoDocument(getAdresseEnvoi(tiers), tiers, typeDocument, getCodeDocument(params));
			final CTypeInfoArchivage infoArchivage = buildInfoArchivagePM(typeDocument, cleArchivage, tiers.getNumero(), params.getDateTraitement());

			final String titre = String.format("IMPÔT SUR LE BÉNÉFICE ET LE CAPITAL %d (du %s au %s)",
			                                   declaration.getPeriode().getAnnee(),
			                                   RegDateHelper.dateToDisplayString(declaration.getDateDebutExerciceCommercial()),
			                                   RegDateHelper.dateToDisplayString(declaration.getDateFinExerciceCommercial()));

			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocumentPM(tiers, dateExpedition, TRAITE_PAR, NOM_SERVICE_EXPEDITEUR, infraService.getACIOIPM(), infraService.getCAT(), titre);

			final FichierImpression.Document.RefusDelai refusDelai = buildRefusDelai(params);
			final FichierImpression.Document.AccordDelai accordDelai = buildAccordDelai(params);
			final FichierImpression.Document.AccordDelaiApresSommation sursis = buildSursis(params);

			final FichierImpression.Document document = new FichierImpression.Document();
			document.setInfoDocument(infoDocument);
			document.setInfoArchivage(infoArchivage);
			document.setInfoEnteteDocument(infoEnteteDocument);
			document.setRefusDelai(refusDelai);
			document.setAccordDelai(accordDelai);
			document.setAccordDelaiApresSommation(sursis);

			return document;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private static CTypeInfoDocument buildInfoDocument(AdresseEnvoiDetaillee adresseEnvoi, ContribuableImpositionPersonnesMorales contribuable, TypeDocumentEditique typeDocument, String codeDocument) {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissementPM(adresseEnvoi,
		                                                                                                        false,
		                                                                                                        ServiceInfrastructureService.noOIPM);
		final STypeZoneAffranchissement zoneAffranchissement = assigneIdEnvoiPM(infoDoc, contribuable, infosAffranchissement);
		infoDoc.setAffranchissement(new CTypeAffranchissement(zoneAffranchissement, null));
		infoDoc.setVersionXSD(VERSION_XSD_PM);

		infoDoc.setCodDoc(codeDocument);
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(typeDocument));
		infoDoc.setTypDoc(TYPE_DOCUMENT_CO);

		return infoDoc;
	}

	private static String getCodeDocument(ImpressionLettreDecisionDelaiPMHelperParams params) {
		switch (params.getTypeLettre()) {
		case ACCORD:
			return CODE_DOCUMENT_ACCORD;
		case ACCORD_SURSIS:
			return CODE_DOCUMENT_SURSIS;
		case REFUS:
			return CODE_DOCUMENT_REFUS;
		default:
			throw new IllegalArgumentException("Type de lettre non-supporté : " + params.getTypeLettre());
		}
	}

	@Nullable
	private static FichierImpression.Document.RefusDelai buildRefusDelai(ImpressionLettreDecisionDelaiPMHelperParams params) {
		if (params.getTypeLettre() != ImpressionLettreDecisionDelaiPMHelperParams.TypeLettre.REFUS) {
			return null;
		}

		final DeclarationImpotOrdinairePM di = params.getDi();
		return new FichierImpression.Document.RefusDelai(XmlUtils.regdate2xmlcal(RegDate.get(di.getPeriode().getAnnee())),
		                                                 String.valueOf(params.getDateDemande().index()));
	}

	@Nullable
	private static FichierImpression.Document.AccordDelai buildAccordDelai(ImpressionLettreDecisionDelaiPMHelperParams params) {
		if (params.getTypeLettre() != ImpressionLettreDecisionDelaiPMHelperParams.TypeLettre.ACCORD) {
			return null;
		}

		return new FichierImpression.Document.AccordDelai(String.valueOf(params.getDateDelaiAccorde().index()));
	}

	@Nullable
	private static FichierImpression.Document.AccordDelaiApresSommation buildSursis(ImpressionLettreDecisionDelaiPMHelperParams params) {
		if (params.getTypeLettre() != ImpressionLettreDecisionDelaiPMHelperParams.TypeLettre.ACCORD_SURSIS) {
			return null;
		}

		final DeclarationImpotOrdinairePM di = params.getDi();
		return new FichierImpression.Document.AccordDelaiApresSommation(XmlUtils.regdate2xmlcal(RegDate.get(di.getPeriode().getAnnee())),
		                                                                String.valueOf(params.getDateDemande().index()),
		                                                                String.valueOf(params.getDateSommation().index()),
		                                                                String.valueOf(params.getDateDelaiAccorde().index()));
	}
}
