package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.registrefoncier.rattrapage.RattrapageModelesCommunautesRFProcessorResults;

/**
 * Générateur du rapport PDF d'exécution du batch de rattrapage des regroupement des communautés sur les modèles de communauté RF
 */
public class PdfRattrapageModelesCommunautesRFRapport extends PdfRapport {

	public void write(final RattrapageModelesCommunautesRFProcessorResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {
		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport du rattrapage des regroupement des communautés sur les modèles de communauté RF.");

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
			if (status.isInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[]{.6f, .4f}, table -> {
				table.addLigne("Nombre de communautés traitées :", String.valueOf(results.getProcessed()));
				table.addLigne("Nombre de communautés modifiées :", String.valueOf(results.getUpdated().size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErreurs().size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Communautés modifiées
		{
			final String filename = "communautes_modifiees.csv";
			final String titre = "Liste des communautés modifiées";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = genererCommunautesModifiees(results.getUpdated(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = asCsvFileErreurs(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile genererCommunautesModifiees(List<RattrapageModelesCommunautesRFProcessorResults.Updated> liste, String filename, StatusManager status) {

		// on détermine le nombre maximum de membres des communautés reçues
		final int maxMembres = liste.stream()
				.map(RattrapageModelesCommunautesRFProcessorResults.Updated::getMembres)
				.map(List::size)
				.max(Comparator.naturalOrder())
				.orElse(1);

		return CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<RattrapageModelesCommunautesRFProcessorResults.Updated>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("COMMUNAUTE_ID").append(COMMA);
				b.append("EGRID").append(COMMA);
				b.append("ID_RF").append(COMMA);
				b.append("NOMBRE_MEMBRES").append(COMMA);
				for (int i = 0; i < maxMembres; ++i) {
					b.append("MEMBRE_").append(i).append(COMMA);
				}
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RattrapageModelesCommunautesRFProcessorResults.Updated elt) {
				b.append(elt.getCommunauteId()).append(COMMA);
				b.append(elt.getEgrid()).append(COMMA);
				b.append(elt.getIdRF()).append(COMMA);
				final List<String> membres = elt.getMembres();
				b.append(membres.size()).append(COMMA);
				for (String membre : membres) {
					b.append(CsvHelper.asCsvField(membre)).append(COMMA);
				}
				return true;
			}
		});
	}

	private TemporaryFile asCsvFileErreurs(List<RattrapageModelesCommunautesRFProcessorResults.Erreur> erreurs, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(erreurs, filename, status, new CsvHelper.FileFiller<RattrapageModelesCommunautesRFProcessorResults.Erreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("COMMUNAUTE_ID").append(COMMA);
				b.append("MESSAGE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RattrapageModelesCommunautesRFProcessorResults.Erreur elt) {
				b.append(elt.getCommunauteId()).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.getMessage()));
				return true;
			}
		});
	}
}
