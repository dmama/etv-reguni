package ch.vd.unireg.documentfiscal;

import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import ch.vd.editique.unireg.CTypeAffranchissement;
import ch.vd.editique.unireg.CTypeInfoArchivage;
import ch.vd.editique.unireg.CTypeInfoDocument;
import ch.vd.editique.unireg.CTypeInfoEnteteDocument;
import ch.vd.editique.unireg.FichierImpression;
import ch.vd.editique.unireg.STypeLettreBienvenue;
import ch.vd.editique.unireg.STypeZoneAffranchissement;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.editique.ConstantesEditique;
import ch.vd.unireg.editique.EditiqueAbstractHelperImpl;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiquePrefixeHelper;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.TypeLettreBienvenue;

public class ImpressionLettreBienvenueHelperImpl extends EditiqueAbstractHelperImpl implements ImpressionLettreBienvenueHelper {

	private static final String CODE_DOCUMENT_LETTRE_BIENVENUE = TypeDocumentEditique.LETTRE_BIENVENUE.getCodeDocumentEditique().substring(0, 4);

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.LETTRE_BIENVENUE;
	}

	@Override
	public FichierImpression.Document buildDocument(LettreBienvenue lettre, RegDate dateTraitement, boolean batch, boolean duplicata) throws EditiqueException {
		try {
			final Entreprise entreprise = lettre.getEntreprise();
			final CTypeInfoDocument infoDocument = buildInfoDocument(getAdresseEnvoi(entreprise), entreprise);
			final CTypeInfoArchivage infoArchivage = buildInfoArchivage(getTypeDocumentEditique(), construitCleArchivage(lettre), entreprise.getNumero(), dateTraitement);
			final RegDate dateEnvoi = duplicata ? dateTraitement : lettre.getDateEnvoi();
			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocument(entreprise, dateEnvoi, TRAITE_PAR, NOM_SERVICE_EXPEDITEUR, infraService.getACIOIPM(), infraService.getCAT());
			final FichierImpression.Document.LettreBienvenue lb = new FichierImpression.Document.LettreBienvenue(mapType(lettre.getType()));

			final FichierImpression.Document document = new FichierImpression.Document();
			document.setInfoDocument(infoDocument);
			document.setInfoArchivage(infoArchivage);
			document.setInfoEnteteDocument(infoEnteteDocument);
			document.setLettreBienvenue(lb);

			return document;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private static CTypeInfoDocument buildInfoDocument(AdresseEnvoiDetaillee adresseEnvoi, Entreprise entreprise) {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissement(adresseEnvoi,
		                                                                                                      false,
		                                                                                                      ServiceInfrastructureService.noOIPM);
		final STypeZoneAffranchissement zoneAffranchissement = assigneIdEnvoi(infoDoc, entreprise, infosAffranchissement);
		infoDoc.setAffranchissement(new CTypeAffranchissement(zoneAffranchissement, null));
		infoDoc.setVersionXSD(VERSION_XSD);

		infoDoc.setCodDoc(CODE_DOCUMENT_LETTRE_BIENVENUE);
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(TypeDocumentEditique.LETTRE_BIENVENUE));
		infoDoc.setTypDoc(TYPE_DOCUMENT_CO);

		return infoDoc;
	}


	static STypeLettreBienvenue mapType(TypeLettreBienvenue type) {
		switch (type) {
		case APM_VD_NON_RC:
			return STypeLettreBienvenue.APM;
		case HS_HC_ETABLISSEMENT:
			return STypeLettreBienvenue.HCHS_ETABLISSEMENT_STABLE_VD;
		case HS_HC_IMMEUBLE:
			return STypeLettreBienvenue.HCHS_IMMEUBLE_VD;
		case VD_RC:
			return STypeLettreBienvenue.INSCRIPTION_VD;
		default:
			throw new IllegalArgumentException("Valeur non acceptée ici : " + type);
		}
	}

	@Override
	public String construitIdDocument(LettreBienvenue lettre) {
		return String.format("LB %s %s",
		                     StringUtils.leftPad(lettre.getEntreprise().getNumero().toString(), 9, '0'),
		                     new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate()));
	}

	@Override
	public String construitCleArchivage(LettreBienvenue lettre) {
		return String.format("%s %s",
		                     StringUtils.rightPad("Lettre bienvenue", 19, ' '),
		                     new SimpleDateFormat("MMddHHmmssSSS").format(DateHelper.getCurrentDate())
		);
	}
}