package ch.vd.unireg.declaration.snc;

import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.editique.ConstantesEditique;
import ch.vd.unireg.editique.EditiqueAbstractHelperImpl;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiquePrefixeHelper;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.xml.editique.pm.CTypeAffranchissement;
import ch.vd.unireg.xml.editique.pm.CTypeInfoArchivage;
import ch.vd.unireg.xml.editique.pm.CTypeInfoDocument;
import ch.vd.unireg.xml.editique.pm.CTypeInfoEnteteDocument;
import ch.vd.unireg.xml.editique.pm.CTypeQuestSNCRappel;
import ch.vd.unireg.xml.editique.pm.FichierImpression;
import ch.vd.unireg.xml.editique.pm.STypeZoneAffranchissement;

public class ImpressionRappelQuestionnaireSNCHelperImpl extends EditiqueAbstractHelperImpl implements ImpressionRappelQuestionnaireSNCHelper {

	private static final String CODE_DOCUMENT_RQSNC = TypeDocumentEditique.RAPPEL_SQNC.getCodeDocumentEditique().substring(0, 4);
	private MessageHelper messageHelper;

	@Override
	public TypeDocumentEditique getTypeDocumentEditique(QuestionnaireSNC questionnaire) {
		return TypeDocumentEditique.RAPPEL_SQNC;
	}

	@Override
	public FichierImpression.Document buildDocument(QuestionnaireSNC questionnaire, RegDate dateRappel, RegDate dateEnvoiCourrier) throws EditiqueException {
		try {
			final ContribuableImpositionPersonnesMorales pm = questionnaire.getTiers();
			final CTypeInfoDocument infoDocument = buildInfoDocument(getAdresseEnvoi(pm), pm);
			final CTypeInfoArchivage infoArchivage = buildInfoArchivage(TypeDocumentEditique.RAPPEL_SQNC, construitCleArchivageDocument(questionnaire), pm.getNumero(), dateRappel);

			final String titre = messageHelper.getMessage("editique.titre.lettre.rappel.qsnc", questionnaire.getPeriode().getAnnee());
			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocument(pm, dateEnvoiCourrier, TRAITE_PAR, NOM_SERVICE_EXPEDITEUR, infraService.getACIOIPM(), infraService.getCAT(), titre);
			final CTypeQuestSNCRappel rappel = buildDocumentRappel(questionnaire);

			final FichierImpression.Document document = new FichierImpression.Document();
			document.setInfoDocument(infoDocument);
			document.setInfoArchivage(infoArchivage);
			document.setInfoEnteteDocument(infoEnteteDocument);
			document.setQuestSNCRappel(rappel);
			return document;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private CTypeQuestSNCRappel buildDocumentRappel(QuestionnaireSNC questionnaire) {
		final CTypeQuestSNCRappel questSNCRappel = new CTypeQuestSNCRappel();
		final PeriodeFiscale periode = questionnaire.getPeriode();
		final Integer annee = periode.getAnnee();
		questSNCRappel.setPeriodeFiscale(XmlUtils.regdate2xmlcal(RegDate.get(annee)));
		final ForFiscalPrincipal ffp = getForPrincipalInteressant(questionnaire.getTiers(), annee);
		final String siege = getNomCommuneOuPays(ffp);
		questSNCRappel.setSiege(siege);
		final String raisonSociale = getNomRaisonSociale(questionnaire.getTiers());
		questSNCRappel.setNomSociete(raisonSociale);
		if (periode.isShowCodeControleRappelQuestionnaireSNC() && StringUtils.isNotBlank(questionnaire.getCodeControle())) {
			questSNCRappel.setCodeControleNIP(questionnaire.getCodeControle());
		}

		return questSNCRappel;
	}

	@Nullable
	private static ForFiscalPrincipal getForPrincipalInteressant(ContribuableImpositionPersonnesMorales tiers, int pf) {
		final DateRange periode = new DateRangeHelper.Range(RegDate.get(pf, 1, 1), RegDate.get(pf, 12, 31));
		for (ForFiscalPrincipal ffp : CollectionsUtils.revertedOrder(tiers.getForsFiscauxPrincipauxActifsSorted())) {
			if (ffp.getGenreImpot() == GenreImpot.REVENU_FORTUNE && DateRangeHelper.intersect(periode, ffp)) {
				return ffp;
			}
			else if (ffp.getDateFin() != null && ffp.getDateFin().year() < pf) {
				// plus aucune chance, autant s'en aller tout de suite...
				break;
			}
		}
		return null;
	}

	private static CTypeInfoDocument buildInfoDocument(AdresseEnvoiDetaillee adresseEnvoi, ContribuableImpositionPersonnesMorales pm) {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissement(adresseEnvoi,
		                                                                                                      false,
		                                                                                                      ServiceInfrastructureService.noOIPM);
		final STypeZoneAffranchissement zoneAffranchissement = assigneIdEnvoi(infoDoc, pm, infosAffranchissement);
		infoDoc.setAffranchissement(new CTypeAffranchissement(zoneAffranchissement, null));
		infoDoc.setVersionXSD(VERSION_XSD);

		infoDoc.setCodDoc(CODE_DOCUMENT_RQSNC);
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(TypeDocumentEditique.RAPPEL_SQNC));
		infoDoc.setTypDoc(TYPE_DOCUMENT_CO);

		return infoDoc;
	}

	@Override
	public String getIdDocument(QuestionnaireSNC questionnaire) {
		return String.format("RQSNC %09d %04d%02d %s",
		                     questionnaire.getTiers().getNumero(),
		                     questionnaire.getPeriode().getAnnee(),
		                     questionnaire.getNumero(),
		                     new SimpleDateFormat("MMddHHmmssSSS").format(DateHelper.getCurrentDate()));
	}

	@Override
	public String construitCleArchivageDocument(QuestionnaireSNC questionnaire) {
		return String.format("%04d%02d %s %s",
		                     questionnaire.getPeriode().getAnnee(),
		                     questionnaire.getNumero(),
		                     StringUtils.rightPad("Rappel QSNC", 19, ' '),
		                     new SimpleDateFormat("MMddHHmmssSSS").format(questionnaire.getLogCreationDate()));
	}

	public void setMessageHelper(MessageHelper messageHelper) {
		this.messageHelper = messageHelper;
	}
}
