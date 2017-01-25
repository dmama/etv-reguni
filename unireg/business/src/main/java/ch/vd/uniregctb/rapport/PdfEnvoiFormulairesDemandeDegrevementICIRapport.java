package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.foncier.EnvoiFormulairesDemandeDegrevementICIResults;

/**
 * Rapport PDF contenant les résultats du job de migration des demandes de dégrèvement de SIMPA-PM.
 */
public class PdfEnvoiFormulairesDemandeDegrevementICIRapport extends PdfRapport {

	public void write(final EnvoiFormulairesDemandeDegrevementICIResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de l'envoi en masse des formulaires de demande de dégrèvement ICI.");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Nombre de threads :", String.valueOf(results.nbThreads));
				table.addLigne("Nombre maximal d'envois :", Optional.ofNullable(results.nbMaxEnvois).map(String::valueOf).orElse(StringUtils.EMPTY));
				table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.dateTraitement));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.wasInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[]{.6f, .4f}, table -> {
				table.addLigne("Nombre de droits de propriété inspectés :", String.valueOf(results.getNbDroitsInspectes()));
				table.addLigne("Nombre de droits ignorés :", String.valueOf(results.getNbDroitsIgnores()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErreurs().size()));
				table.addLigne("Nombre de formulaires envoyés :", String.valueOf(results.getEnvois().size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération :", formatTimestamp(dateGeneration));
			});
		}

		// formulaires envoyés
		{
			final String filename = "formulaires_envoyes.csv";
			final String titre = "Formulaires envoyés";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = envoyesAsCsvFile(results.getEnvois(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// droits ignorés
		{
			final String filename = "droits_immeubles_ignores.csv";
			final String titre = "Droits/immeubles ignorés";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = ignoresAsCsvFile(results.getIgnores(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// erreurs
		{
			final String filename = "erreurs.csv";
			final String titre = "Erreurs";
			final String listeVide = "(aucune)";
			try (TemporaryFile contenu = erreursAsCsvFile(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private static TemporaryFile envoyesAsCsvFile(List<EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementEnvoyee> envois, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!envois.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(envois, filename, status, new CsvHelper.FileFiller<EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementEnvoyee>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA);
					b.append("ID_IMMEUBLE").append(COMMA);
					b.append("PF").append(COMMA);
					b.append("NOM_COMMUNE").append(COMMA);
					b.append("NO_OFS_COMMUNE").append(COMMA);
					b.append("NO_PARCELLE").append(COMMA);
					b.append("INDEX1").append(COMMA);
					b.append("INDEX2").append(COMMA);
					b.append("INDEX3");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementEnvoyee elt) {
					b.append(elt.noContribuable).append(COMMA);
					b.append(elt.idImmeuble).append(COMMA);
					b.append(elt.periodeFiscale).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.nomCommune)).append(COMMA);
					b.append(elt.noOfsCommune).append(COMMA);
					b.append(elt.noParcelle).append(COMMA);
					b.append(Optional.ofNullable(elt.index1).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
					b.append(Optional.ofNullable(elt.index2).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
					b.append(Optional.ofNullable(elt.index3).map(String::valueOf).orElse(StringUtils.EMPTY));
					return true;
				}
			});
		}
		return contenu;
	}

	private static TemporaryFile ignoresAsCsvFile(List<EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee> ignores, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!ignores.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(ignores, filename, status, new CsvHelper.FileFiller<EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA);
					b.append("ID_IMMEUBLE").append(COMMA);
					b.append("NOM_COMMUNE").append(COMMA);
					b.append("NO_OFS_COMMUNE").append(COMMA);
					b.append("NO_PARCELLE").append(COMMA);
					b.append("INDEX1").append(COMMA);
					b.append("INDEX2").append(COMMA);
					b.append("INDEX3").append(COMMA);
					b.append("RAISON").append(COMMA);
					b.append("MESSAGE_COMPLEMENTAIRE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee elt) {
					b.append(elt.noContribuable).append(COMMA);
					b.append(elt.idImmeuble).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.nomCommune)).append(COMMA);
					b.append(elt.noOfsCommune).append(COMMA);
					b.append(elt.noParcelle).append(COMMA);
					b.append(Optional.ofNullable(elt.index1).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
					b.append(Optional.ofNullable(elt.index2).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
					b.append(Optional.ofNullable(elt.index3).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
					b.append(elt.raison).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.messageAdditionnel));
					return true;
				}
			});
		}
		return contenu;
	}

	private static TemporaryFile erreursAsCsvFile(List<EnvoiFormulairesDemandeDegrevementICIResults.Erreur> erreurs, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!erreurs.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(erreurs, filename, status, new CsvHelper.FileFiller<EnvoiFormulairesDemandeDegrevementICIResults.Erreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA);
					b.append("ID_IMMEUBLE").append(COMMA);
					b.append("NOM_COMMUNE").append(COMMA);
					b.append("NO_OFS_COMMUNE").append(COMMA);
					b.append("NO_PARCELLE").append(COMMA);
					b.append("INDEX1").append(COMMA);
					b.append("INDEX2").append(COMMA);
					b.append("INDEX3").append(COMMA);
					b.append("ERREUR");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EnvoiFormulairesDemandeDegrevementICIResults.Erreur elt) {
					b.append(elt.noContribuable).append(COMMA);
					b.append(elt.idImmeuble).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.nomCommune)).append(COMMA);
					b.append(elt.noOfsCommune).append(COMMA);
					b.append(elt.noParcelle).append(COMMA);
					b.append(Optional.ofNullable(elt.index1).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
					b.append(Optional.ofNullable(elt.index2).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
					b.append(Optional.ofNullable(elt.index3).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
					b.append(CsvHelper.asCsvField(elt.msg));
					return true;
				}
			});
		}
		return contenu;
	}
}