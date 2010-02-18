package ch.vd.uniregctb.rapport;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.listes.suisseoupermiscresident.ListeContribuablesResidentsSansForVaudoisResults;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

/**
 * Rapport PDF du batch qui liste les contribuables résidents suisses ou permis C sans for vaudois
 */
public class PdfListeContribuablesResidentsSansForVaudoisRapport extends PdfRapport {

	public void write(final ListeContribuablesResidentsSansForVaudoisResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job qui listes les contribuables résidents suisses ou titulaires d'un permis C sans for vaudois");

		// Résultats
		addEntete1("Résultats");
		{
		    if (results.isInterrompu()) {
		        addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
		                + "les valeurs ci-dessous sont donc incomplètes.");
		    }

		    addTableSimple(2, new PdfRapport.TableSimpleCallback() {
		        public void fillTable(PdfTableSimple table) throws DocumentException {
		            table.addLigne("Nombre total de contribuables inspectés :", String.valueOf(results.getNombreContribuablesInspectes()));
		            table.addLigne("Nombre de contribuables identifiés :", String.valueOf(results.getContribuablesIdentifies().size()));
		            table.addLigne("Nombre de contribuables ignorés :", String.valueOf(results.getContribuablesIgnores().size()));
		            table.addLigne("Nombre d'erreurs :", String.valueOf(results.getListeErreurs().size()));
			        table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
		            table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
		        }
		    });
		}

		// contribuables cibles de ce job
		{
		    String filename = "ctbs_identifies.csv";
		    String contenu = buildListeContribuablesIdentifies(results.getContribuablesIdentifies(), filename, status);
		    String titre = "Liste des contribuables identifiés";
		    String listVide = "(aucun)";
		    addListeDetaillee(writer, results.getContribuablesIdentifies().size(), titre, listVide, filename, contenu);
		}

		// contribuables ignorés
		{
		    String filename = "ctbs_ignores.csv";
		    String contenu = buildContribuablesIgnores(results.getContribuablesIgnores(), filename, status);
		    String titre = "Liste des contribuables ignorés";
		    String listVide = "(aucun)";
		    addListeDetaillee(writer, results.getContribuablesIdentifies().size(), titre, listVide, filename, contenu);
		}

		// erreurs
		{
		    String filename = "ctbs_en_erreur.csv";
		    String contenu = buildErreurs(results.getListeErreurs(), filename, status);
		    String titre = "Liste des erreurs";
		    String listVide = "(aucune)";
		    addListeDetaillee(writer, results.getListeErreurs().size(), titre, listVide, filename, contenu);
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private String buildListeContribuablesIdentifies(List<Long> ctbIds, String filename, StatusManager status) {

		String contenu = null;
		if (ctbIds.size() > 0) {
			final StringBuilder b = new StringBuilder((ctbIds.size() + 1 ) * 10);
			b.append("Numéro\n");

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);
			final GentilIterator<Long> iterator = new GentilIterator<Long>(ctbIds);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					status.setMessage(message, iterator.getPercent());
				}
				b.append(iterator.next());
				if (!iterator.isLast()) {
					b.append("\n");
				}
			}
			contenu = b.toString();
		}
		return contenu;
	}

	private String buildContribuablesIgnores(List<ListeContribuablesResidentsSansForVaudoisResults.InfoContribuableIgnore> liste, String filename, StatusManager status) {

		String contenu = null;
		if (liste.size() > 0) {
			final StringBuilder b = new StringBuilder((liste.size() + 1 ) * 50);
			b.append("Numéro" + COMMA + "Raison\n");

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);
			final GentilIterator<ListeContribuablesResidentsSansForVaudoisResults.InfoContribuableIgnore> iterator = new GentilIterator<ListeContribuablesResidentsSansForVaudoisResults.InfoContribuableIgnore>(liste);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					status.setMessage(message, iterator.getPercent());
				}

				final ListeContribuablesResidentsSansForVaudoisResults.InfoContribuableIgnore info = iterator.next();
				b.append(info.ctbId).append(COMMA);
				b.append(escapeChars(info.cause.getDescription()));
				if (!iterator.isLast()) {
					b.append("\n");
				}
			}
			contenu = b.toString();
		}
		return contenu;
	}

	private String buildErreurs(List<ListesResults.Erreur> liste, String filename, StatusManager status) {

		String contenu = null;
		if (liste.size() > 0) {
			final StringBuilder b = new StringBuilder((liste.size() + 1 ) * 100);
			b.append("Numéro" + COMMA + "Raison" + COMMA + "Complément\n");

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);
			final GentilIterator<ListesResults.Erreur> iterator = new GentilIterator<ListesResults.Erreur>(liste);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					status.setMessage(message, iterator.getPercent());
				}

				final ListesResults.Erreur ligne = iterator.next();
				b.append(ligne.noCtb).append(COMMA);
				b.append(escapeChars(ligne.getDescriptionRaison())).append(COMMA);
				b.append(escapeChars(ligne.details));
				if (!iterator.isLast()) {
					b.append("\n");
				}
			}
			contenu = b.toString();
		}
		return contenu;
	}
}
