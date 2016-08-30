package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.situationfamille.ReinitialiserBaremeDoubleGainResults;

/**
 * Rapport PDF contenant les résultats de la réinitialisation des barèmes double-gain.
 */
public class PdfReinitDoubleGainRapport extends PdfRapport {

	public void write(final ReinitialiserBaremeDoubleGainResults results, String nom, String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job de réinitialisation des barèmes double-gain");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
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
					table.addLigne("Nombre total de situations de familles inspectées:", String.valueOf(results.nbSituationsTotal));
					table.addLigne("Nombre de situations réinitialisées:", String.valueOf(results.situationsTraitees.size()));
					table.addLigne("Nombre de situations en erreur:", String.valueOf(results.situationsEnErrors.size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
				}
			});
		}

		// Situations réinitialisées
		{
			String filename = "situations_reinitialisees.csv";
			String titre = "Liste des situations réinitialisées";
			String listVide = "(aucun situation réinitialisée)";
			try (TemporaryFile contenu = situationsTraiteesAsCsvFile(results.situationsTraitees, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Situations en erreur
		{
			String filename = "situations_en_erreur.csv";
			String titre = "Liste des situations en erreur";
			String listVide = "(aucun situation en erreur)";
			try (TemporaryFile contenu = asCsvFile(results.situationsEnErrors, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile situationsTraiteesAsCsvFile(List<ReinitialiserBaremeDoubleGainResults.Situation> situations, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		int size = situations.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(situations, filename, status, new CsvHelper.FileFiller<ReinitialiserBaremeDoubleGainResults.Situation>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de l'office d'impôt").append(COMMA);
					b.append("Numéro de contribuable").append(COMMA);
					b.append("Numéro de l'ancienne situation de famille").append(COMMA);
					b.append("Numéro de la nouvelle situation de famille");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, ReinitialiserBaremeDoubleGainResults.Situation situation) {
					b.append(situation.officeImpotID).append(COMMA);
					b.append(situation.ctbId).append(COMMA);
					b.append(situation.ancienneId).append(COMMA);
					b.append(situation.nouvelleId);
					return true;
				}
			});
		}
		return contenu;
	}

}
