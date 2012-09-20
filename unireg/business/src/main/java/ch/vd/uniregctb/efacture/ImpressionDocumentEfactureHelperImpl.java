package ch.vd.uniregctb.efacture;

import java.text.SimpleDateFormat;

import noNamespace.FichierImpressionDocument;
import noNamespace.InfoArchivageDocument;
import noNamespace.InfoDocumentDocument1;
import noNamespace.InfoEnteteDocumentDocument1;
import noNamespace.LettresEFactureDocument;
import noNamespace.TypAdresse;
import noNamespace.TypFichierImpression;
import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.editique.impl.EditiqueHelperImpl;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeDocument;

public class ImpressionDocumentEfactureHelperImpl extends EditiqueAbstractHelper implements ImpressionDocumentEfactureHelper {
	private static final String VERSION_XSD = "1.0";
	private EditiqueHelperImpl editiqueHelper;
	private AdresseService adresseService;

	@Override
	public String construitIdDocument(Integer annee, Integer numeroDoc, Tiers tiers) {
		return null;
	}

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
		return editiqueHelper.buildInfoArchivage(getTypeDocumentEditique(params.getTypeDocument()), params.getTiers().getNumero(), construitIdArchivageDocument(params), RegDate.get(params.getDateTraitement()));
	}

	@Override
	public String construitIdArchivageDocument(ImpressionDocumentEfactureParams params) {
		return String.format(
				"%s%s %s %s",
				RegDate.get(params.getDateTraitement()).year(),
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
		final String politesse = adresseService.getFormulePolitesse(params.getTiers()).formuleAppel();
		lettresEFacture.setPolitesse(politesse);
		return lettresEFacture;
	}

	private InfoEnteteDocumentDocument1.InfoEnteteDocument remplitEnteteDocument(ImpressionDocumentEfactureParams params) throws EditiqueException {
		InfoEnteteDocumentDocument1.InfoEnteteDocument infoEnteteDocument = InfoEnteteDocumentDocument1.Factory.newInstance().addNewInfoEnteteDocument();

		try {
			infoEnteteDocument.setPrefixe(buildPrefixeEnteteDocument(getTypeDocumentEditique(params.getTypeDocument())));

			final TypAdresse porteAdresse = editiqueHelper.remplitPorteAdresse(params.getTiers(), infoEnteteDocument);
			infoEnteteDocument.setPorteAdresse(porteAdresse);

			final InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur expediteur = editiqueHelper.remplitExpediteurCAT(infoEnteteDocument);
			expediteur.setDateExpedition(DateHelper.dateToIndexString(params.getDateTraitement()));

			infoEnteteDocument.setExpediteur(expediteur);
			InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire destinataire = editiqueHelper.remplitDestinataire(params.getTiers(), infoEnteteDocument);
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
		final String prefixe = buildPrefixeInfoDocument(getTypeDocumentEditique(params.getTypeDocument()));
		infoDocument.setPrefixe(prefixe);
		infoDocument.setTypDoc("");
		infoDocument.setCodDoc("");
		infoDocument.setVersion(VERSION_XSD);
		editiqueHelper.remplitAffranchissement(infoDocument, params.getTiers());
		return infoDocument;
	}

	public void setEditiqueHelper(EditiqueHelperImpl editiqueHelper) {
		this.editiqueHelper = editiqueHelper;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}
}
