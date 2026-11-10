package net.dogpoo.api.common.handler;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.StringTypeHandler;

public class EmptyToNullTypeHandler extends StringTypeHandler {
	public void setNonNullParameter(PreparedStatement ps, int i,
			String parameter, JdbcType jdbcType) throws SQLException {
		ps.setString(i, ("".equals(parameter)) ? null : parameter);
	}
}