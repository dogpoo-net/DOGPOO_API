package net.dogpoo.api.common.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

public class BooleanTypeHandler implements TypeHandler<Boolean> {
	private static final Integer YES = Integer.valueOf(1);

	private static final Integer NO = Integer.valueOf(0);

	private Integer booleanToYesNo(boolean b) {
		if (b) {
			return YES;
		}
		return NO;
	}

	private boolean yesNoToBoolean(Integer s) {
		return (YES.equals(s));
	}

	public void setParameter(PreparedStatement ps, int i, Boolean parameter,
			JdbcType jdbcType) throws SQLException {
		ps.setInt(i, booleanToYesNo(parameter.booleanValue()).intValue());
	}

	public Boolean getResult(ResultSet rs, String columnName)
			throws SQLException {
		return Boolean.valueOf(yesNoToBoolean(Integer.valueOf(rs
				.getInt(columnName))));
	}

	public Boolean getResult(ResultSet rs, int columnIndex) throws SQLException {
		return Boolean.valueOf(yesNoToBoolean(Integer.valueOf(rs
				.getInt(columnIndex))));
	}

	public Boolean getResult(CallableStatement cs, int columnIndex)
			throws SQLException {
		return Boolean.valueOf(yesNoToBoolean(Integer.valueOf(cs
				.getInt(columnIndex))));
	}
}