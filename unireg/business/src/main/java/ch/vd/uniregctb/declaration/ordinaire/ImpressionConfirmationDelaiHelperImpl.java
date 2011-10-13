package ch.vd.uniregctb.declaration.ordinaire;

import java.text.SimpleDateFormat;

import noNamespace.ConfirmationDelaiDocument;
import noNamespace.ConfirmationDelaiDocument.ConfirmationDelai;
import noNamespace.FichierImpressionDocument;
import noNamespace.InfoArchivageDocument;
import noNamespace.InfoDocumentDocument1;
import noNamespace.InfoDocumentDocument1.InfoDocument;
import noNamespace.InfoEnteteDocumentDocument1;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.TypAdresse;
import noNamespace.TypFichierImpression;
import noNamespace.TypFichierImpression.Document;
import noNamespace.TypPeriode;
import noNamespace.TypPeriode.Entete;
import noNamespace.TypPeriode.Entete.ImpCcn;
import noNamespace.TypPeriode.Entete.Tit;
import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.editique.impl.EditiqueServiceImpl;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class ImpressionConfirmationDelaiHelperImpl implements ImpressionConfirmationDelaiHelper {

	private static final String VERSION_XSD = "1.0";

	/**
	 * Le type de document à transmettre au service d'archivage
	 */
	public static final String TYPE_DOCUMENT_CONFIRMATION_DELAI = "387";

	private EditiqueHelper editiqueHelper;
	private AdresseService adresseService;

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.CONFIRMATION_DELAI;
	}

	@Override
	public FichierImpressionDocument remplitConfirmationDelai(ImpressionConfirmationDelaiHelperParams params) throws EditiqueException {
		try {
			final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
			TypFichierImpression typeFichierImpression = mainDocument.addNewFichierImpression();
			InfoDocument infoDocument = remplitInfoDocument(params);
			InfoArchivageDocument.InfoArchivage infoArchivage = remplitInfoArchivage(params);
			InfoEnteteDocument infoEnteteDocument;
			infoEnteteDocument = remplitEnteteDocument(params);
			Document document = typeFichierImpression.addNewDocument();
			document.setConfirmationDelai(remplitSpecifiqueConfirmationDelai(params));
			document.setInfoEnteteDocument(infoEnteteDocument);
			document.setInfoDocument(infoDocument);
			document.setInfoArchivage(infoArchivage);
			typeFichierImpression.setDocumentArray(new Document[]{document});
			return mainDocument;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}

	}

	private ConfirmationDelai remplitSpecifiqueConfirmationDelai(ImpressionConfirmationDelaiHelperParams params) throws EditiqueException {

		ConfirmationDelai confirmationDelai = ConfirmationDelaiDocument.Factory.newInstance().addNewConfirmationDelai();
		confirmationDelai.setDateAccord(RegDateHelper.toIndexString(params.getDateAccord()));
		TypPeriode periode = confirmationDelai.addNewPeriode();
		final String prefixe = getTypeDocumentEditique().getCodeDocumentEditique();
		periode.setPrefixe(prefixe + "PERIO");
		periode.setOrigDuplicat("ORG");
		periode.setHorsSuisse("");
		periode.setHorsCanton("");
		periode.setAnneeFiscale(params.getDi().getPeriode().getAnnee().toString());
		periode.setDateDecompte(RegDateHelper.toIndexString(params.getDi().getEtatDeclarationActif(TypeEtatDeclaration.EMISE).getDateObtention()));
		periode.setDatDerCalculAc("");
		Entete entete = periode.addNewEntete();
		Tit tit = entete.addNewTit();
		tit.setPrefixe(prefixe + "TITIM");
		tit.setLibTit("Impôt cantonal et communal / Impôt fédéral direct");
		ImpCcn impCcn = entete.addNewImpCcn();
		impCcn.setPrefixe(prefixe + "IMPCC");
		impCcn.setLibImpCcn(String.format("Délai pour le dépôt de la déclaration d'impôt %d", params.getDi().getPeriode().getAnnee()));
		final String formuleAppel = adresseService.getFormulePolitesse(params.getDi().getTiers()).formuleAppel();
		confirmationDelai.setCivil(formuleAppel);
		confirmationDelai.setOFS(editiqueHelper.getCommune(params.getDi()));
		return confirmationDelai;
	}

	private InfoEnteteDocument remplitEnteteDocument(ImpressionConfirmationDelaiHelperParams params) throws EditiqueException {
		InfoEnteteDocument infoEnteteDocument = InfoEnteteDocumentDocument1.Factory.newInstance().addNewInfoEnteteDocument();

		try {
			infoEnteteDocument.setPrefixe(getTypeDocumentEditique().getCodeDocumentEditique() + "HAUT1");

			final TypAdresse porteAdresse = editiqueHelper.remplitPorteAdresse(params.getDi().getTiers(), infoEnteteDocument);
			infoEnteteDocument.setPorteAdresse(porteAdresse);

			final Expediteur expediteur = editiqueHelper.remplitExpediteurCAT(infoEnteteDocument);
			expediteur.setDateExpedition(RegDateHelper.toIndexString(RegDate.get()));
			expediteur.setTraitePar(params.getTraitePar());

			// ici c'est un peu tordu : on veut afficher le numéro de téléphone du CAT dans l'entête mais
			// le numéro de téléphone du collaborateur dans le cadre (affaire traitée par), là où est
			// normalement prévue l'adresse e-mail du collaborateur
			if (!StringUtils.isBlank(params.getNoTelephone())) {
				expediteur.setAdrMes(params.getNoTelephone());
			}

			infoEnteteDocument.setExpediteur(expediteur);
			Destinataire destinataire = editiqueHelper.remplitDestinataire(params.getDi().getTiers(), infoEnteteDocument);
			infoEnteteDocument.setDestinataire(destinataire);
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
		return infoEnteteDocument;
	}

	private InfoDocument remplitInfoDocument(ImpressionConfirmationDelaiHelperParams params) throws EditiqueException {
		final InfoDocument infoDocument = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
		final String prefixe = getTypeDocumentEditique().getCodeDocumentEditique() + "DOCUM";
		infoDocument.setPrefixe(prefixe);
		infoDocument.setTypDoc("CD");
		infoDocument.setCodDoc("CONF_DEL");
		infoDocument.setVersion(VERSION_XSD);
		infoDocument.setLogo("CANT");
		infoDocument.setPopulations("PP");
		infoDocument.setIdEnvoi("");
		return infoDocument;
	}

	private InfoArchivageDocument.InfoArchivage remplitInfoArchivage(ImpressionConfirmationDelaiHelperParams params) {
		InfoArchivageDocument.InfoArchivage infoArchivage = InfoArchivageDocument.Factory.newInstance().addNewInfoArchivage();
		infoArchivage.setPrefixe(getTypeDocumentEditique().getCodeDocumentEditique() + "FOLDE");
		infoArchivage.setNomApplication("FOLDERS");
		infoArchivage.setTypDossier(EditiqueServiceImpl.TYPE_DOSSIER_UNIREG);
		String numeroCTB = FormatNumeroHelper.numeroCTBToDisplay(params.getDi().getTiers().getNumero());
		infoArchivage.setNomDossier(numeroCTB);
		infoArchivage.setTypDocument(TYPE_DOCUMENT_CONFIRMATION_DELAI);
		String idDocument = construitIdArchivageDocument(params);
		infoArchivage.setIdDocument(idDocument);
		infoArchivage.setDatTravail(String.valueOf(params.getDateAccord().index()));
		return infoArchivage;
	}

	public String construitIdArchivageDocument(ImpressionConfirmationDelaiHelperParams params) {

		final String idDelai = createStringIdDelai(params);
		return String.format(
				"%s %s %s",
				idDelai,
				StringUtils.rightPad("Confirmation Delai", 19, ' '),
				new SimpleDateFormat("MMddHHmmssSSS").format(
						params.getLogCreationDateDelai()
				)
		);
	}

	protected String createStringIdDelai(ImpressionConfirmationDelaiHelperParams params) {
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
	public String construitIdDocument(DelaiDeclaration delai) {
		final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) delai.getDeclaration();
		final String principal = AuthenticationHelper.getCurrentPrincipal();
		final long ts = (System.nanoTime() % 1000000000L) / 1000L;      // fraction de seconde en microsecondes
		return String.format("%d %02d %09d %d_%s_%d", di.getPeriode().getAnnee(), di.getNumero(), di.getTiers().getNumero(), delai.getId(), principal, ts);
	}

	public void setEditiqueHelper(EditiqueHelper editiqueHelper) {
		this.editiqueHelper = editiqueHelper;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

}
