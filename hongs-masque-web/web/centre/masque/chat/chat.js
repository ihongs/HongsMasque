
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
    var proxy   = hsGetValue(opts, "proxy" );

    if (! proxy) {
        proxy   = location.origin+hsFixUri().replace(/\/$/, '');
    }

    this.formBox = formBox;
    this.chatBox = chatBox;
    this.proxy = proxy ;
    this.param = {
        site_id: siteId,
        mine_id: mineId,
        room_id: roomId,
        token  : token
    };
    this.mates = {};

    var url = proxy.replace(/^\w+:/, location.protocol == "https:" ? "wss:" : "ws:")
            + "/centre/masque/socket/"+siteId+"/"+mineId+"/"+roomId+"?token="+token;
    this.conn(url);
}
HsChat.prototype = {
    conn: function(url) {
        var that = this;
        function reco() {
            $.hsView(
                {
                    title: "通讯已断开, 您是否要重新连接?",
                    notes: "网络似乎出了点问题, 尝试多次但仍未成功, 您可以继续重新连接, 也可以选择关闭窗口.",
                    glass: "alert-warning",
                    alert: "static"
                },
                {
                    label: "重新连接" ,
                    glass: "btn-success",
                    click: function () {
                        that.conn(url);
                    }
                },
                {
                    label: "关闭" ,
                    glass: "btn-danger" ,
                    click: function () {
                        window.close();
                    }
                }
            );
        }

        wsobj = new WebSocket(url);
        wsobj.onmessage = function(ev) {
            that.recv(JSON.parse(ev.data));
        };
        wsobj.onclose = reco;
        wsobj.onerror = reco;
    },

    recv: function(rst) {
        rst = hsResponse(rst);

    },

    send: function(req) {

    },

    getCurrRoom: function() {
        $.hsAjax( {
            url : this.proxy + "/centre/masque/room/search.act",
            data: this.param,
            success: function(rst) {
                func(rst.info);
            }
        });
    },

    getMateInfo: function(func, mateId) {
        var req = { "mate_id" : mateId};
        $.hsAjax( {
            url : this.proxy + "/centre/masque/mate/search.act",
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
            url : this.proxy + "centre/masque/chat/search.act",
            data: $.extend(req, this.param),
            success: function(rst) {
                func(rst.list);
            }
        });
    },

    addChatLine: function(info, rever ) {

    }
};
