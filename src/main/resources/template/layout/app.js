function showPopup(source, text, hideTimeout) {
	var popup;

	if (source.substring) {
		popup = document.getElementById(source);
	} else {
		for (var i = 0; i < source.childNodes.length; i++) {
			if (source.childNodes[i].className === "popuptext") {
				popup = source.childNodes[i];
				break;
			}
		}
	}

	if (popup) {
		if (text) {
			popup.innerHTML = text;
		}
		popup.classList.toggle("show");

		if (hideTimeout) {
			setTimeout(function () {
				popup.classList.remove("show");
			}, hideTimeout);
		}
	} else if (text) {
		alert(text);
	}
}

function dismiss(popup) {
	popup.classList.remove('show');
	enableClick(popup.parentElement);
}

function hide(event, popup){
	popup.classList.remove("show");
	event.stopPropagation();
	event.preventDefault();
}

function hasChildren(node) {
	if (node.tagName != null) {
		var children = node.tagName.toUpperCase() === "FORM" ? node.elements : node.childNodes;
		if (children && children.length > 0) {
			return true;
		}
	}
	return false;
}

function createRequestString(data, recurse) {
	var data_string = '';

	if (data.tagName) {
		var elem = data.tagName.toUpperCase() === "FORM" ? data.elements : data.childNodes;
		for (var i = 0; i < elem.length; i++) {
			var e = elem[i];
			
			if (e.tagName == null || (e.disabled != null && e.disabled !== false) || e.type === "button") {
				continue;
			}

			if(e.hasAttribute("name")) {
				if (e.tagName.toUpperCase() === "SELECT") {
					if (data_string.length > 0 && !data_string.endsWith("&")) {
						data_string += "&";
					}
					data_string += e.name + "=" + encodeURIComponent(e.options[e.selectedIndex].value);
					continue;
				} else if (e.type === "checkbox") {
					if(e.checked === false && e.value != null && e.value !== "on"){
						continue;
					}

					if (data_string.length > 0 && !data_string.endsWith("&")) {
						data_string += "&";
					}

					data_string += e.name + "=";

					if(e.checked){
						data_string += (e.value == null ? "true" : e.value);
					} else {
						data_string += "false";
					}

					continue;
				} else if (e.tagName.toUpperCase() === 'INPUT' || e.tagName.toUpperCase() === 'TEXTAREA') {
					if (data_string.length > 0 && !data_string.endsWith("&")) {
						data_string += "&";
					}

					if (e.type === 'number' && !e.checkValidity()) {
						throw "Invalid form values"
					}

					data_string += e.name + (e.value !== "" ? "=" + encodeURIComponent(e.value) : "=");
					continue;
				}
			}

			var inner = hasChildren(e) ? createRequestString(e) : '';
			if (inner.length > 0) {
				if (data_string.length > 0) {
					if (!data_string.endsWith("&")) {
						data_string += "&";
					}
					data_string += inner;
				} else {
					data_string = inner;
				}
			}
		}
		return data_string;
	}
	if (data instanceof Array) {
		for (var i = 0; i < data.length; i++) {
			if (data_string !== "" && !data_string.endsWith("&")) {
				data_string += "&";
			}
			data_string += recurse + "[" + i + "]=" + data[i];
		}
		return data_string;
	}
	for (var i in data) {
		if (data_string !== "" && !data_string.endsWith("&")) {
			data_string += "&";
		}
		if (typeof data[i] == "object") {
			if (recurse == null) {
				data_string += createRequestString(data[i], i);
			} else {
				data_string += createRequestString(data[i], recurse + "[" + i + "]");
			}
		}
		else if (recurse == null) {
			data_string += i + "=" + data[i];
		} else {
			data_string += recurse + "[" + i + "]=" + data[i];
		}
	}
	return data_string;
}

function toJsonObj(text) {
	return JSON && JSON.parse(text) || $.parseJSON(text);
}

function submitFormAjax(source, endpoint, string_data, on_success, cleanup) {
	submitAjax(source, 'POST', endpoint, string_data, on_success, cleanup)
}

function submitAjax(source, method, endpoint, string_data, on_success, cleanup) {
	//document.getElementById("save_spinner").style.display = "block";
	var xmlhttp;
	if (window.XMLHttpRequest) {
		xmlhttp = new XMLHttpRequest();
	} else {
		xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
	}

	xmlhttp.onreadystatechange = function () {
		if (xmlhttp.readyState === 4) {
			try {
				if (xmlhttp.status === 200) {
					var response = toJsonObj(xmlhttp.responseText);

					if (response.error) {
						showPopup(source, response.error);
					} else {
						on_success(response);
					}
				} else {
					showPopup(source, "Error contacting server");
				}
			} finally {
				if (cleanup) {
					cleanup();
				}
			}
		}
	};

	xmlhttp.open(method, endpoint, true);
	xmlhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	xmlhttp.send(string_data);
}


function copyToClipboard(elementId) {
	try {
		document.getElementById(elementId).select();
		document.execCommand("copy");
	} catch (e) {
		//ignore.
	}
}

function toggle(source) {
	var checkboxes = document.getElementsByName(source.name);
	for (var i = 0, n = checkboxes.length; i < n; i++) {
		if (!checkboxes[i].disabled) {
			checkboxes[i].checked = source.checked;
		}
	}
}

function disableClick(source) {
	source.originalClickHandler = source.onclick;
	source.onclick = function (event) {
		event.preventDefault();

	}
}

function enableClick(source) {
	if (source.originalClickHandler) {
		setTimeout(function () {
			source.onclick = source.originalClickHandler;
		}, 800);
	}
}

function upon(test, fn) {
	if (typeof(test) == 'function' && test()) {
		fn();
	} else if (typeof(test) == 'string' && window[test]) {
		fn();
	} else {
		setTimeout(function () {
			upon(test, fn);
		}, 50);
	}
}

function saveForm(source, formId, endpoint) {
	document.getElementById("save_spinner").style.display = "block";
	try {
		disableClick(source);
		var data = createRequestString(document.getElementById(formId));

		submitFormAjax(source, endpoint + "save", data, function (response) {
			if (response.saved) {
				showPopup("save_msg", "Saved", 1500);
			}
		}, function () {
			enableClick(source);
			document.getElementById("save_spinner").style.display = "none";
		});
	} catch (err) {
		showPopup("save_msg", err, 1500);
		enableClick(source);
		document.getElementById("save_spinner").style.display = "none";
	}
}

function switchClass(element, clazz, toRemove) {
	if (clazz && !element.classList.contains(clazz)) {
		element.classList.add(clazz);
	}
	if (toRemove) {
		element.classList.remove(toRemove);
	}
}

function insertAtCursor(field, value) {
	if (document.selection) { //IE
		field.focus();
		document.selection.createRange().text = value;
	} else if (field.selectionStart || field.selectionStart === 0) {
		var startPos = field.selectionStart;
		var endPos = field.selectionEnd;
		field.value = field.value.substring(0, startPos) + value + field.value.substring(endPos, field.value.length);

		if (window.navigator.userAgent.indexOf("Edge") > -1) {// MS Edge
			var pos = startPos + value.length;
			field.focus();
			field.setSelectionRange(pos, pos);
		}
	} else {
		field.value += value;
	}
}

function removeChildren(element) {
	while (element.firstChild) {
		element.removeChild(element.firstChild);
	}
}

function isNotBlank(s){
	return (/\S/.test(s));
}

function isEmailValid(email) {
	var val = /\S+@\S+\.\S+/;
	return val.test(email);
}

function validateEmail(field) {
	if (isEmailValid(field.value)) {
		field.setCustomValidity("");
	} else {
		field.setCustomValidity("Invalid e-mail address.");
	}
}

function hasErrors(parent) {
	if (parent.tagName) {
		var elem = parent.tagName.toUpperCase() === "FORM" ? parent.elements : parent.childNodes;
		for (var i = 0; i < elem.length; i++) {
			if(elem[i].tagName) {
				var tag = elem[i].tagName.toUpperCase();
				if ((tag === 'INPUT' || tag === 'TEXTAREA') && !elem[i].checkValidity()) {
					return true;
				}
				if(hasChildren(elem[i]) && hasErrors(elem[i])){
					return true;
				}
			}
		}
	}
	return false;
}

function countOccurrences(str, search) {
	var out = 0;
	var pos = 0;

	while ((pos = str.indexOf(search, pos)) >= 0) {
		out++;
		pos++;
	}

	return out;
}