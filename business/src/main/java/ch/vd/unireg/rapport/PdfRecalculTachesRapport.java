package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.tache.TacheSyncResults;

/**
 * Rapport PDF contenant les résultats du rapprochement des ctb et des propriétaires fonciers.
 */
public class PdfRecalculTachesRapport extends PdfRapport {

	public void write(final TacheSyncResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport de re-calcul des tâches d'envoi et d'annulation de déclaration d'impôt");

		// Paramètres
		addEntete1("Paramètre");
		{
			addTableSimple(2, table -> {
				table.addLigne("Nettoyage seul : ", String.valueOf(results.isCleanupOnly()));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Nombre total d'actions menées :", String.valueOf(results.getActions().size()));
				table.addLigne("Nombre total d'erreurs :", String.valueOf(results.getExceptions().size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Action menées
		{
			final String filename = "actions_menees.csv";
			final String titre = "Liste des actions menées par le job";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = actionsAsCsvFile(results.getActions(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = errorsAsCsvFile(results.getExceptions(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile actionsAsCsvFile(List<TacheSyncResults.ActionInfo> actions, String fileName, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(actions, fileName, status, new CsvHelper.FileFiller<TacheSyncResults.ActionInfo>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("CTB_ID").append(COMMA);
				b.append("ACTION");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, TacheSyncResults.ActionInfo elt) {
				b.append(elt.ctbId).append(COMMA);
				b.append(asCsvField(escapeChars(elt.actionMsg)));
				return true;
			}
		});
	}

	private TemporaryFile errorsAsCsvFile(List<TacheSyncResults.ExceptionInfo> exceptions, String fileName, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(exceptions, fileName, status, new CsvHelper.FileFiller<TacheSyncResults.ExceptionInfo>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("CTB_ID").append(COMMA);
				b.append("EXCEPTION");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, TacheSyncResults.ExceptionInfo elt) {
				b.append(elt.ctbId).append(COMMA);
				b.append(asCsvField(escapeChars(elt.exceptionMsg)));
				return true;
			}
		});
	}
}

