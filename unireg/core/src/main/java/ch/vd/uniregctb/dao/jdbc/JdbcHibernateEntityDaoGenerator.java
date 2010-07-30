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
import ch.vd.uniregctb.dao.jdbc.meta.Column;
import ch.vd.uniregctb.dao.jdbc.meta.ColumnType;
import ch.vd.uniregctb.dao.jdbc.meta.Entity;
import ch.vd.uniregctb.dao.jdbc.meta.JoinColumnType;
import ch.vd.uniregctb.dao.jdbc.meta.UserTypeColumnType;

public class JdbcHibernateEntityDaoGenerator {

	protected static final Logger LOGGER = Logger.getLogger(JdbcHibernateEntityDaoGenerator.class);

	private String table;
	private Class<?> baseClass;
	private List<Entity> entities = new ArrayList<Entity>();
	private Map<String, Column> allColumns = new HashMap<String, Column>();

	public JdbcHibernateEntityDaoGenerator(Class... hibernateEntities) throws Exception {

		this.baseClass = HibernateEntityUtils.getBaseClass(hibernateEntities[0]);

		for (Class clazz : hibernateEntities) {
			final Entity entity = Entity.determine(clazz);

			if (table == null) {
				table = entity.getTable();
			}
			else {
				Assert.isEqual(table, entity.getTable());
			}

			entities.add(entity);

			final List<Column> columns = entity.getColumns();
			for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
				final Column c = columns.get(i);
				final Column existing = allColumns.get(c.getName());
				if (existing == null) {
					allColumns.put(c.getName(), c);
				}
				else {
					Assert.isEqual(c.getType().getSqlType(), existing.getType().getSqlType());
					// si la colonne existe déjà (possible car les getters des classes de base sont processés plusieurs fois lors de hiérarchie d'entités),
					// on remplace celle de l'entité par la colonne existante, de manière à n'importe qu'une instance par colonne réelle.
					columns.set(i, existing);
				}
			}
		}
	}

	public void generate(String inputTemplate, String outputFilename) throws IOException {

		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(ResourceUtils.getFile(inputTemplate))));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilename)));

		// Construction des portions de code dynamiques

		final List<Column> columns = new ArrayList<Column>(allColumns.values());
		Collections.sort(columns);

		String primaryKey = null;
		Column foreignKey = null;
		Column discriminant = null;
		String discriminantVar = null;

		String baseSelect = "";
		String mappingCode = "";

		for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
			final Column c = columns.get(i);
			c.setIndex(i + 1);
			final boolean last = (i == columnsSize - 1);
			baseSelect += "\"" + c.getName() + (last ? "" : ",") + " \" + // " + (i + 1) + "\n";

			if (c.isPrimaryKey()) {
				Assert.isNull(primaryKey);
				primaryKey = c.getName();
			}
			if (c.isParentForeignKey()) {
				Assert.isNull(foreignKey, "Duplicated foreign key found = [" + foreignKey + ", " + c.getName() + "]");
				foreignKey = c;
			}
			if (c.isDiscriminator()) {
				Assert.isNull(discriminant);
				discriminant = c;
				discriminantVar = toVar(c.getName());
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
			final Entity e = entities.get(i);
			final boolean first = (i == 0);

			String tab = "";
			if (e.getDiscriminant() != null) { // hiérarchie dans une table
				mappingCode += (first ? "" : "else ") + "if (" + discriminantVar + ".equals(\"" + e.getDiscriminant() + "\")) {\n";
				tab = "\t";
				hierarchy = true;
			}

			mappingCode += "\n";
			
			for (Column c : e.getColumns()) {
				if (!c.isDiscriminator() && !c.isParentForeignKey()) {
					mappingCode += generateDeclareAndGetValue(tab, c);
				}
			}

			mappingCode += "\n" + tab + e.getType().getSimpleName() + " o = new " + e.getType().getSimpleName() + "();\n";

			for (Column c : e.getColumns()) {
				if (!c.isDiscriminator() && !c.isParentForeignKey()) {
					mappingCode += tab + "o." + toSetter(c.getProperty()) + "(" + toVar(c.getName()) + ");\n";
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
			mappingCode += "pair.setFirst(" + toVar(foreignKey.getName()) + ");\n";
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
				line = replace(line, "FOREIGN_KEY", foreignKey.getName());
			}
			writer.write(line);
			writer.newLine();
			line = input.readLine();
		}

		input.close();
		writer.close();
	}

	private String generateDeclareAndGetValue(String tab, Column c) {
		final ColumnType colType = c.getType();

		String mappingCode = "";
		
		if (colType.needNullCheck()) {

			mappingCode += tab + "final " + colType.getSqlType().getSimpleName() + " " + toTempVar(c) + " = " + generateGetValue(c) + ";\n";
			if (colType instanceof UserTypeColumnType) {
				final UserTypeColumnType userType = (UserTypeColumnType) colType;
				mappingCode +=
						tab + "final " + colType.getJavaType().getSimpleName() + " " + toVar(c.getName()) + " = (rs.wasNull() ? null : " + userType.getConvertMethod(toTempVar(c)) + ");\n";
			}
			else if (colType instanceof JoinColumnType) {
				final JoinColumnType joinType = (JoinColumnType) colType;
				mappingCode +=
						tab + "final " + colType.getJavaType().getSimpleName() + " " + toVar(c.getName()) + " = (rs.wasNull() ? null : " + joinType.getConvertMethod(toTempVar(c)) + ");\n";
			}
			else {
				mappingCode += tab + "final " + colType.getJavaType().getSimpleName() + " " + toVar(c.getName()) + " = (rs.wasNull() ? null : " + toTempVar(c) + ");\n";
			}
		}
		else {
			if (colType instanceof UserTypeColumnType) {
				final UserTypeColumnType userType = (UserTypeColumnType) colType;
				mappingCode +=
						tab + "final " + colType.getJavaType().getSimpleName() + " " + toVar(c.getName()) + " = " + userType.getConvertMethod(generateGetValue(c)) + ";\n";
			}
			else if (colType instanceof JoinColumnType) {
				final JoinColumnType joinType = (JoinColumnType) colType;
				mappingCode +=
						tab + "final " + colType.getJavaType().getSimpleName() + " " + toVar(c.getName()) + " = " + joinType.getConvertMethod(generateGetValue(c)) + ";\n";
			}
			else {
				mappingCode += tab + "final " + colType.getJavaType().getSimpleName() + " " + toVar(c.getName()) + " = " + generateGetValue(c) + ";\n";
			}

		}
		return mappingCode;
	}

	private static String toTempVar(Column c) {
		return "temp" + c.getIndex();
	}

	private String generateGetValue(Column c) {
		final int index = c.getIndex();
		Assert.isTrue(index > 0);
		return "rs." + c.getType().getResultGetter() + "(" + index + ")";
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
