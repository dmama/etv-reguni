package ch.vd.uniregctb.dao.jdbc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.HibernateEntityUtils;
import ch.vd.uniregctb.hibernate.meta.JoinPropertyType;
import ch.vd.uniregctb.hibernate.meta.Property;
import ch.vd.uniregctb.hibernate.meta.PropertyType;
import ch.vd.uniregctb.hibernate.meta.MetaEntity;
import ch.vd.uniregctb.hibernate.meta.UserTypePropertyType;

public class JdbcHibernateEntityDaoGenerator {

	protected static final Logger LOGGER = Logger.getLogger(JdbcHibernateEntityDaoGenerator.class);

	private String table;
	private final Class<?> baseClass;
	private final List<MetaEntity> entities = new ArrayList<MetaEntity>();
	private final Map<String, Property> allProperties = new HashMap<String, Property>();

	public JdbcHibernateEntityDaoGenerator(Class... hibernateEntities) throws Exception {

		this.baseClass = HibernateEntityUtils.getBaseClass(hibernateEntities[0]);

		for (Class clazz : hibernateEntities) {
			final MetaEntity entity = MetaEntity.determine(clazz);

			if (table == null) {
				table = entity.getTable();
			}
			else {
				Assert.isEqual(table, entity.getTable());
			}

			entities.add(entity);

			final List<Property> properties = entity.getProperties();
			for (int i = 0, propSize = properties.size(); i < propSize; i++) {
				final Property p = properties.get(i);
				if (p.isCollection()) {
					continue;
				}
				final Property existing = allProperties.get(p.getColumnName());
				if (existing == null) {
					allProperties.put(p.getColumnName(), p);
				}
				else {
					Assert.isEqual(p.getType().getSqlType(), existing.getType().getSqlType());
					// si la colonne existe déjà (possible car les getters des classes de base sont processés plusieurs fois lors de hiérarchie d'entités),
					// on remplace celle de l'entité par la colonne existante, de manière à n'importe qu'une instance par colonne réelle.
					properties.set(i, existing);
				}
			}
		}
	}

	public void generate(String inputTemplate, String outputFilename) throws IOException {

		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(ResourceUtils.getFile(inputTemplate))));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilename)));

		// Construction des portions de code dynamiques

		final List<Property> properties = new ArrayList<Property>(allProperties.values());
		Collections.sort(properties);

		String primaryKey = null;
		Property foreignKey = null;
		Property discriminant = null;
		String discriminantVar = null;

		String baseSelect = "";
		String mappingCode = "";

		for (int i = 0, propSize = properties.size(); i < propSize; i++) {
			final Property p = properties.get(i);
			p.setIndex(i + 1);
			final boolean last = (i == propSize - 1);
			baseSelect += "\"" + p.getColumnName() + (last ? "" : ",") + " \" + // " + (i + 1) + "\n";

			if (p.isPrimaryKey()) {
				Assert.isNull(primaryKey);
				primaryKey = p.getColumnName();
			}
			if (p.isParentForeignKey()) {
				Assert.isNull(foreignKey, "Duplicated foreign key found = [" + foreignKey + ", " + p.getColumnName() + "]");
				foreignKey = p;
			}
			if (p.isDiscriminator()) {
				Assert.isNull(discriminant);
				discriminant = p;
				discriminantVar = toVar(p.getColumnName());
			}
		}
		baseSelect += "\"from " + table + "\"";
		Assert.notNull(primaryKey);

		mappingCode += "\n";

		// le discriminant
		if (discriminant != null) {
			mappingCode += generateDeclareAndGetValue("", discriminant);
		}

		// la foreign key
		if (foreignKey != null) {
			mappingCode += generateDeclareAndGetValue("", foreignKey);
		}

		// l'objet retourné
		mappingCode += "final " + baseClass.getSimpleName() + " res;\n\n";

		// création et remplissage des objets
		boolean hierarchy = false;
		for (int i = 0, entitiesSize = entities.size(); i < entitiesSize; i++) {
			final MetaEntity e = entities.get(i);
			final boolean first = (i == 0);

			String tab = "";
			if (e.getDiscriminant() != null) { // hiérarchie dans une table
				mappingCode += (first ? "" : "else ") + "if (" + discriminantVar + ".equals(\"" + e.getDiscriminant() + "\")) {\n";
				tab = "\t";
				hierarchy = true;
			}

			mappingCode += "\n";
			
			for (Property p : e.getProperties()) {
				if (!p.isDiscriminator() && !p.isParentForeignKey() && !p.isCollection()) {
					mappingCode += generateDeclareAndGetValue(tab, p);
				}
			}

			mappingCode += "\n" + tab + e.getType().getSimpleName() + " o = new " + e.getType().getSimpleName() + "();\n";

			for (Property p : e.getProperties()) {
				if (!p.isDiscriminator() && !p.isParentForeignKey() && !p.isCollection()) {
					mappingCode += tab + "o." + toSetter(p.getName()) + "(" + toVar(p.getColumnName()) + ");\n";
				}
			}

			mappingCode += tab + "res = o;\n";
			if (e.getDiscriminant() != null) {
				mappingCode += "}\n";
			}
		}

		if (hierarchy) {
			mappingCode += "else {\n";
			mappingCode += "\tthrow new IllegalArgumentException(\"Type inconnu = [\" + " + discriminantVar + " + \"]\");\n";
			mappingCode += "}\n";
		}
		mappingCode += "\n";

		if (foreignKey != null) {
			mappingCode += "final Pair<Long, " + baseClass.getSimpleName() + "> pair = new Pair<Long, " + baseClass.getSimpleName() + ">();\n";
			mappingCode += "pair.setFirst(" + toVar(foreignKey.getColumnName()) + ");\n";
			mappingCode += "pair.setSecond(res);\n\n";
			mappingCode += "return pair;";
		}
		else {
			mappingCode += "return res;";
		}

		// Remplissage du template et création du fichier
		String line = input.readLine();
		while (line != null) {
			line = replace(line, "BASE_SELECT", baseSelect);
			line = replace(line, "PRIMARY_KEY", primaryKey);
			line = replace(line, "MAPPING_CODE", mappingCode);
			if (foreignKey != null) {
				line = replace(line, "FOREIGN_KEY", foreignKey.getColumnName());
			}
			writer.write(line);
			writer.newLine();
			line = input.readLine();
		}

		input.close();
		writer.close();
	}

	private String generateDeclareAndGetValue(String tab, Property p) {
		final PropertyType colType = p.getType();

		String mappingCode = "";
		
		if (colType.needNullCheck()) {

			mappingCode += tab + "final " + colType.getSqlType().getSimpleName() + " " + toTempVar(p) + " = " + generateGetValue(p) + ";\n";
			if (colType instanceof UserTypePropertyType) {
				final UserTypePropertyType userType = (UserTypePropertyType) colType;
				mappingCode +=
						tab + "final " + colType.getJavaType().getSimpleName() + " " + toVar(p.getColumnName()) + " = (rs.wasNull() ? null : " + userType.getConvertMethod(toTempVar(p)) + ");\n";
			}
			else if (colType instanceof JoinPropertyType) {
				final JoinPropertyType joinType = (JoinPropertyType) colType;
				mappingCode +=
						tab + "final " + colType.getJavaType().getSimpleName() + " " + toVar(p.getColumnName()) + " = (rs.wasNull() ? null : " + joinType.getConvertMethod(toTempVar(p)) + ");\n";
			}
			else {
				mappingCode += tab + "final " + colType.getJavaType().getSimpleName() + " " + toVar(p.getColumnName()) + " = (rs.wasNull() ? null : " + toTempVar(p) + ");\n";
			}
		}
		else {
			if (colType instanceof UserTypePropertyType) {
				final UserTypePropertyType userType = (UserTypePropertyType) colType;
				mappingCode +=
						tab + "final " + colType.getJavaType().getSimpleName() + " " + toVar(p.getColumnName()) + " = " + userType.getConvertMethod(generateGetValue(p)) + ";\n";
			}
			else if (colType instanceof JoinPropertyType) {
				final JoinPropertyType joinType = (JoinPropertyType) colType;
				mappingCode +=
						tab + "final " + colType.getJavaType().getSimpleName() + " " + toVar(p.getColumnName()) + " = " + joinType.getConvertMethod(generateGetValue(p)) + ";\n";
			}
			else {
				mappingCode += tab + "final " + colType.getJavaType().getSimpleName() + " " + toVar(p.getColumnName()) + " = " + generateGetValue(p) + ";\n";
			}

		}
		return mappingCode;
	}

	private static String toTempVar(Property p) {
		return "temp" + p.getIndex();
	}

	private String generateGetValue(Property p) {
		final int index = p.getIndex();
		Assert.isTrue(index > 0);
		return "rs." + p.getType().getResultGetter() + "(" + index + ")";
	}

	private String toSetter(String property) {
		return "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
	}

	/**
	 * Converti un nom de colonne (FOR_TYPE) en nom de variable (forType)
	 *
	 * @param columnName le nom de la colonne à convertir
	 * @return le nom de la variable
	 */
	private String toVar(String columnName) {
		Assert.notNull(columnName);
		Assert.isTrue(columnName.length() > 1, "Nom de colonne trop court = [" + columnName + "]");
		StringBuffer sb = new StringBuffer();
		Matcher m = Pattern.compile("_([a-z])([a-z]*)", Pattern.CASE_INSENSITIVE).matcher(columnName.toLowerCase());
		while (m.find()) {
			m.appendReplacement(sb, m.group(1).toUpperCase() + m.group(2).toLowerCase());
		}
		return m.appendTail(sb).toString();
	}

	private String replace(String line, String pattern, String value) {
		final int index = line.indexOf("${" + pattern + "}");
		if (index >= 0) {
			final String before = line.substring(0, index);
			if (StringUtils.isBlank(before)) {
				value = value.replaceAll("\\n", "\n" + before);
			}
		}
		return line.replaceAll("\\$\\{" + pattern + "\\}", value);
	}
}
