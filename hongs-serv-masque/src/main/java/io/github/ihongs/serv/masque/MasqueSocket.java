package io.github.ihongs.serv.masque;

import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.SocketHelper;
import io.github.ihongs.action.VerifyHelper;
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
            /**
             * 校验身份
             */
            try {
                new VerifyHelper ( )
                    .addRulesByForm("masque","auth")
                    .verify ( hepr.getRequestData( ), false, true);
            } catch (CruxException ex ) {
                CoreLogger.error ( ex );
                hepr.fault(ex.getLocalizedMessage());
                hepr.flush();
                sess.close();
                return;
            }

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
            SocketHelper hepr = SocketHelper.getInstance(sess, "error");
        ) {
            hepr.fault( "Unsupported!" );
            CoreLogger.debug (msg);
            delSession(sess);
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
            String msg = ar.getMessage();
            CoreLogger.debug (msg);
            delSession(sess);
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