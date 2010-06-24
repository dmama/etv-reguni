<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />
<tiles:insert template="/WEB-INF/jsp/templates/templateIFrame.jsp">
	<tiles:put name="head">
		<style>
			span.bouton {
				width: 50%;
				text-align: center;
			}
		</style>
	</tiles:put>

	<tiles:put name="title"></tiles:put>
	<tiles:put name="body">
	<form:form name="form" id="formTermes">
		<fieldset>
			<legend><fmt:message key="label.param.parametres-pf-edit" /></legend>
			<table>
			<tr>
				<th></th>
				<th><fmt:message key="label.param.entete.VD"/></th>
				<th><fmt:message key="label.param.entete.HC"/></th>
				<th><fmt:message key="label.param.entete.HS"/></th>
				<th><fmt:message key="label.param.entete.dep"/></th>
			</tr>
			<tr>
				
				<th><fmt:message key="label.param.som.reg"/></th>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationReglementaireVaud" />
						<jsp:param name="id" value="sommationReglementaireVaud" />
						<jsp:param name="form" value="formTermes"/>
					</jsp:include>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationReglementaireHorsCanton" />
						<jsp:param name="id" value="sommationReglementaireHorsCanton" />
						<jsp:param name="form" value="formTermes"/>
					</jsp:include>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationReglementaireHorsSuisse" />
						<jsp:param name="id" value="sommationReglementaireHorsSuisse" />
						<jsp:param name="form" value="formTermes"/>
					</jsp:include>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationReglementaireDepense" />
						<jsp:param name="id" value="sommationReglementaireDepense" />
						<jsp:param name="form" value="formTermes"/>
					</jsp:include>
				</td>
				
			</tr>
			<tr>
				<th><fmt:message key="label.param.som.eff"/></th>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationEffectiveVaud" />
						<jsp:param name="id" value="sommationEffectiveVaud" />
						<jsp:param name="form" value="formTermes"/>
					</jsp:include>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationEffectiveHorsCanton" />
						<jsp:param name="id" value="sommationEffectiveHorsCanton" />
						<jsp:param name="form" value="formTermes"/>
					</jsp:include>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationEffectiveHorsSuisse" />
						<jsp:param name="id" value="sommationEffectiveHorsSuisse" />
						<jsp:param name="form" value="formTermes"/>
					</jsp:include>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationEffectiveDepense" />
						<jsp:param name="id" value="sommationEffectiveDepense" />
						<jsp:param name="form" value="formTermes"/>
					</jsp:include>
				</td>
			</tr>
			<tr>
				<th><fmt:message key="label.param.masse.di"/></th>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="finEnvoiMasseDIVaud" />
						<jsp:param name="id" value="finEnvoiMasseDIVaud" />
						<jsp:param name="form" value="formTermes"/>
					</jsp:include>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="finEnvoiMasseDIHorsCanton" />
						<jsp:param name="id" value="finEnvoiMasseDIHorsCanton" />
						<jsp:param name="form" value="formTermes"/>
					</jsp:include>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="finEnvoiMasseDIHorsSuisse" />
						<jsp:param name="id" value="finEnvoiMasseDIHorsSuisse" />
						<jsp:param name="form" value="formTermes"/>
					</jsp:include>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="finEnvoiMasseDIDepense" />
						<jsp:param name="id" value="finEnvoiMasseDIDepense" />
						<jsp:param name="form" value="formTermes"/>
					</jsp:include>
				</td>
			</tr>
		</table>
		</fieldset>
		<div>
			<span class="bouton">
				<input type="submit" id="maj" value="<fmt:message key="label.bouton.mettre.a.jour" />">
			</span>
			<span class="bouton">
				<input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler" />" onclick="self.parent.tb_remove()">
			</span>
		</div>
	</form:form>	
	</tiles:put>
</tiles:insert>
