<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.bouclements.contribuable" /></tiles:put>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${command.pmId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du contribuable" />

		<fieldset>
			<legend><span><fmt:message key="label.exercices.commerciaux" /></span></legend>

			<c:if test="${not empty exercices}">
				<display:table name="exercices" id="ex" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator" requestURI="/exercices/list.do">
					<display:column titleKey="label.date.debut" style="width: 50%;">
						<c:choose>
							<c:when test="${ex.first}">
								<form:form action="change-date-debut.do" commandName="command" method="post">
									<unireg:regdate regdate="${ex.dateDebut}"/>
									&nbsp;
									<c:set var="idbase">
										<unireg:regdate regdate="${ex.dateDebut}" format="yyyyMMdd"/>
									</c:set>
									<span id="dd-${idbase}-ro" class="bouclement-edit-field"><unireg:raccourciModifier link="#dd-${idbase}" onClick="DateBouclementEdition.initiateEditDateDebut('${idbase}');"/></span>
									<span style="display: none;" id="dd-${idbase}-rw" class="bouclement-edit-field">
										<img alt="" src="<c:url value="/images/right-arrow.png"/>" style="height: 1em;"/>
										&nbsp;
										<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
											<jsp:param name="path" value="nouvelleDate"/>
											<jsp:param name="id" value="nouvelleDate-${idbase}"/>
										</jsp:include>
										&nbsp;
										<form:hidden path="pmId"/>
										<input type="hidden" name="ancienneDate" value="<unireg:regdate regdate="${ex.dateDebut}"/>">
										<input type="submit" value="Valider"/>
										<unireg:buttonTo name="Abandonner" action="/exercices/edit.do" params="{pmId:${command.pmId}}" method="GET"/>
									</span>
								</form:form>
							</c:when>
							<c:otherwise>
								<unireg:regdate regdate="${ex.dateDebut}"/>
							</c:otherwise>
						</c:choose>
						<c:if test="${ex.first}">
						</c:if>
					</display:column>
					<display:column titleKey="label.date.fin" style="width: 50%;">
						<c:choose>
							<c:when test="${!ex.withDI && !ex.tooOldToHaveDI}">
								<form:form action="change-date-fin.do" commandName="command" method="post">
									<unireg:regdate regdate="${ex.dateFin}"/>
									&nbsp;
									<c:set var="idbase">
										<unireg:regdate regdate="${ex.dateFin}" format="yyyyMMdd"/>
									</c:set>
									<span id="df-${idbase}-ro" class="bouclement-edit-field"><unireg:raccourciModifier link="#df-${idbase}" onClick="DateBouclementEdition.initiateEditDateFin('${idbase}');"/></span>
									<span style="display: none;" id="df-${idbase}-rw" class="bouclement-edit-field">
										<img alt="" src="<c:url value="/images/right-arrow.png"/>" style="height: 1em;"/>
										&nbsp;
										<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
											<jsp:param name="path" value="nouvelleDate"/>
											<jsp:param name="id" value="nouvelleDate-${idbase}"/>
										</jsp:include>
										&nbsp;
										<form:hidden path="pmId"/>
										<input type="hidden" name="ancienneDate" value="<unireg:regdate regdate="${ex.dateFin}"/>">
										<input type="submit" value="Valider"/>
										<unireg:buttonTo name="Abandonner" action="/exercices/edit.do" params="{pmId:${command.pmId}}" method="GET"/>
									</span>
								</form:form>
							</c:when>
							<c:otherwise>
								<unireg:regdate regdate="${ex.dateFin}"/>
							</c:otherwise>
						</c:choose>
					</display:column>
				</display:table>
			</c:if>

			<c:if test="${dateDebutAjoutable}">
				<form:form action="ajoute-nouvelle-date-debut.do" commandName="command" method="post">
					<table border="0">
						<tbody>
						<tr>
							<td>
								<span id="dd-new-ro" class="bouclement-edit-field">
									<unireg:raccourciAjouter display="label.bouton.ajouter.nouvelle.date.debut.exercice.commercial"
									                         tooltip="label.bouton.ajouter.nouvelle.date.debut.exercice.commercial" link="#dd-new" onClick="DateBouclementEdition.initiateAddDateDebut();"/>
								</span>
								<span style="display: none;" id="dd-new-rw" class="bouclement-edit-field">
									<img alt="" src="<c:url value="/images/right-arrow.png"/>" style="height: 1em;"/>
									&nbsp;
									<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
										<jsp:param name="path" value="nouvelleDate"/>
										<jsp:param name="id" value="nouvelleDate-new"/>
									</jsp:include>
									&nbsp;
									<form:hidden path="pmId"/>
									<input type="submit" value="Valider"/>
									<unireg:buttonTo name="Abandonner" action="/exercices/edit.do" params="{pmId:${command.pmId}}" method="GET"/>
								</span>
							</td>
						</tr>
						</tbody>
					</table>
				</form:form>
			</c:if>

		</fieldset>
		<!-- Fin Caracteristiques generales -->

		<!-- Scripts -->
		<script type="text/javascript">

			var DateBouclementEdition = {

				initiateAddDateDebut: function() {
					$('.bouclement-add-field').hide();
					$('#dd-new-rw').show();
					$('#nouvelleDate-new').focus();
				},

				initiateEditDateDebut: function(id) {
					$('.bouclement-edit-field').hide();
					$('#dd-' + id + '-rw').show();
					$('#nouvelleDate-' + id).focus();
				},

				initiateEditDateFin: function(id) {
					$('.bouclement-edit-field').hide();
					$('#df-' + id + '-rw').show();
					$('#nouvelleDate-' + id).focus();
				}

			};

			$(function() {
				<c:choose>
					<c:when test="${mode == 'add-dd'}">
						DateBouclementEdition.initiateAddDateDebut();
					</c:when>
					<c:when test="${mode == 'edit-dd'}">
						DateBouclementEdition.initiateEditDateDebut('<unireg:regdate regdate="${command.ancienneDate}" format="yyyyMMdd"/>');
					</c:when>
					<c:when test="${mode == 'edit-df'}">
						DateBouclementEdition.initiateEditDateFin('${mode_edit_df}');
					</c:when>
				</c:choose>
			});

		</script>

		<!-- Debut Bouton -->
		<table>
			<tr><td>
				<unireg:buttonTo name="Retour" action="/tiers/visu.do" method="get" params="{id:${command.pmId}}" />
			</td></tr>
		</table>
		<!-- Fin Bouton -->

	</tiles:put>

</tiles:insert>