
function HsChat( context, opts ) {
    context = jQuery( context  );
    context.data("HsChat", this);
    context.addClass( "HsChat" );

    var sendBox = context.find(".sendbox");
    var chatBox = context.find(".chatbox");
    var siteId  = hsGetValue(opts, "siteId");
    var mineId  = hsGetValue(opts, "mineId");
    var roomId  = hsGetValue(opts, "roomId");
    var token   = hsGetValue(opts, "token" );
    var proxy   = hsGetValue(opts, "proxy" );

    this.sendBox = sendBox;
    this.chatBox = chatBox;
    this.proxy = proxy || hsFixUri("centre/masque");
    this.param = {
         token : token ,
        site_id: siteId,
        mine_id: mineId,
        room_id: roomId
    };
    this.mates = {};

    this.init();
    this.conn();
}
HsChat.prototype = {
    init: function() {
        var that = this;
        this.sendBox.on("submit", function(ev) {
            that.send(hsSerialDic(this));
            ev.stopPropagation();
            ev.preventDefault ();
            return false;
        });
    },

    conn: function() {
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
                    glass: "btn-info" ,
                    click: function () {
                          that.conn ();
                    }
                },
                {
                    label: "关闭",
                    glass: "btn-link" ,
                    click: function () {
                        window.close();
                    }
                }
            );
        }

        /**
         * 代理 URL 格式:
         *  http://abc/xyz
         *  //abc/xyz
         *  /xyz
         */
        var proto = location.protocol === "https:" ? "wss:" : "ws:";
        var proxy =  this.proxy ;
        if (!/^\w+:/.test(proxy)) {
        if (!/^\/\//.test(proxy)) {
            proxy = location.host + proxy;
        }
            proxy = proto  + "//" + proxy;
        } else {
            proxy = proxy.replace(/^\\w+:/, proto);
        }

        var wsobj = new WebSocket(proxy + "/socket/"
              + this.param.site_id  + "/"
              + this.param.mine_id  + "/"
              + this.param.room_id  + "?token="
              + this.param.token
            );
        this.wso = wsobj;
        wsobj.onclose = reco;
        wsobj.onerror = reco;
        wsobj.onmessage = function(ev) {
            that.recv(JSON.parse(ev.data));
        };
    },

    send: function(req) {
        this.wso.send(JSON.stringify(req));
    },

    recv: function(rst) {
        rst = hsResponse(rst);
        if (rst.ok) {
            this.addChatLine(rst.info);
        }
    },

    getCurrRoom: function(func) {
        $.hsAjax( {
            url : this.proxy + "/room/search.act",
            data: this.param,
            success: function(rst) {
                rst = hsResponse(rst);
                if (rst.ok) {
                    func(rst.list[0] || {});
                }
            }
        });
    },

    getMateInfo: function(func, mateId) {
        var req = { "mate_id" : mateId};
        $.hsAjax( {
            url : this.proxy + "/mate/search.act",
            data: $.extend(req, this.param),
            success: function(rst) {
                rst = hsResponse(rst);
                if (rst.ok) {
                    func(rst.list[0] || {});
                }
            }
        });
    },

    getChatList: function(func, ctime ) {
        var req = this.ctime
                ? {"ctime:lt" : ctime }
                : {      "ab" :"fresh"};
        $.hsAjax( {
            url : this.proxy + "/chat/search.act",
            data: $.extend(req, this.param),
            success: function(rst) {
                rst = hsResponse(rst);
                if (rst.ok) {
                    func(rst.list || [  ] );
                }
            }
        });
    },

    addChatLine: function(info) {
        var chatItem = $(
          '<div class="chat-item">'
        + '<div class="chat-line">'
        + '<div class="chat-head">'
        + '<div class="chat-head-icon"></div>'
        + '</div>'
        + '<div class="chat-body">'
        + '<div class="chat-body-name"></div>'
        + '<div class="chat-body-text"></div>'
        + '</div>'
        + '</div>'
        + '</div>'
        ).appendTo(this.chatBox);
        var chatLine = chatItem.children();
        var chatHead = chatItem.find(".chat-head");
        var chatIcon = chatItem.find(".chat-head-icon");
        var chatName = chatItem.find(".chat-body-name");
        var chatText = chatItem.find(".chat-body-text");
        var mateInfo = this.mates [ info.mate_id ];
        var mateId   = info.mate_id;
        var that     = this;

        if (mateId === this.param.mine_id) {
            chatItem.addClass("chat-mine");
            chatLine.append  ( chatHead  );
        } 

        switch (info.kind) {
            default:
                chatText.text( info.note );
        }

        if (mateInfo) {
            chatName.text( mateInfo.name );
            chatIcon.css ("background-url", mateInfo.head);
            return;
        }

        this.getMateInfo(function(mateInfo) {
            that.mates [mateId] = mateInfo;
            chatName.text( mateInfo.name );
            chatIcon.css ("background-url", mateInfo.head);
        }, mateId);
    }
};
