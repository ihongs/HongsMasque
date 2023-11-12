package io.github.ihongs.util.verify;

import io.github.ihongs.CruxException;
import io.github.ihongs.db.DB;
import io.github.ihongs.dh.Roster;
import io.github.ihongs.util.Digest;
import io.github.ihongs.util.Synt;
import java.util.Map;

/**
 * 验证是否许可访问
 * @author hong
 */
public class MasqueChatAuth extends Rule {

    @Override
    public Object verify(Value watch) throws Wrong {
        Object val = watch.get();
        Map values = watch.getValues();
        Map cleans = watch.getCleans();
        String sid = (String) values.get("site_id");
        String mid = (String) values.get("mine_id");
        String rid = (String) values.get("room_id");
        String tok = (String) val ;
        String old ;

        Map ro;
        try {
            ro = DB .getInstance( "masque" )
            .with ("site")
            .field( "sk" )
            .where( "id=? AND state=1", sid)
            .getOne();
        } catch ( CruxException e) {
            throw e.toExemption( );
        }
        if (ro.isEmpty()) {
            throw new Wrong("@masque:core.masque.wrong.sites");
        }

        // 原始 token
        old = Digest.md5(sid+"/"+mid+"/"+rid+"/"+ro.get("sk"));
        if (old.equals(tok)) {
            cleans.put("token_level", 1);
            return val;
        }

        // 临时 token
        tok = Synt.asString(Roster.get("masque.token." + tok));
        if (old.equals(tok)) {
            cleans.put("token_level", 2);
            return val;
        }

        /**/throw new Wrong("@masque:core.masque.wrong.token");
    }

}
