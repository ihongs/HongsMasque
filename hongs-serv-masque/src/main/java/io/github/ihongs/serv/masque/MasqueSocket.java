package io.github.ihongs.serv.masque;

import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.SocketHelper;
import io.github.ihongs.action.VerifyHelper;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.reflex.Async;
import io.github.ihongs.util.verify.Wrongs;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.websocket.OnOpen;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * 消息外部通道
 *
 * <pre>
 * 共有三种模式
 * 用户聊天 : /centre/masque/socket/{site_id}/{mine_id}/{room_id}
 * 用户通知 : /centre/masque/socket/{site_id}/{mine_id}/!
 * 离线通知 : /centre/masque/socket/{site_id}/0/.
 * </pre>
 *
 * @author hong
 */
@ServerEndpoint(
    configurator = SocketHelper.Config.class,
    value = "/centre/masque/socket/{site_id}/{mine_id}/{room_id}"
)
public class MasqueSocket {

    @OnOpen
    public void onOpen(Session sess) {
        try (
            SocketHelper hepr = SocketHelper.getInstance(sess, "open");
        ) {
            Map          prop = sess.getUserProperties();
            Map          data = new HashMap(/***/);
            VerifyHelper veri = new VerifyHelper();
            Async< Map > pipe = MasqueTunnel.getCeiver();

            /**
             * 这里相较 Action 的校验不同
             * 不能使用 ActionHelper 获取请求数据
             * 只能通过 VerifyHelper 的值传递会话
             * 校验过程中以此提取请求、会话数据等
             */
            try {
                data.putAll( hepr.getRequestData() );
                veri.addRulesByForm( "masque", "auth" );
                data = veri.verify(data, false , true );
                data.put("mate_id",data.get("mine_id"));
                veri.getRules().clear( );
                veri.addRulesByForm( "masque", "chat" );
            } catch (Wrongs wr) {
                hepr.reply( wr.toReply( (byte) 0 ) );
                sess.close();
                return;
            } catch (HongsException ex ) {
                CoreLogger.error  ( ex );
                hepr.fault(ex.getLocalizedMessage());
                sess.close();
                return;
            }

            // 注入环境备后用
            prop.put("data", data);
            prop.put(VerifyHelper.class.getName(), veri);
            prop.put(MasqueTunnel.class.getName(), pipe);

            addSession(sess);
        }
        catch (Exception|Error er) {
            CoreLogger.error ( er);

            delSession(sess);
            try {
                sess.close();
            }
            catch (IOException ex) {
                CoreLogger.error(er);
            }
        }
    }

    @OnMessage
    public void onMessage(Session sess, String msg) {
        try (
            SocketHelper hepr = SocketHelper.getInstance(sess, "message");
        ) {
            Map          prop = sess.getUserProperties();
            Map          data = ( Map ) prop.get("data");
            VerifyHelper veri = (VerifyHelper) prop.get(VerifyHelper.class.getName());
            Async< Map > pipe = (Async< Map >) prop.get(MasqueTunnel.class.getName());

            // 解析数据
            Map dat;
            if (msg.startsWith("{") && msg.endsWith("}") ) {
                dat = (  Map  ) Dawn.toObject(msg);
            } else {
                dat = ActionHelper.parseQuery(msg);
            }

            // 验证数据
            try {
                dat.putAll(data);
                dat.put("id", Core.newIdentity());
                dat = veri.verify( dat, false, true);
            } catch (Wrongs wr) {
                hepr.reply( wr.toReply(( byte ) 9 ));
                return;
            } catch (HongsException ex ) {
                hepr.fault(ex.getLocalizedMessage());
                CoreLogger.error (  ex );
                return;
            }

            // 送入管道
            pipe.add(dat);
        }
        catch (Exception|Error er) {
            CoreLogger.error ( er);
        }
    }

    @OnError
    public void onError(Session sess, Throwable ar) {
        try (
            SocketHelper hepr = SocketHelper.getInstance(sess, "error");
        ) {
            delSession(sess);
            CoreLogger.debug ( ar.getMessage() );
        }
        catch (Exception|Error er) {
            CoreLogger.error ( er);
        }
    }

    @OnClose
    public void onClose(Session sess) {
        try (
            SocketHelper hepr = SocketHelper.getInstance(sess, "close");
        ) {
            delSession(sess);
        }
        catch (Exception|Error er) {
            CoreLogger.error ( er);
        }
    }

    //** 静态工具方法 **/

    public static final Map<String, Map<String, Map<String, Set<Session>>>> SESSIONS = new HashMap(); // site_id:room_id:mate_id:[Session]

    synchronized private static void addSession(Session sess) {
        String sid = (String) sess.getPathParameters().get("site_id");
        String rid = (String) sess.getPathParameters().get("room_id");
        String mid = (String) sess.getPathParameters().get("mine_id");

        Map<String, Map<String, Set<Session>>> siteRoom;
        Map<String, Set<Session>> roomMate;
        Set<Session> mateSess;

        siteRoom = SESSIONS.get(sid);
        if (siteRoom == null) {
            siteRoom = new HashMap();
            SESSIONS.put(sid, siteRoom);
        }

        roomMate = siteRoom.get(rid);
        if (roomMate == null) {
            roomMate = new HashMap();
            siteRoom.put(rid, roomMate);
        }

        mateSess = roomMate.get(mid);
        if (mateSess == null) {
            mateSess = new HashSet();
            roomMate.put(mid, mateSess);
        }

        // 登记会话
        mateSess.add(sess);
    }

    synchronized private static void delSession(Session sess) {
        String sid = (String) sess.getPathParameters().get("site_id");
        String rid = (String) sess.getPathParameters().get("room_id");
        String mid = (String) sess.getPathParameters().get("mine_id");

        Map<String, Map<String, Set<Session>>> siteRoom;
        Map<String, Set<Session>> roomMate;
        Set<Session> mateSess;

        siteRoom = SESSIONS.get(sid);
        if (siteRoom == null) {
            return;
        }

        roomMate = siteRoom.get(rid);
        if (roomMate == null) {
            return;
        }

        mateSess = roomMate.get(mid);
        if (mateSess == null) {
            return;
        }

        // 移除会话
        mateSess.remove(sess);

        if (mateSess.isEmpty()) {
            roomMate.remove(mid);
        }

        if (roomMate.isEmpty()) {
            siteRoom.remove(rid);
        }

        if (siteRoom.isEmpty()) {
            SESSIONS.remove(sid);
        }
    }

}