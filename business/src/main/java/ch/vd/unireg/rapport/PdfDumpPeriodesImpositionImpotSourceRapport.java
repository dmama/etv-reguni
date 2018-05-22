package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.metier.piis.DumpPeriodesImpositionImpotSourceResults;

public class PdfDumpPeriodesImpositionImpotSourceRapport extends PdfRapport {

	/**
	 * Génère un rapport au format PDF à partir des résultats du job.
	 */
	public void write(final DumpPeriodesImpositionImpotSourceResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		final PdfDumpPeriodesImpositionImpotSourceRapport document = new PdfDumpPeriodesImpositionImpotSourceRapport();
		final PdfWriter writer = PdfWriter.getInstance(document, os);
		document.open();
		document.addMetaInfo(nom, description);
		document.addEnteteUnireg();

		// Titre
		document.addTitrePrincipal("Rapport d'exécution du job d'export des périodes d'imposition IS");

		// Paramètres
		document.addEntete1("Paramètres");
		{
			document.addTableSimple(2, table -> {
				table.addLigne("Nombre de threads :", String.valueOf(results.getNbThreads()));
			});
		}

		// Résultats
		document.addEntete1("Résultats");
		{
			if (results.isInterrupted()) {
				document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						                    + "les valeurs ci-dessous sont donc incomplètes.");
			}

			document.addTableSimple(new float[]{60, 40}, table -> {
				table.addLigne("Nombre total de personnes physiques analysées :", String.valueOf(results.getNbPersonnesPhysiquesAnalysees()));
				table.addLigne("Nombre de personnes physiques ignorées :", String.valueOf(results.getIgnores().size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErrors().size()));
				table.addLigne("Nombre de période d'imposition IS trouvées :", String.valueOf(results.getInfos().size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
			});
		}

		// Personnes physiques ignorées
		{
			final String filename = "ignores.csv";
			final String titre = "Liste des personnes physiques ignorées";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = getIgnoresAsCsvFile(results, status, filename)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String titre = "Liste des erreurs rencontrées";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = getErreursAsCsvFile(results, status, filename)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// périodes d'imposition IS calculées
		{
			final String filename = "piis.csv";
			final String titre = "Liste des périodes d'imposition IS calculées";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = getPeriodesAsCsv(results, status, filename)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		document.close();

		status.setMessage("Génération du rapport terminée.");
	}

	private static TemporaryFile getPeriodesAsCsv(DumpPeriodesImpositionImpotSourceResults results, StatusManager status, String filename) {
		return CsvHelper.asCsvTemporaryFile(results.getInfos(), filename, status, new CsvHelper.FileFiller<DumpPeriodesImpositionImpotSourceResults.Info>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA);
				b.append("PF").append(COMMA);
				b.append("TYPE").append(COMMA);
				b.append("DATE_DEBUT").append(COMMA);
				b.append("DATE_FIN").append(COMMA);
				b.append("TYPE_AUTORITE_FISCALE").append(COMMA);
				b.append("NO_OFS").append(COMMA);
				b.append("LOCALISATION");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, DumpPeriodesImpositionImpotSourceResults.Info elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(elt.dateDebut.year()).append(COMMA);
				b.append(elt.type).append(COMMA);
				b.append(elt.dateDebut).append(COMMA);
				b.append(elt.dateFin).append(COMMA);
				b.append(elt.typeAutoriteFiscale != null ? elt.typeAutoriteFiscale.name() : EMPTY).append(COMMA);
				b.append(elt.noOfs != null ? Integer.toString(elt.noOfs) : EMPTY).append(COMMA);
				b.append(elt.localisation);
				return true;
			}
		});
	}

	private static TemporaryFile getErreursAsCsvFile(DumpPeriodesImpositionImpotSourceResults results, StatusManager status, String filename) {
		return CsvHelper.asCsvTemporaryFile(results.getErrors(), filename, status, new CsvHelper.FileFiller<DumpPeriodesImpositionImpotSourceResults.Error>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA).append("ERREUR");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, DumpPeriodesImpositionImpotSourceResults.Error elt) {
				b.append(elt.noCtb).append(COMMA).append(CsvHelper.escapeChars(elt.message));
				return true;
			}
		});
	}

	private static TemporaryFile getIgnoresAsCsvFile(DumpPeriodesImpositionImpotSourceResults results, StatusManager status, String filename) {
		return CsvHelper.asCsvTemporaryFile(results.getIgnores(), filename, status, new CsvHelper.FileFiller<DumpPeriodesImpositionImpotSourceResults.Ignore>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA).append("RAISON");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, DumpPeriodesImpositionImpotSourceResults.Ignore elt) {
				b.append(elt.noCtb).append(COMMA).append(CsvHelper.escapeChars(elt.cause.msg));
				return true;
			}
		});
	}


}
