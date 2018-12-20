package io.github.ihongs.util.verify;

import io.github.ihongs.HongsException;
import io.github.ihongs.db.DB;
import io.github.ihongs.normal.serv.Record;
import io.github.ihongs.util.Digest;
import io.github.ihongs.util.Synt;
import java.util.Map;

/**
 * 验证是否许可访问
 * @author hong
 */
public class MasqueChatAuth extends Rule {

    @Override
    public Object verify(Object value) throws Wrong, Wrongs, HongsException {
        String sid   = (String) values.get("site_id");
        String mid   = (String) values.get("mine_id");
        String rid   = (String) values.get("room_id");
        String token = (String) value ;
        String secur ;

        Map ro = DB.getInstance ( "masque" )
            .with ("site")
            .field( "sk" )
            .where( "id=? AND state=1", sid)
            .getOne();
        if (ro.isEmpty()) {
            throw new Wrong("core.wrong.sites").setLocalizedContext("masque");
        }

        // 原始 token
        secur = Digest.md5(sid+"/"+mid+"/"+rid+"/"+ro.get("sk"));
        if (secur.equals(token)) {
            cleans.put("token_level", 1);
            return value;
        }

        // 临时 token
        token = Synt.asString(Record.get("masque.token."+token));
        if (secur.equals(token)) {
            cleans.put("token_level", 2);
            return value;
        }

        /**/throw new Wrong("core.wrong.token").setLocalizedContext("masque");
    }

}
