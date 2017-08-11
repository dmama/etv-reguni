package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.parentes.CalculParentesResults;
import ch.vd.uniregctb.parentes.ParenteUpdateInfo;

public class PdfCalculParentesRapport extends PdfRapport {

	/**
	 * Génère un rapport au format PDF à partir des résultats de job.
	 */

	public void write(final CalculParentesResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

			Assert.notNull(status);

			// Création du document PDF
			PdfCalculParentesRapport document = new PdfCalculParentesRapport();
			PdfWriter writer = PdfWriter.getInstance(document, os);
			document.open();
			document.addMetaInfo(nom, description);
			document.addEnteteUnireg();

			// Titre
			document.addTitrePrincipal("Rapport d'exécution du job calcul des relations de parenté");

			// Paramètres
			document.addEntete1("Paramètres");
			{
				document.addTableSimple(2, table -> {
					table.addLigne("Nombre de threads :", String.valueOf(results.nbThreads));
					table.addLigne("Mode :", String.valueOf(results.mode));
				});
			}

			// Résultats
			document.addEntete1("Résultats");
			{
				if (results.interrupted) {
					document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
							+ "les valeurs ci-dessous sont donc incomplètes.");
				}

				document.addTableSimple(2, table -> {
					table.addLigne("Nombre total de relations mises à jour :", String.valueOf(results.getUpdates().size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErreurs().size()));
					table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
				});
			}

			// Relations créées
			{
				String filename = "parentes.csv";
				String titre = "Liste des relations de parenté mises à jour";
				String listVide = "(aucune)";
				try (TemporaryFile contenu = asCsvFileTraite(results.getUpdates(), filename, status)) {
					document.addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}

			// Erreurs
			{
				String filename = "erreurs.csv";
				String titre = "Liste des erreurs rencontrées";
				String listVide = "(aucune)";
				try (TemporaryFile contenu = asCsvFileErreur(results.getErreurs(), filename, status)) {
					document.addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}

			document.close();

			status.setMessage("Génération du rapport terminée.");
	}

	private static TemporaryFile asCsvFileTraite(List<ParenteUpdateInfo> list, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<ParenteUpdateInfo>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("ACTION").append(COMMA);
				b.append("NO_CTB_PARENT").append(COMMA);
				b.append("NO_CTB_ENFANT").append(COMMA);
				b.append("DATE_DEBUT").append(COMMA);
				b.append("DATE_FIN");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ParenteUpdateInfo elt) {
				b.append(elt.action).append(COMMA);
				b.append(elt.noCtbParent).append(COMMA);
				b.append(elt.noCtbEnfant).append(COMMA);
				b.append(RegDateHelper.dateToDashString(elt.dateDebut)).append(COMMA);
				b.append(RegDateHelper.dateToDashString(elt.dateFin));
				return true;
			}
		});
	}

	private static TemporaryFile asCsvFileErreur(List<CalculParentesResults.InfoErreur> list, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<CalculParentesResults.InfoErreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB_ENFANT").append(COMMA);
				b.append("DETAILS");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, CalculParentesResults.InfoErreur elt) {
				b.append(elt.noCtbEnfant).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.msg));
				return true;
			}
		});
	}
}
