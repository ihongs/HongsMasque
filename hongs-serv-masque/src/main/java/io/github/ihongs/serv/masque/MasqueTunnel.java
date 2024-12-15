package io.github.ihongs.serv.masque;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxException;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.daemon.Async;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.websocket.Session;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

/**
 * 管道集合
 * <pre>
 * masque.properties 配置项:
 * core.masque.ceiver.max.tasks 接收管道容量
 * core.masque.ceiver.max.servs 接收管道线程数
 * core.masque.sender.max.tasks 发送管道容量
 * core.masque.sender.max.servs 发送管道线程数
 * </pre>
 * @author hong
 */
public final class MasqueTunnel {

    /**
     * 获取接收管道对象
     * @return
     */
    public static Consumer<Map> getCeiver() {
        return Core.getInterior().got(Ceiver.class.getName(), () -> {
            CoreConfig cc = CoreConfig.getInstance("masque");
            return new Ceiver ( Ceiver.class.getName(),
                cc.getProperty("core.masque.ceiver.max.tasks", Integer.MAX_VALUE),
                cc.getProperty("core.masque.ceiver.max.servs", 1) );
        });
    }

    /**
     * 获取发送管道对象
     * @return
     */
    public static Consumer<Msg> getSender() {
        return Core.getInterior().got(Sender.class.getName(), () -> {
            CoreConfig cc = CoreConfig.getInstance("masque");
            return new Sender ( Ceiver.class.getName(),
                cc.getProperty("core.masque.ceiver.max.tasks", Integer.MAX_VALUE),
                cc.getProperty("core.masque.ceiver.max.servs", 1) );
        });
    }

    /**
     * 接收管道
     */
    private static class Ceiver
    extends Async<Map>
    implements Consumer<Map>, Core.Singleton {

        private Ceiver(String name, int maxTasks, int maxServs) {
            super(name, maxTasks, maxServs);
        }

        @Override
        public void accept(Map info) {
            this.add(info);
        }

        @Override
        public void run(Map info) {
            try {
                MasqueTunnel.send(info);
            }
            catch (CruxException e) {
                CoreLogger.error(e);
            }
            finally {
                Core.getInstance( ).reset( );
            }
        }

    }

    /**
     * 发送管道
     */
    private static class Sender
    extends Async<Msg>
    implements Consumer<Msg>, Core.Singleton {

        private Sender(String name, int maxTasks, int maxServs) {
            super(name, maxTasks, maxServs);
        }

        @Override
        public void accept(Msg info) {
            this.add(info);
        }

        @Override
        public void run(Msg info) {
            try {
                MasqueTunnel.send(info);
            }
            catch (CruxException e) {
                CoreLogger.error(e);
            }
            finally {
                Core.getInstance( ).reset( );
            }
        }

    }

    /**
     * 发送消息结构体
     */
    private static class Msg {

        final  String  msg ;
        final  String  url ;
        final  Session ses ;

        public Msg(String msg, String  url) {
            if ("".equals(url)) {
                 url = null;
            }
            this.msg = msg ;
            this.url = url ;
            this.ses = null;
        }

        public Msg(String msg, Session ses) {
            this.msg = msg ;
            this.ses = ses ;
            this.url = null;
        }

    }

    private static class Mst
    implements AutoCloseable {

        private final DB db;
        private final Table ctb;
        private final Table stb;
        private final long  now;
        private final PreparedStatement unreadUpdate;
        private final PreparedStatement unreadInsert;
        private final PreparedStatement unsendUpdate;
        private final PreparedStatement unsendInsert;
        private String sid = null;
        private String rid = null;

        public Mst() throws CruxException {
            now = System.currentTimeMillis();
            db  = DB.getInstance( "masque" );
                  db.open();
            ctb = db.getTable("chat");
            stb = db.getTable("clue");
            unreadInsert = db.prepare("INSERT INTO `"+stb.tableName+"` (`unread`,`mtime`,`site_id`,`room_id`,`mate_id`,`id`) VALUES (1, ?,?, ?,?, ?)");
            unreadUpdate = db.prepare("UPDATE `"+stb.tableName+"` SET `unread`=`fresh`+1,`mtime`=? WHERE `site_id`=? AND `room_id`=? AND `mate_id`=?");
            unsendInsert = db.prepare("INSERT INTO `"+stb.tableName+"` (`unsend`,`mtime`,`site_id`,`room_id`,`mate_id`,`id`) VALUES (1, ?,?, ?,?, ?)");
            unsendUpdate = db.prepare("UPDATE `"+stb.tableName+"` SET `unsend`=`fresh`+1,`mtime`=? WHERE `site_id`=? AND `room_id`=? AND `mate_id`=?");
        }

        public void insert( Map info ) throws CruxException {
            ctb.insert(info);

            sid = (String) info.get("site_id");
            rid = (String) info.get("room_id");
        }

        public void unread(String mid) throws CruxException {
            try {
                    unreadUpdate.setObject(1, now);
                    unreadUpdate.setObject(2, sid);
                    unreadUpdate.setObject(3, rid);
                    unreadUpdate.setObject(4, mid);
                if (unreadUpdate.executeUpdate(  ) == 0) {
                    unreadInsert.setObject(1, now);
                    unreadInsert.setObject(2, sid);
                    unreadInsert.setObject(3, rid);
                    unreadInsert.setObject(4, mid);
                    unreadInsert.setObject(5, Core.newIdentity());
                    unreadInsert.executeUpdate(  );
                }
            }
            catch (SQLException ex) {
                throw new CruxException(ex);
            }
        }

        public void unsend(String mid) throws CruxException {
            try {
                    unsendUpdate.setObject(1, now);
                    unsendUpdate.setObject(2, sid);
                    unsendUpdate.setObject(3, rid);
                    unsendUpdate.setObject(4, mid);
                if (unsendUpdate.executeUpdate(  ) == 0) {
                    unsendInsert.setObject(1, now);
                    unsendInsert.setObject(2, sid);
                    unsendInsert.setObject(3, rid);
                    unsendInsert.setObject(4, mid);
                    unsendInsert.setObject(5, Core.newIdentity());
                    unsendInsert.executeUpdate(  );
                }
            }
            catch (SQLException ex) {
                throw new CruxException(ex);
            }
        }

        public String getNoteUrl(String siteId)
        throws CruxException {
            Loop rs = db.with  ("site"       )
                        .select("note_url"   )
                        .filter("id = ?" , siteId)
                        .limit (1)
                        .select( );
            for(Map ro : rs) {
                return (String) ro.get("note_url");
            }
            return null;
        }

        public Set<String> getMateIds(String siteId, String roomId)
        throws CruxException {
            Set  ms = new HashSet( );
            Loop rs = db.with  ("clue"       )
                        .select("mate_id"    )
                        .filter("site_id = ? AND room_id = ?", siteId, roomId)
                        .select( );
            for(Map ro : rs) {
                ms.add ( ro.get("mate_id") );
            }
            return  ms ;
        }

        @Override
        public void close() {
            db.close();
        }

    }

    private static void send(Map info) throws CruxException {
        Core core = Core.getInstance();
        try {

        String siteId = (String) info.get("site_id");
        String roomId = (String) info.get("room_id");
        String mateId = (String) info.get("mate_id");

        Consumer<Msg> sndr = getSender();
        Map<String, Set<Session> > mess ;
        Set<String>   mids ;
        String msg;

        msg = "{\"ok\":true,\"ern\":\"\",\"err\":\"\",\"msg\":\"\""
            + ",\"info\":"
            + Dist.toString(info)
            + "}" ;

        // 保存消息
        Mst  chat = new Mst(/**/);
        chat.insert( info );

        // 组内成员
        mids = chat.getMateIds (siteId, roomId);
        mids.remove(mateId);

        // 发送消息
        mess = MasqueSocket.getSessions(siteId, roomId);
        if (mess != null) {
            for(Map.Entry<String, Set<Session>> et : mess.entrySet()) {
                Set<Session> ss = et.getValue();
                for(Session  se : ss) {
                    sndr.accept( new MasqueTunnel.Msg(msg, se) );
                }
                mids.remove( et.getKey( ) );
            }
        }

        // 发送通知
        mess = MasqueSocket.getSessions(siteId, Masque.ROOM_ALL);
        Iterator <String> it;
        it = mids.iterator();
        while (it.hasNext()) {
            String mid = it.next();
            Set<Session> ss = mess.get(mid);
            if (ss != null && !ss.isEmpty()) {
                for(Session se : ss) {
                    sndr.accept( new MasqueTunnel.Msg(msg, se) );
                }
                it.remove();
                // 未读计数
                try {
                    chat.unread(mid);
                }
                catch (CruxException ex) {
                    CoreLogger.error(ex);
                }
            } else {
                // 未发计数
                try {
                    chat.unsend(mid);
                }
                catch (CruxException ex) {
                    CoreLogger.error(ex);
                }
            }
        }

        if (mids.isEmpty()) {
            return;
        }

        msg = "{\"ok\":true,\"ern\":\"\",\"err\":\"\",\"msg\":\"\""
            + ",\"info\":"
            + Dist.toString(info)
            + ",\"mids\":"
            + Dist.toString(mids)
            + "}" ;

        // 离线通知
        mess = MasqueSocket.getSessions(siteId, Masque.ROOM_OFF);
        if (mess != null) {
            for(Map.Entry<String, Set<Session>> et : mess.entrySet()) {
                Set<Session> ss = et.getValue();
                for(Session  se : ss) {
                    sndr.accept( new MasqueTunnel.Msg(msg, se) );
                }
            }
        } else {
            // HTTP 推送
            String ur = chat.getNoteUrl(siteId);
            if (null != ur ) {
                sndr.accept( new MasqueTunnel.Msg(msg, ur) );
            }
        }

        } finally {
            core.close();
        }
    }

    private static void send(Msg info) throws CruxException {
        if (info.ses != null) {
            info.ses
                .getAsyncRemote ( )
                .sendText(info.msg);
        } else
        if (info.url != null) {
            if (info.url.startsWith("class://")) {
                try {
                    String cls = info.url.substring(8);
                    Object obj = Core.getInstance(cls);
                    ( (Consumer) obj).accept(info.msg);
                }
                catch (ClassCastException ex) {
                    throw new CruxException(ex);
                }
            }  else {
                try (
                    CloseableHttpClient cli = HttpClientBuilder.create().build()
                ) {
                    URI         uri = new URI(info.url);
                    HttpEntity  ent = new StringEntity(info.msg, ContentType.APPLICATION_JSON);
                    ClassicHttpRequest req = ClassicRequestBuilder.post(uri).setEntity(ent).build( );
                    cli.execute(req , (rsp) -> {
                        // 调试输出
                        if (4 == (4 & Core.DEBUG)) {
                            int    code = rsp.getCode();
                            String text = Syno.indent(EntityUtils.toString(rsp.getEntity(), "UTF-8").trim());
                            CoreLogger.debug("Masque remote notify, URL: {}, MSG: {}, RSP({}): {}", info.url, info.msg, code, text);
                        }
                        return null;
                    });
                }
                catch (URISyntaxException | IOException ex) {
                    throw new CruxException(ex);
                }
            }
        }
    }

}