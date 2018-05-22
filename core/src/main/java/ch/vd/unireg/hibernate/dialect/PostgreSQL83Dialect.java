/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package ch.vd.unireg.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.PositionSubstringFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.id.SequenceGenerator;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.type.StandardBasicTypes;

/**
 * *** WARNING ***
 * <p>
 * Version du fichier récupérée du repo SVN Hibernate pour avoir le spooling des séquence. A supprimer quand une nouvelle version
 * d'Hibernate supportant cette fonctionnalité sera disponible. An SQL dialect for Postgres
 * <p>
 * ***************
 *
 * @author Gavin King
 */
public class PostgreSQL83Dialect extends Dialect {

	public PostgreSQL83Dialect() {
		super();
		registerColumnType( Types.BIT, "bool" );
		registerColumnType( Types.BIGINT, "int8" );
		registerColumnType( Types.SMALLINT, "int2" );
		registerColumnType( Types.TINYINT, "int2" );
		registerColumnType( Types.INTEGER, "int4" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float4" );
		registerColumnType( Types.DOUBLE, "float8" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, "bytea" );
		registerColumnType( Types.CLOB, "text" );
		registerColumnType( Types.BLOB, "oid" );
		registerColumnType( Types.NUMERIC, "numeric($p, $s)" );

		registerFunction( "abs", new StandardSQLFunction("abs") );
		registerFunction( "sign", new StandardSQLFunction("sign", StandardBasicTypes.INTEGER) );

		registerFunction( "acos", new StandardSQLFunction("acos", StandardBasicTypes.DOUBLE) );
		registerFunction( "asin", new StandardSQLFunction("asin", StandardBasicTypes.DOUBLE) );
		registerFunction( "atan", new StandardSQLFunction("atan", StandardBasicTypes.DOUBLE) );
		registerFunction( "cos", new StandardSQLFunction("cos", StandardBasicTypes.DOUBLE) );
		registerFunction( "cot", new StandardSQLFunction("cot", StandardBasicTypes.DOUBLE) );
		registerFunction( "exp", new StandardSQLFunction("exp", StandardBasicTypes.DOUBLE) );
		registerFunction( "ln", new StandardSQLFunction("ln", StandardBasicTypes.DOUBLE) );
		registerFunction( "log", new StandardSQLFunction("log", StandardBasicTypes.DOUBLE) );
		registerFunction( "sin", new StandardSQLFunction("sin", StandardBasicTypes.DOUBLE) );
		registerFunction( "sqrt", new StandardSQLFunction("sqrt", StandardBasicTypes.DOUBLE) );
		registerFunction( "cbrt", new StandardSQLFunction("cbrt", StandardBasicTypes.DOUBLE) );
		registerFunction( "tan", new StandardSQLFunction("tan", StandardBasicTypes.DOUBLE) );
		registerFunction( "radians", new StandardSQLFunction("radians", StandardBasicTypes.DOUBLE) );
		registerFunction( "degrees", new StandardSQLFunction("degrees", StandardBasicTypes.DOUBLE) );

		registerFunction( "stddev", new StandardSQLFunction("stddev", StandardBasicTypes.DOUBLE) );
		registerFunction( "variance", new StandardSQLFunction("variance", StandardBasicTypes.DOUBLE) );

		registerFunction( "random", new NoArgSQLFunction("random", StandardBasicTypes.DOUBLE) );

		registerFunction( "round", new StandardSQLFunction("round") );
		registerFunction( "trunc", new StandardSQLFunction("trunc") );
		registerFunction( "ceil", new StandardSQLFunction("ceil") );
		registerFunction( "floor", new StandardSQLFunction("floor") );

		registerFunction( "chr", new StandardSQLFunction("chr", StandardBasicTypes.CHARACTER) );
		registerFunction( "lower", new StandardSQLFunction("lower") );
		registerFunction( "upper", new StandardSQLFunction("upper") );
		registerFunction( "substr", new StandardSQLFunction("substr", StandardBasicTypes.STRING) );
		registerFunction( "initcap", new StandardSQLFunction("initcap") );
		registerFunction( "to_ascii", new StandardSQLFunction("to_ascii") );
		registerFunction( "quote_ident", new StandardSQLFunction("quote_ident", StandardBasicTypes.STRING) );
		registerFunction( "quote_literal", new StandardSQLFunction("quote_literal", StandardBasicTypes.STRING) );
		registerFunction( "md5", new StandardSQLFunction("md5") );
		registerFunction( "ascii", new StandardSQLFunction("ascii", StandardBasicTypes.INTEGER) );
		registerFunction( "length", new StandardSQLFunction("length", StandardBasicTypes.LONG) );
		registerFunction( "char_length", new StandardSQLFunction("char_length", StandardBasicTypes.LONG) );
		registerFunction( "bit_length", new StandardSQLFunction("bit_length", StandardBasicTypes.LONG) );
		registerFunction( "octet_length", new StandardSQLFunction("octet_length", StandardBasicTypes.LONG) );

		registerFunction( "age", new StandardSQLFunction("age") );
		registerFunction( "current_date", new NoArgSQLFunction("current_date", StandardBasicTypes.DATE, false) );
		registerFunction( "current_time", new NoArgSQLFunction("current_time", StandardBasicTypes.TIME, false) );
		registerFunction( "current_timestamp", new NoArgSQLFunction("current_timestamp", StandardBasicTypes.TIMESTAMP, false) );
		registerFunction( "date_trunc", new StandardSQLFunction( "date_trunc", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "localtime", new NoArgSQLFunction("localtime", StandardBasicTypes.TIME, false) );
		registerFunction( "localtimestamp", new NoArgSQLFunction("localtimestamp", StandardBasicTypes.TIMESTAMP, false) );
		registerFunction( "now", new NoArgSQLFunction("now", StandardBasicTypes.TIMESTAMP) );
		registerFunction( "timeofday", new NoArgSQLFunction("timeofday", StandardBasicTypes.STRING) );

		registerFunction( "current_user", new NoArgSQLFunction("current_user", StandardBasicTypes.STRING, false) );
		registerFunction( "session_user", new NoArgSQLFunction("session_user", StandardBasicTypes.STRING, false) );
		registerFunction( "user", new NoArgSQLFunction("user", StandardBasicTypes.STRING, false) );
		registerFunction( "current_database", new NoArgSQLFunction("current_database", StandardBasicTypes.STRING, true) );
		registerFunction( "current_schema", new NoArgSQLFunction("current_schema", StandardBasicTypes.STRING, true) );

		registerFunction( "to_char", new StandardSQLFunction("to_char", StandardBasicTypes.STRING) );
		registerFunction( "to_date", new StandardSQLFunction("to_date", StandardBasicTypes.DATE) );
		registerFunction( "to_timestamp", new StandardSQLFunction("to_timestamp", StandardBasicTypes.TIMESTAMP) );
		registerFunction( "to_number", new StandardSQLFunction("to_number", StandardBasicTypes.BIG_DECIMAL) );

		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(","||",")" ) );

		registerFunction( "locate", new PositionSubstringFunction() );

		registerFunction( "str", new SQLFunctionTemplate(StandardBasicTypes.STRING, "cast(?1 as varchar)") );

		getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE);
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName );
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "nextval ('" + sequenceName + "')";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName; //starts with 1, implicitly
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	@Override
	public String getCascadeConstraintsString() {
		return "";//" cascade";
	}
	@Override
	public boolean dropConstraints() {
		return true;
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public String getQuerySequencesString() {
		return "select relname from pg_class where relkind='S'";
	}

	@Override
	public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
		return new PostgreSQL83LimitHandler(sql, selection);
	}

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select currval('" + table + '_' + column + "_seq')";
	}

	@Override
	public String getIdentityColumnString(int type) {
		return type==Types.BIGINT ?
			"bigserial not null" :
			"serial not null";
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}

	@Override
	public String getNoColumnsInsertString() {
		return "default values";
	}

	@Override
	public Class<?> getNativeIdentifierGeneratorClass() {
		return SequenceGenerator.class;
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public boolean useInputStreamToInsertBlob() {
		return false;
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	/**
	 * Workaround for postgres bug #1453
	 */
	@Override
	public String getSelectClauseNullString(int sqlType) {
		String typeName = getTypeName(sqlType, 1, 1, 0);
		//trim off the length/precision/scale
		int loc = typeName.indexOf('(');
		if (loc>-1) {
			typeName = typeName.substring(0, loc);
		}
		return "null::" + typeName;
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public boolean supportsTemporaryTables() {
		return true;
	}

	@Override
	public String getCreateTemporaryTableString() {
		return "create temporary table";
	}

	@Override
	public String getCreateTemporaryTablePostfix() {
		return "on commit drop";
	}

	/*public boolean dropTemporaryTableAfterUse() {
		//we have to, because postgres sets current tx
		//to rollback only after a failed create table
		return true;
	}*/

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select now()";
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		return bool ? "true" : "false";
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	/**
	 * Constraint-name extractor for Postgres contraint violation exceptions.
	 * Orginally contributed by Denny Bartelt.
	 */
	private static final ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		@Override
		public String extractConstraintName(SQLException sqle) {
			try {
				int sqlState = Integer.valueOf(JdbcExceptionHelper.extractSqlState(sqle));
				switch (sqlState) {
					// CHECK VIOLATION
					case 23514: return extractUsingTemplate("violates check constraint \"","\"", sqle.getMessage());
					// UNIQUE VIOLATION
					case 23505: return extractUsingTemplate("violates unique constraint \"","\"", sqle.getMessage());
					// FOREIGN KEY VIOLATION
					case 23503: return extractUsingTemplate("violates foreign key constraint \"","\"", sqle.getMessage());
					// NOT NULL VIOLATION
					case 23502: return extractUsingTemplate("null value in column \"","\" violates not-null constraint", sqle.getMessage());
					// RESTRICT VIOLATION
					case 23001: return null;
					// ALL OTHER
					default: return null;
				}
			} catch (NumberFormatException nfe) {
				return null;
			}
		}
	};

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		// Register the type of the out param - PostgreSQL uses Types.OTHER
		statement.registerOutParameter(col, Types.OTHER);
		col++;
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return (ResultSet) ps.getObject(1);
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	//only necessary for postgre < 7.4
	//http://anoncvs.postgresql.org/cvsweb.cgi/pgsql/doc/src/sgml/ref/create_sequence.sgml
	@Override
	protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) {
		return getCreateSequenceString( sequenceName ) + " start " + initialValue + " increment " + incrementSize;
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// seems to not really...
//	public boolean supportsRowValueConstructorSyntax() {
//		return true;
//	}

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		// seems to have spotty LOB suppport
		return false;
	}
}