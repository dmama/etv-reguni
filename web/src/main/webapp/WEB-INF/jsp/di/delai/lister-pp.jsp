<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<fieldset>
	<legend><span><fmt:message key="label.delais" /></span></legend>

	<c:choose>
		<c:when test="${command.allowedDelai && !command.depuisTache}">
			<table border="0">
				<tr>
					<td>
						<unireg:linkTo name="Ajouter" title="Ajouter" action="/di/delai/ajouter-pp.do" params="{id:${command.id}}" link_class="add"/>
					</td>
				</tr>
			</table>
			<jsp:include page="../../tiers/common/delai/delais-pp.jsp">
				<jsp:param name="page" value="edit"/>
			</jsp:include>
		</c:when>
		<c:otherwise>
			<jsp:include page="../../tiers/common/delai/delais-pp.jsp">
				<jsp:param name="page" value="visu"/>
			</jsp:include>
		</c:otherwise>
	</c:choose>

</fieldset>
