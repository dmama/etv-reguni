package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.declaration.ordinaire.pm.DeterminationDIsPMResults;

/**
 * Rapport PDF contenant les résultats de l'exécution d'un job de détermination des tâches des DIs PM.
 */
public class PdfDeterminationDIsPMRapport extends PdfRapport {

	public void write(final DeterminationDIsPMResults results, final String nom, final String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job de détermination des DIs PM à émettre pour l'année " + results.annee);

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Période fiscale considérée:", String.valueOf(results.annee));
				table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
				table.addLigne("Nombre de threads:", String.valueOf(results.nbThreads));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.interrompu) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Nombre total de contribuables:", String.valueOf(results.nbCtbsTotal));
				table.addLigne("Nombre de contribuables traités:", String.valueOf(results.traites.size()));
				table.addLigne("Nombre de contribuables ignorés:", String.valueOf(results.ignores.size()));
				table.addLigne("Nombre de contribuables en erreur:", String.valueOf(results.erreurs.size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
			});
		}

		// CTBs traités
		{
			final String filename = "contribuables_traites.csv";
			final String titre = "Liste des contribuables traités";
			final String listVide = "(aucun contribuable traité)";
			try (TemporaryFile contenu = traitesAsCsvFile(results.traites, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// CTBs ignorés
		{
			final String filename = "contribuables_ignores.csv";
			final String titre = "Liste des contribuables ignorés";
			final String listVide = "(aucun contribuable ignoré)";
			try (TemporaryFile contenu = asCsvFile(results.ignores, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// CTBs en erreurs
		{
			final String filename = "contribuables_en_erreur.csv";
			final String titre = "Liste des contribuables en erreur";
			final String listVide = "(aucun contribuable en erreur)";
			try (TemporaryFile contenu = asCsvFile(results.erreurs, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile traitesAsCsvFile(List<DeterminationDIsPMResults.Traite> list, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<DeterminationDIsPMResults.Traite>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de l'office d'impôt").append(COMMA);
					b.append("Numéro de contribuable").append(COMMA);
					b.append("Début de la période").append(COMMA);
					b.append("Fin de la période").append(COMMA);
					b.append("Raison").append(COMMA);
					b.append("Type de document").append(COMMA);
					b.append("Type de contribuable");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, DeterminationDIsPMResults.Traite info) {
					b.append(info.officeImpotID).append(COMMA);
					b.append(info.noCtb).append(COMMA);
					b.append(info.dateDebut).append(COMMA);
					b.append(info.dateFin).append(COMMA);
					b.append(CsvHelper.escapeChars(info.raison.description())).append(COMMA);
					b.append(info.typeDocument != null ? info.typeDocument : EMPTY).append(COMMA);
					b.append(info.typeContribuable != null ? info.typeContribuable : EMPTY);
					return true;
				}
			});
		}
		return contenu;
	}
}