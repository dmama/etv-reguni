package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.tiers.rattrapage.origine.RecuperationOriginesNonHabitantsResults;

public class PdfRecuperationOriginesNonHabitantsRapport extends PdfRapport {

	/**
	 * Génère un rapport au format PDF à partir des résultats du job.
	 */
	public void write(final RecuperationOriginesNonHabitantsResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		final PdfRecuperationDonneesAnciensHabitantsRapport document = new PdfRecuperationDonneesAnciensHabitantsRapport();
		final PdfWriter writer = PdfWriter.getInstance(document, os);
		document.open();
		document.addMetaInfo(nom, description);
		document.addEnteteUnireg();

		// Titre
		document.addTitrePrincipal("Rapport d'exécution du job de récupération des données d'origine des non-habitants");

		// Paramètres
		document.addEntete1("Paramètres");
		{
			document.addTableSimple(2, table -> {
				table.addLigne("Nombre de threads :", String.valueOf(results.getNbThreads()));
				table.addLigne("Mode simulation :", String.valueOf(results.isDryRun()));
			});
		}

		// Résultats
		document.addEntete1("Résultats");
		{
			if (results.isInterrupted()) {
				document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						                    + "les valeurs ci-dessous sont donc incomplètes.");
			}

			document.addTableSimple(new float[] {60, 40}, table -> {
				final int sizeIgnores = results.getIgnores().size();
				final int sizeTraites = results.getTraites().size();
				final int sizeErreurs = results.getErreurs().size();
				table.addLigne("Nombre total de personnes physiques analysées :", String.valueOf(sizeErreurs + sizeIgnores + sizeTraites));
				table.addLigne("Nombre de cas ignorés :", String.valueOf(sizeIgnores));
				table.addLigne("Nombre d'erreurs :", String.valueOf(sizeErreurs));
				table.addLigne("Nombre de non-habitants modifiés :", String.valueOf(sizeTraites));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
			});
		}

		// Personnes physiques ignorées
		{
			final String filename = "ignores.csv";
			final String titre = "Liste des cas ignorés";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = getIgnoresCsvFile(results, status, filename)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String titre = "Liste des erreurs rencontrées";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = getErreursCsvFile(results, status, filename)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// non-habitants modifiés
		{
			final String filename = "modifications.csv";
			final String titre = "Liste des non-habitants modifiés";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = getModificationsCsvFile(results, status, filename)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		document.close();

		status.setMessage("Génération du rapport terminée.");
	}

	private static TemporaryFile getModificationsCsvFile(RecuperationOriginesNonHabitantsResults results, StatusManager status, String filename) {
		return CsvHelper.asCsvTemporaryFile(results.getTraites(), filename, status, new CsvHelper.FileFiller<RecuperationOriginesNonHabitantsResults.InfoTraitement>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA);
				b.append("LIEU_ORIGINE").append(COMMA);
				b.append("CANTON_ORIGINE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RecuperationOriginesNonHabitantsResults.InfoTraitement elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(CsvHelper.escapeChars(elt.getLibelle())).append(COMMA);
				b.append(CsvHelper.escapeChars(elt.getSigleCanton()));
				return true;
			}
		});
	}

	private static TemporaryFile getErreursCsvFile(RecuperationOriginesNonHabitantsResults results, StatusManager status, String filename) {
		return CsvHelper.asCsvTemporaryFile(results.getErreurs(), filename, status, new CsvHelper.FileFiller<RecuperationOriginesNonHabitantsResults.InfoErreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA).append("ERREUR");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RecuperationOriginesNonHabitantsResults.InfoErreur elt) {
				b.append(elt.noCtb).append(COMMA).append(CsvHelper.escapeChars(elt.getMessage()));
				return true;
			}
		});
	}

	private static TemporaryFile getIgnoresCsvFile(RecuperationOriginesNonHabitantsResults results, StatusManager status, String filename) {
		return CsvHelper.asCsvTemporaryFile(results.getIgnores(), filename, status, new CsvHelper.FileFiller<RecuperationOriginesNonHabitantsResults.InfoIgnore>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA).append("RAISON");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RecuperationOriginesNonHabitantsResults.InfoIgnore elt) {
				b.append(elt.noCtb).append(COMMA).append(CsvHelper.escapeChars(elt.getMessage()));
				return true;
			}
		});
	}
}