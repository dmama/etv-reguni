package ch.vd.uniregctb.rapport;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.source.EnvoiSommationLRsResults;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.util.Date;

/**
 * Rapport PDF contenant les résultats du rapprochement des ctb et des propriétaires fonciers.
 */
public class PdfEnvoiSommationsLRsRapport extends PdfRapport {

	public void write(final EnvoiSommationLRsResults results, final String nom, final String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport de l'envoi des sommations des listes récapitulatives");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Catégorie de débiteurs:", results.categorie.name());
					table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
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
					table.addLigne("Nombre total de listes récapitulatives:", String.valueOf(results.nbLRsTotal));
					table.addLigne("Nombre de listes récapitulatives sommées:", String.valueOf(results.LRSommees.size()));
					table.addLigne("Nombre de listes récapitulatives en erreur:", String.valueOf(results.SommationLREnErrors.size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
				}
			});
		}

		// LR sommées
		{
			String filename = "listes_recapitulatives_sommees.csv";
			String contenu = asCsvFile(results.LRSommees, filename, status);
			String titre = "Liste des débiteurs traités";
			String listVide = "(aucun débiteur traité)";
			addListeDetaillee(writer, results.LRSommees.size(), titre, listVide, filename, contenu);
		}

		// Sommations LR en erreurs
		{
			String filename = "sommation_en_erreur.csv";
			String contenu = asCsvFile(results.SommationLREnErrors, filename, status);
			String titre = "Liste des débiteurs en erreur";
			String listVide = "(aucun débiteur en erreur)";
			addListeDetaillee(writer, results.SommationLREnErrors.size(), titre, listVide, filename, contenu);
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}
}