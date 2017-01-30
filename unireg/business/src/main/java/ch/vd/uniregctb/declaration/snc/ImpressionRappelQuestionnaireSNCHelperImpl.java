package ch.vd.uniregctb.declaration.snc;

import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.editique.unireg.CTypeAffranchissement;
import ch.vd.editique.unireg.CTypeInfoArchivage;
import ch.vd.editique.unireg.CTypeInfoDocument;
import ch.vd.editique.unireg.CTypeInfoEnteteDocument;
import ch.vd.editique.unireg.CTypeQuestSNCRappel;
import ch.vd.editique.unireg.FichierImpression;
import ch.vd.editique.unireg.STypeZoneAffranchissement;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.EditiqueAbstractHelperImpl;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiquePrefixeHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.GenreImpot;

public class ImpressionRappelQuestionnaireSNCHelperImpl extends EditiqueAbstractHelperImpl implements ImpressionRappelQuestionnaireSNCHelper {

	private static final String CODE_DOCUMENT_RQSNC = TypeDocumentEditique.RAPPEL_SQNC.getCodeDocumentEditique().substring(0, 4);

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

			final String titre = String.format("INVITATION À DÉPOSER LE QUESTIONNAIRE %d SNC/SC - RAPPEL", questionnaire.getPeriode().getAnnee());
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
		final Integer annee = questionnaire.getPeriode().getAnnee();
		final ForFiscalPrincipal ffp = getForPrincipalInteressant(questionnaire.getTiers(), annee);
		final String siege = getNomCommuneOuPays(ffp);
		final String raisonSociale = getNomRaisonSociale(questionnaire.getTiers());
		return new CTypeQuestSNCRappel(XmlUtils.regdate2xmlcal(RegDate.get(annee)), raisonSociale, siege);
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

		final Pair<STypeZoneAffranchissement, String> infosAffranchissement = getInformationsAffranchissement(adresseEnvoi, false, ServiceInfrastructureService.noOIPM);
		assigneIdEnvoi(infoDoc, pm, infosAffranchissement);
		infoDoc.setAffranchissement(new CTypeAffranchissement(infosAffranchissement.getLeft(), null));
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
}
