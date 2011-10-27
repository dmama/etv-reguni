package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ordinaire.ImportCodesSegmentResults;

public class PdfImportCodesSegmentRapport extends PdfRapport {

	public void write(final ImportCodesSegmentResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job d'import des codes de segmentation des déclarations d'impôt");

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\nles valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de contribuables inspectés :", String.valueOf(results.getNombreTiersAnalyses()));
					table.addLigne("Nombre de codes modifiés :", String.valueOf(results.getTraites().size()));
					table.addLigne("Nombre de codes laissés en l'état :", String.valueOf(results.getIgnores().size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErreurs().size()));
					table.addLigne("Durée d'exécution :", formatDureeExecution(results));
					table.addLigne("Date de génération :", formatTimestamp(dateGeneration));
				}
			});
		}

		// Cas traités
		{
			final String filename = "codes_modifies.csv";
			final String contenu = buildContenuTraites(results.getTraites(), status, filename);
			final String titre = "Liste des contribuables dont le code de segmentation a été modifié";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, results.getTraites().size(), titre, listVide, filename, contenu);
		}

		// Cas ignorés
		{
			final String filename = "ignores.csv";
			final String contenu = buildContenuIgnores(results.getIgnores(), status, filename);
			final String titre = "Liste des contribuables dont le code de segmentation n'a pas été touché";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, results.getIgnores().size(), titre, listVide, filename, contenu);
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String contenu = buildContenuErreurs(results.getErreurs(), status, filename);
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, results.getErreurs().size(), titre, listVide, filename, contenu);
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private String buildContenuTraites(List<ImportCodesSegmentResults.Traite> traites, StatusManager status, String filename) {
		return CsvHelper.asCsvFile(traites, filename, status, 25, new CsvHelper.Filler<ImportCodesSegmentResults.Traite>() {
			@Override
			public void fillHeader(StringBuilder b) {
				b.append("NO_CTB").append(COMMA).append("CODE_SEGMENT");
			}

			@Override
			public void fillLine(StringBuilder b, ImportCodesSegmentResults.Traite elt) {
				b.append(elt.noTiers).append(COMMA).append(elt.codeSegment);
			}
		});
	}

	private String buildContenuIgnores(List<ImportCodesSegmentResults.Ignore> ignores, StatusManager status, String filename) {
		return CsvHelper.asCsvFile(ignores, filename, status, 100, new CsvHelper.Filler<ImportCodesSegmentResults.Ignore>() {
			@Override
			public void fillHeader(StringBuilder b) {
				b.append("NO_CTB").append(COMMA).append("RAISON");
			}

			@Override
			public void fillLine(StringBuilder b, ImportCodesSegmentResults.Ignore elt) {
				b.append(elt.noTiers).append(COMMA).append(CsvHelper.escapeChars(elt.cause));
			}
		});
	}

	private String buildContenuErreurs(List<ImportCodesSegmentResults.Erreur> erreurs, StatusManager status, String filename) {
		return CsvHelper.asCsvFile(erreurs, filename, status, 100, new CsvHelper.Filler<ImportCodesSegmentResults.Erreur>() {
			@Override
			public void fillHeader(StringBuilder b) {
				b.append("NO_CTB").append(COMMA).append("ERREUR").append(COMMA).append("DETAILS");
			}

			@Override
			public void fillLine(StringBuilder b, ImportCodesSegmentResults.Erreur elt) {
				b.append(elt.noTiers).append(COMMA).append(CsvHelper.escapeChars(elt.type.getDescription())).append(COMMA);
				b.append(CsvHelper.escapeChars(elt.details));
			}
		});
	}
}
