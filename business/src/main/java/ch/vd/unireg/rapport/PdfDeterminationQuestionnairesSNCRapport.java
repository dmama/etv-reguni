package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.declaration.snc.DeterminationQuestionnairesSNCResults;

/**
 * Rapport PDF contenant les résultats du job de génération en masse des tâches d'envoi des questionnaires SNC.
 */
public class PdfDeterminationQuestionnairesSNCRapport extends PdfRapport {

	public void write(final DeterminationQuestionnairesSNCResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job de détermination des questionnaires SNC à émettre pour l'année " + results.periodeFiscale);

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Période fiscale considérée:", String.valueOf(results.periodeFiscale));
				table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
				table.addLigne("Nombre de threads:", String.valueOf(results.nbThreads));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.wasInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Nombre d'entreprises inspectées :", String.valueOf(results.getNbContribuablesInspectes()));
				table.addLigne("Nombre d'actions menées :", String.valueOf(results.getTraites().size()));
				table.addLigne("Nombre de cas ignorés :", String.valueOf(results.getIgnores().size()));
				table.addLigne("Nombre de cas en erreur :", String.valueOf(results.getErreurs().size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Actions menées
		{
			final String filename = "taches.csv";
			final String titre = "Liste des tâches créées/annulées";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = traitesAsCsvFile(results.getTraites(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Cas ignorés (for IRF existant, mais pas sur la bonne PF..., tâche déjà là...)
		{
			final String filename = "ignores.csv";
			final String titre = "Liste des cas ignorés";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = ignoresAsCsvFile(results.getIgnores(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Cas en erreur
		{
			final String filename = "erreurs.csv";
			final String titre = " Liste des cas en erreur";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = erreursAsCsvFile(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile traitesAsCsvFile(List<DeterminationQuestionnairesSNCResults.Traite> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<DeterminationQuestionnairesSNCResults.Traite>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de contribuable").append(COMMA);
					b.append("Raison sociale").append(COMMA);
					b.append("Action").append(COMMA);
					b.append("Détails");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, DeterminationQuestionnairesSNCResults.Traite elt) {
					b.append(elt.noCtb).append(COMMA);
					b.append(elt.nomCtb).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.getDescriptionRaison())).append(COMMA);
					b.append(elt.details);
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile ignoresAsCsvFile(List<DeterminationQuestionnairesSNCResults.Ignore> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<DeterminationQuestionnairesSNCResults.Ignore>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de contribuable").append(COMMA);
					b.append("Raison sociale").append(COMMA);
					b.append("Raison");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, DeterminationQuestionnairesSNCResults.Ignore elt) {
					b.append(elt.noCtb).append(COMMA);
					b.append(elt.nomCtb).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.getDescriptionRaison()));
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile erreursAsCsvFile(List<DeterminationQuestionnairesSNCResults.Erreur> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<DeterminationQuestionnairesSNCResults.Erreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de contribuable").append(COMMA);
					b.append("Raison sociale").append(COMMA);
					b.append("Erreur").append(COMMA);
					b.append("Détails");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, DeterminationQuestionnairesSNCResults.Erreur elt) {
					b.append(elt.noCtb).append(COMMA);
					b.append(elt.nomCtb).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.getDescriptionRaison())).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.details));
					return true;
				}
			});
		}
		return contenu;
	}
}