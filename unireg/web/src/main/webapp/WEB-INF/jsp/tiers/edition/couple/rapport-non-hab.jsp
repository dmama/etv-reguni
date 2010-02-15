<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Rapport Menage Commun -->
<fieldset class="coupleMenageCommun">
	<legend><span><fmt:message key="label.rapport.menage.commun" /></span></legend>
	<table>
		<c:set var="ligneForme" value="${ligneForme + 1}" scope="request" />
		<tr class="<c:if test="${(ligneForme % 2) == 0 }">even</c:if><c:if test="${ligneForme % 2 == 1}">odd</c:if>">
			<td colspan="2">
				<form:radiobutton path="nouveauCtb" id="nouveauCtb" value="true" onclick="nouveauCtbHandle()"/>
				<label for="nouveauCtb"><fmt:message key="label.couple.nouveau.contribuable" /></label>
				<script type="text/javascript" language="Javascript">
					function nouveauCtbHandle() {
						togglePanels('nouveauCtb', 'ctbExistantPanel', 'nouveauCtbPanel');
					}
					Element.addObserver(window, "load" , nouveauCtbHandle);
				</script>
				<div id="nouveauCtbPanel" style="display: none;">
					<table>
						<tr class="<c:if test="${(ligneForme % 2) == 0 }">even</c:if><c:if test="${ligneForme % 2 == 1}">odd</c:if>">
							<td width="10%">&nbsp;</td>
							<td width="150px"><fmt:message key="label.date.debut" />&nbsp;:</td>
							<td>
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="dateDebut" />
									<jsp:param name="id" value="dateDebut" />
								</jsp:include>
								<font color="#FF0000">*</font>
							</td>
						</tr>
					</table>
				</div>
			</td>
		</tr>
		<c:set var="ligneForme" value="${ligneForme + 1}" scope="request" />
		<tr class="<c:if test="${(ligneForme % 2) == 0 }">even</c:if><c:if test="${ligneForme % 2 == 1}">odd</c:if>">
			<td colspan="2">
				<form:radiobutton path="nouveauCtb" id="ctbExistant" value="false" onclick="ctbExistantHandle()" />
				
				<c:url var="ctbURL" value="/couple/list-ctb.do?TB_iframe=true&modal=true&height=350&width=800">
					<c:if test="${not empty command.premierePersonne}">
						<c:param name="numeroPP1" value="${command.premierePersonne.numero}" />
					</c:if>
					<c:if test="${not empty command.secondePersonne}">
						<c:param name="numeroPP2" value="${command.secondePersonne.numero}" />
					</c:if>
				</c:url>
				
				<label for="ctbExistant"><fmt:message key="label.couple.contribuable.existant" /></label>
				<script type="text/javascript" language="Javascript">
					function ctbExistantHandle() {
						togglePanels('ctbExistant', 'nouveauCtbPanel', 'ctbExistantPanel');
						if (!ctbInitialized) {
							rechercheCtb();
						}
					}
					function rechercheCtb() {
						tb_show("", "<c:out value="${ctbURL}" />");
						ctbInitialized=true;
					}
					var ctbInitialized=<c:out value="${not command.nouveauCtb}"/>;
				</script>
				<div id="ctbExistantPanel" style="display: none;">
					<table>
						<c:set var="contribuableHasErrors" value="${false}"/>
						<spring:hasBindErrors name="command">
							<spring:bind path="command.troisiemeTiers">
								<c:if test="${errors.fieldErrorCount > 0}">
									<tr>
										<td colspan="2">
											<form:errors path="troisiemeTiers" cssClass="error"/>
											<c:set var="contribuableHasErrors" value="${true}"/>
										</td>
									</tr>
								</c:if>
							</spring:bind>
						</spring:hasBindErrors>
						
						<c:if test="${not empty command.troisiemeTiers}">
							<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
								<td width="10%">&nbsp;</td>
								<td>
									<table>
										<tr>
											<td width="150px"><fmt:message key="label.date.debut" />&nbsp;:</td>
											<td>
												<c:choose>
													<c:when test="${not empty command.troisiemeTiers and command.troisiemeTiers.natureTiers == 'MenageCommun'}">
														<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
															<jsp:param name="path" value="dateCoupleExistant" />
															<jsp:param name="id" value="dateCoupleExistant" />
														</jsp:include>
														<font color="#FF0000">*</font>
													</c:when>
													<c:otherwise>
														<unireg:regdate regdate="${command.dateCoupleExistant}" />
													</c:otherwise>
												</c:choose>
											</td>
										</tr>
									</table>
								</td>
							</tr>
						</c:if>
						
						<tr>
							<td width="10%">&nbsp;</td>
							<td>
								<a id="searchLink" href="javascript:;" class="replay" onclick="rechercheCtb();return false">&nbsp;<fmt:message key="label.couple.contribuable.chercher"/></a>
							</td>
						</tr>
						<tr class="<c:if test="${(ligneForme % 2) == 0 }">even</c:if><c:if test="${ligneForme % 2 == 1}">odd</c:if>" >
							<td width="10%">&nbsp;</td>
							<td>
								<c:choose>
									<c:when test="${not empty command.troisiemeTiers}">
										<jsp:include page="../../../general/contribuable.jsp">
											<jsp:param name="page" value="couple" />
											<jsp:param name="path" value="troisiemeTiers" />
											<jsp:param name="className" value="coupleContribuableOuvert"/>
										</jsp:include>
									</c:when>
									<c:otherwise>
										<c:if test="${not contribuableHasErrors}">
											<fmt:message key="label.couple.aucun.contribuable" />
										</c:if>
									</c:otherwise>
								</c:choose>
							</td>
						</tr>
					</table>
				</div>
			</td>
		</tr>
		<c:set var="ligneForme" value="${ligneForme + 1}" scope="request" />
		<tr class="<c:if test="${(ligneForme % 2) == 0 }">even</c:if><c:if test="${ligneForme % 2 == 1}">odd</c:if>">
			<td width="25%"><fmt:message key="label.commentaire" />&nbsp;:</td>
			<td width="75%">
				<form:textarea path="remarque" id="remarque" cols="80" rows="5"/>
			</td>
		</tr>
	</table>
	
</fieldset>
<!-- Fin Rapport Menage Commun -->