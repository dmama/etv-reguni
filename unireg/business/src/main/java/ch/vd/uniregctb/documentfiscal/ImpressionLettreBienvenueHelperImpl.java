package ch.vd.uniregctb.documentfiscal;

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
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiquePrefixeHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.TypeLettreBienvenue;

public class ImpressionLettreBienvenueHelperImpl extends EditiqueAbstractHelper implements ImpressionLettreBienvenueHelper {

	private static final String TYPE_DOCUMENT = "CO";           // pour "Courrier", apparemment
	private static final String CODE_DOCUMENT_LETTRE_BIENVENUE = TypeDocumentEditique.LETTRE_BIENVENUE.getCodeDocumentEditique().substring(0, 4);
	private static final String TRAITE_PAR = "Registre PM";

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.LETTRE_BIENVENUE;
	}

	@Override
	public FichierImpression.Document buildDocument(LettreBienvenue lettre, RegDate dateTraitement, boolean batch) throws EditiqueException {
		try {
			final Entreprise entreprise = lettre.getEntreprise();
			final CTypeInfoDocument infoDocument = buildInfoDocument(getAdresseEnvoi(entreprise));
			final CTypeInfoArchivage infoArchivage = buildInfoArchivage(getTypeDocumentEditique(), construitCleArchivage(lettre), entreprise.getNumero(), dateTraitement);
			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocument(entreprise, lettre.getDateEnvoi(), TRAITE_PAR, infraService.getACIOIPM());
			final FichierImpression.Document.LettreBienvenue lb = new FichierImpression.Document.LettreBienvenue(mapType(lettre.getType()));
			return new FichierImpression.Document(infoDocument, infoArchivage, infoEnteteDocument, null, null, null, null, null, null, lb, null, null, null, null);
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private static CTypeInfoDocument buildInfoDocument(AdresseEnvoiDetaillee adresseEnvoi) {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();

		// TODO document HS à renvoyer à l'OIPM ?

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissement(adresseEnvoi, false, ServiceInfrastructureService.noOIPM);
		infoDoc.setIdEnvoi(infosAffranchissement.getRight());
		infoDoc.setAffranchissement(new CTypeAffranchissement(infosAffranchissement.getLeft(), null));

		infoDoc.setCodDoc(CODE_DOCUMENT_LETTRE_BIENVENUE);
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(TypeDocumentEditique.LETTRE_BIENVENUE));
		infoDoc.setTypDoc(TYPE_DOCUMENT);

		return infoDoc;
	}


	private static STypeLettreBienvenue mapType(TypeLettreBienvenue type) {
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
		                     new SimpleDateFormat("yyyyMMddHHmmssSSS").format(lettre.getLogCreationDate()));
	}

	@Override
	public String construitCleArchivage(LettreBienvenue lettre) {
		return String.format("%s %s",
		                     StringUtils.rightPad("Lettre bienvenue", 19, ' '),
		                     new SimpleDateFormat("MMddHHmmssSSS").format(lettre.getLogCreationDate())
		);
	}
}
