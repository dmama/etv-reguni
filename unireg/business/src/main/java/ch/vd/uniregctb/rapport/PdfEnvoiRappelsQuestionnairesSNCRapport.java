package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.declaration.snc.EnvoiRappelsQuestionnairesSNCResults;

/**
 * Rapport PDF contenant les résultats du job de génération en masse des rappels des questionnaires SNC.
 */
public class PdfEnvoiRappelsQuestionnairesSNCRapport extends PdfRapport {

	public void write(final EnvoiRappelsQuestionnairesSNCResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job d'envoi en masse des rappels des questionnaires SNC");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
				table.addLigne("Période fiscale :", results.getPeriodeFiscale() == null ? "Toutes" : String.valueOf(results.getPeriodeFiscale()));
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
				table.addLigne("Nombre de questionnaires inspectés :", String.valueOf(results.getNombreRappelsEmis() + results.getErreurs().size() + results.getIgnores().size()));
				table.addLigne("Nombre de rappels envoyés :", String.valueOf(results.getNombreRappelsEmis()));
				table.addLigne("Nombre de cas ignorés :", String.valueOf(results.getIgnores().size()));
				table.addLigne("Nombre de cas en erreur :", String.valueOf(results.getErreurs().size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Rappels envoyés
		{
			final String filename = "rappels_envoyes.csv";
			final String titre = "Liste rappels envoyés";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = traitesAsCsvFile(results.getRappelsEmis(), filename, status)) {
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

	private TemporaryFile traitesAsCsvFile(List<EnvoiRappelsQuestionnairesSNCResults.RappelEmis> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<EnvoiRappelsQuestionnairesSNCResults.RappelEmis>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA);
					b.append("PERIODE_FISCALE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EnvoiRappelsQuestionnairesSNCResults.RappelEmis elt) {
					b.append(elt.noCtb).append(COMMA);
					b.append(elt.pf);
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile ignoresAsCsvFile(List<EnvoiRappelsQuestionnairesSNCResults.RappelIgnore> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<EnvoiRappelsQuestionnairesSNCResults.RappelIgnore>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA);
					b.append("PERIODE_FISCALE").append(COMMA);
					b.append("RAISON").append(COMMA);
					b.append("DETAILS");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EnvoiRappelsQuestionnairesSNCResults.RappelIgnore elt) {
					b.append(elt.noCtb).append(COMMA);
					b.append(elt.pf).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.cause.name())).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.detail));
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile erreursAsCsvFile(List<EnvoiRappelsQuestionnairesSNCResults.ErreurTraitement> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<EnvoiRappelsQuestionnairesSNCResults.ErreurTraitement>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA);
					b.append("PERIODE_FISCALE").append(COMMA);
					b.append("CAUSE").append(COMMA);
					b.append("DETAILS");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EnvoiRappelsQuestionnairesSNCResults.ErreurTraitement elt) {
					b.append(elt.noCtb).append(COMMA);
					b.append(elt.pf).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.cause.name())).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.detail));
					return true;
				}
			});
		}
		return contenu;
	}
}