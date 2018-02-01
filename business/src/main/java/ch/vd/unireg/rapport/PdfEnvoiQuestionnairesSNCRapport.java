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
import ch.vd.unireg.declaration.snc.EnvoiQuestionnairesSNCEnMasseResults;

/**
 * Rapport PDF contenant les résultats du job de génération en masse des questionnaires SNC.
 */
public class PdfEnvoiQuestionnairesSNCRapport extends PdfRapport {

	public void write(final EnvoiQuestionnairesSNCEnMasseResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job d'envoi en masse des questionnaires SNC " + results.getPeriodeFiscale());

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Période fiscale considérée :", String.valueOf(results.getPeriodeFiscale()));
				table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
				table.addLigne("Nombre maximum d'envois :", results.getNbMaxEnvois() == null ? "Pas de max" : String.valueOf(results.getNbMaxEnvois()));
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
				table.addLigne("Nombre d'entreprises inspectées :", String.valueOf(results.getNombreTiersInspectes()));
				table.addLigne("Nombre de questionnaires envoyés :", String.valueOf(results.getNombreEnvoyes()));
				table.addLigne("Nombre de cas ignorés :", String.valueOf(results.getNombreIgnores()));
				table.addLigne("Nombre de cas en erreur :", String.valueOf(results.getNombreErreurs()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Questionnaires envoyés
		{
			final String filename = "envois.csv";
			final String titre = "Liste des entreprises pour lesquelles un questionnaire SNC a été envoyé";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = traitesAsCsvFile(results.getEnvoyes(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Cas ignorés
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

	private TemporaryFile traitesAsCsvFile(List<EnvoiQuestionnairesSNCEnMasseResults.QuestionnaireEnvoye> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<EnvoiQuestionnairesSNCEnMasseResults.QuestionnaireEnvoye>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de contribuable");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EnvoiQuestionnairesSNCEnMasseResults.QuestionnaireEnvoye elt) {
					b.append(elt.noCtb);
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile ignoresAsCsvFile(List<EnvoiQuestionnairesSNCEnMasseResults.ContribuableIgnore> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<EnvoiQuestionnairesSNCEnMasseResults.ContribuableIgnore>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de contribuable").append(COMMA);
					b.append("Raison");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EnvoiQuestionnairesSNCEnMasseResults.ContribuableIgnore elt) {
					b.append(elt.noCtb).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.cause.name()));
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile erreursAsCsvFile(List<EnvoiQuestionnairesSNCEnMasseResults.TraitementEnErreur> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<EnvoiQuestionnairesSNCEnMasseResults.TraitementEnErreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de contribuable").append(COMMA);
					b.append("Erreur").append(COMMA);
					b.append("Détails");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EnvoiQuestionnairesSNCEnMasseResults.TraitementEnErreur elt) {
					b.append(elt.noCtb).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.cause.name())).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.details));
					return true;
				}
			});
		}
		return contenu;
	}
}