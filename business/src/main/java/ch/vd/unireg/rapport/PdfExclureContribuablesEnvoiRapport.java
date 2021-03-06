package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.tiers.ExclureContribuablesEnvoiResults;

/**
 * Rapport PDF contenant les résultats de l'exécution d'un job de traitement des DIs.
 */
public class PdfExclureContribuablesEnvoiRapport extends PdfRapport {

	/**
	 * Génère un rapport au format PDF à partir des résultats de job.
	 */
	public void write(final ExclureContribuablesEnvoiResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job d'exclusion de contribuables de l'envoi automatique de DIs");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Date de limite d'exclusion:", RegDateHelper.dateToDisplayString(results.dateLimiteExclusion));
				table.addLigne("Contribuables à exclure:", "(voir le fichier contribuables_a_exclure.csv)");
			});
			// ids en entrées
			String filename = "contribuables_a_exclure.csv";
			try (TemporaryFile contenu = ctbIdsAsCsvFile(results.ctbsIds, filename, status)) {
				attacheFichier(writer, contenu.getFullPath(), filename, description, CsvHelper.MIME_TYPE, 500);
			}
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.interrompu) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Nombre total de contribuables traités:", String.valueOf(results.nbCtbsTotal));
				table.addLigne("Nombre de contribuables ignorés:", String.valueOf(results.ctbsIgnores.size()));
				table.addLigne("Nombre de contribuables en erreur:", String.valueOf(results.ctbsEnErrors.size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
			});
		}

		// DIs ignorées
		{
			String filename = "ctbs_ignorees.csv";
			String titre = "Liste des contribuables ignorés";
			String listVide = "(aucun contribuable ignoré)";
			try (TemporaryFile contenu = asCsvFile(results.ctbsIgnores, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// DIs en erreur
		{
			String filename = "ctbs_en_erreur.csv";
			String titre = "Liste des contribuables en erreur";
			String listVide = "(aucun contribuable en erreur)";
			try (TemporaryFile contenu = asCsvFile(results.ctbsEnErrors, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}
}