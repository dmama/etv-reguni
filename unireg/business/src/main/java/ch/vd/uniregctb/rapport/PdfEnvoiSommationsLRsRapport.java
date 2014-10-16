package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.declaration.source.EnvoiSommationLRsResults;

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
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Catégorie de débiteurs:", (results.categorie == null ? "-" : results.categorie.name()));
					if (results.dateFinPeriode != null) {
						table.addLigne("Fin de période:", RegDateHelper.dateToDisplayString(results.dateFinPeriode));
					}
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
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre total de listes récapitulatives:", String.valueOf(results.nbLRsTotal));
					table.addLigne("Nombre de listes récapitulatives sommées:", String.valueOf(results.lrSommees.size()));
					table.addLigne("Nombre de listes récapitulatives en erreur:", String.valueOf(results.sommationLREnErreurs.size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
				}
			});
		}

		// LR sommées
		{
			String filename = "listes_recapitulatives_sommees.csv";
			String contenu = asCsvFile(results.lrSommees, filename, status);
			String titre = "Liste des débiteurs traités";
			String listVide = "(aucun débiteur traité)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		// Sommations LR en erreurs
		{
			String filename = "sommation_en_erreur.csv";
			String contenu = asCsvFile(results.sommationLREnErreurs, filename, status);
			String titre = "Liste des débiteurs en erreur";
			String listVide = "(aucun débiteur en erreur)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}
}