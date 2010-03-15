package ch.vd.vuta.model;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class HostDataSource extends DriverManagerDataSource {

	private String schema;

	public HostDataSource() {

	}

	/**
	 * @return the schema
	 */
	public String getSchema() {
		return schema;
	}

	/**
	 * @param schema
	 *            the schema to set
	 */
	public void setSchema(String schema) {
		this.schema = schema;
	}

}
