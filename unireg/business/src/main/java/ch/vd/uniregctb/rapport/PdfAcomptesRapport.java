package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.acomptes.AcomptesResults;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.common.StatusManager;

/**
 * Rapport PDF contenant les résultats du rapprochement des ctb et des propriétaires fonciers.
 */
public class PdfAcomptesRapport extends PdfRapport {

	private static final String TYPE_IMPOT_ICC = "ICC";
	private static final String TYPE_IMPOT_IFD = "IFD";

	public void write(final AcomptesResults results, final String nom, final String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport de génération des populations pour les bases acomptes");

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre total traité :", String.valueOf(results.getNombreContribuablesAssujettisTraites()));
					table.addLigne("Nombre total en erreur :", String.valueOf(results.getListeErreurs().size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
				}
			});
		}

		// Contribuables ok
		{
			final String filename = "contribuables_acomptes.csv";
			final String contenu = genererAcomptes(results, filename, status);
			final String titre = "Liste des populations pour les bases acomptes";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, results.getListeContribuablesAssujettis().size(), titre, listVide, filename, contenu);
		}

		// Contribuables en erreurs
		{
			final String filename = "contribuables_acomptes_en_erreur.csv";
			final String contenu = genererErreursAcomptes(results, filename, status);
			final String titre = "Liste des populations pour les bases acomptes en erreur";
			final String listVide = "(aucun contribuable en erreur)";
			addListeDetaillee(writer, results.getListeErreurs().size(), titre, listVide, filename, contenu);
		}

		// contribuables ignorés (for intersectant avec la periode fiscale mais pas d'assujettissement, ou assujettissement ne donnant pas droit aux acomptes)
		{
			final String filename = "contribuables_acomptes_ignorés.csv";
			final String contenu = genererListeIgnoresAcomptes(results, filename, status);
			final String titre = " Liste des populations ignorées ayant un for sur une période fiscale concernée";
			final String listeVide = "(aucun)";
			addListeDetaillee(writer, results.getContribuablesIgnores().size(), titre, listeVide, filename, contenu);
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private String genererAcomptes(AcomptesResults results, String filename, StatusManager status) {

		String contenu = null;
		final List<AcomptesResults.InfoContribuableAssujetti> list = results.getListeContribuablesAssujettis();
		final int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvFile(list, filename, status, new CsvHelper.FileFiller<AcomptesResults.InfoContribuableAssujetti>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de contribuable").append(COMMA);
					b.append("Nom du contribuable principal").append(COMMA);
					b.append("Prénom du contribuable principal").append(COMMA);
					b.append("For principal").append(COMMA);
					b.append("For de gestion").append(COMMA);
					b.append("Type de population").append(COMMA);
					b.append("Type d'impôt").append(COMMA);
					b.append("Période fiscale").append(COMMA);
					b.append("Fors secondaires");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, AcomptesResults.InfoContribuableAssujetti ligne) {
					final String nom = ligne.getNom() != null ? ligne.getNom().trim() : null;
					final String prenom = ligne.getPrenom() != null ? ligne.getPrenom().trim() : null;

					final AcomptesResults.InfoAssujettissementContribuable assujettissementIcc = ligne.getAssujettissementIcc();
					final AcomptesResults.InfoAssujettissementContribuable assujettissementIfd = ligne.getAssujettissementIfd();

					// ICC
					if (assujettissementIcc != null) {
						fillLigneBuffer(b, ligne.getNumeroCtb(), nom, prenom, assujettissementIcc, TYPE_IMPOT_ICC, assujettissementIfd != null);
					}

					// IFD
					if (assujettissementIfd != null) {
						fillLigneBuffer(b, ligne.getNumeroCtb(), nom, prenom, assujettissementIfd, TYPE_IMPOT_IFD, false);
					}

					return assujettissementIcc != null || assujettissementIfd != null;
				}
			});
		}
		return contenu;
	}

	private String genererErreursAcomptes(AcomptesResults results, String filename, StatusManager status) {

		String contenu = null;
		final List<AcomptesResults.Erreur> list = results.getListeErreurs();
		final int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvFile(list, filename, status, new CsvHelper.FileFiller<ListesResults.Erreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de contribuable").append(COMMA);
					b.append("Erreur").append(COMMA);
					b.append("Complément");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, ListesResults.Erreur ligne) {
					b.append(ligne.noCtb).append(COMMA);
					b.append(escapeChars(ligne.getDescriptionRaison())).append(COMMA);
					b.append(escapeChars(ligne.details));
					return true;
				}
			});
		}
		return contenu;
	}

	private String genererListeIgnoresAcomptes(AcomptesResults results, String filename, StatusManager status) {

		String contenu = null;
		final List<AcomptesResults.InfoContribuableIgnore> list = results.getContribuablesIgnores();
		final int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvFile(list, filename, status, new CsvHelper.FileFiller<AcomptesResults.InfoContribuableIgnore>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de contribuable").append(COMMA);
					b.append("Année fiscale").append(COMMA);
					b.append("Complément");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, AcomptesResults.InfoContribuableIgnore ligne) {
					b.append(ligne.getNumeroCtb()).append(COMMA);
					b.append(ligne.getAnneeFiscale()).append(COMMA);
					b.append(escapeChars(ligne.toString()));
					return true;
				}
			});
		}
		return contenu;
	}

	private void fillLigneBuffer(CsvHelper.LineFiller b, long numeroCtb, String nom, String prenom, AcomptesResults.InfoAssujettissementContribuable assujettissement, String typeImpot, boolean withCR) {
		b.append(numeroCtb).append(COMMA);

		b.append(escapeChars(nom)).append(COMMA);
		b.append(escapeChars(prenom)).append(COMMA);

		if (assujettissement.noOfsForPrincipal != null) {
			b.append(assujettissement.noOfsForPrincipal);
		}
		b.append(COMMA);

		if (assujettissement.noOfsForGestion != null) {
			b.append(assujettissement.noOfsForGestion);
		}
		b.append(COMMA);

		if (assujettissement.typeContribuable != null) {
			b.append(assujettissement.typeContribuable.getDisplay());
		}
		b.append(COMMA);

		b.append(typeImpot).append(COMMA);
		b.append(assujettissement.anneeFiscale).append(COMMA);

		if (!assujettissement.ofsForsSecondaires.isEmpty()) {
			final Iterator<Integer> iterator = assujettissement.ofsForsSecondaires.iterator();
			b.append(iterator.next());
			while (iterator.hasNext()) {
				b.append('-').append(iterator.next());
			}
		}

		if (withCR) {
			b.append('\n');
		}
	}

}