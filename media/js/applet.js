var appletjs = {



	attribute_html: function(name,value) {
		return ' '+name+'="'+value+'"';
	},



	param_html: function(name,value) {
		return '\n\t<param name="'+name+'" value="'+value+'" />';
	},



	isIE: function() {
		if (navigator == null) return false;
		return navigator.userAgent.match(/MSIE/) != null; },



	// classic applet-tags - for testing and comparison use only!
	applet_html: function(attributes, params, minimumVersion, fallbackContent) {
		var html = '<applet';
		for(key in attributes)
			html += appletjs.attribute_html(key, attributes[key]);
		if(attributes.mayscript)
			html += 'MAYSCRIPT';
		html += '>';

		for(key in params)
			html += appletjs.param_html(key, params[key]);

		html += (
			fallbackContent ||
			'This content requires Java.<br><a href="http://java.com">Go here</a> to download  and install the latest version of Java.' );

		html += '</applet>';
		return html;
	},





	either_html: function(attributes, params, minimumVersion, fallbackContent, IE) {

		var html = '\n<object';

		if (IE) {
			/*
			* Creates the opening object tag for IE using classid and codebase attributes.
			* If these attributes are not set, appletjs will request that the applet be run
			* with the latest installed JRE, or JDK 1.6, whichever is higher. For more information, see:
			*
			* http://download.oracle.com/javase/6/docs/technotes/guides/plugin/developer_guide/using_tags.html#object
			*/

			// Use specified or latest installed JRE:
			html += appletjs.attribute_html('classid', (attributes['classid'] || 'clsid:8AD9C840-044E-11D1-B3E9-00805F499D93'));
			// Use specified or latest installed JRE:
			html += appletjs.attribute_html('codebase', (attributes['codebase'] || 'http://java.sun.com/update/1.6.0/jinstall-6u23-windows-i586.cab#Version=1,6,0,23'));
		}
		else {
			/*
			* Creates the opening object tag for standards-compliant browsers. Requests the minumum JRE version
			* specified with minimumVersion.
			*/
			html += appletjs.attribute_html('type','application/x-java-applet;version=' + (minimumVersion || '1.6.0'));
		}

		if(attributes.width || attributes.height) {
			html += appletjs.attribute_html('width', attributes.width);
			html += appletjs.attribute_html('height', attributes.height);
		}

		if(attributes.id) {
			// make applet JS-scriptable from page.
			// i.e. where id=myAppletObjectID: document.myAppletObjectID.myPublicFunction();
			html += appletjs.attribute_html('id', attributes.id);
		}

		html += '>';

		for(key in attributes)
			if(key != 'width' && key != 'height' && key != 'id')
				html += appletjs.param_html(key, attributes[key]);
		for(key in params)
			html += appletjs.param_html(key, params[key]);

		html += '\n\t'+ (
			fallbackContent || 'This content requires Java.<br><a href="http://java.com">Go here</a> to download  and install the latest version of Java.'
		);

		html += '\n</object>';

		return html;
	},



	IE_html:	function(attributes, params, minimumVersion, fallbackContent) {
		return appletjs.either_html(attributes, params, minimumVersion, fallbackContent, true);
	},



	non_IE_html: function(attributes, params, minimumVersion, fallbackContent) {
		return appletjs.either_html(attributes, params, minimumVersion, fallbackContent, false);
	},



	// IE or non_IE based on isIE
	html: function(attributes, params, minimumVersion, fallbackContent) {
		return appletjs.either_html(attributes, params, minimumVersion, fallbackContent, appletjs.isIE());
	},



	// both IE and non_ID, using IE concealing tags
	mixed_html: function(attributes, params, minimumVersion, fallbackContent) {
		var html = '';
		html += '\n<!--[if !IE]> -->' + appletjs.non_IE_html(attributes, params, minimumVersion, fallbackContent) + '\n<!--<![endif]-->';
		html += '\n<!--[if IE]>' + appletjs.IE_html(attributes, params, minimumVersion, fallbackContent) + '\n<![endif]-->';
		return html;
	},



	// inserts correct tag-set directly into the element identified by 'elementID'
	into_div: function(html, div_ID) {
		document.getElementById(div_ID).innerHTML = html;
	}



}
