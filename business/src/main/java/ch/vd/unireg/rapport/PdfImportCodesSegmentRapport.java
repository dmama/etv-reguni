package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImportCodesSegmentResults;

public class PdfImportCodesSegmentRapport extends PdfRapport {

	public void write(final ImportCodesSegmentResults results, final int nbLignesLuesFichierEntree, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

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

			addTableSimple(2, table -> {
				table.addLigne("Nombre de lignes valides dans le fichier initial :", String.valueOf(nbLignesLuesFichierEntree));
				table.addLigne("Nombre de données uniques inspectées :", String.valueOf(results.getNombreTiersAnalyses()));
				table.addLigne("Nombre de codes modifiés :", String.valueOf(results.getTraites().size()));
				table.addLigne("Nombre de codes laissés en l'état :", String.valueOf(results.getIgnores().size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErreurs().size()));
				table.addLigne("Durée d'exécution :", formatDureeExecution(results));
				table.addLigne("Date de génération :", formatTimestamp(dateGeneration));
			});
		}

		// Cas traités
		{
			final String filename = "codes_modifies.csv";
			final String titre = "Liste des contribuables dont le code de segmentation a été modifié";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = buildContenuTraites(results.getTraites(), status, filename)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Cas ignorés
		{
			final String filename = "ignores.csv";
			final String titre = "Liste des contribuables dont le code de segmentation n'a pas été touché";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = buildContenuIgnores(results.getIgnores(), status, filename)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = buildContenuErreurs(results.getErreurs(), status, filename)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile buildContenuTraites(List<ImportCodesSegmentResults.Traite> traites, StatusManager status, String filename) {
		return CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<ImportCodesSegmentResults.Traite>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA).append("CODE_SEGMENT");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ImportCodesSegmentResults.Traite elt) {
				b.append(elt.noTiers).append(COMMA).append(elt.codeSegment);
				return true;
			}
		});
	}

	private TemporaryFile buildContenuIgnores(List<ImportCodesSegmentResults.Ignore> ignores, StatusManager status, String filename) {
		return CsvHelper.asCsvTemporaryFile(ignores, filename, status, new CsvHelper.FileFiller<ImportCodesSegmentResults.Ignore>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA).append("RAISON");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ImportCodesSegmentResults.Ignore elt) {
				b.append(elt.noTiers).append(COMMA).append(CsvHelper.escapeChars(elt.cause));
				return true;
			}
		});
	}

	private TemporaryFile buildContenuErreurs(List<ImportCodesSegmentResults.Erreur> erreurs, StatusManager status, String filename) {
		return CsvHelper.asCsvTemporaryFile(erreurs, filename, status, new CsvHelper.FileFiller<ImportCodesSegmentResults.Erreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA).append("ERREUR").append(COMMA).append("DETAILS");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ImportCodesSegmentResults.Erreur elt) {
				b.append(elt.noTiers).append(COMMA).append(CsvHelper.escapeChars(elt.type.getDescription())).append(COMMA);
				b.append(CsvHelper.escapeChars(elt.details));
				return true;
			}
		});
	}
}
