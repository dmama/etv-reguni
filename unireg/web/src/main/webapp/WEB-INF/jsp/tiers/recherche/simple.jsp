<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<fieldset>
	<legend><span><fmt:message key="label.recherche.simple"/></span></legend>
	<table><tr>
		<td style="vertical-align:middle"><input type="text" id="simple-search-input" width="100%" class="text ui-widget-content ui-corner-all" placeholder="Entrez vos critères de recherche ici" autofocus/></td>
		<td width="12px"></td>
		<td style="vertical-align:middle" width="2%"><a href="#" onclick="return Search.toogleMode();" style="font-size:11px">recherche avancée</a></td>
	</tr></table>
</fieldset>

<div id="simple-search-results"><%-- cette DIV est mise-à-jour par Ajax --%></div>
