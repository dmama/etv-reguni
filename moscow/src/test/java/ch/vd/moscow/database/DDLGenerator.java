package ch.vd.moscow.database;

import java.io.FileWriter;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.util.ResourceUtils;

public class DDLGenerator {

	private static Logger LOGGER = Logger.getLogger(DDLGenerator.class);

	private static String[] contextFiles = {"classpath:moscow-database.xml", "classpath:moscow-hibernate.xml", "classpath:ut/moscow-properties-ut.xml" };

	public static void main(String[] args) throws Exception {

		final URL log4jUrl = ResourceUtils.getURL("classpath:log4j.xml");
		DOMConfigurator.configure(log4jUrl);

		String outFileBase = "target/schema";
		if (args.length > 0) {
			outFileBase = args[0];
		}
		String outFileDrop = outFileBase + "_drop.sql";
		String outFileCreate = outFileBase + "_create.sql";

		final ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(contextFiles);

		LocalSessionFactoryBean factory = (LocalSessionFactoryBean) applicationContext.getBean("&sessionFactory");

		final Dialect dialect = new PostgreSQL81Dialect();
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
