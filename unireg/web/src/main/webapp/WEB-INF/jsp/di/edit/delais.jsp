<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="depuisTache" value="${param.depuisTache}" />
<fieldset>
	<c:if test="${depuisTache == null && command.allowedDelai}">
		<legend><span><fmt:message key="label.delais" /></span></legend>
		<table border="0">
			<tr>
				<td>
					<a href="delai.do?idDI=${command.id}&height=120&width=650&index=&TB_iframe=true&modal=true" 
					class="add thickbox" title="Ajouter">&nbsp;<fmt:message key="label.bouton.ajouter" /></a>
				</td>
			</tr>
		</table>
		<jsp:include page="../../tiers/common/delai/delais.jsp">
			<jsp:param name="page" value="edit"/>
		</jsp:include>
	</c:if>

	<c:if test="${!command.allowedDelai || depuisTache != null}">
		<jsp:include page="../../tiers/common/delai/delais.jsp">
			<jsp:param name="page" value="visu"/>
		</jsp:include>
	</c:if>
	
</fieldset>