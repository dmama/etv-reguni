package ch.vd.uniregctb.documentfiscal;

import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import ch.vd.editique.unireg.CTypeAffranchissement;
import ch.vd.editique.unireg.CTypeInfoArchivage;
import ch.vd.editique.unireg.CTypeInfoDocument;
import ch.vd.editique.unireg.CTypeInfoEnteteDocument;
import ch.vd.editique.unireg.FichierImpression;
import ch.vd.editique.unireg.STypeZoneAffranchissement;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.EditiqueAbstractHelperImpl;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiquePrefixeHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Entreprise;

public class ImpressionDemandeBilanFinalHelperImpl extends EditiqueAbstractHelperImpl implements ImpressionDemandeBilanFinalHelper {

	private static final String CODE_DOCUMENT_BILAN_FINAL = TypeDocumentEditique.DEMANDE_BILAN_FINAL.getCodeDocumentEditique().substring(0, 4);

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.DEMANDE_BILAN_FINAL;
	}

	@Override
	public FichierImpression.Document buildDocument(DemandeBilanFinal lettre, RegDate dateTraitement) throws EditiqueException {
		try {
			final Entreprise entreprise = lettre.getEntreprise();
			final CTypeInfoDocument infoDocument = buildInfoDocument(getAdresseEnvoi(entreprise), entreprise);
			final CTypeInfoArchivage infoArchivage = buildInfoArchivage(getTypeDocumentEditique(), construitCleArchivage(lettre), entreprise.getNumero(), dateTraitement);
			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocument(entreprise, lettre.getDateEnvoi(), TRAITE_PAR, NOM_SERVICE_EXPEDITEUR, infraService.getACIOIPM(), infraService.getCAT());

			final FichierImpression.Document.DemandeBilanFinal dbf = new FichierImpression.Document.DemandeBilanFinal(XmlUtils.regdate2xmlcal(RegDate.get(lettre.getPeriodeFiscale())),
			                                                                                                          RegDateHelper.toIndexString(lettre.getDateRequisitionRadiation()));
			return new FichierImpression.Document(infoDocument, infoArchivage, infoEnteteDocument, null, null, null, null, null, null, null, null, null, null, dbf, null, null, null, null, null);
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private static CTypeInfoDocument buildInfoDocument(AdresseEnvoiDetaillee adresseEnvoi, Entreprise entreprise) {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissement(adresseEnvoi, false, ServiceInfrastructureService.noOIPM);
		assigneIdEnvoi(infoDoc, entreprise, infosAffranchissement);     // TODO est-ce vraiment nécessaire dans la mesure où il n'y a pas de batch pour ce document ?
		infoDoc.setAffranchissement(new CTypeAffranchissement(infosAffranchissement.getLeft(), null));
		infoDoc.setVersionXSD(VERSION_XSD);

		infoDoc.setCodDoc(CODE_DOCUMENT_BILAN_FINAL);
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(TypeDocumentEditique.DEMANDE_BILAN_FINAL));
		infoDoc.setTypDoc(TYPE_DOCUMENT_CO);

		return infoDoc;
	}

	@Override
	public String construitIdDocument(DemandeBilanFinal lettre) {
		return String.format("DBF %s %s",
		                     StringUtils.leftPad(lettre.getEntreprise().getNumero().toString(), 9, '0'),
		                     new SimpleDateFormat("yyyyMMddHHmmssSSS").format(lettre.getLogCreationDate()));

	}

	@Override
	public String construitCleArchivage(DemandeBilanFinal lettre) {
		return String.format("%s %s",
		                     StringUtils.rightPad("Demande bilan final", 19, ' '),
		                     new SimpleDateFormat("MMddHHmmssSSS").format(lettre.getLogCreationDate())
		);
	}
}
