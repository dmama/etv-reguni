package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.registrefoncier.ProprietaireRapproche;
import ch.vd.uniregctb.registrefoncier.RapprocherCtbResults;

/**
 * Rapport PDF contenant les résultats du rapprochement des ctb et des propriétaires fonciers.
 */
public class PdfRapprochementCtbRapport extends PdfRapport {

	/**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    public void write(final RapprocherCtbResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfWriter writer = PdfWriter.getInstance(this, os);
        open();
        addMetaInfo(nom, description);
        addEnteteUnireg();

        // Titre
        addTitrePrincipal("Rapport d'exécution du rapprochement des contribuables et des propriétaires fonciers");

        // Paramètres
        addEntete1("Paramètres");
        {
            addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                @Override
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.dateTraitement));
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

            addTableSimple(new float[] {70f, 30f}, new PdfRapport.TableSimpleCallback() {
                @Override
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de contribuables inspectés", String.valueOf(results.nbCtbsTotal));

	                final Map<ProprietaireRapproche.CodeRetour, Integer> stats = results.getStats();
                    table.addLigne("Individus trouvés avec correspondance exacte :", String.valueOf(stats.get(ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_EXACT)));
	                table.addLigne("Individus trouvés avec correspondance sauf date de naissance :", String.valueOf(stats.get(ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_EXACT_SAUF_NAISSANCE)));
                    table.addLigne("Individus trouvés sans correspondance exacte :", String.valueOf(stats.get(ProprietaireRapproche.CodeRetour.INDIVIDUS_TROUVE_NON_EXACT) + stats.get(ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_NON_EXACT)));
                    table.addLigne("Pas de contribuable trouvé :", String.valueOf(stats.get(ProprietaireRapproche.CodeRetour.CTB_NON_TROUVE)));
                    table.addLigne("Pas d'individu trouvé :", String.valueOf(stats.get(ProprietaireRapproche.CodeRetour.INDIVIDU_NON_TROUVE)));
                    table.addLigne("Plus de deux individus trouvés :", String.valueOf(stats.get(ProprietaireRapproche.CodeRetour.PLUS_DE_DEUX_INDIV_TROUVES)));
                    table.addLigne("Nombre d'erreurs :", String.valueOf(results.ctbsEnErreur.size()));
	                table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
                }
            });
        }

        // CTBs rapprochés
        {
            final String filename = "contribuables_rapproches.csv";
            final String contenu = ctbRapprocheAsCsvFile(results.listeRapproche, filename, status);
            final String titre = "Liste des contribuables rapprochés";
            final String listVide = "(aucun)";
            addListeDetaillee(writer, results.listeRapproche.size(), titre, listVide, filename, contenu);
        }

        // les erreurs
        {
            final String filename = "erreurs.csv";
            final String contenu = asCsvFile(results.ctbsEnErreur, filename, status);
            final String titre = "Liste des erreurs";
            final String listVide = "(aucune)";
            addListeDetaillee(writer, results.ctbsEnErreur.size(), titre, listVide, filename, contenu);
        }

        close();
        status.setMessage("Génération du rapport terminée.");
    }

    /**
     * Construit le contenu du fichier détaillé des contribuables rapprochés
     */
	private String ctbRapprocheAsCsvFile(List<ProprietaireRapproche> listeRapprochee, String filename, StatusManager status) {
		return CsvHelper.asCsvFile(listeRapprochee, filename, status, 100, new CsvHelper.Filler<ProprietaireRapproche>() {
			@Override
			public void fillHeader(StringBuilder b) {
				b.append("NumeroFoncier").append(COMMA);
				b.append("Nom").append(COMMA);
				b.append("Prénom").append(COMMA);
				b.append("DateNaissance").append(COMMA);
				b.append("NoCTB").append(COMMA);
				b.append("NoCTB1").append(COMMA);
				b.append("Nom1").append(COMMA);
				b.append("Prénom1").append(COMMA);
				b.append("DateNaissance1").append(COMMA);
				b.append("NoCTB2").append(COMMA);
				b.append("Nom2").append(COMMA);
				b.append("Prénom2").append(COMMA);
				b.append("DateNaissance2").append(COMMA);
				b.append("FormulePolitesse").append(COMMA);
				b.append("NomCourrier1").append(COMMA);
				b.append("NomCourrier2").append(COMMA);
				b.append("Résultat");
			}

			@Override
			public void fillLine(StringBuilder b, ProprietaireRapproche elt) {
				b.append(elt.getNumeroRegistreFoncier()).append(COMMA);
				b.append(convertNullToEmpty(elt.getNom())).append(COMMA);
				b.append(convertNullToEmpty(elt.getPrenom())).append(COMMA);
				b.append(convertNullToEmpty(elt.getDateNaissance())).append(COMMA);
				b.append(convertNullToEmpty(elt.getNumeroContribuable())).append(COMMA);
				b.append(convertNullToEmpty(elt.getNumeroContribuable1())).append(COMMA);
				b.append(convertNullToEmpty(elt.getNom1())).append(COMMA);
				b.append(convertNullToEmpty(elt.getPrenom1())).append(COMMA);
				b.append(convertNullToEmpty(elt.getDateNaissance1())).append(COMMA);
				b.append(convertNullToEmpty(elt.getNumeroContribuable2())).append(COMMA);
				b.append(convertNullToEmpty(elt.getNom2())).append(COMMA);
				b.append(convertNullToEmpty(elt.getPrenom2())).append(COMMA);
				b.append(convertNullToEmpty(elt.getDateNaissance2())).append(COMMA);
				b.append(convertNullToEmpty(elt.getFormulePolitesse())).append(COMMA);
				b.append(convertNullToEmpty(elt.getNomCourrier1())).append(COMMA);
				b.append(convertNullToEmpty(elt.getNomCourrier2())).append(COMMA);
				b.append(elt.getResultat().getValeur()).append(COMMA);
			}
		});
	}
	
	/**
	 * Convertit la valeur null en chaine vide poour une string passée en paramètre
	 *
	 * @param data
	 * @return chaine vide
	 */
	private static String convertNullToEmpty(String data) {
		return StringUtils.trimToEmpty(data);
	}

	private static String convertNullToEmpty(RegDate data) {
		return data == null ? StringUtils.EMPTY : data.toString();
	}

	private static String convertNullToEmpty(Long data) {
		return data == null ? StringUtils.EMPTY : data.toString();
	}
}
