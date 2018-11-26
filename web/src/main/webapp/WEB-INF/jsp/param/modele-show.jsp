<fieldset style="margin: 10px" class="information">
	<legend><fmt:message key="label.param.modele"/></legend>
	<div class="button-add">
		<a href="modele-add.do?pf=${periodeSelectionnee.id}" class="add" title="${titleParametres}"><fmt:message key="label.param.add"/></a>
	</div>
	<table>
		<tr>
			<th class="colonneModele"><fmt:message key="title.param.periode" /></th>
			<th class="colonneModele"><fmt:message key="title.param.type"/></th>
			<th class="colonneModele">&nbsp;</th>
			<th class="colonneModeleAction"><fmt:message key="title.param.action"/></th>
		</tr>
		<%--@elvariable id="modeles" type="java.util.List"--%>
		<c:forEach var="modele" items="${modeles}">
			<tr class="odd">
				<td >${periodeSelectionnee.annee}</td>
				<td ><fmt:message key="option.type.document.${modele.typeDocument}"/></td>
				<td >
					<c:if test="${not empty error_modele[modele.id]}">
						<span class="error">${error_modele[modele.id]}</span>
					</c:if>&nbsp;
				</td>
				<td style="" class="colonneModeleAction">
					<unireg:raccourciAnnuler link="modele-suppr.do?md=${modele.id}&pf=${periodeSelectionnee.id}"/>
				</td>
			</tr>
		</c:forEach>
	</table>
	<c:if test="${not empty modeles}">
		<div style="margin-top: 5px"><fmt:message key="label.param.modele.select"/>:
			<select name="md" id="modele">
				<c:forEach var="modele" items="${modeles}">
					<c:set var="selected" value=""/>
					<%--@elvariable id="modeleSelectionne" type="ch.vd.unireg.declaration.ModeleDocument"--%>
					<c:if test="${modele.id == modeleSelectionne.id }">
						<c:set var="selected">
							selected="selected"
						</c:set>
					</c:if>
					<option value="${modele.id}" ${selected}>
						<fmt:message key="option.type.document.${modele.typeDocument}"/>
					</option>
				</c:forEach>
			</select>
		</div>

		<fieldset style="margin: 10px" class="information">
			<legend><fmt:message key="title.param.modele.feuille"/></legend>
			<table border="0">
				<tr>
					<td>
						<fmt:message key="option.type.document.${modeleSelectionne.typeDocument}" var="libTypeDocument"/>
						<fmt:message key="label.param.periode.et.modele" var="periodeEtModele">
							<fmt:param value="${periodeSelectionnee.annee}" />
							<fmt:param value="${libTypeDocument}" />
						</fmt:message>
						<a href="feuille/add.do?pf=${periodeSelectionnee.id}&md=${modeleSelectionne.id}" class="add" title="${periodeEtModele}"><fmt:message key="label.param.add"/></a>
					</td>
					<td width="25%">&nbsp;</td>
					<td width="25%">&nbsp;</td>
					<td width="25%">&nbsp;</td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
			<table>
				<tr>
					<th class="colonneFeuille"><fmt:message key="title.param.periode" /></th>
					<th class="colonneFeuille"><fmt:message key="title.param.num.cadev"/></th>
					<th class="colonneFeuille"><fmt:message key="title.param.num.form.aci"/></th>
					<th class="colonneFeuille"><fmt:message key="title.param.int.feuille"/></th>
					<th class="colonneFeuille">&nbsp;</th>
					<th class="colonneFeuilleAction" ><fmt:message key="title.param.action"/></th>
				</tr>
					<%--@elvariable id="feuilles" type="java.util.List"--%>
				<c:forEach var="feuille" varStatus="i" items="${feuilles}">
					<tr class="odd">
						<td class="colonneFeuille">${periodeSelectionnee.annee}</td>
						<td class="colonneFeuille">${feuille.noCADEV}</td>
						<td class="colonneFeuille">${feuille.noFormulaireACI}</td>
						<td class="colonneFeuille">
							<c:out value="${feuille.intituleFeuille}"/>
							<c:if test="${feuille.principal}">
								<span class="info_icon" style="margin-left: 1em;" title="Feuillet principal"></span>
							</c:if>
						</td>
						<td class="colonneFeuille">
							<c:if test="${not empty error_feuille[feuille.id]}">
								<span class="error">${error_feuille[feuille.id]}</span>
							</c:if>&nbsp;
						</td>
						<td class="colonneFeuilleAction" class="colonneAction">
							<c:if test="${i.index > 0}">
								<unireg:raccourciMoveUp link="feuille/move.do?mfd=${feuille.id}&dir=UP" tooltip="Monte d'un cran la feuille"/>
								<c:if test="${i.index == fn:length(feuilles) - 1}">
									<a href="#" class="padding noprint">&nbsp;</a>
								</c:if>
							</c:if>
							<c:if test="${i.index < fn:length(feuilles) - 1}">
								<unireg:raccourciMoveDown link="feuille/move.do?mfd=${feuille.id}&dir=DOWN" tooltip="Descend d'un cran la feuille"/>
							</c:if>
							<unireg:raccourciModifier link="feuille/edit.do?pf=${periodeSelectionnee.id}&md=${modeleSelectionne.id}&mfd=${feuille.id}" tooltip="${periodeEtModele}"/>
							<unireg:raccourciAnnuler link="feuille/suppr.do?pf=${periodeSelectionnee.id}&md=${modeleSelectionne.id}&mfd=${feuille.id}"/>
						</td>
					</tr>
				</c:forEach>
			</table>
		</fieldset>
	</c:if>
</fieldset>