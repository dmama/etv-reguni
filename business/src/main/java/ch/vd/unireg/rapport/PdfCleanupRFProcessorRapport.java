package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.registrefoncier.importcleanup.CleanupRFProcessorResults;

/**
 * Rapport PDF contenant le rapport du cleanup des données du RF.
 */
public class PdfCleanupRFProcessorRapport extends PdfRapport {

	public void write(final CleanupRFProcessorResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du batch de nettoyage des données d'import du RF.");

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[]{.6f, .4f}, table -> {
				table.addLigne("Nombre d'imports traités :", String.valueOf(results.getProcessed().size()));
				table.addLigne("Nombre d'imports ignorés :", String.valueOf(results.getIgnored().size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getNbErreurs()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results.endTime - results.startTime));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Imports traités
		{
			String filename = "imports_traites.csv";
			String titre = "Liste des imports traités";
			String listVide = "(aucun import traité)";
			try (TemporaryFile contenu = processedAsCsvFile(results.getProcessed(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Imports ignorés
		{
			String filename = "imports_ignores.csv";
			String titre = "Liste des imports ignorés";
			String listVide = "(aucun import ignoré)";
			try (TemporaryFile contenu = ignoredAsCsvFile(results.getIgnored(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Cas en erreur
		{
			final String filename = "erreurs.csv";
			final String titre = " Liste des erreurs";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = errorsAsCsvFile(results.getErrors(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private static TemporaryFile processedAsCsvFile(List<CleanupRFProcessorResults.Processed> traites, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<CleanupRFProcessorResults.Processed>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("IMPORT_ID").append(COMMA);
				b.append("DATE_VALEUR").append(COMMA);
				b.append("TYPE").append(COMMA);
				b.append("NOMBRE_MUTATIONS");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, CleanupRFProcessorResults.Processed elt) {
				b.append(elt.getImportId()).append(COMMA);
				b.append(RegDateHelper.dateToDisplayString(elt.getDateValeur())).append(COMMA);
				b.append(elt.getType()).append(COMMA);
				b.append(elt.getMutCount());
				return true;
			}
		});
	}

	private static TemporaryFile ignoredAsCsvFile(List<CleanupRFProcessorResults.Ignored> traites, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<CleanupRFProcessorResults.Ignored>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("IMPORT_ID").append(COMMA);
				b.append("DATE_VALEUR").append(COMMA);
				b.append("TYPE").append(COMMA);
				b.append("RAISON");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, CleanupRFProcessorResults.Ignored elt) {
				b.append(elt.getImportId()).append(COMMA);
				b.append(RegDateHelper.dateToDisplayString(elt.getDateValeur())).append(COMMA);
				b.append(elt.getType()).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.getReason().getDescription()));
				return true;
			}
		});
	}

	private TemporaryFile errorsAsCsvFile(List<CleanupRFProcessorResults.Error> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<CleanupRFProcessorResults.Error>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("IMPORT_ID").append(COMMA);
					b.append("MESSAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, CleanupRFProcessorResults.Error elt) {
					b.append(elt.getImportId()).append(COMMA);
					b.append(CsvHelper.asCsvField(elt.getMessage()));
					return true;
				}
			});
		}
		return contenu;
	}
}