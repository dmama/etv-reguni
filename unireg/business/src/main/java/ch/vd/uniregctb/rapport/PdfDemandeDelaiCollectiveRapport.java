package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.declaration.ordinaire.common.DemandeDelaiCollectiveResults;

/**
 * Rapport PDF contenant les résultats de l'exécution d'un job de traitement des DIs.
 */
public class PdfDemandeDelaiCollectiveRapport extends PdfRapport {

	public void write(final DemandeDelaiCollectiveResults results, String nom, String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du traitement d'une demande de délais collective");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Période fiscale:", String.valueOf(results.annee));
				table.addLigne("Date de délai:", RegDateHelper.dateToDisplayString(results.dateDelai));
				table.addLigne("Contribuables à traiter:", "(voir le fichier contribuables_a_traiter.csv)");
				table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
			});
			// ids en entrées
			String filename = "contribuables_a_traiter.csv";
			try (TemporaryFile contenu = ctbIdsAsCsvFile(results.ctbsIds, filename, status)) {
				attacheFichier(writer, contenu.getFullPath(), filename, "Contribuables à traiter", CsvHelper.MIME_TYPE, 500);
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
				table.addLigne("Nombre de déclarations traitées:", String.valueOf(results.traites.size()));
				table.addLigne("Nombre de déclarations ignorés:", String.valueOf(results.ignores.size()));
				table.addLigne("Nombre d'erreurs:", String.valueOf(results.errors.size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
			});
		}

		// DIs traitées
		{
			String filename = "dis_traitees.csv";
			String titre = "Liste des déclarations traitées";
			String listVide = "(aucun déclaration traitée)";
			try (TemporaryFile contenu = delaisTraitesAsCsvFile(results.traites, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// DIs ignorées
		{
			String filename = "dis_ignorees.csv";
			String titre = "Liste des déclarations ignorées";
			String listVide = "(aucun déclaration ignorée)";
			try (TemporaryFile contenu = asCsvFile(results.ignores, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// les erreur
		{
			String filename = "erreurs.csv";
			String titre = "Liste des erreurs";
			String listVide = "(aucune erreur)";
			try (TemporaryFile contenu = asCsvFile(results.errors, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile delaisTraitesAsCsvFile(List<DemandeDelaiCollectiveResults.Traite> traites, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		int size = traites.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<DemandeDelaiCollectiveResults.Traite>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de contribuable").append(COMMA).append("Numéro de déclaration");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, DemandeDelaiCollectiveResults.Traite t) {
					b.append(t.ctbId).append(COMMA).append(t.diId);
					return true;
				}
			});
		}
		return contenu;
	}

}