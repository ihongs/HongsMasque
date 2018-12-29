
function HsChat( context, opts ) {
    context = jQuery( context  );
    context.data("HsChat", this);
    context.addClass( "HsChat" );

    var formBox = context.find( "form"   );
    var chatBox = context.find(".chatbox");
    var siteId  = hsGetValue(opts, "siteId");
    var mineId  = hsGetValue(opts, "mineId");
    var roomId  = hsGetValue(opts, "roomId");
    var token   = hsGetValue(opts, "token" );

    this.formBox = formBox;
    this.chatBox = chatBox;
    this.mates = {};
    this.param = {
        site_id: siteId,
        mine_id: mineId,
        room_id: roomId,
        token  : token
    };

    this.conn(siteId+"/"+mineId+"/"+roomId+"?token="+token);
}
HsChat.prototype = {
    conn: function(uri) {
        wsobj = new WebSocket(hsFixUri("centre/masque/socket/"+uri));
        wsobj.onmessage = function(ev) {

        };
        var that = this;
        function reconnect() {
            hsNote(
                "通讯已断开, 即将重连...", "warning",
                 function () { that.conn( uri ); }, 3
            );
        }
        wsobj.onclose = reconnect;
        wsobj.onerror = reconnect;
    },

    getCurrRoom: function() {
        $.hsAjax( {
            url : hsFixUri("centre/masque/room/search.act"),
            data: this.param,
            success: function(rst) {
                func(rst.info);
            }
        });
    },

    getMateInfo: function(func, mateId) {
        var req = { "mate_id" : mateId};
        $.hsAjax( {
            url : hsFixUri("centre/masque/mate/search.act"),
            data: $.extend(req, this.param),
            success: function(rst) {
                func(rst.info);
            }
        });
    },

    getChatList: function(func, ctime ) {
        var req = this.ctime
                ? {"ctime:lt" : ctime }
                : {      "ab" :"fresh"};
        $.hsAjax( {
            url : hsFixUri("centre/masque/chat/search.act"),
            data: $.extend(req, this.param),
            success: function(rst) {
                func(rst.list);
            }
        });
    },

    addChatLine: function(info, rever ) {

    }
};
