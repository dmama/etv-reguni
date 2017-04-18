package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.validation.ValidationJobResults;

/**
 * Rapport PDF contenant les résultats du rapprochement des ctb et des propriétaires fonciers.
 */
public class PdfValidationRapport extends PdfRapport {

	public void write(final ValidationJobResults results, String nom, String description, final Date dateGeneration, OutputStream os,
	                  StatusManager statusManager) throws Exception {

		Assert.notNull(statusManager);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport de la validation de tous les tiers");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
				table.addLigne("Calcul des assujettissements:", Boolean.toString(results.calculatePeriodesImposition));
				table.addLigne("Cohérence des dates des DIs", Boolean.toString(results.coherencePeriodesImpositionWrtDIs));
				table.addLigne("Calcul des adresses:", Boolean.toString(results.calculateAdresses));
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
				table.addLigne("Nombre total de tiers:", String.valueOf(results.nbTiersTotal));
				table.addLigne("Nombre de tiers ne validant pas:", String.valueOf(results.erreursValidation.size()));
				table.addLigne("Nombre de périodes d'imposition qui ne sont pas calculables:", String
						.valueOf(results.erreursPeriodesImposition.size()));
				table.addLigne("Nombre de DIs émises dont les dates ne correspondent pas aux dates d'assujettissement:", String
						.valueOf(results.erreursCoherenceDI.size()));
				table.addLigne("Nombre de tiers dont les adresses ne sont pas calculables:", String
						.valueOf(results.erreursAdresses.size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
			});
		}

		// CTBs en erreurs
		{
			String filename = "tiers_invalides.csv";
			String titre = "Liste des tiers invalides";
			String listVide = "(aucun tiers invalide)";
			try (TemporaryFile contenu = asCsvFile(results.erreursValidation, filename, statusManager)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Assujettissements
		if (results.calculatePeriodesImposition) {
			String filename = "periodes_imposition_incalculables.csv";
			String titre = "Liste des périodes d'imposition qui ne sont pas calculables";
			String listVide = "(aucune période d'imposition incalculable)";
			try (TemporaryFile contenu = asCsvFile(results.erreursPeriodesImposition, filename, statusManager)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Cohérence DI
		if (results.calculatePeriodesImposition && results.coherencePeriodesImpositionWrtDIs) {
			String filename = "periodes_dis_incoherentes.csv";
			String titre = "Liste des DIs émises dont les dates ne correspondent pas aux dates d'assujettissement";
			String listVide = "(aucune DI émise dont les dates ne correspondent pas aux dates d'assujettissement)";
			try (TemporaryFile contenu = asCsvFile(results.erreursCoherenceDI, filename, statusManager)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Adresses
		if (results.calculateAdresses) {
			String filename = "tiers_adresses_incalculables.csv";
			String titre = "Liste des tiers dont les adresses ne sont pas calculables";
			String listVide = "(aucun tiers dont les adresses ne sont pas calculables)";
			try (TemporaryFile contenu = asCsvFile(results.erreursAdresses, filename, statusManager)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		statusManager.setMessage("Génération du rapport terminée.");
	}

}