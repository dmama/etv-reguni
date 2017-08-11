package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.registrefoncier.dataimport.TraitementFinsDeDroitRFResults;

/**
 * Rapport PDF contenant le rapport de calcul des dates de fin de droits (RF).
 */
public class PdfFinsDeDroitRFRapport extends PdfRapport {

	public void write(final TraitementFinsDeDroitRFResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du batch de calcul des dates de fin métier sur les droits RF.");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Nombre de threads :", String.valueOf(results.getNbThreads()));
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
				table.addLigne("Nombre d'immeubles traités :", String.valueOf(results.getTraites().size()));
				table.addLigne("Nombre d'immeubles ignorés :", String.valueOf(results.getIgnores().size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getNbErreurs()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results.endTime - results.startTime));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Immeubles traités
		{
			String filename = "immeubles_traites.csv";
			String titre = "Liste des immeubles traités";
			String listVide = "(aucun immeuble traité)";
			try (TemporaryFile contenu = traitesAsCsvFile(results.getTraites(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Immeubles ignorés
		{
			String filename = "immeubles_ignores.csv";
			String titre = "Liste des immeubles ignorés";
			String listVide = "(aucun immeuble ignoré)";
			try (TemporaryFile contenu = ignoresAsCsvFile(results.getIgnores(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Cas en erreur
		{
			final String filename = "erreurs.csv";
			final String titre = " Liste des immeubles en erreur";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = erreursAsCsvFile(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private static TemporaryFile traitesAsCsvFile(List<TraitementFinsDeDroitRFResults.Traite> traites, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<TraitementFinsDeDroitRFResults.Traite>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("IMMEUBLE_ID");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, TraitementFinsDeDroitRFResults.Traite elt) {
				b.append(elt.getImmeubleId());
				return true;
			}
		});
	}

	private static TemporaryFile ignoresAsCsvFile(List<TraitementFinsDeDroitRFResults.Ignore> traites, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<TraitementFinsDeDroitRFResults.Ignore>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("IMMEUBLE_ID");
				b.append("RAISON");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, TraitementFinsDeDroitRFResults.Ignore elt) {
				b.append(elt.getImmeubleId());
				b.append(elt.getRaison());
				return true;
			}
		});
	}

	private TemporaryFile erreursAsCsvFile(List<TraitementFinsDeDroitRFResults.Erreur> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<TraitementFinsDeDroitRFResults.Erreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("IMMEUBLE_ID").append(COMMA);
					b.append("MESSAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, TraitementFinsDeDroitRFResults.Erreur elt) {
					b.append(elt.getImmeubleId()).append(COMMA);
					b.append(CsvHelper.asCsvField(elt.getMessage()));
					return true;
				}
			});
		}
		return contenu;
	}
}