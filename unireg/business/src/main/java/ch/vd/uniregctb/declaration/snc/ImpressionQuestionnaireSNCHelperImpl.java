package ch.vd.uniregctb.declaration.snc;

import java.text.SimpleDateFormat;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.editique.unireg.CTypeAffranchissement;
import ch.vd.editique.unireg.CTypeInfoArchivage;
import ch.vd.editique.unireg.CTypeInfoDocument;
import ch.vd.editique.unireg.CTypeInfoEnteteDocument;
import ch.vd.editique.unireg.CTypeQuestSNC;
import ch.vd.editique.unireg.FichierImpression;
import ch.vd.editique.unireg.STypeZoneAffranchissement;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.EditiqueAbstractHelperImpl;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiquePrefixeHelper;
import ch.vd.uniregctb.editique.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.GenreImpot;

public class ImpressionQuestionnaireSNCHelperImpl extends EditiqueAbstractHelperImpl implements ImpressionQuestionnaireSNCHelper {

	private static final String CODE_DOCUMENT_QSNC = TypeDocumentEditique.QSNC.getCodeDocumentEditique().substring(0, 4);

	@Override
	public TypeDocumentEditique getTypeDocumentEditique(QuestionnaireSNC questionnaire) {
		return TypeDocumentEditique.QSNC;
	}

	@Override
	public FichierImpression.Document buildDocument(QuestionnaireSNC questionnaire) throws EditiqueException {
		try {
			final Entreprise pm = (Entreprise) questionnaire.getTiers();
			final CTypeInfoDocument infoDocument = buildInfoDocument(getAdresseEnvoi(pm), pm);
			final CTypeInfoArchivage infoArchivage = buildInfoArchivage(TypeDocumentEditique.QSNC, construitCleArchivageDocument(questionnaire), pm.getNumero(), RegDate.get());
			final CTypeInfoEnteteDocument infoEnteteDocument = buildInfoEnteteDocument(pm, RegDate.get(), TRAITE_PAR, NOM_SERVICE_EXPEDITEUR, infraService.getACIOIPM(), infraService.getCAT());
			final CTypeQuestSNC qsnc = buildDocumentQuestionnaire(questionnaire);
			return new FichierImpression.Document(infoDocument, infoArchivage, infoEnteteDocument, null, null, null, null, null, null, null, null, qsnc, null, null, null, null, null);
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private CTypeQuestSNC buildDocumentQuestionnaire(QuestionnaireSNC questionnaire) throws EditiqueException {
		try {
			final ForFiscalPrincipal ffp = getForPrincipalInteressant(questionnaire.getTiers(), questionnaire.getPeriode().getAnnee());
			final String siege = getNomCommuneOuPays(ffp);
			final String numCommune = ffp != null ? String.valueOf(ffp.getNumeroOfsAutoriteFiscale()) : StringUtils.EMPTY;
			final String delaiRetourImprime = RegDateHelper.toIndexString(extractDelaiRetourImprime(questionnaire));
			final String codeRoutage = String.format("%d-%d", ServiceInfrastructureService.noOIPM, 0);    // TODO changer ce 0 en autre chose... mais quoi ?
			return new CTypeQuestSNC(XmlUtils.regdate2xmlcal(RegDate.get(questionnaire.getPeriode().getAnnee())),
			                         buildAdresse(infraService.getACIOIPM()),
			                         delaiRetourImprime,
			                         codeRoutage,
			                         siege,
			                         numCommune,
			                         buildCodeBarre(questionnaire, extractModeleFeuilleDocumentEditique(questionnaire), ServiceInfrastructureService.noOIPM));
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private static ModeleFeuilleDocumentEditique extractModeleFeuilleDocumentEditique(QuestionnaireSNC questionnaire) {
		final ModeleDocument md = questionnaire.getModeleDocument();
		if (md == null) {
			throw new IllegalArgumentException("Le questionnaire n'a pas de modèle de document associé.");
		}

		final Set<ModeleFeuilleDocument> feuilles = md.getModelesFeuilleDocument();
		if (feuilles == null || feuilles.isEmpty()) {
			throw new IllegalArgumentException("Le modèle de document des questionnaires SNC pour la PF " + md.getPeriodeFiscale().getAnnee() + " n'a pas de feuille associée.");
		}

		final ModeleFeuilleDocument feuille = feuilles.iterator().next();
		if (feuille == null) {
			throw new IllegalArgumentException("La feuille trouvée dans le modèle de document des questionnaires SNC pour la PF " + md.getPeriodeFiscale().getAnnee() + " est vide.");
		}

		return new ModeleFeuilleDocumentEditique(feuille, 1);
	}

	@NotNull
	private static RegDate extractDelaiRetourImprime(QuestionnaireSNC questionnaire) throws EditiqueException {
		// si présent, on prend ça
		if (questionnaire.getDelaiRetourImprime() != null) {
			return questionnaire.getDelaiRetourImprime();
		}

		// sinon, on va chercher le premier délai accordé (= le délai initial)
		final RegDate initial = questionnaire.getPremierDelai();
		if (initial == null) {
			throw new EditiqueException("Impossible de déterminer le délai de retour à imprimer sur le questionnaire SNC.");
		}
		return initial;
	}

	@Nullable
	private static ForFiscalPrincipal getForPrincipalInteressant(Tiers tiers, int pf) {
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

		infoDoc.setCodDoc(CODE_DOCUMENT_QSNC);
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(TypeDocumentEditique.QSNC));
		infoDoc.setTypDoc(TYPE_DOCUMENT_DI);

		return infoDoc;
	}

	@Override
	public String getIdDocument(QuestionnaireSNC questionnaire) {
		return String.format("QSNC %09d %02d %04d %s",
		                     questionnaire.getTiers().getNumero(),
		                     questionnaire.getNumero(),
		                     questionnaire.getPeriode().getAnnee(),
		                     new SimpleDateFormat("MMddHHmmssSSS").format(DateHelper.getCurrentDate()));
	}

	private static String construitCleArchivageDocument(QuestionnaireSNC questionnaire) {
		return String.format(
				"%s%s %s %s",
				questionnaire.getPeriode().getAnnee().toString(),
				StringUtils.leftPad(questionnaire.getNumero().toString(), 2, '0'),
				StringUtils.rightPad("Questionnaire SNC", 19, ' '),
				new SimpleDateFormat("MMddHHmmssSSS").format(
						DateHelper.getCurrentDate()
				)
		);
	}
}
