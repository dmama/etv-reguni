package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.rattrapage.etatdeclaration.CorrectionEtatDeclarationResults;

/**
 * Rapport PDF d'exécution du batch de correction des flags "habitant"
 */
public class PdfCorrectionEtatDeclarationRapport extends PdfRapport {

	public void write(final CorrectionEtatDeclarationResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal(String.format("Rapport d'exécution du job de suppression des doublons des états des déclarations\n%s", formatTimestamp(dateGeneration)));

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\nles valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de déclarations inspectées :", String.valueOf(results.nbDeclarationsTotal));
					table.addLigne("Nombre d'états inspectés :", String.valueOf(results.nbEtatsTotal));
					table.addLigne("Nombre de doublons supprimés :", String.valueOf(results.doublons.size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
					table.addLigne("Durée d'exécution :", formatDureeExecution(results));
				}
			});
		}

		// Doublons
		{
			final String filename = "doublons_supprimes.csv";
			final String contenu = traitesAsCsvFile(results.doublons, filename, status);
			final String titre = "Liste des nouveaux habitants";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}


		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String contenu = asCsvFile(results.erreurs, filename, status);
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}


	private String traitesAsCsvFile(List<CorrectionEtatDeclarationResults.Doublon> list, String filename, StatusManager status) {
		String contenu = null;
		int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvFile(list, filename, status, new CsvHelper.FileFiller<CorrectionEtatDeclarationResults.Doublon>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Id du contribuable").append(COMMA);
					b.append("Id de la déclaration d'impôt").append(COMMA);
					b.append("Id de l'état").append(COMMA);
					b.append("Type de l'état").append(COMMA);
					b.append("Date d'obtention").append(COMMA);
					b.append("Date de création").append(COMMA);
					b.append("Utilisateur de création").append(COMMA);
					b.append("Date de modification").append(COMMA);
					b.append("Utilisateur de modification").append(COMMA);
					b.append("Date d'annulation").append(COMMA);
					b.append("Utilisateur d'annulation");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, CorrectionEtatDeclarationResults.Doublon doublon) {
					b.append(doublon.ctbId).append(COMMA);
					b.append(doublon.diId).append(COMMA);
					b.append(doublon.id).append(COMMA);
					b.append(doublon.type).append(COMMA);
					b.append(doublon.dateObtention).append(COMMA);
					b.append(doublon.logCreationDate).append(COMMA);
					b.append(doublon.logCreationUser).append(COMMA);
					b.append(doublon.logModificationDate).append(COMMA);
					b.append(doublon.logModificationUser).append(COMMA);
					b.append(doublon.annulationDate).append(COMMA);
					b.append(doublon.annulationUser).append(COMMA);
					return true;
				}
			});
		}
		return contenu;
	}
}
