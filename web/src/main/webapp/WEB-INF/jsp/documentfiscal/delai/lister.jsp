<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="command" type="ch.vd.unireg.documentfiscal.AutreDocumentFiscalView"--%>
<%--@elvariable id="documentRetourne" type="java.lang.Boolean"--%>

<fieldset>
	<legend><span><fmt:message key="label.delais" /></span></legend>
	<c:if test="${isAjoutDelaiAutorise}">
	<table border="0">
		<tr>
			<td>
				<unireg:linkTo name="Ajouter" title="Ajouter" action="/autresdocs/delai/ajouter.do" params="{id:${command.id}}" link_class="add"/>
			</td>
		</tr>
	</table>
	</c:if>
	<jsp:include page="delais.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
</fieldset>
