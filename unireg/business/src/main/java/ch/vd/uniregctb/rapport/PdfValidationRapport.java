package ch.vd.uniregctb.rapport;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.validation.ValidationJobResults;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.util.Date;

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
		addTitrePrincipal("Rapport de la validation de tous les contribuables");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
					table.addLigne("Calcul des assujettissements:", Boolean.toString(results.calculateAssujettissements));
					table.addLigne("Cohérence des dates des DIs", Boolean.toString(results.coherenceAssujetDi));
					table.addLigne("Calcul des adresses:", Boolean.toString(results.calculateAdresses));
					table.addLigne("Cohérence des autorités des fors fiscaux:", Boolean.toString(results.coherenceAutoritesForsFiscaux));
				}
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.interrompu) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre total de contribuables:", String.valueOf(results.nbCtbsTotal));
					table.addLigne("Nombre de contribuables ne validant pas:", String.valueOf(results.erreursValidation.size()));
					table.addLigne("Nombre de périodes d'assujettissement qui ne sont pas calculables:", String
							.valueOf(results.erreursAssujettissement.size()));
					table.addLigne("Nombre de DIs émises dont les dates ne correspondent pas aux dates d'assujettissement:", String
							.valueOf(results.erreursCoherenceDI.size()));
					table.addLigne("Nombre de contribuables dont les adresses ne sont pas calculables:", String
							.valueOf(results.erreursAdresses.size()));
					table.addLigne("Nombre de fors fiscaux dont les types d'autorités fiscales ne sont pas cohérents:", String
							.valueOf(results.erreursAutoritesForsFiscaux.size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
				}
			});
		}

		// CTBs en erreurs
		{
			String filename = "contribuables_invalides.csv";
			String contenu = asCsvFile(results.erreursValidation, filename, statusManager);
			String titre = "Liste des contribuables invalides";
			String listVide = "(aucun contribuable invalide)";
			addListeDetaillee(writer, results.erreursValidation.size(), titre, listVide, filename, contenu);
		}

		// Assujettissements
		if (results.calculateAssujettissements) {
			String filename = "periodes_assujettissements_incalculables.csv";
			String contenu = asCsvFile(results.erreursAssujettissement, filename, statusManager);
			String titre = "Liste des périodes d'assujettissement qui ne sont pas calculables";
			String listVide = "(aucune période d'assujettissement incalculable)";
			addListeDetaillee(writer, results.erreursAssujettissement.size(), titre, listVide, filename, contenu);
		}

		// Cohérence DI
		if (results.calculateAssujettissements && results.coherenceAssujetDi) {
			String filename = "periodes_dis_incoherentes.csv";
			String contenu = asCsvFile(results.erreursCoherenceDI, filename, statusManager);
			String titre = "Liste des DIs émises dont les dates ne correspondent pas aux dates d'assujettissement";
			String listVide = "(aucune DI émise dont les dates ne correspondent pas aux dates d'assujettissement)";
			addListeDetaillee(writer, results.erreursCoherenceDI.size(), titre, listVide, filename, contenu);
		}

		// Adresses
		if (results.calculateAdresses) {
			String filename = "contribuables_adresses_incalculables.csv";
			String contenu = asCsvFile(results.erreursAdresses, filename, statusManager);
			String titre = "Liste des contribuables dont les adresses ne sont pas calculables";
			String listVide = "(aucun contribuable dont les adresses ne sont pas calculables)";
			addListeDetaillee(writer, results.erreursAdresses.size(), titre, listVide, filename, contenu);
		}

		// Autorités fiscales
		if (results.coherenceAutoritesForsFiscaux) {
			String filename = "autorites_fors_fiscaux_incoherentes.csv";
			String contenu = asCsvFile(results.erreursAutoritesForsFiscaux, filename, statusManager);
			String titre = "Liste des fors fiscaux dont les autorités fiscales ne sont pas cohérentes";
			String listVide = "(aucun for fiscal dont l'autorité fiscale n'est pas cohérente)";
			addListeDetaillee(writer, results.erreursAutoritesForsFiscaux.size(), titre, listVide, filename, contenu);
		}

		close();

		statusManager.setMessage("Génération du rapport terminée.");
	}

}