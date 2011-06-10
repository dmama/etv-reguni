package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.rattrapage.pm.MigrationCoquillesPM;

/**
 * Rapport PDF de la migration des coquilles vides des PMs
 */
public class PdfMigrationCoquillesPMRapport extends PdfRapport {

	public void write(final MigrationCoquillesPM.MigrationResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport de la migration des coquilles des personnes morales");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
				}
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[]{50f, 30f}, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de PMs à migrer :", String.valueOf(results.total));
					table.addLigne("Nombre de PMs migrées :", String.valueOf(results.traitees.size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
					table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
				}
			});
		}

		// Messages identifiés
		{
			final String filename = "pm_migrees.csv";
			final String contenu = ctbIdsAsCsvFile(results.traitees, filename, status);
			final String titre = "Liste des personnes morales migrées";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, results.traitees.size(), titre, listVide, filename, contenu);
		}


		// erreurs
		{
			final String filename = "erreurs.csv";
			final String contenu = asCsvFile(results.erreurs, filename, status);
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, results.erreurs.size(), titre, listVide, filename, contenu);
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}
}