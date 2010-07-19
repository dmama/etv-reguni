package ch.vd.uniregctb.dao.jdbc.meta;

import java.util.List;

public class Entity {
	private String discriminant;
	private Class<?> type;
	private List<Column> columns;

	public Entity(String discriminant, Class<?> type) {
		this.discriminant = discriminant;
		this.type = type;
	}

	public String getDiscriminant() {
		return discriminant;
	}

	public Class<?> getType() {
		return type;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public void setColumns(List<Column> columns) {
		this.columns = columns;
	}
}
