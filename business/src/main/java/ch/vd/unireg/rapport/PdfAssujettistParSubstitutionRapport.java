package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.listes.assujettis.AssujettisParSubstitutionResults;

public class PdfAssujettistParSubstitutionRapport extends PdfRapport {

	/**
	 * Génère un rapport au format PDF à partir des résultats de job.
	 */

	public void write(final AssujettisParSubstitutionResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		PdfAssujettistParSubstitutionRapport document = new PdfAssujettistParSubstitutionRapport();
		PdfWriter writer = PdfWriter.getInstance(document, os);
		document.open();
		document.addMetaInfo(nom, description);
		document.addEnteteUnireg();

		// Titre
		document.addTitrePrincipal("Rapport d'exécution du job qui liste les assujettissements par substitution.");

		// Paramètres
		document.addEntete1("Paramètres");
		{
			document.addTableSimple(2, table -> {
				table.addLigne("Nombre de threads :", String.valueOf(results.nbThreads));
			});
		}

		// Résultats
		document.addEntete1("Résultats");
		{
			if (results.interrupted) {
				document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						                    + "les valeurs ci-dessous sont donc incomplètes.");
			}

			document.addTableSimple(2, table -> {
				table.addLigne("Nombre total de relations analysées :", String.valueOf(results.getRapportsSubstitutions().size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErreurs().size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
			});
		}

		// Relations créées
		{
			String filename = "substitutions.csv";
			String titre = "Liste des assujettissements par substitution";
			String listVide = "(aucune)";
			try (TemporaryFile contenu = asCsvFileTraite(results.getRapportsSubstitutions(), filename, status)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Erreurs
		{
			String filename = "erreurs.csv";
			String titre = "Liste des erreurs rencontrées";
			String listVide = "(aucune)";
			try (TemporaryFile contenu = asCsvFileErreur(results.getErreurs(), filename, status)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		document.close();

		status.setMessage("Génération du rapport terminée.");
	}

	private static TemporaryFile asCsvFileTraite(List<AssujettisParSubstitutionResults.InfoRapportSubstitution> list, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<AssujettisParSubstitutionResults.InfoRapportSubstitution>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB_SUBSTITUANT").append(COMMA);
				b.append("NOM_PRENOM_SUBSTITUANT").append(COMMA);
				b.append("DATE_NAISSANCE_SUBSTITUANT").append(COMMA);
				b.append("NAVS13_SUBSTITUANT").append(COMMA);
				b.append("ASSUJETTI_SUBSTITUANT").append(COMMA);
				b.append("NO_CTB_SUBSTITUE").append(COMMA);
				b.append("NOM_PRENOM_SUBSTITUE").append(COMMA);
				b.append("DATE_NAISSANCE_SUBSTITUE").append(COMMA);
				b.append("NAVS13_SUBSTITUE").append(COMMA);
				b.append("ASSUJETTI_SUBSTITUE").append(COMMA);
				b.append("DATE_DEBUT").append(COMMA);
				b.append("DATE_FIN");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, AssujettisParSubstitutionResults.InfoRapportSubstitution elt) {
				b.append(elt.substituant.noCtb).append(COMMA);
				b.append(elt.substituant.nomPrenom).append(COMMA);
				b.append(RegDateHelper.dateToDashString(elt.substituant.dateNaissance)).append(COMMA);
				b.append(AvsHelper.formatNouveauNumAVS(elt.substituant.navs13)).append(COMMA);
				b.append(elt.substituant.assujetti?"OUI":"NON").append(COMMA);
				b.append(elt.substitue.noCtb).append(COMMA);
				b.append(elt.substitue.nomPrenom).append(COMMA);
				b.append(RegDateHelper.dateToDashString(elt.substitue.dateNaissance)).append(COMMA);
				b.append(AvsHelper.formatNouveauNumAVS(elt.substitue.navs13)).append(COMMA);
				b.append(elt.substitue.assujetti?"OUI":"NON").append(COMMA);
				b.append(RegDateHelper.dateToDashString(elt.dateOuvertureRapport)).append(COMMA);
				b.append(RegDateHelper.dateToDashString(elt.dateFermetureRapport)).append(COMMA);
				return true;
			}
		});
	}

	private static TemporaryFile asCsvFileErreur(List<AssujettisParSubstitutionResults.Erreur> list, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<AssujettisParSubstitutionResults.Erreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("ID_RAPPORT").append(COMMA);
				b.append("DETAILS");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, AssujettisParSubstitutionResults.Erreur elt) {
				b.append(elt.idRapport).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.description));
				return true;
			}
		});
	}
}
