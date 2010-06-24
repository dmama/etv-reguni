<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%@page import="java.util.List"%>
<%@page import="ch.vd.infrastructure.model.CollectiviteAdministrative"%>
<%@page import="ch.vd.uniregctb.security.IFOSecAuthenticationProcessingFilter"%>

<tiles:insert template="/WEB-INF/jsp/templates/templateLight.jsp">
	<tiles:put name="menu" type="String"></tiles:put>
	
	<tiles:put name="title" type="String">S&eacute;lection de l'OID de travail</tiles:put>
	<tiles:put name="body" type="String">
		<form action="index.do">

		&nbsp;
		<p>
		&nbsp;
		<select name="<%=IFOSecAuthenticationProcessingFilter.IFOSEC_OID_REQUEST_KEY%>">
		<%
			List<CollectiviteAdministrative> collectivites = (List<CollectiviteAdministrative>) request.getSession().getAttribute(IFOSecAuthenticationProcessingFilter.IFOSEC_OID_USER_LIST);
			
			for (CollectiviteAdministrative collectivite : collectivites) {
				if (collectivite != null) {
					%>
					<option value="<%=collectivite.getNoColAdm()%>"><%=collectivite.getNomCourt()%></option>
					<%
				}
			}
		%>
		</select>
		<br>
		<input type="submit" value="Choisir" />
		</form>
	</tiles:put>
</tiles:insert>
