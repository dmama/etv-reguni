package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionChemisesTOResults;

/**
 * Rapport PDF contenant les résultats du rapprochement des ctb et des propriétaires fonciers.
 */
public class PdfImpressionChemisesTORapport extends PdfRapport{

	public void write(final ImpressionChemisesTOResults results, String nom, String description, final Date dateGeneration,
	                      OutputStream os, StatusManager status) throws Exception {

	    Assert.notNull(status);

	    // Création du document PDF
	    PdfWriter writer = PdfWriter.getInstance(this, os);
	    open();
	    addMetaInfo(nom, description);
	    addEnteteUnireg();

	    // Titre
	    addTitrePrincipal("Rapport d'impression des chemises de taxation d'office");

	    // Paramètres
	    addEntete1("Paramètres");
	    {
	        addTableSimple(2, new PdfRapport.TableSimpleCallback() {
	            @Override
	            public void fillTable(PdfTableSimple table) throws DocumentException {
	                table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
	                table.addLigne("Nombre maximal de chemises à imprimer :", Integer.toString(results.getNbMax()));
	                table.addLigne("Office d'impôt : ", results.getNomOid() == null ? "tous" : results.getNomOid());
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
	            @Override
	            public void fillTable(PdfTableSimple table) throws DocumentException {
	                table.addLigne("Nombre total d'impressions :", Integer.toString(results.getNbChemisesImprimees()));
	                table.addLigne("Nombre total d'erreurs :", Integer.toString(results.getErreurs().size()));
		            table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
	                table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
	            }
	        });
	    }

	    // Impressions OK
	    {
	        final String filename = "chemises_to_imprimees.csv";
	        final String contenu = genererListeChemisesTO(results, filename, status);
	        final String titre = "Liste des chemises TO imprimées";
	        final String listVide = "(aucun)";
	        addListeDetaillee(writer, results.getNbChemisesImprimees(), titre, listVide, filename, contenu);
	    }

	    // Impressions en erreur
	    {
	        final String filename = "chemises_to_erreurs.csv";
	        final String contenu = genererErreursChemisesTO(results, filename, status);
	        final String titre = "Liste des déclarations d'impôt en erreur";
	        final String listVide = "(aucune)";
	        addListeDetaillee(writer, results.getErreurs().size(), titre, listVide, filename, contenu);
	    }

	    close();

	    status.setMessage("Génération du rapport terminée.");
	}
	

	private String genererErreursChemisesTO(ImpressionChemisesTOResults results, String filename, StatusManager status) {
	    String contenu = null;
		final List<ImpressionChemisesTOResults.Erreur> list = results.getErreurs();
		final int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvFile(list, filename, status, new CsvHelper.FileFiller<ImpressionChemisesTOResults.Erreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("ID_DECLARATION").append(COMMA).append("MESSAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, ImpressionChemisesTOResults.Erreur elt) {
					b.append(elt.getIdDeclaration()).append(COMMA);
					b.append(asCsvField(elt.getDetails()));
					return true;
				}
			});
		}
	    return contenu;
	}

	private String genererListeChemisesTO(ImpressionChemisesTOResults results, String filename, StatusManager status) {
	    String contenu = null;
	    final List<ImpressionChemisesTOResults.ChemiseTO> list = results.getChemisesImprimees();
	    final int size = list.size();
	    if (size > 0) {
		    contenu = CsvHelper.asCsvFile(list, filename, status, new CsvHelper.FileFiller<ImpressionChemisesTOResults.ChemiseTO>() {
			    @Override
			    public void fillHeader(CsvHelper.LineFiller b) {
				    b.append("OID").append(COMMA);
				    b.append("NO_CTB").append(COMMA);
				    b.append("NOM_CTB").append(COMMA);
				    b.append("DATE_DEBUT_PERIODE").append(COMMA);
				    b.append("DATE_FIN_PERIODE").append(COMMA);
				    b.append("DATE_SOMMATION");
			    }

			    @Override
			    public boolean fillLine(CsvHelper.LineFiller b, ImpressionChemisesTOResults.ChemiseTO elt) {
				    b.append(elt.officeImpotID != null ? elt.officeImpotID : EMPTY).append(COMMA);
				    b.append(elt.noCtb).append(COMMA);
					b.append(escapeChars(elt.nomCtb)).append(COMMA);
				    b.append(elt.getDateDebutDi()).append(COMMA);
				    b.append(elt.getDateFinDi()).append(COMMA);
				    b.append(elt.getDateSommationDi());
				    return true;
			    }
		    });
	    }
	    return contenu;
	}
}