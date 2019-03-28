package ch.vd.unireg.efacture;

import java.text.SimpleDateFormat;

import noNamespace.FichierImpressionDocument;
import noNamespace.InfoArchivageDocument;
import noNamespace.InfoDocumentDocument1;
import noNamespace.InfoEnteteDocumentDocument1;
import noNamespace.LettresEFactureDocument;
import noNamespace.TypAdresse;
import noNamespace.TypFichierImpression;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.editique.EditiqueAbstractLegacyHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiquePrefixeHelper;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.editique.ZoneAffranchissementEditique;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.type.TypeDocument;

public class ImpressionDocumentEfactureHelperImpl extends EditiqueAbstractLegacyHelper implements ImpressionDocumentEfactureHelper {
	private static final String VERSION_XSD = "1.0";

	@Override
	public TypeDocumentEditique getTypeDocumentEditique(TypeDocument typeDoc) {
		switch (typeDoc){
		case E_FACTURE_ATTENTE_CONTACT:
			return TypeDocumentEditique.E_FACTURE_ATTENTE_CONTACT;
		case E_FACTURE_ATTENTE_SIGNATURE:
			return TypeDocumentEditique.E_FACTURE_ATTENTE_SIGNATURE;
		default:
			throw new IllegalArgumentException("Type de document non supporté : " + typeDoc);
		}
	}

	@Override
	public FichierImpressionDocument remplitDocumentEfacture(ImpressionDocumentEfactureParams params) throws EditiqueException {
		try {
			final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
			final TypFichierImpression typeFichierImpression = mainDocument.addNewFichierImpression();
			final InfoDocumentDocument1.InfoDocument infoDocument = remplitInfoDocument(params);
			final InfoEnteteDocumentDocument1.InfoEnteteDocument infoEnteteDocument = remplitEnteteDocument(params);

			final TypFichierImpression.Document document = typeFichierImpression.addNewDocument();
			document.setLettresEFacture(remplitSpecifiqueLettresEFactures(params));
			document.setInfoEnteteDocument(infoEnteteDocument);
			document.setInfoDocument(infoDocument);

			InfoArchivageDocument.InfoArchivage infoArchivage = remplitInfoArchivage(params);
			document.setInfoArchivage(infoArchivage);
			typeFichierImpression.setDocumentArray(new TypFichierImpression.Document[]{ document });

			return mainDocument;
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private InfoArchivageDocument.InfoArchivage remplitInfoArchivage(ImpressionDocumentEfactureParams params) {
		return legacyEditiqueHelper.buildInfoArchivage(getTypeDocumentEditique(params.getTypeDocument()), params.getTiers().getNumero(), construitIdArchivageDocument(params),
				RegDateHelper.get(params.getDateTraitement()));
	}

	@Override
	public String construitIdArchivageDocument(ImpressionDocumentEfactureParams params) {
		return String.format(
				"%s%s %s %s",
				RegDateHelper.get(params.getDateTraitement()).year(),
				StringUtils.leftPad("1", 2, '0'),
				StringUtils.rightPad(getLibelleCourt(params.getTypeDocument()), 19, ' '),
				new SimpleDateFormat("MMddHHmmssSSS").format(
						params.getDateTraitement()
				)
		);
	}

	@Override
	public String construitIdDocument(ImpressionDocumentEfactureParams params) {
		return String.format("%s %s", params.getTiers().getNumero(), new SimpleDateFormat("yyyyMMddHHmmssSSS").format(params.getDateTraitement()));
	}

	private String getLibelleCourt(TypeDocument typeDocument) {
		switch (typeDocument){
			case E_FACTURE_ATTENTE_CONTACT:
				return "Efacture contact";
			case E_FACTURE_ATTENTE_SIGNATURE:
				return "Efacture signature";
			default:
				throw new IllegalArgumentException("Type de document non supporté : " + typeDocument);
		}
	}

	private LettresEFactureDocument.LettresEFacture remplitSpecifiqueLettresEFactures(ImpressionDocumentEfactureParams params) {
		final LettresEFactureDocument.LettresEFacture lettresEFacture = LettresEFactureDocument.Factory.newInstance().addNewLettresEFacture();
		lettresEFacture.setDateDemande(RegDateHelper.toIndexString(params.getDateDemande()));
		lettresEFacture.setNumContrib(FormatNumeroHelper.numeroCTBToDisplay(params.getTiers().getNumero()));
		lettresEFacture.setNumAdherEnCours(params.getNoAdherentCourant());
		if (params.getNoAdherentPrecedent() != null && params.getDateDemandePrecedente() != null) {
			final LettresEFactureDocument.LettresEFacture.InscriptionPrecedente precedente = lettresEFacture.addNewInscriptionPrecedente();
			precedente.setDateInscriptionPrecedente(RegDateHelper.toIndexString(params.getDateDemandePrecedente()));
			precedente.setNumAdherPrecedent(params.getNoAdherentPrecedent());
		}
		final String politesse = adresseService.getFormulePolitesse(params.getTiers(), null).getFormuleAppel();
		lettresEFacture.setPolitesse(politesse);
		return lettresEFacture;
	}

	InfoEnteteDocumentDocument1.InfoEnteteDocument remplitEnteteDocument(ImpressionDocumentEfactureParams params) throws EditiqueException {
		InfoEnteteDocumentDocument1.InfoEnteteDocument infoEnteteDocument = InfoEnteteDocumentDocument1.Factory.newInstance().addNewInfoEnteteDocument();

		try {
			infoEnteteDocument.setPrefixe(EditiquePrefixeHelper.buildPrefixeEnteteDocument(getTypeDocumentEditique(params.getTypeDocument())));

			final TypAdresse porteAdresse = legacyEditiqueHelper.remplitPorteAdresse(params.getTiers(), infoEnteteDocument);
			infoEnteteDocument.setPorteAdresse(porteAdresse);

			final InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur expediteur = legacyEditiqueHelper.remplitExpediteurCAT(infoEnteteDocument);
			expediteur.setDateExpedition(DateHelper.dateToIndexString(params.getDateTraitement()));

			infoEnteteDocument.setExpediteur(expediteur);
			InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire destinataire = legacyEditiqueHelper.remplitDestinataire(params.getTiers(), infoEnteteDocument);
			infoEnteteDocument.setDestinataire(destinataire);
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
		return infoEnteteDocument;
	}

	/**
	 * Alimente la partie infoDocument du Document
	 */
	private InfoDocumentDocument1.InfoDocument remplitInfoDocument(ImpressionDocumentEfactureParams params) throws EditiqueException {
		final InfoDocumentDocument1.InfoDocument infoDocument = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
		final String prefixe = EditiquePrefixeHelper.buildPrefixeInfoDocument(getTypeDocumentEditique(params.getTypeDocument()));
		infoDocument.setPrefixe(prefixe);
		infoDocument.setTypDoc("");
		infoDocument.setCodDoc("");
		infoDocument.setVersion(VERSION_XSD);

		final ZoneAffranchissementEditique zoneAffranchissement = legacyEditiqueHelper.remplitAffranchissement(infoDocument, params.getTiers());
		if (zoneAffranchissement == null || zoneAffranchissement == ZoneAffranchissementEditique.INCONNU) {
			infoDocument.setIdEnvoi(Integer.toString(ServiceInfrastructureService.noACI));     // retour à l'ACI pour tous les documents qu'on ne sait pas où envoyer...
		}
		return infoDocument;
	}
}
