package ch.vd.uniregctb.dao.jdbc;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;
import org.hibernate.usertype.UserType;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.dao.jdbc.meta.BooleanColumnType;
import ch.vd.uniregctb.dao.jdbc.meta.Column;
import ch.vd.uniregctb.dao.jdbc.meta.ColumnType;
import ch.vd.uniregctb.dao.jdbc.meta.DateColumnType;
import ch.vd.uniregctb.dao.jdbc.meta.Entity;
import ch.vd.uniregctb.dao.jdbc.meta.EnumTypeAdresseColumnType;
import ch.vd.uniregctb.dao.jdbc.meta.EnumUserTypeColumnType;
import ch.vd.uniregctb.dao.jdbc.meta.IntegerColumnType;
import ch.vd.uniregctb.dao.jdbc.meta.JoinColumnType;
import ch.vd.uniregctb.dao.jdbc.meta.LongColumnType;
import ch.vd.uniregctb.dao.jdbc.meta.RegDateColumnType;
import ch.vd.uniregctb.dao.jdbc.meta.StringColumnType;
import ch.vd.uniregctb.dao.jdbc.meta.TimestampColumnType;
import ch.vd.uniregctb.dao.jdbc.meta.UserTypeColumnType;
import ch.vd.uniregctb.hibernate.EnumTypeAdresseUserType;
import ch.vd.uniregctb.hibernate.EnumUserType;
import ch.vd.uniregctb.hibernate.RegDateUserType;

public class JdbcHibernateEntityDaoGenerator {

	protected static final Logger LOGGER = Logger.getLogger(JdbcHibernateEntityDaoGenerator.class);

	private static final LongColumnType longColumnType = new LongColumnType();
	private static final IntegerColumnType integerColumnType = new IntegerColumnType();
	private static final StringColumnType stringColumnType = new StringColumnType();
	private static final DateColumnType dateColumnType = new DateColumnType();
	private static final BooleanColumnType booleanColumnType = new BooleanColumnType();
	private static final TimestampColumnType timestampColumnType = new TimestampColumnType();

	private static final Map<Class<?>, ColumnType> columnTypeMap = new HashMap<Class<?>, ColumnType>();

	static {
		columnTypeMap.put(longColumnType.getJavaType(), longColumnType);
		columnTypeMap.put(integerColumnType.getJavaType(), integerColumnType);
		columnTypeMap.put(Integer.TYPE, integerColumnType);
		columnTypeMap.put(stringColumnType.getJavaType(), stringColumnType);
		columnTypeMap.put(dateColumnType.getJavaType(), dateColumnType);
		columnTypeMap.put(booleanColumnType.getJavaType(), booleanColumnType);
		columnTypeMap.put(Boolean.TYPE, booleanColumnType);
		columnTypeMap.put(timestampColumnType.getJavaType(), timestampColumnType);
	}

	private String table;
	private Class<?> baseClass;
	private List<Entity> entities = new ArrayList<Entity>();
	private Map<String, Column> allColumns = new HashMap<String, Column>();

	public JdbcHibernateEntityDaoGenerator(String parentForeignKeyName, Class... hibernateEntities) throws Exception {

		this.baseClass = getBaseClass(hibernateEntities[0]);

		for (Class clazz : hibernateEntities) {

			Entity entity = null;
			final List<Column> columns = new ArrayList<Column>();

			final List<Annotation> annotations = getAllAnnotations(clazz);
			for (Annotation a : annotations) {
				if (a instanceof DiscriminatorValue) {
					final DiscriminatorValue d = (DiscriminatorValue) a;
					if (entity != null) {
						throw new IllegalArgumentException("Duplicated discriminator = [" + entity.getDiscriminant() + ", " + d.value() + "]) on class [" + clazz.getSimpleName() + "]");
					}
					entity = new Entity(d.value(), clazz);
				}
				else if (a instanceof DiscriminatorColumn) {
					final DiscriminatorColumn d = (DiscriminatorColumn) a;
					columns.add(newColumn(d.name(), stringColumnType, null, true, false, false));
				}
				else if (a instanceof Table) {
					final Table t = (Table) a;
					this.table = t.name();
				}
			}

			if (entity == null) { // pas de hierarchie
				entity = new Entity(null, clazz);
			}

			final Map<String, PropertyDescriptor> descriptors = getPropertyDescriptors(clazz);
			for (PropertyDescriptor descriptor : descriptors.values()) {
				if (descriptor.getName().equals("class")) {
					continue;
				}

				final Method readMethod = descriptor.getReadMethod();
				if (readMethod == null) {
//					LOGGER.debug("Ignoring descriptor [" + descriptor.getName() + "] from class [" + clazz.getName() + "] without read method");
					continue;
				}

				final Method writeMethod = descriptor.getWriteMethod();
				if (writeMethod == null) {
//					LOGGER.debug("Ignoring descriptor [" + descriptor.getName() + "] from class [" + clazz.getName() + "] without write method");
					continue;
				}
				if (!Modifier.isPublic(writeMethod.getModifiers())) {
					LOGGER.warn("Write method for descriptor [" + descriptor.getName() + "] from class [" + clazz.getName() + "] is not public");
					continue;
				}

				boolean estTransient = false;
				boolean estColonne = false;
				boolean estCollection = false;
				String columnName = null;
				Class<?> returnType = null;
				UserType userType = null;
				boolean primaryKey = false;
				boolean parentForeignKey = false;
				boolean otherForeignKey = false;

				final Annotation[] fieldAnnotations = readMethod.getAnnotations();
				for (Annotation a : fieldAnnotations) {
					if (a instanceof javax.persistence.Column) {
						final javax.persistence.Column c = (javax.persistence.Column) a;
						columnName = c.name();
						returnType = readMethod.getReturnType();
						estColonne = true;
					}
					else if (a instanceof Id) {
						columnName = "ID";
						returnType = Long.class;
						primaryKey = true;
						estColonne = true;
					}
					else if (a instanceof JoinColumn) {
						final JoinColumn j = (JoinColumn) a;
						columnName = j.name();
						if (descriptor.getName().equals(parentForeignKeyName)) {
							parentForeignKey = true;
							returnType = Long.class;
						}
						else {
							otherForeignKey = true;
							returnType = readMethod.getReturnType();
						}
						estColonne = true;
					}
					else if (a instanceof OneToMany) {
						estCollection = true;
					}
					else if (a instanceof Type) {
						final Type t = (Type) a;
						final String userTypeClassname = t.type();
						final Class<?> userTypeClass = Class.forName(userTypeClassname);
						userType = (UserType) userTypeClass.newInstance();
					}
					else if (a instanceof Transient) {
						estTransient = true;
					}
				}


				if (estTransient || estCollection) { // on ignore les collections, elles seront chargées indépendemment
					continue;
				}
				if (!estColonne) {
					LOGGER.warn("No @Transient nor @Column annotation found on descriptor [" + descriptor.getName() + "] from class [" + clazz.getName() + "]");
					continue;
				}

				final ColumnType columnType;
				if (userType != null) {
					if (userType instanceof RegDateUserType) {
						columnType = new RegDateColumnType((RegDateUserType) userType);
					}
					else if (userType instanceof EnumUserType) {
						columnType = new EnumUserTypeColumnType(returnType, (EnumUserType) userType);
					}
					else if (userType instanceof EnumTypeAdresseUserType) {
						columnType = new EnumTypeAdresseColumnType((EnumTypeAdresseUserType) userType);
					}
					else {
						throw new NotImplementedException("Type de user-type inconnu = [" + userType.getClass().getName() + "]");
					}

				}
				else if (otherForeignKey) {
					columnType = new JoinColumnType(returnType);
				}
				else {
					columnType = columnTypeMap.get(returnType);
					Assert.notNull(columnType, "Type java non-enregistré [" + returnType.getName() + "] (propriété = [" + descriptor.getName() + "] de la classe [" + clazz.getSimpleName() + "])");
				}

				columns.add(newColumn(columnName, columnType, descriptor.getName(), false, primaryKey, parentForeignKey));
			}

			Collections.sort(columns);
			entity.setColumns(columns);
			entities.add(entity);
			for (Column c : columns) {
				allColumns.put(c.getName(), c);
			}
		}
	}

	private Column newColumn(String name, ColumnType columnType, String property, boolean discriminator, boolean primaryKey, boolean parentForeignKey) {
		Column c = allColumns.get(name);
		if (c == null) {
			c = new Column(name, columnType, property, discriminator, primaryKey, parentForeignKey);
			allColumns.put(name, c);
		}
		else {
			Assert.isEqual(c.getType().getSqlType(), columnType.getSqlType());
		}
		return c;
	}

	private Class<?> getBaseClass(Class<?> clazz) {
		while (clazz != null) {
			final Annotation[] as = clazz.getAnnotations();
			if (as != null) {
				for (Annotation a : as) {
					if (a instanceof Table) {
						return clazz;
					}
				}
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}

	public static Map<String, PropertyDescriptor> getPropertyDescriptors(Class<?> clazz) throws IntrospectionException {

		BeanInfo info = Introspector.getBeanInfo(clazz);
		PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
		HashMap<String, PropertyDescriptor> pds = new HashMap<String, PropertyDescriptor>();

		for (PropertyDescriptor descriptor : descriptors) {
			pds.put(descriptor.getName(), descriptor);
		}

		return pds;
	}

	private List<Annotation> getAllAnnotations(Class clazz) {
		List<Annotation> list = new ArrayList<Annotation>();
		while (clazz != null) {
			final Annotation[] as = clazz.getAnnotations();
			if (as != null) {
				list.addAll(Arrays.asList(as));
			}
			clazz = clazz.getSuperclass();
		}
		return list;
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
