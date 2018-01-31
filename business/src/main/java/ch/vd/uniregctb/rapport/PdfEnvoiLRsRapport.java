package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.declaration.source.EnvoiLRsResults;

/**
 * Rapport PDF contenant les résultats de la génération des listes nominatives.
 */
public class PdfEnvoiLRsRapport extends PdfRapport {

	public void write(final EnvoiLRsResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport de l'envoi des listes récapitulatives");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
				table.addLigne("Date de fin de période:", results.getMoisFinPeriode());
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
				table.addLigne("Nombre total de débiteurs :", String.valueOf(results.nbDPIsTotal));
				table.addLigne("Nombre LR générées :", String.valueOf(results.LRTraitees.size()));
				table.addLigne("dont mensuelles :", String.valueOf(results.nbLrMensuellesTraitees));
				table.addLigne("     trimestrielles :", String.valueOf(results.nbLrTrimestriellesTraitees));
				table.addLigne("     semestrielles :", String.valueOf(results.nbLrSemestriellesTraitees));
				table.addLigne("     annuelles :", String.valueOf(results.nbLrAnnuellesTraitees));
				table.addLigne("     uniques :", String.valueOf(results.nbLrUniquesTraitees));
				table.addLigne("Nombre de LR en erreur :", String.valueOf(results.LREnErreur.size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
			});
		}

		// Débiteurs traités
		{
			String filename = "lr_generees.csv";
			String titre = "Liste des listes récapitulatives générées";
			String listVide = "(aucune liste récapitulative générée)";
			try (TemporaryFile contenu = asCsvFile(results.LRTraitees, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Débiteurs en erreurs
		{
			String filename = "lr_en_erreur.csv";
			String titre = "Liste des erreurs";
			String listVide = "(aucune erreur)";
			try (TemporaryFile contenu = asCsvFile(results.LREnErreur, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}
}