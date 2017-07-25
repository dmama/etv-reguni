package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.listes.afc.pm.ExtractionDonneesRptPMResults;

/**
 * Générateur du rapport PDF d'exécution du batch d'extraction des listes des données de référence RPT
 */
public class PdfExtractionDonneesRptPMRapport extends PdfRapport {

	public void write(final ExtractionDonneesRptPMResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport de génération des populations des données de référence RPT");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
			    table.addLigne("Période fiscale :", String.valueOf(results.periodeFiscale));
			    table.addLigne("Mode d'extraction :", results.getMode().getDescription());
				table.addLigne("Nombre de threads :", String.valueOf(results.getNombreThreads()));
			    table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Nombre de contribuables inspectés :", String.valueOf(results.getNombreCtbAnalyses()));
				table.addLigne("Contribuables ignorés :", String.valueOf(results.getListeCtbsIgnores().size()));
				table.addLigne("Contribuables avec Décision ACI :", String.valueOf(results.getListeCtbDecisionACI().size()));
				table.addLigne("Contribuables en erreur :", String.valueOf(results.getListeErreurs().size()));
				table.addLigne("Nombre de périodes trouvées :", String.valueOf(results.getListePeriode().size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Répartition par modes d'imposition
	/*	addEntete1("Répartition des périodes trouvées");
		{
			addTableSimple(2, table -> {
				final Map<ModeImposition, Integer> decoupage = results.get();
				for (ModeImposition modeImposition : ModeImposition.values()) {
					final Integer nb = decoupage.get(modeImposition);
					if (nb != null && nb > 0) {
						table.addLigne(modeImposition.texte(), String.valueOf(nb));
					}
				}
			});
		}*/

		{
			final String filename = String.format("donnees_rpt_%s_%d.csv", results.getMode().name().toLowerCase(), results.periodeFiscale);
			final String titre = "Liste des périodes";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = genererListePeriodes(results, filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// Contribuables en erreurs
		{
			final String filename = "contribuables_en_erreur.csv";
			final String titre = "Liste des contribuables en erreur";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = genererListeErreurs(results, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// contribuables ignorés (for intersectant avec la periode fiscale mais pas d'assujettissement, ou assujettissement ne donnant pas droit aux acomptes)
		{
			final String filename = "contribuables_ignorés.csv";
			final String titre = " Liste des contribuables ignorés ayant un for sur la période fiscale concernée";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = genererListeIgnores(results, filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// contribuables Avec décisions ACI
		{
			final String filename = "contribuables_decision_aci.csv";
			final String titre = " Liste des contribuables possédant une décision ACI sur la période N ou N-1";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = genererListeDecisions(results, filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile genererListeErreurs(ExtractionDonneesRptPMResults results, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		final List<ExtractionDonneesRptPMResults.Erreur> liste = results.getListeErreurs();
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<ListesResults.Erreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NUMERO_CTB").append(COMMA);
					b.append("ERREUR").append(COMMA);
					b.append("DESCRIPTION");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, ListesResults.Erreur erreur) {
					b.append(erreur.noCtb).append(COMMA);
					b.append(escapeChars(erreur.getDescriptionRaison())).append(COMMA);
					b.append(escapeChars(erreur.details));
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile genererListeIgnores(ExtractionDonneesRptPMResults results, String filename, StatusManager status) {
		return genererListe(results.getListeCtbsIgnores(), filename, status);
	}

	private TemporaryFile genererListePeriodes(ExtractionDonneesRptPMResults results, String filename, StatusManager status) {
		return genererListe(results.getListePeriode(), filename, status);
	}

	private TemporaryFile genererListeDecisions(ExtractionDonneesRptPMResults results, String filename, StatusManager status) {
		return genererListe(results.getListeCtbDecisionACI(), filename, status);
	}

	private <T extends ExtractionDonneesRptPMResults.InfoCtbBase> TemporaryFile genererListe(List<T> liste, String filename, StatusManager status) {
		final String[] nomsColonnes = (!liste.isEmpty() ? liste.get(0).getNomsColonnes() : null);
		return CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<T>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				if (nomsColonnes != null) {
					for (int i = 0 ; i < nomsColonnes.length ; ++ i) {
						if (i > 0) {
							b.append(COMMA);
						}
						b.append(nomsColonnes[i]);
					}
				}
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, T elt) {
				final Object[] values = elt.getValeursColonnes();
				for (int i = 0 ; i < values.length ; ++ i) {
					if (i > 0) {
						b.append(COMMA);
					}
					b.append(getDisplayValue(values[i]));
				}
				return values.length > 0;
			}
		});
	}

	private static String getDisplayValue(Object elt) {
		if (elt == null) {
			return EMPTY;
		}
		else if (elt instanceof RegDate) {
			return RegDateHelper.dateToDisplayString((RegDate) elt);
		}
		else if (elt instanceof String) {
			return CsvHelper.escapeChars((String) elt);
		}
		else {
			return elt.toString();
		}
	}
}