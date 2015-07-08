package ch.vd.uniregctb.ddl;

import java.io.FileWriter;
import java.net.URL;

import org.apache.log4j.xml.DOMConfigurator;
import org.hibernate.dialect.Oracle10gDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.ResourceUtils;

import ch.vd.shared.hibernate.config.DescriptiveSessionFactoryBean;
import ch.vd.uniregctb.common.ClientConstants;
import ch.vd.uniregctb.hibernate.dialect.Oracle10gDialectWithNVarChar;

public class DDLGenerator {

	private static Logger LOGGER = LoggerFactory.getLogger(DDLGenerator.class);

	private static String[] contextFiles = {ClientConstants.UNIREG_CORE_DAO,
			ClientConstants.UNIREG_CORE_SF,
			"ddlgen/unireg-ddlgen-datasource.xml",
			"ddlgen/unireg-ddlgen-hibernate.xml"};

	public static void main(String[] args) throws Exception {

		final URL log4jUrl = ResourceUtils.getURL("classpath:ddlgen/log4j.xml");
		DOMConfigurator.configure(log4jUrl);

		//System.out.println("Nb args: "+args.length);
		String outFileBase = "target/unireg-schema";
		if (args.length > 0) {
			outFileBase = args[0];
		}
		String outFileDrop = outFileBase + "_drop.sql";
		String outFileCreate = outFileBase + "_create.sql";

		final ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(contextFiles);
		final DescriptiveSessionFactoryBean factory = (DescriptiveSessionFactoryBean) applicationContext.getBean("&sessionFactory");

		final Oracle10gDialect dialect = new Oracle10gDialectWithNVarChar();
		String[] drops = factory.getConfiguration().generateDropSchemaScript(dialect);
		try (FileWriter dropFile = new FileWriter(outFileDrop)) {
			for (String d : drops) {
				dropFile.write(d + ";\n\n");
			}
		}

		String[] creations = factory.getConfiguration().generateSchemaCreationScript(dialect);
		try (FileWriter createFile = new FileWriter(outFileCreate)) {
			for (String d : creations) {
				createFile.write(d + ";\n\n");
			}
		}

		LOGGER.info("Les fichiers SQL sont disponibles ici:\n  - " + outFileCreate + "\n  - " + outFileDrop);
		LOGGER.info("DDL generated successfully!");
	}

}
