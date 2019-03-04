package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.declaration.ordinaire.common.AjouterDelaiPourMandataireResults;
import ch.vd.unireg.declaration.ordinaire.common.InfosDelaisMandataire;

/**
 * Rapport PDF contenant les résultats de l'exécution d'un job de traitement des DIs.
 */
public class PdfAjouterDelaiPourMandataireRapport extends PdfRapport {

	public void write(final AjouterDelaiPourMandataireResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du traitement d'ajout de délai pour les mandataires");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Date de délai:", RegDateHelper.dateToDisplayString(results.dateDelai));
				table.addLigne("Contribuables à traiter:", "(voir le fichier contribuables_a_traiter.csv)");
				table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
			});
			// ids en entrées
			String filename = "Delais_a_ajouter.csv";
			try (TemporaryFile contenu = infosDelaisAsCsvFile(results.infosDelais, filename, status)) {
				attacheFichier(writer, contenu.getFullPath(), filename, "Délais à ajouter", CsvHelper.MIME_TYPE, 500);
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



		 /** Traduit la liste d'infos en un fichier CSV
			*/
	private  TemporaryFile infosDelaisAsCsvFile(List<InfosDelaisMandataire> infos, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(infos, filename, status, new CsvHelper.FileFiller<InfosDelaisMandataire>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA).append("PERIODE_FISCALE")
						.append(COMMA).append("STATUT").append(COMMA).append("IDE_AMDATAIRE")
						.append(COMMA).append("RAISON_SOCIALE_MANDATAIRE").append(COMMA).append("IDENTIFIANT_DEMANDE").append(COMMA).append("DATE_SOUMISSION");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, InfosDelaisMandataire elt) {
				b.append(elt.getNumeroTiers()).append(COMMA);
				b.append(elt.getPeriodeFiscale()).append(COMMA);
				b.append(elt.getStatut()).append(COMMA);
				b.append(elt.getIdeMandataire()).append(COMMA);
				b.append(elt.getRaisonSocialeMandataire()).append(COMMA);
				b.append(elt.getIdentifiantDemandeMandataire()).append(COMMA);
				b.append(RegDateHelper.dateToDisplayString(elt.getDateSoumission())).append(COMMA);
				return true;
			}
		});
	}

	private TemporaryFile delaisTraitesAsCsvFile(List<AjouterDelaiPourMandataireResults.Traite> traites, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		int size = traites.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<AjouterDelaiPourMandataireResults.Traite>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de contribuable").append(COMMA).append("Numéro de déclaration");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, AjouterDelaiPourMandataireResults.Traite t) {
					b.append(t.ctbId).append(COMMA).append(t.diId);
					return true;
				}
			});
		}
		return contenu;
	}

}