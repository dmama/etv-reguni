package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.GentilIterator;
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
			addListeDetaillee(writer, results.doublons.size(), titre, listVide, filename, contenu);
		}


		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String contenu = asCsvFile(results.erreurs, filename, status);
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, results.erreurs.size(), titre, listVide, filename, contenu);
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}


	private String traitesAsCsvFile(List<CorrectionEtatDeclarationResults.Doublon> list, String filename, StatusManager status) {
		String contenu = null;
		int size = list.size();
		if (size > 0) {

		    StringBuilder b = new StringBuilder(AVG_LINE_LEN * list.size());
			b.append("Id du contribuable");
			b.append(COMMA);
			b.append("Id de la déclaration d'impôt");
			b.append(COMMA);
			b.append("Id de l'état");
			b.append(COMMA);
			b.append("Type de l'état");
			b.append(COMMA);
			b.append("Date d'obtention");
			b.append(COMMA);
			b.append("Date de création");
			b.append(COMMA);
			b.append("Utilisateur de création");
			b.append(COMMA);
			b.append("Date de modification");
			b.append(COMMA);
			b.append("Utilisateur de modification");
			b.append(COMMA);
			b.append("Date d'annulation");
			b.append(COMMA);
			b.append("Utilisateur d'annulation\n");

			final GentilIterator<CorrectionEtatDeclarationResults.Doublon> iter = new GentilIterator<CorrectionEtatDeclarationResults.Doublon>(list);
		    while (iter.hasNext()) {
		        if (iter.isAtNewPercent()) {
		            status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
		        }

		        CorrectionEtatDeclarationResults.Doublon doublon = iter.next();
		        StringBuilder bb = new StringBuilder(AVG_LINE_LEN);
			    bb.append(doublon.ctbId).append(COMMA);
			    bb.append(doublon.diId).append(COMMA);
			    bb.append(doublon.id).append(COMMA);
			    bb.append(doublon.type).append(COMMA);
			    bb.append(doublon.dateObtention).append(COMMA);
			    bb.append(doublon.logCreationDate).append(COMMA);
			    bb.append(doublon.logCreationUser).append(COMMA);
			    bb.append(doublon.logModificationDate).append(COMMA);
			    bb.append(doublon.logModificationUser).append(COMMA);
			    bb.append(doublon.annulationDate).append(COMMA);
			    bb.append(doublon.annulationUser).append(COMMA);
		        bb.append('\n');

		        b.append(bb);
		    }
		    contenu = b.toString();
		}
		return contenu;
	}
}
