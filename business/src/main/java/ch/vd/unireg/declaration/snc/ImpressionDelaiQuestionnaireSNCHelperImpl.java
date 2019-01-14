package ch.vd.unireg.declaration.snc;

import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.editique.ConstantesEditique;
import ch.vd.unireg.editique.EditiqueAbstractHelperImpl;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiquePrefixeHelper;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.xml.editique.pm.CTypeAffranchissement;
import ch.vd.unireg.xml.editique.pm.CTypeInfoArchivage;
import ch.vd.unireg.xml.editique.pm.CTypeInfoDocument;
import ch.vd.unireg.xml.editique.pm.CTypeInfoEnteteDocument;
import ch.vd.unireg.xml.editique.pm.FichierImpression;
import ch.vd.unireg.xml.editique.pm.STypeZoneAffranchissement;

public class ImpressionDelaiQuestionnaireSNCHelperImpl extends EditiqueAbstractHelperImpl implements ImpressionDelaiQuestionnaireSNCHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpressionDelaiQuestionnaireSNCHelperImpl.class);

	private static final String CODE_DOCUMENT_ACCORD = TypeDocumentEditique.ACCORD_DELAI_QSNC.getCodeDocumentEditique().substring(0, 4);
	private static final String CODE_DOCUMENT_REFUS = TypeDocumentEditique.REFUS_DELAI_QSNC.getCodeDocumentEditique().substring(0, 4);
	private MessageHelper messageHelper;

	@Override
	public TypeDocumentEditique getTypeDocumentEditique(DelaiDeclaration delai) {
		return isDelaiAccorde(delai) ? TypeDocumentEditique.ACCORD_DELAI_QSNC : TypeDocumentEditique.REFUS_DELAI_QSNC;
	}


	@Override
	public FichierImpression.Document buildDocument(DelaiDeclaration delai, String cleArchivageDocument, RegDate dateExpedition) throws EditiqueException {
		try {
			LOGGER.info("construction ");
			final Entreprise snc = (Entreprise) delai.getDeclaration().getTiers();
			final QuestionnaireSNC questionnaire = (QuestionnaireSNC) delai.getDeclaration();

			final CTypeInfoDocument infoDocument = buildInfoDocument(getAdresseEnvoi(snc), delai);
			final CTypeInfoArchivage infoArchivage = buildInfoArchivage(isDelaiAccorde(delai) ? TypeDocumentEditique.ACCORD_DELAI_QSNC : TypeDocumentEditique.REFUS_DELAI_QSNC,
			                                                            cleArchivageDocument, snc.getNumero(), RegDate.get());
			final String titre = String.format("QUESTIONNAIRE SNC/SC %d ",
			                                   questionnaire.getPeriode().getAnnee());
			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocument(snc, dateExpedition, TRAITE_PAR, NOM_SERVICE_EXPEDITEUR, infraService.getACIOIPM(), infraService.getCAT(), titre);

			final FichierImpression.Document document = new FichierImpression.Document();
			document.setInfoDocument(infoDocument);
			document.setInfoArchivage(infoArchivage);
			document.setInfoEnteteDocument(infoEnteteDocument);
			if (isDelaiAccorde(delai)) {
				document.setAccordDelaiSNC(buildDocumentAccordDelaiSNC(delai));
			}
			else {
				document.setRefusDelaiSNC(buildRefusDelaiSNC(delai));
			}
			return document;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}


	private FichierImpression.Document.RefusDelaiSNC buildRefusDelaiSNC(DelaiDeclaration delai) {
		return new FichierImpression.Document.RefusDelaiSNC(XmlUtils.regdate2xmlcal(RegDate.get(delai.getDeclaration().getPeriode().getAnnee())),
		                                                    String.valueOf(delai.getDateDemande().index()));
	}

	private FichierImpression.Document.AccordDelaiSNC buildDocumentAccordDelaiSNC(DelaiDeclaration delai) {
		return new FichierImpression.Document.AccordDelaiSNC(String.valueOf(delai.getDelaiAccordeAu().index()));
	}

	private CTypeInfoDocument buildInfoDocument(AdresseEnvoiDetaillee adresseEnvoi, DelaiDeclaration delai) {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissement(adresseEnvoi,
		                                                                                                      false,
		                                                                                                      ServiceInfrastructureService.noOIPM);

		final STypeZoneAffranchissement zoneAffranchissement = assigneIdEnvoi(infoDoc, (Entreprise) delai.getDeclaration().getTiers(), infosAffranchissement);
		infoDoc.setAffranchissement(new CTypeAffranchissement(zoneAffranchissement, null));
		infoDoc.setVersionXSD(VERSION_XSD);

		infoDoc.setCodDoc(isDelaiAccorde(delai) ? CODE_DOCUMENT_ACCORD : CODE_DOCUMENT_REFUS);
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(isDelaiAccorde(delai) ? TypeDocumentEditique.ACCORD_DELAI_QSNC : TypeDocumentEditique.REFUS_DELAI_QSNC));
		infoDoc.setTypDoc(TYPE_DOCUMENT_CO);

		return infoDoc;
	}

	private boolean isDelaiAccorde(DelaiDeclaration delai) {
		return delai.getEtat() == EtatDelaiDocumentFiscal.ACCORDE;
	}

	@Override
	public String construitCleArchivageDocument(DelaiDeclaration delai) {
		final QuestionnaireSNC questionnaire = (QuestionnaireSNC) delai.getDeclaration();
		final String texte = isDelaiAccorde(delai) ? " Accord delai QSNC" : " Refus delai QSNC";
		return String.format(
				"%s%s %s %s",
				questionnaire.getPeriode().getAnnee().toString(),
				StringUtils.leftPad(questionnaire.getNumero().toString(), 2, '0'),
				StringUtils.rightPad(texte, 19, ' '),
				new SimpleDateFormat("MMddHHmmssSSS").format(
						DateHelper.getCurrentDate()
				)
		);
	}

	@Override
	public String getIdDocument(DelaiDeclaration delai) {
		final String texte = isDelaiAccorde(delai) ? " Accord delai QSNC" : " Refus delai QSNC";
		return String.format(texte + " %09d %d %04d %s",
		                     delai.getDeclaration().getTiers().getNumero(),
		                     delai.getId(),
		                     delai.getDateDemande().year(),
		                     new SimpleDateFormat("MMddHHmmssSSS").format(DateHelper.getCurrentDate()));
	}

	@Override
	public String getDescriptionDocument(DelaiDeclaration delai) {

		final String description;
		switch (delai.getEtat()) {
		case ACCORDE:
			description = messageHelper.getMessage("ajout.delai.qsnc.lettre.accord.delai", RegDateHelper.dateToDisplayString(delai.getDelaiAccordeAu()));
			break;
		case REFUSE:
			description = messageHelper.getMessage("ajout.delai.qsnc.lettre.refus.delai");
			break;
		default:

			throw new IllegalArgumentException("Type de lettre non-supporté, etat du délai  : " + delai.getEtat());
		}
		return messageHelper.getMessage("ajout.delai.qsnc.lettre.description", description, delai.getDateDemande().year(), FormatNumeroHelper.numeroCTBToDisplay(delai.getDeclaration().getTiers().getNumero()));
	}


	public void setMessageHelper(MessageHelper messageHelper) {
		this.messageHelper = messageHelper;
	}
}
