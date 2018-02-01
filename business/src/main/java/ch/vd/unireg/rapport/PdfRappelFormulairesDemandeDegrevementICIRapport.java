package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.foncier.RappelFormulairesDemandeDegrevementICIResults;

/**
 * Rapport PDF contenant les résultats du job d'envoi des rappels des demandes de dégrèvement ICI.
 */
public class PdfRappelFormulairesDemandeDegrevementICIRapport extends PdfRapport {

	public void write(final RappelFormulairesDemandeDegrevementICIResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution des rappels en masse des formulaires de demande de dégrèvement ICI.");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.dateTraitement));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[]{.6f, .4f}, table -> {
				table.addLigne("Nombre de formulaires inspectés :", String.valueOf(results.getIgnores().size() + results.getTraites().size() + results.getErreurs().size()));
				table.addLigne("Nombre de formulaires ignorés :", String.valueOf(results.getIgnores().size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErreurs().size()));
				table.addLigne("Nombre de rappels envoyés :", String.valueOf(results.getTraites().size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération :", formatTimestamp(dateGeneration));
			});
		}

		// rappels envoyés
		{
			final String filename = "rappels_envoyes.csv";
			final String titre = "Rappels envoyés";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = rappelsAsCsvFile(results.getTraites(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// formulaires ignorés
		{
			final String filename = "formulaires_ignores.csv";
			final String titre = "Formulaires ignorés";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = ignoresAsCsvFile(results.getIgnores(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// erreurs
		{
			final String filename = "erreurs.csv";
			final String titre = "Erreurs";
			final String listeVide = "(aucune)";
			try (TemporaryFile contenu = erreursAsCsvFile(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private static TemporaryFile rappelsAsCsvFile(List<RappelFormulairesDemandeDegrevementICIResults.Traite> rappels, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!rappels.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(rappels, filename, status, new CsvHelper.FileFiller<RappelFormulairesDemandeDegrevementICIResults.Traite>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA);
					b.append("ID_FORMULAIRE").append(COMMA);
					b.append("DATE_ENVOI_INITIAL").append(COMMA);
					b.append("PF").append(COMMA);
					b.append("NOM_COMMUNE").append(COMMA);
					b.append("NO_OFS_COMMUNE").append(COMMA);
					b.append("NO_PARCELLE").append(COMMA);
					b.append("INDEX1").append(COMMA);
					b.append("INDEX2").append(COMMA);
					b.append("INDEX3");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, RappelFormulairesDemandeDegrevementICIResults.Traite elt) {
					b.append(elt.noCtb).append(COMMA);
					b.append(elt.idFormulaire).append(COMMA);
					b.append(elt.dateEnvoiFormulaire).append(COMMA);
					b.append(elt.periodeFiscale).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.nomCommune)).append(COMMA);
					b.append(elt.ofsCommune).append(COMMA);
					b.append(elt.noParcelle).append(COMMA);
					b.append(elt.index1).append(COMMA);
					b.append(elt.index2).append(COMMA);
					b.append(elt.index3);
					return true;
				}
			});
		}
		return contenu;
	}

	private static TemporaryFile ignoresAsCsvFile(List<RappelFormulairesDemandeDegrevementICIResults.Ignore> ignores, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!ignores.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(ignores, filename, status, new CsvHelper.FileFiller<RappelFormulairesDemandeDegrevementICIResults.Ignore>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA);
					b.append("ID_FORMULAIRE").append(COMMA);
					b.append("DATE_ENVOI_INITIAL").append(COMMA);
					b.append("PF").append(COMMA);
					b.append("NOM_COMMUNE").append(COMMA);
					b.append("NO_OFS_COMMUNE").append(COMMA);
					b.append("NO_PARCELLE").append(COMMA);
					b.append("INDEX1").append(COMMA);
					b.append("INDEX2").append(COMMA);
					b.append("INDEX3").append(COMMA);
					b.append("RAISON").append(COMMA);
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, RappelFormulairesDemandeDegrevementICIResults.Ignore elt) {
					b.append(elt.noCtb).append(COMMA);
					b.append(elt.idFormulaire).append(COMMA);
					b.append(elt.dateEnvoi).append(COMMA);
					b.append(elt.periodeFiscale).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.nomCommune)).append(COMMA);
					b.append(elt.ofsCommune).append(COMMA);
					b.append(elt.noParcelle).append(COMMA);
					b.append(elt.index1).append(COMMA);
					b.append(elt.index2).append(COMMA);
					b.append(elt.index3).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.raison.libelle));
					return true;
				}
			});
		}
		return contenu;
	}

	private static TemporaryFile erreursAsCsvFile(List<RappelFormulairesDemandeDegrevementICIResults.EnErreur> erreurs, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!erreurs.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(erreurs, filename, status, new CsvHelper.FileFiller<RappelFormulairesDemandeDegrevementICIResults.EnErreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA);
					b.append("ID_FORMULAIRE").append(COMMA);
					b.append("PF").append(COMMA);
					b.append("NOM_COMMUNE").append(COMMA);
					b.append("NO_OFS_COMMUNE").append(COMMA);
					b.append("NO_PARCELLE").append(COMMA);
					b.append("INDEX1").append(COMMA);
					b.append("INDEX2").append(COMMA);
					b.append("INDEX3").append(COMMA);
					b.append("ERREUR");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, RappelFormulairesDemandeDegrevementICIResults.EnErreur elt) {
					b.append(elt.noCtb).append(COMMA);
					b.append(elt.idFormulaire).append(COMMA);
					b.append(elt.periodeFiscale).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.nomCommune)).append(COMMA);
					b.append(elt.ofsCommune).append(COMMA);
					b.append(elt.noParcelle).append(COMMA);
					b.append(elt.index1).append(COMMA);
					b.append(elt.index2).append(COMMA);
					b.append(elt.index3).append(COMMA);
					b.append(CsvHelper.asCsvField(elt.msg));
					return true;
				}
			});
		}
		return contenu;
	}
}