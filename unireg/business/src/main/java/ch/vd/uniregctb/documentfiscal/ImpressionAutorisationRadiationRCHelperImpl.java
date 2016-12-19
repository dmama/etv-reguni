package ch.vd.uniregctb.documentfiscal;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

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
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.EditiqueAbstractHelperImpl;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiquePrefixeHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tiers;

public class ImpressionAutorisationRadiationRCHelperImpl extends EditiqueAbstractHelperImpl implements ImpressionAutorisationRadiationRCHelper {

	private static final String CODE_DOCUMENT_AUT_RADIATION = TypeDocumentEditique.AUTORISATION_RADIATION_RC.getCodeDocumentEditique().substring(0, 4);

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.AUTORISATION_RADIATION_RC;
	}

	@Override
	public FichierImpression.Document buildDocument(AutorisationRadiationRC lettre, RegDate dateTraitement, Signataires signataires, ServiceSecuriteService serviceSecurite) throws EditiqueException {
		try {
			final Entreprise entreprise = lettre.getEntreprise();
			final Tiers rc = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noRC);
			final AdresseEnvoiDetaillee adresseRC = getAdresseEnvoi(rc);
			final CTypeInfoDocument infoDocument = buildInfoDocument(adresseRC, entreprise);
			final CTypeInfoArchivage infoArchivage = buildInfoArchivage(getTypeDocumentEditique(), construitCleArchivage(lettre), entreprise.getNumero(), dateTraitement);
			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocument(entreprise, lettre.getDateEnvoi(), TRAITE_PAR, NOM_SERVICE_EXPEDITEUR, infraService.getACIOIPM(), infraService.getCAT());

			// ce document est un peu particulier dans le sens où, par exemple, le numéro IDE doit venir de l'entreprise, mais
			// l'adresse de destination doit correspondre au RC...
			infoEnteteDocument.getDestinataire().setAdresse(buildAdresse(adresseRC));

			final String rs = tiersService.getDerniereRaisonSociale(entreprise);

			final List<FichierImpression.Document.AutorisationRadiation.Signatures.Signature> listSignatures = new LinkedList<>();
			for (Signataires.VisaFonction signataire : signataires.getSignataires()) {
				if (!signataire.isEmpty()) {
					listSignatures.add(new FichierImpression.Document.AutorisationRadiation.Signatures.Signature(signataire.getNomPrenomOperateur(serviceSecurite).getNomPrenom(),
					                                                                                             signataire.getLibelleFonction()));
				}
			}
			final FichierImpression.Document.AutorisationRadiation.Signatures signatures = new FichierImpression.Document.AutorisationRadiation.Signatures(listSignatures);

			final FichierImpression.Document.AutorisationRadiation ar = new FichierImpression.Document.AutorisationRadiation(RegDateHelper.toIndexString(lettre.getDateDemande()), rs, signatures);
			return new FichierImpression.Document(infoDocument, infoArchivage, infoEnteteDocument, null, null, null, null, null, null, null, null, null, null, null, ar, null, null, null, null);
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

		infoDoc.setCodDoc(CODE_DOCUMENT_AUT_RADIATION);
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(TypeDocumentEditique.AUTORISATION_RADIATION_RC));
		infoDoc.setTypDoc(TYPE_DOCUMENT_CO);

		return infoDoc;
	}

	@Override
	public String construitIdDocument(AutorisationRadiationRC lettre) {
		return String.format("RADRC %s %s",
		                     StringUtils.leftPad(lettre.getEntreprise().getNumero().toString(), 9, '0'),
		                     new SimpleDateFormat("yyyyMMddHHmmssSSS").format(lettre.getLogCreationDate()));

	}

	@Override
	public String construitCleArchivage(AutorisationRadiationRC lettre) {
		return String.format("%s %s",
		                     StringUtils.rightPad("Aut radiation RC", 19, ' '),
		                     new SimpleDateFormat("MMddHHmmssSSS").format(lettre.getLogCreationDate())
		);
	}
}
