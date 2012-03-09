<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<tiles:getAsString name='refresh' ignore='true'/>
		<link rel="SHORTCUT ICON" href="<c:url value="/images/favicon.ico"/>">

		<link media="screen" href="<c:url value="/css/x/jquery-ui.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/screen-all.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/supergra.css"/>" rel="stylesheet" type="text/css">

		<script type="text/javascript" language="javascript">
			function getContextPath() {
			  return '<c:url value="/"/>';
			}
		</script>

		<%@ include file="/WEB-INF/jsp/include/png-workaround.jsp" %>

		<link media="print" href="<c:url value="/css/print/print-all.css"/>" rel="stylesheet" type="text/css">

		<script type="text/javascript" language="Javascript" src="<c:url value="/js/unireg.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery-all.js"/>"></script>

		<title><tiles:getAsString name='title' ignore='false'/></title>
		<tiles:getAsString name='head' ignore='true'/>
	</head>
	<body>

			<div id="sommaire">
				<div style="position: absolute;top: 5px;left: 5px;"><div id="loadingImage" style="visibility: hidden;"><img id="loadingImageSign" src="<c:url value="/images/loading.gif"/>" /></div></div>
				<div class="canton">
					<a href="http://www.vd.ch" target="_blank">
						<span class="label"><fmt:message key="label.canton.vaud" /></span>
					</a>
				</div>

				<h3>Actions</h3>

				<tiles:getAsString name='actions' />
			</div>

			<div id="header" class="supergra">
				<span class="departement"><a href="http://www.aci.vd.ch" target="_blank"><fmt:message key="label.aci" /></a></span>
				<unireg:testMode>
					<div style="color:LightGray; left:360px; top:20px; position:absolute; font-size:21pt; font-weight:bold; z-index:100">
						<span style="color:white;font-size:21pt;font-weight: bold">(</span>
							<unireg:environnement/>
						<span style="color:white;font-size:21pt;font-weight: bold">)</span>
					</div>
				</unireg:testMode>
				<div class="application_supergra iepngfix"></div>

				<%-- le champ d'accès rapide dans l'entête de l'application --%>
				<script type="text/javascript">
					quickSearchTarget='/supergra/entity.do?class=Tiers&id=';
				</script>
				<div class="quicksearch">
					<fmt:message key="label.acces.rapide"/>&nbsp;
					<input class="quicksearch" size="15" type="text" 
						onKeyPress="return Quicksearch.onKeyPress(this, event);"
						onfocus="Quicksearch.onFocus(this, '<fmt:message key="label.acces.rapide.invite"/>')"
						onblur="Quicksearch.onBlur(this, '<fmt:message key="label.acces.rapide.invite"/>')"
						value="<fmt:message key="label.acces.rapide.invite"/>"/> 
				</div>
			</div>

			<div id="content">
				<div id="outils" class="outils">
					<h3>Outils</h3>
					<tiles:getAsString name='tools' ignore='true'/>
					<ul>
						<li>
						<unireg:user></unireg:user>
						</li>
						<tiles:getAsString name='vue' ignore='true'/>
						<li>
							<a href="<c:url value='/'/>" title="Recherche" accesskey="d"><fmt:message key="label.recherche" /></a>
						</li>
						<li>
							<tiles:getAsString name='fichierAide' ignore='true'/>
						</li>
						<li>
							<a href="/fiscalite/kbaci/advancedSearch.htm" title="Base ACI" accesskey="b"><fmt:message key="label.kbaci" /></a>
						</li>
						<li>
							<a href="/iam/accueil/" target="_blank" title="Portail IAM" accesskey="x"><fmt:message key="label.portail.iam" /></a>
						</li>
						<li>
							<a href="<c:url value='/logoutIAM.do'/>" title="Quitter Unireg" accesskey="l"><fmt:message key="label.deconnexion" /></a>
						</li>
					</ul>
				</div>
				<div style="padding: 5px;">
					<div class="empty" style="height: 10px;">&nbsp;</div>
					<br>

					<c:if test="${flash != null && flash.active}">
						<div id="flashdisplay" class="<c:out value='${flash.displayClass}'/>"><c:out value="${flash.messageForDisplay}"/></div>
						<c:if test="${flash.timeout > 0}">
							<script type="text/javascript">
								$('#flashdisplay').delay(<c:out value="${flash.timeout}"/>).fadeOut('slow');
							</script>
						</c:if>
					</c:if>

					<div id="globalErrors">
						<spring:hasBindErrors name="command">
							<c:set var="globalErrorCount" value="0"/>
							<c:if test="${errors.globalErrorCount > 0}">
								<c:forEach var="error" items="${errors.globalErrors}">
									<c:if test="${unireg:startsWith(error.code, 'global.error')}">
										<c:set var="globalErrorCount" value="${globalErrorCount+1}"/>
									</c:if>
								</c:forEach>
							</c:if>
							<c:if test="${globalErrorCount > 0}">
								<table class="action_error" cellspacing="0" cellpadding="0" border="0">
									<tr><td class="heading"><fmt:message key="label.action.problemes.detectes"/></td></tr>
									<tr><td class="details"><ul>
									<c:forEach var="error" items="${errors.globalErrors}">
										<c:if test="${unireg:startsWith(error.code, 'global.error')}">
											<li class="err"><spring:message message="${error}" /></li>
										</c:if>
									</c:forEach>
									</ul></td></tr>
								</table>
							</c:if>
						</spring:hasBindErrors>
					</div>
					<div class="workaround_IE6_bug">
						<tiles:getAsString name='body' />
					</div>
				</div>
			</div>

			<div id="footer">
				<b><fmt:message key="version" /></b>&nbsp;&nbsp;&nbsp;(Build: <fmt:message key="buildtime"/>)
				&nbsp;&nbsp;&nbsp;<b><unireg:environnement/></b>
			</div>

			<%@ include file="/WEB-INF/jsp/include/tabs-workaround.jsp" %>

			<script type="text/javascript" language="Javascript1.3">
				function ouvrirAide(url) {
					window.open(url, "aide", "top=100, left=750, screenY=50, screenX=100, width=500, height=800, directories=no, location=no, menubar=no, status=no, toolbar=no, resizable=yes");
				}
			</script>

	</body>

</html>
