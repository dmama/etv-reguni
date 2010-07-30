<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<c:if test="${param['inputId'] != null && param['dataSource'] != null && (param['readonly'] == null || !param['readonly']) }">
	<div id="<c:out value='${param.inputId}' />_autocomplete" class="autocompleteContainer"></div>
	<script type="text/javascript">
		var <c:out value='${param.inputId}' />_autoComplete =  new AutoComplete("<c:out value='${param.inputId}' />", "<c:out value='${param.inputId}' />_autocomplete");
		var item = <c:out value='${param.inputId}' />_autoComplete;
		item.setDataTextField("<c:out value='${param.dataTextField}' />");
		item.setDataValueField("<c:out value='${param.dataValueField}' />");
		item.setDataSource("<c:out value='${param.dataSource}' />");
		<c:if test="${param.onChange != null or !empty param.onChange}">
		item.onChange =  <c:out value='${param.onChange}' />;
		</c:if>
		<c:if test="${param.autoSynchrone != null or !empty param.autoSynchrone}">
		item.setAutoSynchrone( <c:out value='${param.autoSynchrone}' />);
		</c:if>
	</script>
</c:if>
