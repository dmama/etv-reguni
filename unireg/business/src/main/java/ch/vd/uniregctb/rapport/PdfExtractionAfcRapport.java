package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.ListMergerIterator;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.listes.afc.ExtractionAfcResults;
import ch.vd.uniregctb.listes.afc.TypeExtractionAfc;

/**
 * Générateur du rapport PDF d'exécution du batch d'extraction des listes AFC
 */
public class PdfExtractionAfcRapport extends PdfRapport {

	public void write(final ExtractionAfcResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport de génération des populations AFC");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
			    public void fillTable(PdfTableSimple table) throws DocumentException {
			        table.addLigne("Période fiscale :", String.valueOf(results.periodeFiscale));
			        table.addLigne("Mode d'extraction :", results.mode.getDescription());
				    table.addLigne("Nombre de threads :", String.valueOf(results.getNombreThreads()));
			        table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
			    }
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de contribuables inspectés :", String.valueOf(results.getNombreCtbAnalyses()));
					table.addLigne("Contribuables ignorés :", String.valueOf(results.getListeCtbsIgnores().size()));
					if (results.mode != TypeExtractionAfc.FORTUNE) {
						table.addLigne("Contribuables listés :", String.valueOf(results.getListePrincipale().size()));
					}
					else {
						table.addLigne("Contribuables 'illimités' : ", String.valueOf(results.getListePrincipale().size()));
						table.addLigne("Contribuables 'HC/HS' : ", String.valueOf(results.getListeSecondaire().size()));
					}

					table.addLigne("Contribuables en erreur :", String.valueOf(results.getListeErreurs().size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
				}
			});
		}

		// cas REVENU : une seule liste reprenant la liste principale
		// cas FORTUNE : une seule liste reprenant les listes principales et secondaires
		if (results.mode == TypeExtractionAfc.FORTUNE) {

			final String filename = String.format("contribuables_afc_fortune_%d.csv", results.periodeFiscale);
			final String contenu = genererListeCombineeFortune(results, filename, status);
			final String titre = "Liste des contribuables";
			final String listeVide = "(aucun)";
			final int taille = results.getListePrincipale().size() + results.getListeSecondaire().size();
			addListeDetaillee(writer, taille, titre, listeVide, filename, contenu);
		}
		else {

			// Contribuables de la liste principale
			{
				final String filename = String.format("contribuables_afc_revenu_%d.csv", results.periodeFiscale);
				final String contenu = genererListe(results, true, filename, status);
				final String titre = "Listes des contribuables";
				final String listVide = "(aucun)";
				addListeDetaillee(writer, results.getListePrincipale().size(), titre, listVide, filename, contenu);
			}

			// Contribuables de la liste secondaire (au cas où...)
			if (results.getListeSecondaire().size() > 0) {
				final String filename = String.format("contribuables_afc_revenu_%d_bis.csv", results.periodeFiscale);
				final String contenu = genererListe(results, false, filename, status);
				final String titre = "Listes bis des contribuables";
				final String listVide = "(aucun)";
				addListeDetaillee(writer, results.getListeSecondaire().size(), titre, listVide, filename, contenu);
			}
		}

		// Contribuables en erreurs
		{
			final String filename = "contribuables_en_erreur.csv";
			final String contenu = genererListeErreurs(results, filename, status);
			final String titre = "Liste des contribuables en erreur";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, results.getListeErreurs().size(), titre, listVide, filename, contenu);
		}

		// contribuables ignorés (for intersectant avec la periode fiscale mais pas d'assujettissement, ou assujettissement ne donnant pas droit aux acomptes)
		{
			final String filename = "contribuables_ignorés.csv";
			final String contenu = genererListeIgnores(results, filename, status);
			final String titre = " Liste des contribuables ignorés ayant un for sur la période fiscale concernée";
			final String listeVide = "(aucun)";
			addListeDetaillee(writer, results.getListeCtbsIgnores().size(), titre, listeVide, filename, contenu);
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private String genererListeIgnores(ExtractionAfcResults results, String filename, StatusManager status) {

		String contenu = null;
		final List<ExtractionAfcResults.InfoCtbIgnore> liste = results.getListeCtbsIgnores();
		if (liste.size() > 0) {

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);

			final StringBuilder b = new StringBuilder();
			b.append("NUMERO_CTB").append(COMMA).append("RAISON").append("\n");

			final GentilIterator<ExtractionAfcResults.InfoCtbIgnore> iterator = new GentilIterator<ExtractionAfcResults.InfoCtbIgnore>(liste);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					status.setMessage(message, iterator.getPercent());
				}
				final ExtractionAfcResults.InfoCtbIgnore info = iterator.next();
				b.append(info.noCtb).append(COMMA);
				b.append(escapeChars(info.raisonIgnore)).append("\n");
			}

			contenu = b.toString();
		}
		return contenu;

	}

	private String genererListeErreurs(ExtractionAfcResults results, String filename, StatusManager status) {
		String contenu = null;
		final List<ExtractionAfcResults.Erreur> liste = results.getListeErreurs();
		if (liste.size() > 0) {

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);

			final StringBuilder b = new StringBuilder();
			b.append("NUMERO_CTB").append(COMMA).append("ERREUR").append(COMMA).append("DESCRIPTION").append("\n");

			final GentilIterator<ExtractionAfcResults.Erreur> iterator = new GentilIterator<ExtractionAfcResults.Erreur>(liste);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					status.setMessage(message, iterator.getPercent());
				}
				final ExtractionAfcResults.Erreur erreur = iterator.next();
				b.append(erreur.noCtb).append(COMMA);
				b.append(escapeChars(erreur.getDescriptionRaison())).append(COMMA);
				b.append(escapeChars(erreur.details)).append("\n");
			}

			contenu = b.toString();
		}
		return contenu;
	}

	private String genererListeCombineeFortune(ExtractionAfcResults results, String filename, StatusManager status) {

		Assert.isTrue(results.mode == TypeExtractionAfc.FORTUNE);

		String contenu = null;
		final List<ExtractionAfcResults.InfoCtbListe> listeDesIllimites = results.getListePrincipale();
		final List<ExtractionAfcResults.InfoCtbListe> listeDesLimites = results.getListeSecondaire();
		final int taille = listeDesIllimites.size() + listeDesLimites.size();
		if (taille > 0) {

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);

			final StringBuilder b = new StringBuilder();
			b.append("NUMERO_CTB").append(COMMA).append("NOM_CTB_PRINCIPAL").append(COMMA).append("OFS_FOR_GESTION").append(COMMA).append("CODE_ASSUJETTISSEMENT").append("\n");

			// comparateur exclusivement sur le numéro de contribuable
			final Comparator<ExtractionAfcResults.InfoCtbListe> comparator = new ExtractionAfcResults.InfoComparator<ExtractionAfcResults.InfoCtbListe>();
			final Iterator<ExtractionAfcResults.InfoCtbListe> mergerIterator = new ListMergerIterator<ExtractionAfcResults.InfoCtbListe>(listeDesIllimites, listeDesLimites, comparator);
			final GentilIterator<ExtractionAfcResults.InfoCtbListe> iter = new GentilIterator<ExtractionAfcResults.InfoCtbListe>(mergerIterator, taille);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					status.setMessage(message, iter.getPercent());
				}
				final ExtractionAfcResults.InfoCtbListe info = iter.next();
				b.append(info.noCtb).append(COMMA);
				b.append(escapeChars(info.nomPrenom)).append(COMMA);
				b.append(info.ofsCommuneForGestion).append(COMMA);
				b.append(info.assujettissementIllimite ? "I" : "L").append("\n");
			}

			contenu = b.toString();
		}
		return contenu;
	}

	private String genererListe(ExtractionAfcResults results, boolean listePrincipale, String filename, StatusManager status) {
		String contenu = null;
		final List<ExtractionAfcResults.InfoCtbListe> liste = (listePrincipale ? results.getListePrincipale() : results.getListeSecondaire());
		if (liste.size() > 0) {

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);

			final StringBuilder b = new StringBuilder();
			b.append("NUMERO_CTB").append(COMMA).append("NOM_CTB_PRINCIPAL").append(COMMA).append("OFS_FOR_GESTION").append("\n");

			final GentilIterator<ExtractionAfcResults.InfoCtbListe> iterator = new GentilIterator<ExtractionAfcResults.InfoCtbListe>(liste);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					status.setMessage(message, iterator.getPercent());
				}
				final ExtractionAfcResults.InfoCtbListe info = iterator.next();
				b.append(info.noCtb).append(COMMA);
				b.append(escapeChars(info.nomPrenom)).append(COMMA);
				b.append(info.ofsCommuneForGestion).append("\n");
			}

			contenu = b.toString();
		}
		return contenu;
	}
}
