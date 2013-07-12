
// This function retrieves video id on url
function youtubeIDextract(url){ 
	var video_id = url.split('v=')[1];
	var ampersandPosition = video_id.indexOf('&');
	if(ampersandPosition != -1) {
		video_id = video_id.substring(0, ampersandPosition);
	}
return video_id; 
}

// This function retrieves video id on swf url
function youtubeIDextractSWF(swf_url){ 
	var video_id = swf_url.split('/v/')[1];
	var qPosition = video_id.indexOf('?');
	if(qPosition != -1) {
		video_id = video_id.substring(0, qPosition);
	}
return video_id; 
}

function youtube_uploaded(videoname,yt_link){
$.modal('<h2><strong>Youtube Upload</strong></h2>'+ 
	'<p>Your video,<strong>'+videoname+'</strong>,has already been uploaded on YouTube.'+
	'<br>Here is the <a href="http://www.youtube.com/watch?v='+youtubeIDextractSWF(yt_link)+'" target="_blank">link</a> to your video on YouTube.'+
	'<br> Thanks.</p>',	
	{onClose: function (dialog) {
	        dialog.data.fadeOut('fast', function () {
		    dialog.container.hide('fast', function () {
			dialog.overlay.slideUp('fast', function () {
				$.modal.close();
			});
		    });
	        });
            }});
}

function file_unavailable(videoname){
$.modal('<h2><strong>File Unavailable</strong></h2>'+ 
	'<p>Your video,<strong>'+videoname+'</strong>,has already been deleted on server'+
        '<br>when you uploaded it to YouTube.'+
	'<br>Thanks.</p>',	
	{onClose: function (dialog) {
	        dialog.data.fadeOut('fast', function () {
		    dialog.container.hide('fast', function () {
			dialog.overlay.slideUp('fast', function () {
				$.modal.close();
			});
		    });
	        });
            }});
}

function redirect_paypal(){
$.modal('<h2><strong>Paypal Subscription</strong></h2>'+ 
	'<p>Please wait for a moment. Redirecting to PayPal.</p>',	
	{onClose: function (dialog) {
	        dialog.data.fadeOut('fast', function () {
		    dialog.container.hide('fast', function () {
			dialog.overlay.slideUp('fast', function () {
				$.modal.close();
			});
		    });
	        });
            }});
}
