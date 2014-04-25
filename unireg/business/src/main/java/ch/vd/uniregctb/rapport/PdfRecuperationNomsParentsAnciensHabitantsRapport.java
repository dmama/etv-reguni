package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.tiers.rattrapage.nomsparents.RecuperationNomsParentsAnciensHabitantsResults;

public class PdfRecuperationNomsParentsAnciensHabitantsRapport extends PdfRapport {

	/**
	 * Génère un rapport au format PDF à partir des résultats du job.
	 */
	public void write(final RecuperationNomsParentsAnciensHabitantsResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		final PdfRecuperationNomsParentsAnciensHabitantsRapport document = new PdfRecuperationNomsParentsAnciensHabitantsRapport();
		final PdfWriter writer = PdfWriter.getInstance(document, os);
		document.open();
		document.addMetaInfo(nom, description);
		document.addEnteteUnireg();

		// Titre
		document.addTitrePrincipal("Rapport d'exécution du job de récupération des noms/prénoms des anciens habitants d'après les données civiles connues");

		// Paramètres
		document.addEntete1("Paramètres");
		{
			document.addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de threads :", String.valueOf(results.nbThreads));
					table.addLigne("Ecrasement autorisé :", String.valueOf(results.forceEcrasement));
				}
			});
		}

		// Résultats
		document.addEntete1("Résultats");
		{
			if (results.isInterrupted()) {
				document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						                    + "les valeurs ci-dessous sont donc incomplètes.");
			}

			document.addTableSimple(new float[] {60, 40}, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					final int sizeIgnores = results.getIgnores().size();
					final int sizeTraites = results.getTraites().size();
					final int sizeErreurs = results.getErreurs().size();
					table.addLigne("Nombre total de personnes physiques analysées :", String.valueOf(sizeErreurs + sizeIgnores + sizeTraites));
					table.addLigne("Nombre de cas ignorés :", String.valueOf(sizeIgnores));
					table.addLigne("Nombre d'erreurs :", String.valueOf(sizeErreurs));
					table.addLigne("Nombre d'anciens habitants modifiés :", String.valueOf(sizeTraites));
					table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
				}
			});
		}

		// Personnes physiques ignorées
		{
			final String filename = "ignores.csv";
			final String contenu = CsvHelper.asCsvFile(results.getIgnores(), filename, status, new CsvHelper.FileFiller<RecuperationNomsParentsAnciensHabitantsResults.InfoIgnore>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA).append("RAISON");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, RecuperationNomsParentsAnciensHabitantsResults.InfoIgnore elt) {
					b.append(elt.noCtb).append(COMMA).append(CsvHelper.escapeChars(elt.getMessage()));
					return true;
				}
			});
			final String titre = "Liste des cas ignorés";
			final String listVide = "(aucune)";
			document.addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String contenu = CsvHelper.asCsvFile(results.getErreurs(), filename, status, new CsvHelper.FileFiller<RecuperationNomsParentsAnciensHabitantsResults.InfoErreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA).append("ERREUR");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, RecuperationNomsParentsAnciensHabitantsResults.InfoErreur elt) {
					b.append(elt.noCtb).append(COMMA).append(CsvHelper.escapeChars(elt.getMessage()));
					return true;
				}
			});
			final String titre = "Liste des erreurs rencontrées";
			final String listVide = "(aucune)";
			document.addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		// non-habitants modifiés
		{
			final String filename = "modifications.csv";
			final String contenu = CsvHelper.asCsvFile(results.getTraites(), filename, status, new CsvHelper.FileFiller<RecuperationNomsParentsAnciensHabitantsResults.InfoTraite>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA);
					b.append("MAJ_MERE").append(COMMA);
					b.append("MAJ_PERE").append(COMMA);
					b.append("PRENOMS_MERE").append(COMMA);
					b.append("NOM_MERE").append(COMMA);
					b.append("PRENOMS_PERE").append(COMMA);
					b.append("NOM_PERE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, RecuperationNomsParentsAnciensHabitantsResults.InfoTraite elt) {
					b.append(elt.noCtb).append(COMMA);
					b.append(elt.majMere).append(COMMA);
					b.append(elt.majPere).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.prenomsMere)).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.nomMere)).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.prenomsPere)).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.nomPere));
					return true;
				}
			});
			final String titre = "Liste des non-habitants modifiés";
			final String listVide = "(aucune)";
			document.addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		document.close();

		status.setMessage("Génération du rapport terminée.");
	}


}
