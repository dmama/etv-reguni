package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.registrefoncier.rattrapage.RattraperDatesDebutDroitRFProcessorResults;

/**
 * Générateur du rapport PDF d'exécution du batch batch de rattrapage des dates de début des droits RF
 */
public class PdfRattrapageDatesDebutDroitRFRapport extends PdfRapport {

	public void write(final RattraperDatesDebutDroitRFProcessorResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {
		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport du rattrapage des dates de début des droits RF.");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Immeuble concernés :", String.valueOf(results.getDataSelection()));
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
				table.addLigne("Nombre d'immeubles traités :", String.valueOf(results.getProcessed().size()));
				table.addLigne("Nombre de date de début modifiées :", String.valueOf(results.getDebutUpdated().size()));
				table.addLigne("Nombre de date de fin modifiés :", String.valueOf(results.getFinUpdated().size()));
				table.addLigne("Nombre de droits non-modifiés :", String.valueOf(results.getUntouched().size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErreurs().size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Immeubles traités
		{
			final String filename = "immeubles_traites.csv";
			final String titre = "Liste des immeubles traités";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = genererImmeublesTraites(results.getProcessed(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// Droits avec dates de début modifiées
		{
			final String filename = "droits_dates_debut_modifiees.csv";
			final String titre = "Liste des droits avec dates de début modifiées";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = genererDroitsDebutModifies(results.getDebutUpdated(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// Droits avec dates de fin modifiées
		{
			final String filename = "droits_dates_fin_modifiees.csv";
			final String titre = "Liste des droits avec dates de fin modifiées";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = genererDroitsFinModifies(results.getFinUpdated(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// Droits non-modifiés
		{
			final String filename = "droits_non_modifies.csv";
			final String titre = "Liste des droits non-modifiés";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = genererDroitsNonModifies(results.getUntouched(), filename, status)) {
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

	private TemporaryFile genererImmeublesTraites(List<RattraperDatesDebutDroitRFProcessorResults.Processed> liste, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<RattraperDatesDebutDroitRFProcessorResults.Processed>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("IMMEUBLE_ID").append(COMMA);
				b.append("EGRID").append(COMMA);
				b.append("ID_RF").append(COMMA);
				b.append("COMMUNE").append(COMMA);
				b.append("NO_OFS_COMMUNE").append(COMMA);
				b.append("NO_PARCELLE").append(COMMA);
				b.append("INDEX1").append(COMMA);
				b.append("INDEX2").append(COMMA);
				b.append("INDEX3");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RattraperDatesDebutDroitRFProcessorResults.Processed elt) {
				b.append(elt.getImmeubleId()).append(COMMA);
				b.append(elt.getEgrid()).append(COMMA);
				b.append(elt.getIdRF()).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.getCommune())).append(COMMA);
				b.append(elt.getNoOfsCommune()).append(COMMA);
				b.append(elt.getNoParcelle()).append(COMMA);
				b.append(elt.getIndex1()).append(COMMA);
				b.append(elt.getIndex2()).append(COMMA);
				b.append(elt.getIndex3()).append(COMMA);
				return true;
			}
		});
	}

	private TemporaryFile genererDroitsDebutModifies(List<RattraperDatesDebutDroitRFProcessorResults.DebutUpdated> liste, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<RattraperDatesDebutDroitRFProcessorResults.DebutUpdated>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("DROIT_ID").append(COMMA);
				b.append("IMMEUBLE_ID").append(COMMA);
				b.append("EGRID").append(COMMA);
				b.append("DATE_DEBUT_TECHNIQUE").append(COMMA);
				b.append("DATE_FIN_TECHNIQUE").append(COMMA);
				b.append("DATE_DEBUT_METIER_INITIALE").append(COMMA);
				b.append("MOTIF_DEBUT_INITIAL").append(COMMA);
				b.append("DATE_DEBUT_METIER_MODIFIEE").append(COMMA);
				b.append("MOTIF_DEBUT_MODIFIE").append(COMMA);
				b.append("DATE_FIN_METIER").append(COMMA);
				b.append("MOTIF_FIN");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RattraperDatesDebutDroitRFProcessorResults.DebutUpdated elt) {
				b.append(elt.getDroitId()).append(COMMA);
				b.append(elt.getImmeubleId()).append(COMMA);
				b.append(elt.getEgrid()).append(COMMA);
				b.append(elt.getDateDebut()).append(COMMA);
				b.append(elt.getDateFin()).append(COMMA);
				b.append(elt.getDateDebutMetierInitiale()).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.getMotifDebutInitial())).append(COMMA);
				b.append(elt.getDateDebutMetierCorrigee()).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.getMotifDebutCorrige())).append(COMMA);
				b.append(elt.getDateFinMetier()).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.getMotifFin()));
				return true;
			}
		});
	}

	private TemporaryFile genererDroitsFinModifies(List<RattraperDatesDebutDroitRFProcessorResults.FinUpdated> liste, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<RattraperDatesDebutDroitRFProcessorResults.FinUpdated>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("DROIT_ID").append(COMMA);
				b.append("IMMEUBLE_ID").append(COMMA);
				b.append("EGRID").append(COMMA);
				b.append("DATE_DEBUT_TECHNIQUE").append(COMMA);
				b.append("DATE_FIN_TECHNIQUE").append(COMMA);
				b.append("DATE_DEBUT_METIER").append(COMMA);
				b.append("MOTIF_DEBUT").append(COMMA);
				b.append("DATE_FIN_METIER_INITIALE").append(COMMA);
				b.append("MOTIF_FIN_INITIAL").append(COMMA);
				b.append("DATE_FIN_METIER_MODIFIEE").append(COMMA);
				b.append("MOTIF_FIN_MODIFIE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RattraperDatesDebutDroitRFProcessorResults.FinUpdated elt) {
				b.append(elt.getDroitId()).append(COMMA);
				b.append(elt.getImmeubleId()).append(COMMA);
				b.append(elt.getEgrid()).append(COMMA);
				b.append(elt.getDateDebut()).append(COMMA);
				b.append(elt.getDateFin()).append(COMMA);
				b.append(elt.getDateDebutMetier()).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.getMotifDebut())).append(COMMA);
				b.append(elt.getDateFinMetierInitiale()).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.getMotifFinInitial())).append(COMMA);
				b.append(elt.getDateFinMetierCorrigee()).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.getMotifFinCorrige()));
				return true;
			}
		});
	}

	private TemporaryFile genererDroitsNonModifies(List<RattraperDatesDebutDroitRFProcessorResults.Untouched> liste, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<RattraperDatesDebutDroitRFProcessorResults.Untouched>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("DROIT_ID").append(COMMA);
				b.append("IMMEUBLE_ID").append(COMMA);
				b.append("EGRID").append(COMMA);
				b.append("DATE_DEBUT_TECHNIQUE").append(COMMA);
				b.append("DATE_FIN_TECHNIQUE").append(COMMA);
				b.append("DATE_DEBUT_METIER").append(COMMA);
				b.append("MOTIF_DEBUT").append(COMMA);
				b.append("DATE_FIN_METIER").append(COMMA);
				b.append("MOTIF_FIN");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RattraperDatesDebutDroitRFProcessorResults.Untouched elt) {
				b.append(elt.getDroitId()).append(COMMA);
				b.append(elt.getImmeubleId()).append(COMMA);
				b.append(elt.getEgrid()).append(COMMA);
				b.append(elt.getDateDebut()).append(COMMA);
				b.append(elt.getDateFin()).append(COMMA);
				b.append(elt.getDateDebutMetier()).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.getMotifDebut())).append(COMMA);
				b.append(elt.getDateFinMetier()).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.getMotifFin()));
				return true;
			}
		});
	}

	private TemporaryFile asCsvFileErreurs(List<RattraperDatesDebutDroitRFProcessorResults.Erreur> erreurs, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(erreurs, filename, status, new CsvHelper.FileFiller<RattraperDatesDebutDroitRFProcessorResults.Erreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("IMMEUBLE_ID").append(COMMA);
				b.append("EGRID").append(COMMA);
				b.append("MESSAGE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RattraperDatesDebutDroitRFProcessorResults.Erreur elt) {
				b.append(elt.getImmeubleId()).append(COMMA);
				b.append(elt.getEgrid()).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.getMessage()));
				return true;
			}
		});
	}
}
