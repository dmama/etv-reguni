package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.indexer.jobs.DatabaseIndexationResults;
import ch.vd.unireg.indexer.tiers.TimeLog;

/**
 * Rapport PDF d'exécution du batch d'indexation des tiers de la base de données
 */
public class PdfDatabaseIndexationRapport extends PdfRapport {

	public void write(final DatabaseIndexationResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job d'indexation des tiers.");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Mode :", results.getMode().name());
				table.addLigne("Types de tiers :", String.join(",\n", results.getTypesTiers().stream().map(Enum::name).collect(Collectors.toList())));
				table.addLigne("Nombre de threads :", String.valueOf(results.getNbThreads()));
				table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\nles valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Tiers indexés :", String.valueOf(results.getIndexes().size()));
				table.addLigne("Tiers supprimés :", String.valueOf(results.getSupprimes().size()));

				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErrors().size()));
				table.addLigne("Durée d'exécution :", formatDureeExecution(results));
				table.addLigne("Date de génération :", formatTimestamp(dateGeneration));
			});
		}

		final TimeLog.Stats stats = results.getTimeLog().createStats();
		if (stats.isDispo()) {
			addEntete1("Statistiques");
			{
				addTableSimple(2, table -> {
					table.addLigne("Temps 'exec' threads indexation :", stats.indexerExecTime + " ms");
					table.addLigne("Temps 'cpu' threads indexation  :", stats.indexerCpuTime + " ms");
					table.addLigne("Temps 'wait' threads indexation :", stats.timeWait + " ms" + " (" + stats.percentWait + "%)");
					table.addLigne(" - service infrastructure       :", (stats.timeWaitInfra == 0 ? "<indisponible>" : stats.timeWaitInfra + " ms" + " (" + stats.percentWaitInfra + "%)"));
					table.addLigne(" - service civil                :", (stats.timeWaitCivil == 0 ? "<indisponible>" : stats.timeWaitCivil + " ms" + " (" + stats.percentWaitCivil + "%)"));
					table.addLigne(" - service entreprise           :", (stats.timeWaitEntreprise == 0 ? "<indisponible>" : stats.timeWaitEntreprise + " ms" + " (" + stats.percentWaitEntreprise + "%)"));
					table.addLigne(" - indexer                      :", (stats.timeWaitIndex == 0 ? "<indisponible>" : stats.timeWaitIndex + " ms" + " (" + stats.percentWaitIndex + "%)"));
					table.addLigne(" - autre (scheduler, jdbc, ...) :", stats.timeWaitAutres + " ms" + " (" + stats.percentWaitAutres + "%)");
				});
			}
		}

		// Tiers indexés
		{
			String filename = "indexes.csv";
			String titre = "Liste des tiers indexés";
			String listVide = "(aucune tiers indexé)";
			try (TemporaryFile contenu = idsAsCsvFile(results.getIndexes(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Tiers supprimés
		{
			String filename = "supprimes.csv";
			String titre = "Liste des tiers supprimés";
			String listVide = "(aucune tiers supprimé)";
			try (TemporaryFile contenu = idsAsCsvFile(results.getSupprimes(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Erreurs
		{
			String filename = "erreurs.csv";
			String titre = "Liste des tiers en erreur";
			String listVide = "(aucune tiers en erreur)";
			try (TemporaryFile contenu = erreursAsCsvFile(results.getErrors(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile idsAsCsvFile(List<Long> ids, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		final int size = ids.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(ids, filename, status, new CsvHelper.FileFiller<Long>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("TIERS_ID");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, Long id) {
					b.append(id);
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile erreursAsCsvFile(List<DatabaseIndexationResults.Error> errors, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		final int size = errors.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(errors, filename, status, new CsvHelper.FileFiller<DatabaseIndexationResults.Error>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("TIERS_ID").append(COMMA).append("MESSAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, DatabaseIndexationResults.Error erreur) {
					b.append(erreur.getId()).append(COMMA);
					b.append(escapeChars(erreur.getMessage()));
					return true;
				}
			});
		}
		return contenu;
	}
}