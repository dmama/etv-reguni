package ch.vd.uniregctb.editique.print;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Test;
import org.springframework.util.ResourceUtils;

import ch.vd.securite.model.impl.ProfilOperateurImpl;
import ch.vd.uniregctb.common.BusinessTest;

public class PrintPCLServiceTest extends BusinessTest {

	private final static String PCL_SAMPLE_FILE = "src/test/resources/ch/vd/uniregctb/editique/print/exemple.pcl";
	private final static String PRINTER_NAME = "\\\\vac2rec2k00069\\DSI_R1E_CANON_IR2870_PCL6";

	private PrintPCLService printPCLService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		printPCLService = getBean(PrintPCLService.class, "printPCLService");
	}

	@Test
	public void printLocalStream() throws Exception {
		File file = ResourceUtils.getFile(PCL_SAMPLE_FILE);
		InputStream inputStream = new FileInputStream(file.getAbsolutePath());
		ProfilOperateurImpl profilOperateur = new ProfilOperateurImpl();
		profilOperateur.setImprimante(PRINTER_NAME);
		//TODO [xcifde] a traiter pour Hudson
		/*
		try {
			printPCLService.printLocalStream(profilOperateur, inputStream);
		} catch (PrintPCLException exError){
			Assert.assertFalse("Erreur lors de l'impression du document PCL", true);
		}
		*/
	}

}
