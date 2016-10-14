package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.oid.SuppressionOIDResults;

public class PdfSuppressionOIDRapport extends PdfRapport {

	public void write(final SuppressionOIDResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job de suppression d'un office d'impôt");

		// Résultats
		addEntete1("Résultats");
		{
			if (results.interrompu) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\nles valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Numéro de l'office d'impôt supprimé (OID) :", String.valueOf(results.oid));
				table.addLigne("Nombre de tiers impactés :", String.valueOf(results.total));
				table.addLigne("Nombre de tiers traités :", String.valueOf(results.traites.size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.errors.size()));
				table.addLigne("Durée d'exécution :", formatDureeExecution(results));
				table.addLigne("Date de génération :", formatTimestamp(dateGeneration));
			});
		}

		// Cas traités
		{
			final String filename = "tiers_traites.csv";
			final String titre = "Liste des tiers traités";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = asCsvFile(results.traites, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = asCsvFile(results.errors, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}
}
