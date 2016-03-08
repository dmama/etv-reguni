package ch.vd.uniregctb.declaration.ordinaire.pm;

import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.editique.unireg.CTypeAffranchissement;
import ch.vd.editique.unireg.CTypeInfoArchivage;
import ch.vd.editique.unireg.CTypeInfoDocument;
import ch.vd.editique.unireg.CTypeInfoEnteteDocument;
import ch.vd.editique.unireg.FichierImpression;
import ch.vd.editique.unireg.STypeZoneAffranchissement;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiquePrefixeHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;

public class ImpressionLettreDecisionDelaiPMHelperImpl extends EditiqueAbstractHelper implements ImpressionLettreDecisionDelaiPMHelper {

	private static final String TYPE_DOCUMENT = "CO";           // pour "Courrier", apparemment
	private static final String TRAITE_PAR = "Registre PM";

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
	public FichierImpression.Document buildDocument(ImpressionLettreDecisionDelaiPMHelperParams params, String cleArchivage) throws EditiqueException {
		try {
			final ContribuableImpositionPersonnesMorales tiers = params.getDi().getTiers();
			final TypeDocumentEditique typeDocument = getTypeDocumentEditique(params);
			final CTypeInfoDocument infoDocument = buildInfoDocument(getAdresseEnvoi(tiers), typeDocument, getCodeDocument(params));
			final CTypeInfoArchivage infoArchivage = buildInfoArchivage(typeDocument, cleArchivage, tiers.getNumero(), params.getDateTraitement());
			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocument(tiers, params.getDateTraitement(), TRAITE_PAR, infraService.getACIOIPM());

			final FichierImpression.Document.RefusDelai refusDelai = buildRefusDelai(params);
			final FichierImpression.Document.AccordDelai accordDelai = buildAccordDelai(params);
			final FichierImpression.Document.AccordDelaiApresSommation sursis = buildSursis(params);

			return new FichierImpression.Document(infoDocument, infoArchivage, infoEnteteDocument, null, null, refusDelai, accordDelai, null, sursis, null, null, null);
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private static CTypeInfoDocument buildInfoDocument(AdresseEnvoiDetaillee adresseEnvoi, TypeDocumentEditique typeDocument, String codeDocument) {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();

		// TODO document HS à renvoyer à l'OIPM ?

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissement(adresseEnvoi, false, ServiceInfrastructureService.noOIPM);
		infoDoc.setIdEnvoi(infosAffranchissement.getRight());
		infoDoc.setAffranchissement(new CTypeAffranchissement(infosAffranchissement.getLeft(), null));
		infoDoc.setVersionXSD(VERSION_XSD);

		infoDoc.setCodDoc(codeDocument);
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(typeDocument));
		infoDoc.setTypDoc(TYPE_DOCUMENT);

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
