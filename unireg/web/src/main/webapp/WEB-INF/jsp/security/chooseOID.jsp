<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%@page import="ch.vd.uniregctb.security.ChooseOIDProcessingFilter"%>

<tiles:insert template="/WEB-INF/jsp/templates/templateLight.jsp">
	<tiles:put name="menu" type="String"></tiles:put>
	
	<tiles:put name="title" type="String">Veuillez sélectionner un OID de travail</tiles:put>
	<tiles:put name="body" type="String">
		<form action="index.do">
			<p>
			<select name="<%=ChooseOIDProcessingFilter.IFOSEC_OID_REQUEST_KEY%>">
				<c:forEach items="${command.officesImpot}" var="oi">
					<option value="${oi.noColAdm}">${oi.nomCourt}</option>
				</c:forEach>
			</select>

			<input type="submit" value="Choisir" />
		</form>
	</tiles:put>
</tiles:insert>
