package ch.vd.uniregctb.ddl;

import java.io.FileWriter;

import ch.vd.uniregctb.hibernate.dialect.Oracle10gDialectWithNVarChar;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.hibernate.dialect.Oracle10gDialect;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;

import ch.vd.uniregctb.common.ClientConstants;

public class DDLGenerator {

	private static Logger LOGGER = Logger.getLogger(DDLGenerator.class);

	private static ClassPathXmlApplicationContext applicationContext;
	private static String[] contextFiles = { 	ClientConstants.UNIREG_CORE_DAO,
												ClientConstants.UNIREG_CORE_SF,
												"ddlgen/unireg-ddlgen-datasource.xml",
												"ddlgen/unireg-ddlgen-hibernate.xml" };

	public static void main(String[] args) throws Exception {

		DOMConfigurator.configure("target/classes/ddlgen/log4j.xml");

		//System.out.println("Nb args: "+args.length);
		String outFileBase = "target/unireg-schema";
		if (args.length > 0) {
			outFileBase = args[0];
		}
		String outFileDrop = outFileBase + "_drop.sql";
		String outFileCreate = outFileBase + "_create.sql";

		applicationContext = new ClassPathXmlApplicationContext(contextFiles);

		AnnotationSessionFactoryBean factory = (AnnotationSessionFactoryBean)applicationContext.getBean("&sessionFactory");

		Oracle10gDialect dialect = new Oracle10gDialectWithNVarChar();
		String[] drops = factory.getConfiguration().generateDropSchemaScript(dialect);
		FileWriter dropFile = new FileWriter(outFileDrop);
		for (String d : drops) {
			dropFile.write(d+";\n\n");
		}
		dropFile.close();

		String[] creations = factory.getConfiguration().generateSchemaCreationScript(dialect);
		FileWriter createFile = new FileWriter(outFileCreate);
		for (String d : creations) {
			createFile.write(d+";\n\n");
		}
		createFile.close();

		LOGGER.info("Les fichiers SQL sont disponibles ici:\n  - " + outFileCreate + "\n  - " + outFileDrop);
		LOGGER.info("DDL generated successfully!");
	}

}
