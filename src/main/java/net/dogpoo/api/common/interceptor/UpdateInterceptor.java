package net.dogpoo.api.common.interceptor;

import java.util.Properties;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;

@Intercepts({
		@org.apache.ibatis.plugin.Signature(type = org.apache.ibatis.executor.statement.StatementHandler.class, method = "update", args = { java.sql.Statement.class }),
		@org.apache.ibatis.plugin.Signature(type = org.apache.ibatis.executor.resultset.ResultSetHandler.class, method = "handleResultSets", args = { java.sql.Statement.class }),
		@org.apache.ibatis.plugin.Signature(type = org.apache.ibatis.executor.Executor.class, method = "getTransaction", args = {}),
		@org.apache.ibatis.plugin.Signature(type = org.apache.ibatis.executor.Executor.class, method = "commit", args = { boolean.class }),
		@org.apache.ibatis.plugin.Signature(type = org.apache.ibatis.executor.Executor.class, method = "rollback", args = { boolean.class }),
		@org.apache.ibatis.plugin.Signature(type = org.apache.ibatis.executor.Executor.class, method = "close", args = { boolean.class }) })
public class UpdateInterceptor implements Interceptor {
	public Object intercept(Invocation invocation) throws Throwable {
		return invocation.proceed();
	}

	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	public void setProperties(Properties properties) {
	}
}