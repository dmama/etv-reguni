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
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.EditiqueAbstractHelperImpl;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiquePrefixeHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Entreprise;

public class ImpressionRappelDemandeDegrevementICIHelperImpl extends EditiqueAbstractHelperImpl implements ImpressionRappelDemandeDegrevementICIHelper {

	private static final String CODE_DOCUMENT_RAPPEL_DEMANDE_DEGREVEMENT_ICI = TypeDocumentEditique.RAPPEL_DEMANDE_DEGREVEMENT_ICI.getCodeDocumentEditique().substring(0, 4);

	private ImpressionDemandeDegrevementICIHelper demandeHelper;

	public void setDemandeHelper(ImpressionDemandeDegrevementICIHelper demandeHelper) {
		this.demandeHelper = demandeHelper;
	}

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.RAPPEL_DEMANDE_DEGREVEMENT_ICI;
	}

	@Override
	public FichierImpression.Document buildDocument(DemandeDegrevementICI demande, RegDate dateTraitement) throws EditiqueException {
		try {
			final Entreprise entreprise = demande.getEntreprise();
			final CTypeInfoDocument infoDocument = buildInfoDocument(getAdresseEnvoi(entreprise), entreprise);
			final CTypeInfoArchivage infoArchivage = buildInfoArchivage(getTypeDocumentEditique(), construitCleArchivage(demande), entreprise.getNumero(), dateTraitement);
			final String titre = String.format("%s %d", IMPOT_COMPLEMENTAIRE_IMMEUBLES, demande.getPeriodeFiscale());
			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocument(entreprise, demande.getDateRappel(), TRAITE_PAR, NOM_SERVICE_EXPEDITEUR, infraService.getACIOIPM(), infraService.getCAT(), titre);
			final FichierImpression.Document.LettreDegrevementImmRappel rappel = new FichierImpression.Document.LettreDegrevementImmRappel(XmlUtils.regdate2xmlcal(RegDate.get(demande.getPeriodeFiscale())),
			                                                                                                                               demandeHelper.getSiegeEntreprise(entreprise, dateTraitement),
			                                                                                                                               demandeHelper.buildCodeBarres(demande),
			                                                                                                                               demande.getCodeControle(),
			                                                                                                                               demandeHelper.buildInfoImmeuble(demande),
			                                                                                                                               RegDateHelper.toIndexString(demande.getDateEnvoi()));
			final FichierImpression.Document document = new FichierImpression.Document();
			document.setInfoDocument(infoDocument);
			document.setInfoArchivage(infoArchivage);
			document.setInfoEnteteDocument(infoEnteteDocument);
			document.setLettreDegrevementImmRappel(rappel);
			return document;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private static CTypeInfoDocument buildInfoDocument(AdresseEnvoiDetaillee adresseEnvoi, Entreprise entreprise) {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissement(adresseEnvoi, true, ServiceInfrastructureService.noOIPM);
		assigneIdEnvoi(infoDoc, entreprise, infosAffranchissement);
		infoDoc.setAffranchissement(new CTypeAffranchissement(infosAffranchissement.getLeft(), null));
		infoDoc.setVersionXSD(VERSION_XSD);

		infoDoc.setCodDoc(CODE_DOCUMENT_RAPPEL_DEMANDE_DEGREVEMENT_ICI);
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(TypeDocumentEditique.RAPPEL_DEMANDE_DEGREVEMENT_ICI));
		infoDoc.setTypDoc(TYPE_DOCUMENT_CO);

		return infoDoc;
	}

	@Override
	public String construitIdDocument(DemandeDegrevementICI demande) {
		return String.format("DDICI Rappel %09d %d%05d %s",
		                     demande.getEntreprise().getNumero(),
		                     demande.getPeriodeFiscale(),
		                     demande.getNumeroSequence(),
		                     new SimpleDateFormat("yyyyMMddHHmmssSSS").format(demande.getLogCreationDate()));
	}

	@Override
	public String construitCleArchivage(DemandeDegrevementICI demande) {
		return String.format("%04d%05d %s %s",
		                     demande.getPeriodeFiscale(),
		                     demande.getNumeroSequence(),
		                     StringUtils.rightPad("Rappel DD ICI", 16, ' '),
		                     new SimpleDateFormat("MMddHHmmssSSS").format(
				                     DateHelper.getCurrentDate()
		                     )
		);
	}
}
