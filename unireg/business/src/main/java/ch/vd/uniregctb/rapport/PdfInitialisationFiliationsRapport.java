package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.jobs.InitialisationFiliationsResults;

public class PdfInitialisationFiliationsRapport extends PdfRapport {

	/**
	 * Génère un rapport au format PDF à partir des résultats de job.
	 */

	public void write(final InitialisationFiliationsResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

			Assert.notNull(status);

			// Création du document PDF
			PdfInitialisationFiliationsRapport document = new PdfInitialisationFiliationsRapport();
			PdfWriter writer = PdfWriter.getInstance(document, os);
			document.open();
			document.addMetaInfo(nom, description);
			document.addEnteteUnireg();

			// Titre
			document.addTitrePrincipal("Rapport d'exécution du job génération des relations de filiation");

			// Paramètres
			document.addEntete1("Paramètres");
			{
				document.addTableSimple(2, new TableSimpleCallback() {
					@Override
					public void fillTable(PdfTableSimple table) throws DocumentException {
						table.addLigne("Nombre de threads:", String.valueOf(results.nbThreads));
					}
				});
			}

			// Résultats
			document.addEntete1("Résultats");
			{
				if (results.interrupted) {
					document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
							+ "les valeurs ci-dessous sont donc incomplètes.");
				}

				document.addTableSimple(2, new TableSimpleCallback() {
					@Override
					public void fillTable(PdfTableSimple table) throws DocumentException {
						table.addLigne("Nombre total de relations générées:", String.valueOf(results.getFiliations().size()));
						table.addLigne("Nombre d'erreurs:", String.valueOf(results.getErreurs().size()));
						table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
						table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
					}
				});
			}

			// Relations créées
			{
				String filename = "filiations_generees.csv";
				String contenu = asCsvFileTraite(results.getFiliations(), filename, status);
				String titre = "Liste des relations de filiation générées";
				String listVide = "(aucune)";
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}

			// Erreurs
			{
				String filename = "erreurs.csv";
				String contenu = asCsvFileErreur(results.getErreurs(), filename, status);
				String titre = "Liste des erreurs rencontrées";
				String listVide = "(aucune)";
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}

			document.close();

			status.setMessage("Génération du rapport terminée.");
	}

	private static String asCsvFileTraite(List<InitialisationFiliationsResults.InfoFiliation> list, String filename, StatusManager status) {
		return CsvHelper.asCsvFile(list, filename, status, new CsvHelper.FileFiller<InitialisationFiliationsResults.InfoFiliation>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB_PARENT").append(CsvHelper.COMMA);
				b.append("NO_CTB_ENFANT").append(CsvHelper.COMMA);
				b.append("DATE_DEBUT").append(CsvHelper.COMMA);
				b.append("DATE_FIN");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, InitialisationFiliationsResults.InfoFiliation elt) {
				b.append(elt.noCtbParent).append(COMMA);
				b.append(elt.noCtbEnfant).append(COMMA);
				b.append(RegDateHelper.dateToDashString(elt.dateDebut)).append(COMMA);
				b.append(RegDateHelper.dateToDashString(elt.dateFin));
				return true;
			}
		});
	}

	private static String asCsvFileErreur(List<InitialisationFiliationsResults.InfoErreur> list, String filename, StatusManager status) {
		return CsvHelper.asCsvFile(list, filename, status, new CsvHelper.FileFiller<InitialisationFiliationsResults.InfoErreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB_ENFANT").append(CsvHelper.COMMA);
				b.append("DETAILS");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, InitialisationFiliationsResults.InfoErreur elt) {
				b.append(elt.noCtbEnfant).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.msg));
				return true;
			}
		});
	}
}
