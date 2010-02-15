package ch.vd.uniregctb.rapport;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.registrefoncier.ProprietaireRapproche;
import ch.vd.uniregctb.registrefoncier.RapprocherCtbResults;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

/**
 * Rapport PDF contenant les résultats du rapprochement des ctb et des propriétaires fonciers.
 */
public class PdfRapprochementCtbRapport extends PdfRapport{

	/**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    public void write(final RapprocherCtbResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

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
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
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

            addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de contribuables:", String.valueOf(results.nbCtbsTotal));
                    table.addLigne("Nombre d'Individu trouvés avec correspondance exacte:", String.valueOf(results.nbIndividuTrouvesExact));
                    table.addLigne("Nombre d'Individu trouvés avec correspondance sauf date de naissance:", String.valueOf(results.nbIndividuTrouvesSaufDateNaissance));
                    table.addLigne("Nombre d'Individu trouvé sans correspondance exacte:", String.valueOf(results.nbIndividuTrouvesSansCorrespondance));
                    table.addLigne("Pas de contribuable trouvé:", String.valueOf(results.nbCtbInconnu));
                    table.addLigne("Pas d'individu trouvé:", String.valueOf(results.nbIndviduInconnu));
                    table.addLigne("Plus de deux individus trouvés:", String.valueOf(results.nbPlusDeDeuxIndividu));
                    table.addLigne("Nombre d'erreurs:", String.valueOf(results.ctbsEnErrors.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // CTBs rapprochés
        {
            String filename = "contribuables_rapproches.csv";
            String contenu = ctbRapprocheAsCsvFile(results.listeRapproche, filename, status);
            String titre = "Liste des contribuables rapprochés";
            String listVide = "(aucun contribuable rapprocher)";
            addListeDetaillee(writer, results.listeRapproche.size(), titre, listVide, filename, contenu);
        }

     // les erreur
        {
            String filename = "erreurs.csv";
            String contenu = asCsvFile(results.ctbsEnErrors, filename, status);
            String titre = "Liste des erreurs";
            String listVide = "(aucune erreur)";
            addListeDetaillee(writer, results.ctbsEnErrors.size(), titre, listVide, filename, contenu);
        }


        close();

        status.setMessage("Génération du rapport terminée.");
    }

    /**
     * Construit le contenu du fichier détaillé des contribuables rapprochés
     */
	private String ctbRapprocheAsCsvFile(List<ProprietaireRapproche> listeRapprochee, String filename, StatusManager status) {
		String contenu = null;
		int size = listeRapprochee.size();
		if (size > 0) {
			StringBuilder b = new StringBuilder("NumeroFoncier;");
			b.append("Nom;");
			b.append("Prénom;");
			b.append("DateNaissance;");
			b.append("NoCTB;");
			b.append("NoCTB1;");
			b.append("Nom1;");
			b.append("Prénom1;");
			b.append("DateNaissance1;");
			b.append("NoCTB2;");
			b.append("Nom2;");
			b.append("Prénom2;");
			b.append("DateNaissance2;");
			b.append("FormulePolitesse;");
			b.append("NomCourrier1;");
			b.append("NomCourrier2;");
			b.append("Résultat\n");
			for (ProprietaireRapproche proprietaireRapproche : listeRapprochee) {
				b.append(proprietaireRapproche.getNumeroRegistreFoncier() + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getNom()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getPrenom()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getDateNaissance()) + ";");
				b.append(proprietaireRapproche.getNumeroContribuable() + ";");
				b.append(convertNullToEmpty(String.valueOf(proprietaireRapproche.getNumeroContribuable1())) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getNom1()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getPrenom1()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getDateNaissance1()) + ";");
				b.append(convertNullToEmpty(String.valueOf(proprietaireRapproche.getNumeroContribuable2())) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getNom2()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getPrenom2()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getDateNaissance2()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getFormulePolitesse()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getNomCourrier1()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getNomCourrier2()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getResultat()) + ";");
				b.append("\n");
			}
			contenu = b.toString();

		}
		return contenu;
	}
	
	/**
	 * Convertit la valeur null en chaine vide poour une string passée en paramètre
	 *
	 * @param data
	 * @return chaine vide
	 */
	public static String convertNullToEmpty(final String data) {
		String maChaine;
		maChaine = (data == null) ? "" : data;
		return maChaine;

	}
	public static String convertNullToEmpty(final RegDate data) {
		String maChaine;
		maChaine = (data == null) ? "" : data.toString();
		return maChaine;

	}
}
