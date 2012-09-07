<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<fieldset>
	<c:if test="${!command.depuisTache && command.allowedDelai}">
		<legend><span><fmt:message key="label.delais" /></span></legend>
		<table border="0">
			<tr>
				<td>
					<unireg:linkTo name="&nbsp;Ajouter" title="Ajouter" action="/di/delai/ajouter.do" params="{id:${command.id}}" link_class="add"/>
				</td>
			</tr>
		</table>
		<jsp:include page="../../tiers/common/delai/delais.jsp">
			<jsp:param name="page" value="edit"/>
		</jsp:include>
	</c:if>

	<c:if test="${!command.allowedDelai || command.depuisTache}">
		<jsp:include page="../../tiers/common/delai/delais.jsp">
			<jsp:param name="page" value="visu"/>
		</jsp:include>
	</c:if>
	
</fieldset>
