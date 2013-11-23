(function($){ 

    $.fn.makeOutline = function(player, opts){

        var tickers = [];        // Time codes of the points
        var headerCount = 0;     // Number of of points/headers
        var elemDrag = null;     // Set when a pin is being dragged
        var initialPos = 0;      // Initial position of the dragged pin
        var initialX = 0;        // Initial position of the mouse pointer
        var removed = null;      // Storage for temporarily removed elements
        var isIE = false;
        var startPosition = 0;   // Initial position on the player
        var draggingLimit = 0;   // Set to the width of the player
        var pinContainer = null;        // element where we put our pins
        var headerContainer = null;  //element where we put our headers
        var controller = null
        var insideControls = false;
        var insideVideo = false;
        
        var defaults = {
            canEditOutline: false,
            isFlash: false,
            loadURL: '',
            saveURL: '',
            controllerID: '',
            err: 13
        };
        
        var node = $(this);
        //console.log(node.offset().left);
        node.options = $.extend({},defaults,opts);
        init();

        function init(){
            headerContainer = $("<form>");
            node.append(headerContainer);
            node.append("<p id='outline-link'><a href='http://screenbird.com/H358PB9O/'>How to use this?</a></p>");
            pinContainer = $(".mejs-time-total", player.node);
            startPosition = 0;
            draggingLimit = startPosition + Number(pinContainer.css('width').replace("px",""));
            loadOutline();
            if(node.options.controllerID){ controller = $("#"+node.options.controllerID);}
            if(node.options.canEditOutline){
                headerContainer.submit(function(e){return false;});
                $(document).keydown(inputBoxHandler);
                $(document).mousemove(mouseMove);
                $(document).mouseup(mouseUp);
                if(controller){
                    controller.click(controllerHandler);
                }
            }
            $(window).resize(function(){
                pinContainer = $(".mejs-time-total", player.node);
                startPosition = 0;
                draggingLimit = startPosition + pinContainer.width();
                $(".outline-pin").remove();
                for(var i=0; i<tickers.length; i++){
                    addPin(i+1, tickers[i]);
                }
                $(".mejs-container .mejs-controls").css("opacity","100");
            });
            //For IE
            if(navigator.userAgent.indexOf("MSIE") >= 0){
                isIE = true;
                setInterval(function(e){
                    if(draggingLimit != pinContainer.width()){
                        startPosition = 0;
                        draggingLimit = startPosition + pinContainer.width();
                        $(".outline-pin").remove();
                        for(var i=0; i<tickers.length; i++){
                            addPin(i+1, tickers[i]);
                        }
                    }
                }, 1000);
            }
            // Autoplay - for Firefox
            if(!isNaN(player.duration) && player.duration !=0){
                if(player.paused){
                    player.play();
                }
            }else{
                setTimeout(function(){
                    if(player.paused){
                        player.play();
                    }
                }, 5000);
            }
        }
        function loadOutline(){
            if(node.options.loadURL){
                $.ajax({
                    type: "POST",
                    url: node.options.loadURL,
                    dataType: "json",
                    success: function(data){
                                if(data.length == 0){
                                    alwaysShowControls(false);
                                    if(!node.options.canEditOutline){
                                        node.hide(); 
                                        return
                                    }
                                }else{
                                    node.show();
                                    controller.hide();
                                }
                                for(var i =0; i < data.length; i++){
                                    fields = data[i].fields;
                                    var label = $("<label id='l"+ (headerCount+1) +"'>"+ (headerCount+1) +". </label>");
                                    var textBox = $("<span class='text' id='t"+ (headerCount+1) +"' number='"+ (headerCount+1) +"'>");
                                    textBox.html(fields['text']);
                                    if(node.options.canEditOutline){
                                        textBox.dblclick(editHeader);
                                    }
                                    textBox.click(jumpToTime);
                                    headerContainer.append(label);
                                    headerContainer.append(textBox);
                                    headerContainer.append("<br />");
                                    console.log(Number(fields['current_time']));
                                    tickers.push(Number(fields['current_time']));
                                    addPin(headerCount+1, Number(fields['current_time']));
                                    headerCount++;
                                }
                                if(node.options.canEditOutline){
                                    var label = $("<label id='l"+ (headerCount+1) +"'>"+ (headerCount+1) +". </label>");
                                    var inputBox = $("<input id='i"+ (headerCount+1) +"' type='text' number='"+ (headerCount+1) +"'>");
                                    headerContainer.append(label);
                                    headerContainer.append(inputBox);
                                    headerContainer.append("<br />");
                                }
                             }
                    });
            }
        }
        function saveOutline(){
            if(node.options.saveURL){
                var pins = [];
                var counter = 0;
                var sorted = tickers.sort(function(a,b){ return a-b });
                //console.log(tickers);
                $("span.text", node).each(function(){
                    var text = $(this).html();
                    var position = tickers[counter];
                    var pin = {'text': text, 'position': position};
                    pins.push(pin);
                    counter++;
                });
                $.ajax({
                    type: "POST",
                    url: node.options.saveURL,
                    data: { "pins":JSON.stringify(pins) },
                    dataType: "text",
                    success: function(data){
                        if(data=='OK'){
                        }
                    }
                });
            }
        }
        function addHeader(number){
            if($("#i"+number).length > 0){
                // there is text when you are only editing header
                if($("#i"+number).attr('value') != null && $("#i"+number).attr('value') != ''){
                    var prev = $("#i"+number);
                    var textBox = $("<span class='text' id='t"+ number +"' number='"+ number +"'>");
                    textBox.html( prev.attr('value') );
                    textBox.dblclick(editHeader);
                    textBox.click(jumpToTime);
                    prev.after(textBox);
                    prev.remove();
                    if(number > headerCount){ 
                        addPin(number);
                        setCurrentTimeToPositionOfThisPin(number);
                        headerCount++;
                        addHeaderForm();
                    }else{
                        var next = number+1;
                        while($("#i"+next).length == 0){
                            next++;
                        }
                        $("#i"+ next).focus();
                    }
                }
                saveOutline();
            }
        }
        function editHeader(e){
            var header = $(e.target);
            var number = header.attr('number');
            var input = $("<input id='i"+ number +"' type='text' number='"+ number +"'>");
            input.attr('value', header.html());
            header.after(input);

            //focus on input box
            input.focus();
            var inputs = $("input[number]");
            inputs.each(function(){
                if(!$(this).is(":focus")) {
                    number = $(this).attr("number");
                    addHeader(parseInt(number));
                }
            });
            input.focus();
            input.select();
            header.remove();
        }
        
        function addPin(number, currentTime){
            if(!isNaN(player.duration) && player.duration !=0){
                    var xPos = startPosition - 12;
                    if(currentTime || currentTime == 0){
                        xPos = getPositionOfThisPinGivenTime(currentTime)
                    }
                    var pin = $("<span class='outline-pin' number='"+ number +"'></span>");
                    var remove = $("<span class='outline-remove' number='"+ number +"'>x</span>");
                    var text = $("<span class='outline-text'>"+ number +"</span>");
                    var corner = $("<span class='outline-corner'></span>");
                    if(!isIE){
                        remove.css("display","none");
                    }
                    pin.append(remove);
                    pin.append(text);
                    pin.append(corner);
                    if(!currentTime && currentTime != 0){
                        xPos = getPositionOfThisPinGivenTime(player.currentTime);
                        if((number - 1) > 0){
                            if(xPos <= $(".outline-pin[number='"+ (number -1) +"']").position().left){
                                xPos = $(".outline-pin[number='"+ (number -1) +"']").position().left + 50;
                            }
                        }
                    }
                    if(xPos > draggingLimit){
                        xPos = draggingLimit - 15;
                    }
                    pin.css('left', xPos+"px");
                    pinContainer.append(pin);
                    if(canEditOutline){
                        remove.click(removePin);
                        pin.mousedown(mouseDown);
                        pin.mouseenter(mouseEnter);
                        pin.mouseleave(mouseLeave);
                    }
                    if(!currentTime){
                        setCurrentTimeToPositionOfThisPin(number);
                    }
            }else{
                setTimeout(function(){
                    addPin(number, currentTime);
                }, 2000);
            }
        }

        function removePin(e){
            var number = parseInt($(e.target).attr('number'));
            // Remove the pin on the player
            var pin = $(".outline-pin[number='"+ number +"']");
            pin.remove();
            // Remove its time entry on our time code container
            tickers.splice(number-1, 1);

            // Get the next pin
            pin = $(".outline-pin[number='"+ (number+1) +"']");
            var textBox = $("#t" + number);
            var newNum = number;
            // Adjust the number
            while(pin.length > 0){
                var temp = parseInt(pin.attr('number'));
                pin.attr('number', ''+newNum);
                // Change the number
                var text = $('.outline-text', pin);
                text.html(newNum);
                text.attr('number', ''+newNum);
                var remove = $('.outline-remove', pin);
                remove.attr('number', ''+newNum);
                // Change the header
                textBox.html($("#t" + temp).html());
                textBox = $("#t" + temp);
                
                pin = $(".outline-pin[number='"+ (temp+1) +"']");
                newNum = temp;
            }
            // Remove label and text. Decrement headerCount
            // Remove br
            $("#l" + headerCount).remove();
            $("#t" + headerCount).next().remove();
            $("#t" + headerCount).remove();
            // Remove br
            $("#l" + (headerCount+1)).remove();
            $("#i" + (headerCount+1)).next().remove();
            $("#i" + (headerCount+1)).remove();
            headerCount--;
            addHeaderForm();
            enableTimeFloat();
            if(headerCount == 0){
                node.hide();
                alwaysShowControls(false);
                if(controller){
                    controller.html("Create Outline");
                    controller.show();
                }
            }
            saveOutline();
        }    

        function mouseDown(e){
            player.pause();
            elemDrag = $(e.target);
            if(elemDrag.hasClass('outline-remove')){
                elemDrag = null;
                return;
            }
            while(!elemDrag.hasClass('outline-pin')){
                elemDrag = elemDrag.parent();
            }
            initialPos = elemDrag.position().left;
            initialX = e.pageX;
            return false;
        }

        function mouseUp(e){
            if(elemDrag){
                setCurrentTimeToPositionOfThisPin(elemDrag.attr('number'));
                elemDrag = null;
                initialPos = 0;
                initialX = 0;
                saveOutline();
            }
        }  
          
        function mouseEnter(e){
            // hide the time float while mouse is hovered in our pin
            disableTimeFloat();
            //show remove button
            var target = $(e.target)
            while(!target.hasClass('outline-pin')){
                target = target.parent();
            }
            var number = target.attr('number');
            if(number && !isIE){
                enableRemove(number);
            }
        }

        function mouseLeave(e){
            enableTimeFloat();
            // hide remove button
            var target = $(e.target)
            while(!target.hasClass('outline-pin')){
                target = target.parent();
            }
            var number = target.attr('number');
            if(number && !isIE){
                disableRemove(number);
            }
        }

        function mouseMove(e){
            if(elemDrag){
                var number = parseInt(elemDrag.attr('number'));
                var changeInX = e.pageX - initialX;
                var newPos = initialPos + changeInX;
                if($(".outline-pin[number='"+ (number -1) +"']").length > 0){
                    var prevX = $(".outline-pin[number='"+ (number -1) +"']").position().left;
                    if((prevX + 15) > newPos){
                        newPos = prevX + 15;
                    }
                }else if((startPosition - 12) > newPos){
                    newPos = startPosition - 12;
                }
                if($(".outline-pin[number='"+ (number + 1) +"']").length > 0){
                    var nextX = $(".outline-pin[number='"+ (number + 1) +"']").position().left;
                    if((nextX - 12) < newPos){
                        newPos = nextX - 12;
                    }
                }else if((draggingLimit - 12) < newPos){
                    newPos = draggingLimit - 12;
                }
                elemDrag.css( "left",  newPos+ "px");
                //setCurrentTimeToPositionOfThisPin(number);
            }
            
        }

        function enableRemove(number){
            $('.outline-remove[number="'+ number +'"]').show();
        }

        function disableRemove(number){
            $('.outline-remove[number="'+ number +'"]').hide();
        }

        function enableTimeFloat(){
            if(removed){
                 $("#replacement").after(removed);
                 $("#replacement").remove();
            }
        }

        function disableTimeFloat(){
            removed = $(".mejs-time-float");
            removed.after("<span id='replacement'>");
            removed.remove();
        }

        function setCurrentTimeToPositionOfThisPin(number){
            var percentage = ($(".outline-pin[number='"+ number +"']").position().left + node.options.err) / draggingLimit;
            console.log(percentage);
            console.log(player.duration);
            var newTime = percentage * player.duration;
            //console.log("New time "+newTime);
            player.startTime = newTime;
            player.setCurrentTime(newTime);
            if(tickers.length < number){
                tickers.push(newTime);
            }else{
                tickers[number-1] = newTime;
            }
        }
        function getPositionOfThisPinGivenTime(currentTime){
            var result = (currentTime / player.duration * draggingLimit) - node.options.err;
            return result;
        }

        function inputBoxHandler(e){
            if(e.keyCode == 13 || e.keyCode == 9){
                if (e.keyCode == 9) {
                    e.preventDefault();
                }
                var focused = $(":focus");
                if(focused.is("input") && focused.attr('number')){
                    var number = $(":focus").attr('number');
                    var offset = 0;
                    if (e.shiftKey) {
                        offset = -1;
                    } else {
                        offset = 1;
                    }
                    var next_number = parseInt(number) + offset;
                    var next_header = $("span#t" + next_number);
                    addHeader(parseInt(number));
                    next_header.dblclick();
                }
                return false;
            }
        }

        function jumpToTime(e){
            var header = $(e.target);
            var number = header.attr('number');
            player.setCurrentTime(tickers[number-1]);
            player.play();
        }

        function addHeaderForm(){
            var nextHeaderNumber = headerCount+1;
            if($("#i"+nextHeaderNumber).length == 0){
                var label = $("<label id='l"+ nextHeaderNumber +"'>"+ nextHeaderNumber +". </label>");
                var inputBox = $("<input id='i"+ nextHeaderNumber +"' type='text' number='"+ nextHeaderNumber +"'>");
                headerContainer.append(label);
                headerContainer.append(inputBox);
                headerContainer.append("<br />");
                inputBox.focus();
            }
        }    
        function controllerHandler(e){
            if(!node.is(":visible")){
                node.show();
                controller.hide();
                if(headerCount ==0){
                    alwaysShowControls(true);
                }
            }
            return false;
        } 
        function alwaysShowControls(showControl){
            if(node.options.isFlash){
                $player = $("#me_flash_0_container");
            }else{
                $player = $("#player");
            }
            if(showControl){
                $(".mejs-container .mejs-controls").css("opacity","100");
                $player.unbind('mouseenter mouseleave');
                $(".mejs-container .mejs-controls").unbind('mouseenter mouseleave');
            }else{
                $player.unbind('mouseenter mouseleave');
                $(".mejs-container .mejs-controls").unbind('mouseenter mouseleave');
                $player.bind('mouseenter', function(e){
                    insideVideo = true;
                    $(".mejs-container .mejs-controls").css("opacity","100");
                });
                $player.bind('mouseleave', function(e){
                    insideVideo = false;
                    setTimeout(function(e){
                            if(!insideControls && !insideVideo){
                                $(".mejs-container .mejs-controls").css("opacity","0");
                            }
                        }, 500);
                });
                $(".mejs-container .mejs-controls").mouseenter(function(e){ insideControls = true; $(".mejs-container .mejs-controls").css("opacity","100");});
                $(".mejs-container .mejs-controls").mouseleave(
                    function(e){ 
                        insideControls = false;
                        setTimeout(function(e){
                            if(!insideVideo){
                                $(".mejs-container .mejs-controls").css("opacity","0");
                            }
                        }, 500);
                    }
                );
            }
        }   
    }
})(jQuery);

