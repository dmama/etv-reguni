package ch.vd.uniregctb.webservices.tiers.compare;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;
import ch.vd.interfaces.fiscal.RechercherNoContribuable;

public class ContribuableFileReader {
	 private static final Logger LOGGER = Logger.getLogger(ContribuableFileReader.class);

	public static List<RechercherNoContribuable > chargerContribuable (String fileName){

		  CSVReader reader=null;

		  List<RechercherNoContribuable > result  = new ArrayList<RechercherNoContribuable>();
		try {
			reader = new CSVReader(new FileReader(fileName),';');
		}
		catch (FileNotFoundException e1) {

			LOGGER.error(e1.getMessage());
		}
		    String [] nextLine;
		    try {
				while ((nextLine = reader.readNext()) != null) {
					RechercherNoContribuable infoCourante = new RechercherNoContribuable();
					infoCourante.setAnnee(Integer.parseInt(nextLine[0]));
					infoCourante.setNoAvs(Long.parseLong(nextLine[1]));
					infoCourante.setNom(nextLine[2]);
					infoCourante.setPrenom(nextLine[3]);
					result.add(infoCourante);
				}
			}
			catch (IOException e) {

				LOGGER.error(e.getMessage());
			}

			return result;
	}


}
