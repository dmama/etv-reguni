package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.foncier.InitialisationIFoncResults;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.CollectivitePubliqueRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitHabitationRF;
import ch.vd.unireg.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.DroitVirtuelHeriteRF;
import ch.vd.unireg.registrefoncier.DroitVirtuelTransitifRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.UsufruitRF;

/**
 * Rapport PDF contenant le rapport de l'extraction des données nécessaires à l'initialisation de la taxation IFONC
 */
public class PdfInitialisationIFoncRapport extends PdfRapport {

	public void write(final InitialisationIFoncResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du batch d'extraction des données nécessaires à l'initialisation de la taxation IFONC.");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Date de référence :", RegDateHelper.dateToDisplayString(results.dateReference));
				table.addLigne("Nombre de threads :", String.valueOf(results.nbThreads));
				if (results.ofsCommune != null) {
					table.addLigne("Commune ciblée :", String.valueOf(results.ofsCommune));
				}
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (status.isInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[]{.6f, .4f}, table -> {
				table.addLigne("Nombre d'immeubles inspectés :", String.valueOf(results.getNbImmeublesInspectes()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErreurs().size()));
				table.addLigne("Nombre de lignes extraites :", String.valueOf(results.getLignesExtraites().size()));
				table.addLigne("Nombre d'immeubles ignorés :", String.valueOf(results.getImmeublesIgnores().size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results.endTime - results.startTime));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Extraction des immeubles avec droits valides et sans droit du tout
		{
			final String filename = "extraction.csv";
			final String titre = "Extraction";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = processedAsCsvFile(results.getLignesExtraites(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Cas ignorés
		{
			final String filename = "ignores.csv";
			final String titre = "Immeubles ignorés";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = ignoredAsCsvFile(results.getImmeublesIgnores(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Cas en erreur
		{
			final String filename = "erreurs.csv";
			final String titre = " Liste des erreurs";
			final String listeVide = "(aucune)";
			try (TemporaryFile contenu = errorsAsCsvFile(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private static TemporaryFile processedAsCsvFile(List<InitialisationIFoncResults.InfoExtraction> extraits, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(extraits, filename, status, new CsvHelper.FileFiller<InitialisationIFoncResults.InfoExtraction>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("CTB_ID").append(COMMA);
				b.append("NOM").append(COMMA);
				b.append("PRENOM").append(COMMA);
				b.append("RAISON_SOCIALE").append(COMMA);
				b.append("DATE_NAISSANCE").append(COMMA);
				b.append("TYPE_AYANT_DROIT").append(COMMA);
				b.append("COMMUNAUTE_ID").append(COMMA);
				b.append("TYPE_DROIT").append(COMMA);
				b.append("REGIME_PROPRIETE").append(COMMA);
				b.append("MOTIF_DEBUT").append(COMMA);
				b.append("DATE_DEBUT").append(COMMA);
				b.append("MOTIF_FIN").append(COMMA);
				b.append("DATE_FIN").append(COMMA);
				b.append("PART_PROPRIETE_NUMERATEUR").append(COMMA);
				b.append("PART_PROPRIETE_DENOMINATEUR").append(COMMA);
				b.append("IMMEUBLE_ID").append(COMMA);
				b.append("EGRID").append(COMMA);
				b.append("DATE_RADIATION").append(COMMA);
				b.append("NO_PARCELLE").append(COMMA);
				b.append("INDEX1").append(COMMA);
				b.append("INDEX2").append(COMMA);
				b.append("INDEX3").append(COMMA);
				b.append("OFS_COMMUNE").append(COMMA);
				b.append("NOM_COMMUNE").append(COMMA);
				b.append("AYANT_DROIT_IDRF").append(COMMA);
				b.append("AYANT_DROIT_NORF").append(COMMA);
				b.append("IMMEUBLE_BENEFICIAIRE_ID").append(COMMA);
				b.append("ESTIMATION_FISCALE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, InitialisationIFoncResults.InfoExtraction extrait) {
				b.append(Optional.ofNullable(extrait.idContribuable).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.identificationRF).map(identif -> identif.nom).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.identificationRF).map(identif -> identif.prenom).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.identificationRF).map(identif -> identif.raisonSociale).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.identificationRF).map(identif -> identif.dateNaissance).orElse(null)).append(COMMA);
				b.append(Optional.ofNullable(extrait.classAyantDroit).map(AYANT_DROIT_DISPLAY_STRINGS::get).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.idCommunaute).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.classDroit).map(DROIT_DISPLAY_STRING::get).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.regime).map(Enum::name).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.motifDebut).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(extrait.dateDebut).append(COMMA);
				b.append(Optional.ofNullable(extrait.motifFin).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(extrait.dateFin).append(COMMA);
				b.append(Optional.ofNullable(extrait.part).map(Fraction::getNumerateur).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.part).map(Fraction::getDenominateur).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.infoImmeuble).map(info -> info.idImmeuble).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.infoImmeuble).map(info -> info.egrid).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.infoImmeuble).map(info -> info.dateRadiationImmeuble).map(RegDateHelper::dateToDashString).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.infoImmeuble).map(info -> info.noParcelle).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.infoImmeuble).map(info -> info.index1).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.infoImmeuble).map(info -> info.index2).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.infoImmeuble).map(info -> info.index3).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.infoImmeuble).map(info -> info.noOfsCommune).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.infoImmeuble).map(info -> info.nomCommune).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.idRFAyantDroit).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.noRFAyantDroit).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.idImmeubleBeneficiaire).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(extrait.infoImmeuble).map(info -> info.montantEstimationFiscale).map(String::valueOf).orElse(StringUtils.EMPTY));
				return true;
			}
		});
	}

	private static TemporaryFile ignoredAsCsvFile(List<InitialisationIFoncResults.ImmeubleIgnore> ignores, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(ignores, filename, status, new CsvHelper.FileFiller<InitialisationIFoncResults.ImmeubleIgnore>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("IMMEUBLE_ID").append(COMMA);
				b.append("EGRID").append(COMMA);
				b.append("DATE_RADIATION").append(COMMA);
				b.append("NO_PARCELLE").append(COMMA);
				b.append("INDEX1").append(COMMA);
				b.append("INDEX2").append(COMMA);
				b.append("INDEX3").append(COMMA);
				b.append("OFS_COMMUNE").append(COMMA);
				b.append("NOM_COMMUNE").append(COMMA);
				b.append("ESTIMATION_FISCALE").append(COMMA);
				b.append("RAISON");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, InitialisationIFoncResults.ImmeubleIgnore ignore) {
				b.append(Optional.ofNullable(ignore.infoImmeuble.idImmeuble).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(ignore.infoImmeuble.egrid).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(ignore.infoImmeuble.dateRadiationImmeuble).map(RegDateHelper::dateToDashString).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(ignore.infoImmeuble.noParcelle).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(ignore.infoImmeuble.index1).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(ignore.infoImmeuble.index2).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(ignore.infoImmeuble.index3).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(ignore.infoImmeuble.noOfsCommune).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(ignore.infoImmeuble.nomCommune).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(ignore.infoImmeuble.montantEstimationFiscale).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(CsvHelper.escapeChars(ignore.raison));
				return true;
			}
		});
	}

	private static final Map<Class<? extends AyantDroitRF>, String> AYANT_DROIT_DISPLAY_STRINGS = buildTypeAyantDroitDisplayStrings();

	private static Map<Class<? extends AyantDroitRF>, String> buildTypeAyantDroitDisplayStrings() {
		final Map<Class<? extends AyantDroitRF>, String> map = new HashMap<>();
		map.put(CommunauteRF.class, "Communauté");
		map.put(PersonnePhysiqueRF.class, "Personne physique");
		map.put(PersonneMoraleRF.class, "Personne morale");
		map.put(CollectivitePubliqueRF.class, "Collectivité publique");
		map.put(ImmeubleBeneficiaireRF.class, "Immeuble bénéficiaire");
		return map;
	}

	private static final Map<Class<? extends DroitRF>, String> DROIT_DISPLAY_STRING = buildTypeDroitDisplayStrings();

	private static Map<Class<? extends DroitRF>, String> buildTypeDroitDisplayStrings() {
		final Map<Class<? extends DroitRF>, String> map = new HashMap<>();
		map.put(DroitHabitationRF.class, "Droit d'habitation");
		map.put(DroitProprieteCommunauteRF.class, "Propriété");
		map.put(DroitProprietePersonneMoraleRF.class, "Propriété");
		map.put(DroitProprietePersonnePhysiqueRF.class, "Propriété");
		map.put(DroitProprieteImmeubleRF.class, "Propriété");
		map.put(UsufruitRF.class, "Usufruit");
		map.put(DroitVirtuelHeriteRF.class, "Droit virtuel hérité");
		map.put(DroitVirtuelTransitifRF.class, "Droit virtuel transitif");
		return map;
	}

	private TemporaryFile errorsAsCsvFile(List<InitialisationIFoncResults.ErreurImmeuble> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<InitialisationIFoncResults.ErreurImmeuble>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("IMMEUBLE_ID").append(COMMA);
					b.append("MESSAGE").append(COMMA);
					b.append("STACK_TRACE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, InitialisationIFoncResults.ErreurImmeuble elt) {
					b.append(elt.idImmeuble).append(COMMA);
					b.append(CsvHelper.asCsvField(elt.message)).append(COMMA);
					b.append(CsvHelper.asCsvField(elt.stackTrace));
					return true;
				}
			});
		}
		return contenu;
	}
}