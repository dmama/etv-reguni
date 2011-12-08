package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.listes.listesnominatives.ListesNominativesResults;
import ch.vd.uniregctb.listes.listesnominatives.TypeAdresse;

/**
 * Rapport PDF contenant les résultats du rapprochement des ctb et des propriétaires fonciers.
 */
public class PdfListesNominativesRapport extends PdfRapport{

	public void write(final ListesNominativesResults results, final String nom, final String description, final Date dateGeneration,
	                      OutputStream os, StatusManager status) throws Exception {

	    Assert.notNull(status);

	    // Création du document PDF
	    PdfWriter writer = PdfWriter.getInstance(this, os);
	    open();
	    addMetaInfo(nom, description);
	    addEnteteUnireg();

	    // Titre
	    addTitrePrincipal("Rapport de génération des listes nominatives");

	    // Paramètres
	    addEntete1("Paramètre");
	    {
	        addTableSimple(2, new PdfRapport.TableSimpleCallback() {
	            @Override
	            public void fillTable(PdfTableSimple table) throws DocumentException {
		            table.addLigne("Type d'adresses :", String.valueOf(results.getTypeAdressesIncluses().getDescription()));
			        table.addLigne("Inclure les personnes physiques / ménages :", String.valueOf(results.isAvecContribuablesPP()));
			        table.addLigne("Inclure les personnes morales :", String.valueOf(results.isAvecContribuablesPM()));
			        table.addLigne("Inclure les débiteurs de prestations imposables :", String.valueOf(results.isAvecDebiteurs()));
			        table.addLigne("Nombre de threads :", String.valueOf(results.getNombreThreads()));
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
	                table.addLigne("Nombre total de tiers listés :", String.valueOf(results.getNombreTiersTraites()));
	                table.addLigne("Dont tiers en erreur :", String.valueOf(results.getListeErreurs().size()));
		            table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
	                table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
	            }
	        });
	    }

	    // Contribuables ok
	    {
	        final String filename = "tiers.csv";
	        final String contenu = genererListesNominatives(results, filename, status);
	        final String titre = "Liste des tiers";
	        final String listVide = "(aucun)";
	        addListeDetaillee(writer, results.getListeTiers().size(), titre, listVide, filename, contenu);
	    }

	    // Contribuables en erreurs
	    {
	        final String filename = "tiers_en_erreur.csv";
	        final String contenu = genererErreursListesNominatives(results, filename, status);
	        final String titre = "Liste des tiers en erreur";
	        final String listVide = "(aucun)";
	        addListeDetaillee(writer, results.getListeErreurs().size(), titre, listVide, filename, contenu);
	    }

	    close();

	    status.setMessage("Génération du rapport terminée.");
	}
	
    private String genererListesNominatives(final ListesNominativesResults results, String filename, StatusManager status) {

        String contenu = null;
        final List<ListesNominativesResults.InfoTiers> list = results.getListeTiers();
        final int size = list.size();
        if (size > 0) {
	        contenu = CsvHelper.asCsvFile(list, filename, status, new CsvHelper.FileFiller<ListesNominativesResults.InfoTiers>() {
		        @Override
		        public void fillHeader(CsvHelper.LineFiller b) {
			        b.append("NUMERO_CTB").append(COMMA);
			        b.append("NOM_1").append(COMMA);
			        b.append("NOM_2");
			        if (results.getTypeAdressesIncluses() == TypeAdresse.FORMATTEE) {
				        b.append(COMMA).append("ADRESSE_1").append(COMMA).append("ADRESSE_2").append(COMMA).append("ADRESSE_3");
				        b.append(COMMA).append("ADRESSE_4").append(COMMA).append("ADRESSE_5").append(COMMA).append("ADRESSE_6");
			        }
			        else if (results.getTypeAdressesIncluses() == TypeAdresse.STRUCTUREE_RF) {
				        b.append(COMMA).append("RUE").append(COMMA).append("NPA");
				        b.append(COMMA).append("LOCALITE").append(COMMA).append("PAYS");
			        }
		        }

		        @Override
		        public boolean fillLine(CsvHelper.LineFiller b, ListesNominativesResults.InfoTiers ligne) {
			        b.append(Long.toString(ligne.numeroTiers)).append(COMMA);
			        b.append(escapeChars(ligne.nomPrenom1)).append(COMMA);
			        b.append(escapeChars(ligne.nomPrenom2));

			        if (results.getTypeAdressesIncluses() == TypeAdresse.FORMATTEE) {
				        Assert.isTrue(ligne instanceof ListesNominativesResults.InfoTiersAvecAdresseFormattee);
				        final ListesNominativesResults.InfoTiersAvecAdresseFormattee ligneAvecAdresse = (ListesNominativesResults.InfoTiersAvecAdresseFormattee) ligne;
				        final String[] adresse = ligneAvecAdresse.adresse;
				        for (int indexLigne = 0; indexLigne < adresse.length; ++indexLigne) {
					        b.append(COMMA);
					        b.append(escapeChars(adresse[indexLigne]));
				        }
			        }
			        else if (results.getTypeAdressesIncluses() == TypeAdresse.STRUCTUREE_RF) {
				        Assert.isTrue(ligne instanceof ListesNominativesResults.InfoTiersAvecAdresseStructureeRF);
				        final ListesNominativesResults.InfoTiersAvecAdresseStructureeRF ligneAvecAdresse = (ListesNominativesResults.InfoTiersAvecAdresseStructureeRF) ligne;
				        b.append(COMMA);
				        b.append(escapeChars(ligneAvecAdresse.rue)).append(COMMA);
				        b.append(escapeChars(ligneAvecAdresse.npa)).append(COMMA);
				        b.append(escapeChars(ligneAvecAdresse.localite)).append(COMMA);
				        b.append(escapeChars(ligneAvecAdresse.pays));
			        }

			        return true;
		        }
	        });
        }
        return contenu;
    }

	private String genererErreursListesNominatives(ListesNominativesResults results, String filename, StatusManager status) {

	    String contenu = null;
	    final List<ListesNominativesResults.Erreur> list = results.getListeErreurs();
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
}