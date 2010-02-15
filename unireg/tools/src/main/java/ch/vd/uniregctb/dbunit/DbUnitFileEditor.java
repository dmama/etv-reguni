package ch.vd.uniregctb.dbunit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.dbunit.dataset.CachedDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.XmlDataSet;
import org.dbunit.dataset.xml.XmlProducer;
import org.xml.sax.InputSource;

/**
 * Ce programme permet de modifier en masse la structure des fichiers DBUnit.
 * <p>
 * Au vu de la complexité de modifications à effectuer, le développeur doit coder la modification désirée en dur et exécuter le programme
 * sur le fichier DBUnit désiré.
 * <p>
 * En cas de modification de masse, il est nécessaire de :
 * <ul>
 * <li>compiler: mvn install</li>
 * <li>assembler: mvn assembly:assembly</li>
 * <li>copier le zip généré dans un répertoire de travail</li>
 * <li>exécuter le script dbuniteditor.sh le nombre de fois voulue en passant le nom du fichier en paramètre</li>
 * </ul>
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 *
 */
public class DbUnitFileEditor {

	/**
	 * Paramètre : le nom du fichier DBUnit à modifier
	 */
	public static void traiteFichier(String fileName) throws Exception {

		System.out.print("Traitement du fichier " + fileName + " ...");

		// Lit le fichier d'entrée
		XmlProducer producer = new XmlProducer(new InputSource(fileName));
		IDataSet inputDataSet = new CachedDataSet(producer);

		// ~~~~~~~ modifier ici ~~~~~~~

		// supprime la colonne CTB_DPI_ID sur le table TIERS
		//IDataSet outputDataSet = new DropColumnDataSet(inputDataSet, "CTB_DPI_ID");
		IDataSet outputDataSet = new ModifPPDataSet(inputDataSet);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// Ecrit le fichier de sortie
		String outputFile = fileName; // on reécrit le fichier d'entrée
		OutputStream output = new FileOutputStream(outputFile);
		XmlDataSet.write(outputDataSet, output);
		output.close();

		System.out.println("done");
	}
	
	public static void main(String[] args) throws Exception {
		String repBase = "C:/projets/registre/branches/UniregCTB/2.7/04-Implementation/unireg/";
		String endPath = "/src/test/resources/ch/vd/uniregctb/";
		String repbus = repBase + "business" + endPath;
		String repbusit = repBase + "business-it" + endPath;
		String repcore = repBase + "core" + endPath;
		String repmigreg = repBase + "migreg" + endPath;
		String repnorentes = repBase + "norentes" + endPath;
		String reptesting = repBase + "testing" + endPath;
		String repweb = repBase + "web" + endPath;
		String repwebit = repBase + "web-it" + endPath;
		
		List<String> listeRep = new ArrayList<String>();
		listeRep.add(repbus);
		listeRep.add(repbusit);
		listeRep.add(repcore);
		listeRep.add(repmigreg);
		listeRep.add(repnorentes);
		listeRep.add(reptesting);
		listeRep.add(repweb);
		listeRep.add(repwebit);
		
		for(String rep :listeRep){
			File repertoire = new File(rep);
			List<String> listFile = findFile(repertoire);
			for (String file : listFile) {
				try {
					traiteFichier(file);
				} catch (Exception e) {
					System.out.println("erreur exception : " + e.getMessage());
				}
			}
		}
		try {
			traiteFichier(repBase + "web/src/main/resources/DBUnit4Import/tiers-basic.xml");
		} catch (Exception e) {
			System.out.println("erreur exception : " + e.getMessage());
		}
	}
	
	static private List<String> findFile(File rep){
		List<String> listFile = new ArrayList<String>();
		if (rep.isDirectory()) {
            File[] list = rep.listFiles();
            if (list != null){
                for ( int i = 0; i < list.length; i++) {
                        // Appel récursif sur les sous-répertoires
                	listFile.addAll(findFile( list[i]));
                } 
            }
		}
		else {
			if (rep.getName().endsWith("Test.xml") || rep.getName().endsWith("Test2.xml")) {
				//on ignore 2 fichiers d'événement civil jms
				if (!rep.getName().equals("EvenementCivilUnitaireMDPTest.xml") && 
					!rep.getName().equals("EvenementCivilImportControllerTest.xml")) {
					listFile.add(rep.getAbsolutePath());
				}
			}
		}
		return listFile;
	}
}
