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
import ch.vd.uniregctb.metier.PassageNouveauxRentiersSourciersEnMixteResults;

public class PdfPassageNouveauxRentiersSourciersEnMixteRapport  extends PdfRapport {

	/**
	 * Génère un rapport au format PDF à partir des résultats de job.
	 */

	public void write(final PassageNouveauxRentiersSourciersEnMixteResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

			Assert.notNull(status);

			// Création du document PDF
			PdfPassageNouveauxRentiersSourciersEnMixteRapport  document = new PdfPassageNouveauxRentiersSourciersEnMixteRapport();
			PdfWriter writer = PdfWriter.getInstance(document, os);
			document.open();
			document.addMetaInfo(nom, description);
			document.addEnteteUnireg();

			// Titre
			document.addTitrePrincipal("Rapport d'exécution du job de passage des nouveaux rentiers sourciers en mixte 1");

			// Paramètres
			document.addEntete1("Paramètres");
			{
				document.addTableSimple(2, table -> {
					table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
				});
			}

			// Résultats
			document.addEntete1("Résultats");
			{
				if (results.interrompu) {
					document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
							+ "les valeurs ci-dessous sont donc incomplètes.");
				}

				document.addTableSimple(2, table -> {
					table.addLigne("Nombre total de sourciers candidats:", String.valueOf(results.getNbSourciersTotal()));
					table.addLigne("Nombre de sourciers convertis (Total de fors Mixte 1 ouverts):", String.valueOf(results.sourciersConvertis.size()));
					table.addLigne("Nombre de sourciers convertis via leur conjoint:", String.valueOf(results.nbSourciersConjointsIgnores));
					table.addLigne("Nombre de sourciers non-convertis car trop jeunes :", String.valueOf(results.nbSourciersTropJeunes));
					table.addLigne("Nombre de sourciers non-convertis car hors-Suisse :", String.valueOf(results.nbSourciersHorsSuisse));
					table.addLigne("Nombre de sourciers en erreur:", String.valueOf(results.sourciersEnErreurs.size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
				});
			}

			// Habitants traités
			{
				String filename = "sourciers_convertis.csv";
				String titre = "Liste des sourciers convertis";
				String listVide = "(aucun sourcier converti)";
				try (TemporaryFile contenu = asCsvFileTraite(results.sourciersConvertis, filename, status)) {
					document.addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}

			// Habitants en erreurs
			{
				String filename = "sourciers_en_erreur.csv";
				String titre = "Liste des habitants en erreur";
				String listVide = "(aucun habitant en erreur)";
				try (TemporaryFile contenu = asCsvFileErreur(results.sourciersEnErreurs, filename, status)) {
					document.addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}

			document.close();

			status.setMessage("Génération du rapport terminée.");
	}

	private static TemporaryFile asCsvFileTraite(List<PassageNouveauxRentiersSourciersEnMixteResults.Traite> list, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<PassageNouveauxRentiersSourciersEnMixteResults.Traite>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA);
				b.append("DATE_OUVERTURE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, PassageNouveauxRentiersSourciersEnMixteResults.Traite elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(elt.dateOuverture);
				return true;
			}
		});
	}

	private static TemporaryFile asCsvFileErreur(List<PassageNouveauxRentiersSourciersEnMixteResults.Erreur> list, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<PassageNouveauxRentiersSourciersEnMixteResults.Erreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(CsvHelper.COMMA);
				b.append("RAISON").append(CsvHelper.COMMA);
				b.append("DETAILS");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, PassageNouveauxRentiersSourciersEnMixteResults.Erreur elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(elt.raison.description()).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.details));
				return true;
			}
		});
	}
}
