{% load hitcount_tags %}
{% load videos_tags %}

{% if video.youtube_embed_url %}
    onYouTubePlayerReady = typeof(b) == 'undefined' ? function(){} : onYouTubePlayerReady;
{% endif %}

(function(){
    /*yepnope1.0.2|WTFPL*/(function(a,b,c){function H(){var a=z;a.loader={load:G,i:0};return a}function G(a,b,c){var e=b=="c"?r:q;i=0,b=b||"j",u(a)?F(e,a,b,this.i++,d,c):(h.splice(this.i++,0,a),h.length==1&&E());return this}function F(a,c,d,g,j,l){function q(){!o&&A(n.readyState)&&(p.r=o=1,!i&&B(),n.onload=n.onreadystatechange=null,e(function(){m.removeChild(n)},0))}var n=b.createElement(a),o=0,p={t:d,s:c,e:l};n.src=n.data=c,!k&&(n.style.display="none"),n.width=n.height="0",a!="object"&&(n.type=d),n.onload=n.onreadystatechange=q,a=="img"?n.onerror=q:a=="script"&&(n.onerror=function(){p.e=p.r=1,E()}),h.splice(g,0,p),m.insertBefore(n,k?null:f),e(function(){o||(m.removeChild(n),p.r=p.e=o=1,B())},z.errorTimeout)}function E(){var a=h.shift();i=1,a?a.t?e(function(){a.t=="c"?D(a):C(a)},0):(a(),B()):i=0}function D(a){var c=b.createElement("link"),d;c.href=a.s,c.rel="stylesheet",c.type="text/css";if(!a.e&&(o||j)){var g=function(a){e(function(){if(!d)try{a.sheet.cssRules.length?(d=1,B()):g(a)}catch(b){b.code==1e3||b.message=="security"||b.message=="denied"?(d=1,e(function(){B()},0)):g(a)}},0)};g(c)}else c.onload=function(){d||(d=1,e(function(){B()},0))},a.e&&c.onload();e(function(){d||(d=1,B())},z.errorTimeout),!a.e&&f.parentNode.insertBefore(c,f)}function C(a){var c=b.createElement("script"),d;c.src=a.s,c.onreadystatechange=c.onload=function(){!d&&A(c.readyState)&&(d=1,B(),c.onload=c.onreadystatechange=null)},e(function(){d||(d=1,B())},z.errorTimeout),a.e?c.onload():f.parentNode.insertBefore(c,f)}function B(){var a=1,b=-1;while(h.length- ++b)if(h[b].s&&!(a=h[b].r))break;a&&E()}function A(a){return!a||a=="loaded"||a=="complete"}var d=b.documentElement,e=a.setTimeout,f=b.getElementsByTagName("script")[0],g={}.toString,h=[],i=0,j="MozAppearance"in d.style,k=j&&!!b.createRange().compareNode,l=j&&!k,m=k?d:f.parentNode,n=a.opera&&g.call(a.opera)=="[object Opera]",o="webkitAppearance"in d.style,p=o&&"async"in b.createElement("script"),q=j?"object":n||p?"img":"script",r=o?"img":q,s=Array.isArray||function(a){return g.call(a)=="[object Array]"},t=function(a){return Object(a)===a},u=function(a){return typeof a=="string"},v=function(a){return g.call(a)=="[object Function]"},w=[],x={},y,z;z=function(a){function h(a,b){function i(a){if(u(a))g(a,f,b,0,c);else if(t(a))for(h in a)a.hasOwnProperty(h)&&g(a[h],f,b,h,c)}var c=!!a.test,d=c?a.yep:a.nope,e=a.load||a.both,f=a.callback,h;i(d),i(e),a.complete&&b.load(a.complete)}function g(a,b,d,e,g){var h=f(a),i=h.autoCallback;if(!h.bypass){b&&(b=v(b)?b:b[a]||b[e]||b[a.split("/").pop().split("?")[0]]);if(h.instead)return h.instead(a,b,d,e,g);d.load(h.url,h.forceCSS||!h.forceJS&&/css$/.test(h.url)?"c":c,h.noexec),(v(b)||v(i))&&d.load(function(){H(),b&&b(h.origUrl,g,e),i&&i(h.origUrl,g,e)})}}function f(a){var b=a.split("!"),c=w.length,d=b.pop(),e=b.length,f={url:d,origUrl:d,prefixes:b},g,h;for(h=0;h<e;h++)g=x[b[h]],g&&(f=g(f));for(h=0;h<c;h++)f=w[h](f);return f}var b,d,e=this.yepnope.loader;if(u(a))g(a,0,e,0);else if(s(a))for(b=0;b<a.length;b++)d=a[b],u(d)?g(d,0,e,0):s(d)?z(d):t(d)&&h(d,e);else t(a)&&h(a,e)},z.addPrefix=function(a,b){x[a]=b},z.addFilter=function(a){w.push(a)},z.errorTimeout=1e4,b.readyState==null&&b.addEventListener&&(b.readyState="loading",b.addEventListener("DOMContentLoaded",y=function(){b.removeEventListener("DOMContentLoaded",y,0),b.readyState="complete"},0)),a.yepnope=H()})(this,this.document)

    var recordButtonCSS = [
        '#__paste_vid__wrapper__{{ video.slug }} {',
        '    position: relative;',
        '    height: {{ height|default:360 }}px;',
        '    width: {{ width|default:640 }}px;',
        '}',
        '#_paste_vid__pastevid-record-button {',
        '    display: block;',
        '    text-shadow: 0px 1px 0px black;',
        '    background: #D22;',
        '    color: white;',
        '    padding: 10px 10px;',
        '    text-decoration: none;',
        '    border-radius: 8px;',
        '    font: 15px sans-serif;',
        '    font-weight: bold;',
        '    z-index: 1000;',
        '}',
        '#_paste_vid__pastevid-record {',
        '    display: block;',
        '    position: absolute;',
        '    margin: 0;',
        '    padding: 0;',
        '    top: {{ height|default:360|add:-80 }}px;',
        '    left: {{ width|default:640|add:-160 }}px;',
        '    height: 30px;',
        '    width: 110px;',
        '    z-index: 90;',
        '    background: transparent;',
        '}',
        '#_paste_vid__pastevid-record-button:hover {',
        '    -webkit-box-shadow: 0px 0px 17px rgba(255, 255, 255, 1);',
        '    -moz-box-shadow: 0px 0px 17px rgba(255, 255, 255, 1);',
        '    box-shadow: 0px 0px 17px rgba(255, 255, 255, 1);',
        '}',
        '#_paste_vid__recorder {',
        '    position: absolute;',
        '    height: 1px;',
        '    width: 1px;',
        '}',
        '#_paste_vid__popup, #_paste_vid__popup2, #_paste_vid__video-not-available {',
        '    z-index: 100;',
        '    position: absolute;',
        '    height: {{ height|default:360|add:-60 }}px;',
        '    width: {{ width|default:640|add:-60 }}px;',
        '    top: 0;',
        '    left: 0;',
        '    padding: 30px;',
        '    background-color: #000000;',
        '    color: #ffffff;',
        '    font-family: "Arial", sans-serif;',
        '}',
        '#_paste_vid__meta {',
        '    position: absolute;',
        '    top: 10px;',
        '    left: 10px;',
        '    width: 320px;',
        '    background-color: rgba(0, 0, 0, 0.85);',
        '    color: #ffffff;',
        '    z-index: 900;',
        '    font-weight: bold;',
        '    font-family: "Arial", sans-serif;',
        '    padding: 10px;',
        '    cursor: default;',
        '}',
        '#_paste_vid__meta:hover {',
        '    text-decoration: underline;',
        '    cursor: pointer;',
        '}',
        '#_paste_vid__title {',
        '    font-size: 18px;',
        '}',
        '#_paste_vid__uploader {',
        '    font-size: 12px;',
        '}'
    ].join("\n");

    /**
     * Loads the Java applet.
     *
     **/
    function loadRecorder() {
        var $recorder = $("#_paste_vid__recorder");

        if($recorder.length > 0 && !confirm("The video recorder has already loaded. Reloading may cause unsaved changes to be lost.\n\nContinue?")){
            return;
        }

        $("#_paste_vid__pastevid-record-button").text("Loading...");
        $("#_paste_vid__applet_location").html('');
        applet = $("<applet id='_paste_vid__recorder' code='com.bixly.pastevid.driver.Launch' archive='http://{{ request.META.HTTP_HOST }}{{ MEDIA_URL }}applet/Launch.jar' width='650' height='250' >");
        applet.append('<param name="jnlp_href" value="http://{{ request.META.HTTP_HOST }}/media/applet/launch.jnlp">');
        applet.append('<param name="csrf_token" value="{{ csrf_token }}">');
        applet.append('<param name="user_id" value="{% if user_id %}{{ user_id}}{% else %}{{ user.id }}{% endif %}">');
        applet.append('<param name="an_tok" value="{{ an_tok }}">');
        applet.append('<param name="channel_id" value="{% if channel_id %}{{ channel_id }}{% endif %}">');
        applet.append('<param name="base_url" value="http://{{ request.META.HTTP_HOST }}/">');
        applet.append('<param name="mayscript" value="true">');
        applet.append('<param name="codebase_lookup" value="false">');
        /*
        var attributes = {
        	id:         "_paste_vid__recorder",
            code:       "com.bixly.pastevid.screencap.ScreenRecorder",
            archive:    "ScreenRecorder.jar, lib/commons-codec-1.4.jar, lib/commons-logging-1.1.1.jar, lib/httpclient-4.1.1.jar, lib/httpclient-cache-4.1.1.jar, lib/httpcore-4.1.jar, lib/httpmime-4.1.1.jar, lib/jmf.jar, lib/javaws.jar",
            width:      1,
            height:     1,
            mayscript:  true,
            classloader_cache: false
        };
        var parameters = {
        	jnlp_href:  "/media/applet/launch.jnlp",
        	csrf_token: "{{ csrf_token }}",
        	user_id:    "{{ user.id }}",
        	an_tok:     "{{ an_tok }}",
            base_url:   "http://{{ request.META.HTTP_HOST }}/"
        };
        var version =   "1.5";
        $("#_paste_vid__applet_location").html(appletjs.mixed_html(attributes, parameters, version));
        */
        $("#_paste_vid__applet_location").append(applet);
        setTimeout(function(){
            $("#_paste_vid__pastevid-record-button").text("Reload");
        }, 1000);
    }

    /**
     * Main execution part.
     *
     * Loads the embedded player by injecting in into the host HTML page.
     *
     **/
    yepnope([{
        load: ["http://{{ request.META.HTTP_HOST }}{{ MEDIA_URL }}js/jquery-1.5.1.min.js",
               "http://{{ request.META.HTTP_HOST }}{{ MEDIA_URL }}js/mediaelementjs/mediaelement-and-player.min.js",
               "http://{{ request.META.HTTP_HOST }}{{ MEDIA_URL }}js/script.js",
               "http://{{ request.META.HTTP_HOST }}{{ MEDIA_URL }}js/applet.js",
               "http://{{ request.META.HTTP_HOST }}{{ MEDIA_URL }}js/mediaelementjs/mediaelementplayer.css"],
        complete: function(){
            var $wrapper = $("<div id='__paste_vid__wrapper__{{ video.slug }}'></div>");
            $("#_paste_vid__master__{{ video.slug }}").after($wrapper);
            $wrapper = $("#__paste_vid__wrapper__{{ video.slug }}");

            $("head").append('<style>' + recordButtonCSS + '</style>');

            // Modal dialogs
            $wrapper.append(
                '<div id="_paste_vid__popup">' +
                '    <h2><strong>Screenbird Upload</strong></h2>' +
                '    <p>' +
                '        Video is still being processed on server.<br>' +
                '        Please wait...' +
                '    </p>' +
                '</div>'
            );
            $wrapper.append(
                '<div id="_paste_vid__popup2">' +
                '    <h2><strong>Youtube Upload</strong></h2>' +
                '    <p>Video is still being processed on Youtube.<br />Check video later after it has finished being processed on Youtube.</p>' +
                '</div>'
            );
            $wrapper.append(
                '<div id="_paste_vid__video-not-available">' +
                '    <h2><strong>Oops!</strong></h2>' +
                '    The video is not available. It has already been deleted on our server.<br />Thanks.' +
                '</div>'
            );
            $("#_paste_vid__popup").hide();
            $("#_paste_vid__popup2").hide();
            $("#_paste_vid__video-not-available").hide();

            $wrapper.append("<div id='_paste_vid__meta'><div id='_paste_vid__title'>{{ video.title }}</div><div id='_paste_vid__uploader'>{{ video.uploader }}</div></div>");

            $("#_paste_vid__meta").click(function(e){
                window.open("http://{{ request.META.HTTP_HOST }}{{ video.get_absolute_url }}", "_blank");
                return false;
            });

            // Pastevid's "Record Now" button.
            $wrapper.append(
                '<div id="_paste_vid__pastevid-record">' +
                '    <a href="javascript:;" id="_paste_vid__pastevid-record-button">Record Now</a>' +
                '</div>'
            );

            $wrapper.append('<div id="_paste_vid__applet_location"></div>');

            $("#_paste_vid__pastevid-record-button").click(loadRecorder);

            {% comment %}/*
                This is the part that does the embedding of the video.
                Determines the correct video player to load depending on the
                source of the video (Pastevid or YouTube).
            */{% endcomment %}
            {% if video.youtube_embed_url %}
                $wrapper.append(
                    '<object width="{{ width|default:640 }}" height="{{ height|default:360 }}" class="player-tag">' +
                    '    <param name="movie" value="{{ video.youtube_embed_url }}?wmode=transparent&fs=1&autoplay=1&enablejsapi=1&playerapiid=ytPlayer&rel=0"</param>' +
                    '    <param name="allowFullScreen" value="true"></param>' +
                    '    <param name="allowScriptAccess" value="always"></param>' +
                    '    <param name="wmode" value="transparent">' +
                    '    <atts name="id" value="ytPlayer"></atts>' +
                    '    <embed src="{{ video.youtube_embed_url }}?wmode=transparent&fs=1&autoplay=1&enablejsapi=1&playerapiid=ytPlayer&rel=0"' +
                    '           type="application/x-shockwave-flash"' +
                    '           allowfullscreen="true"' +
                    '           allowscriptaccess="always"' +
                    '           width="{{ width|default:640 }}" height="{{ height|default:360 }}"' +
                    '           id="ytPlayer"' +
                    '           wmode="transparent"></embed>' +
                    '</object>'
                );
            {% else %}
                {% if video_status == 'OK' %}
                    $wrapper.append(
                        '<video width="{{ width|default:640 }}"' +
                        '       height="{{ height|default:360 }}"' +
                        '       id="_paste_vid__player__{{ video.slug }}"' +
                        '       class="player-tag"' +
                        '       controls="controls"' +
                        '       preload="none"' +
                        '       autoplay="false"' +
                        '       poster="http://{{ request.META.HTTP_HOST }}{{ MEDIA_URL }}gfx/black-background.jpg">' +
                        '    {% if not video.expired %}{% if video %}<source type="video/{{ video.video_type }}" src="http://{{ request.META.HTTP_HOST }}{% url get-video-content video.id %}"/>{% endif %}{% endif %}' +
                        '    <object width="{{ width|default:640 }}"' +
                        '            height="{{ height|default:360 }}"' +
                        '            type="application/x-shockwave-flash"' +
                        '            data="http://{{ request.META.HTTP_HOST }}{{ MEDIA_URL }}js/mediaelementjs/flashmediaelement.swf">' +
                        '        <param name="movie" value="http://{{ request.META.HTTP_HOST }}{{ MEDIA_URL }}js/mediaelementjs/flashmediaelement.swf" />' +
                        '        <param name="autoplay" value="false" />' +
                        '        {% if video_status == 'OK' %}<param name="flashvars" value="controls=true&amp;file={% url get-video-content video.id %}" />{% endif %}' +
                        '        <img src="" width="{{ width|default:640 }}" height="{{ height|default:360 }}" title="No video playback capabilities" />' +
                        '    </object>' +
                        '</video>'
                    );
                {% else %}
                    $("#_paste_vid__video-not-available").show();
                {% endif %}
            {% endif %}

            // We only need `master` to have something predictable to append onto.
            $("#_paste_vid__master__{{ video.slug }}").remove();

            {% if video.youtube_embed_url %}
                (function(){
                    var ytplayer, yt_url = '';

                    // This function is automatically called by the player once it loads
                    // `onYouTubePlayerReady` is declared globally.
                    // This function is declared such that existing callbacks
                    // are (hopefully) not removed.
                    var originalOnYouTubePlayerReady = onYouTubePlayerReady;
                    onYouTubePlayerReady = function(playerId) {
                        originalOnYouTubePlayerReady(playerId);
                        ytplayer = document.getElementById(playerId);
                        yt_url = ytplayer.getVideoUrl()
                        ytplayer.addEventListener("onError", "onPlayerError");
                        ytplayer.addEventListener('onStateChange', 'playerStateChanged');
                    };

                    // Called when changing state
                    function playerStateChanged() {
                        var sStatus = ytplayer.getPlayerState();
                        if (sStatus == -1) {
                            console.log("Video has not started.");
                        } else if (sStatus == 0) {
                            console.log("Video has ended.");
                            $("#_paste_vid__pastevid-record-button").show();
                            $("#_paste_vid__meta").show();
                        } else if (sStatus == 1) {
                            console.log("Video is playing.");
                            $("#_paste_vid__pastevid-record-button").hide();
                            $("#_paste_vid__meta").hide();
                        } else if (sStatus == 2) {
                            console.log("Video is paused.");
                            $("#_paste_vid__pastevid-record-button").show();
                            $("#_paste_vid__meta").show();
                        } else if (sStatus == 3) {
                            console.log("Video is buffering.");
                        } else if (sStatus == 5) {
                            console.log("Video is cued.");
                        }
                    }
                    // This function is called when an error is thrown by the player
                    function onPlayerError(errorCode) {
                        {% if not yt_processing %}
                            if (errorCode==100) {
                                location.href = '/{{ video.slug }}/yt_url_removed/' + youtubeIDextract(yt_url);
                            } else {
                                alert("An error occured of type:" + errorCode);
                            }
                        {% else %}
                            $("#_paste_vid__popup2").show();
                        {% endif %}
                    }
                });
            {% else %}
                var player = new MediaElementPlayer("#_paste_vid__player__{{ video.slug }}", {
                    success: function(me){
                        (function(me){
                            if({% if video %}{% get_hit_count for video %}{% else %}0{% endif %} == 0){
                                $("#_paste_vid__popup").show();
                            }

                            {% if video %}
                                // add event listener
                                me.addEventListener('loadeddata',
                                function () {
                                    if({% get_hit_count for video %} == 0){
                                        $("#_paste_vid__popup").fadeOut();
                                    }
                                });
                                me.addEventListener('ended', function(e) {
                                    console.log("ended");
                                    $("#_paste_vid__pastevid-record-button").show();
                                    $("#_paste_vid__meta").show();
                                }, false);
                                me.addEventListener('pause', function(e) {
                                    console.log("paused");
                                    $("#_paste_vid__pastevid-record-button").show();
                                    $("#_paste_vid__meta").show();
                                }, false);
                                me.addEventListener('play', function(e) {
                                    console.log("play");
                                    $("#_paste_vid__pastevid-record-button").hide();
                                    $("#_paste_vid__meta").hide();
                                }, false);
                            {% endif %}
                        })(me);
                    }
                });
            {% endif %}

            (function(){
                // Display record button
                var video = document.getElementById('_paste_vid__player__{{ video.slug }}');
                if(!video.paused){
                    $("#_paste_vid__pastevid-record-button").hide();
                    $("#_paste_vid__meta").hide();
                }
                video.addEventListener("play", function () {
                    $("#_paste_vid__pastevid-record-button").hide();
                    $("#_paste_vid__meta").hide();
                    video.play();
                }, false);
                video.addEventListener("ended", function () {
                    $("#_paste_vid__pastevid-record-button").show();
                    $("#_paste_vid__meta").show();
                }, false);
                video.addEventListener("pause", function () {
                    $("#_paste_vid__pastevid-record-button").show();
                    $("#_paste_vid__meta").show();
                   video.pause();
                }, false);
            });
        }
    }]);
})();
